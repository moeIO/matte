package com.moeio.matte.backend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;

import org.pircbotx.UtilSSLSocketFactory;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.InboxStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import com.moeio.matte.ChannelActivity;
import com.moeio.matte.R;
import com.moeio.matte.irc.Channel;
import com.moeio.matte.irc.ChannelMessage;
import com.moeio.matte.irc.Client;
import com.moeio.matte.irc.ClientManager;
import com.moeio.matte.irc.GenericMessage;
import com.moeio.matte.irc.MessageParser;
import com.moeio.matte.irc.Query;
import com.moeio.matte.irc.Server;
import com.moeio.matte.irc.ServerPreferences;

public class IRCService extends Service implements ServiceEventListener {
	// Whether the activity is bound or not
	private boolean appActive;

	public class IRCBinder extends Binder {
		public IRCService getService() {
			return IRCService.this;
		}
	}

	private final IBinder binder = new IRCBinder();
	public ServiceEventListener connectedEventListener;
	private IRCListener listener;
	private ClientManager clientManager;
	private HashMap<String, Server> serverMap;
	private ArrayList<Server> servers;

	private Notification.Builder notificationBuilder;
	private static final int NOTIFICATION_ID = 1337;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		this.servers = new ArrayList<Server>();
		this.serverMap = new HashMap<String, Server>();
		this.clientManager = new ClientManager(this.getApplicationContext());
		this.listener = new IRCListener(this);

		// Load preferences from configuration.
		ArrayList<ServerPreferences> preferences = this.loadPreferences();

		for (ServerPreferences serverPrefs : preferences) {
			this.addServer(serverPrefs);
			if (serverPrefs.isAutoConnected()) {
				this.connect(serverPrefs.getName());
			}
		}

		this.startForeground(NOTIFICATION_ID, this.updateNotification(
				R.drawable.ic_launcher,
				getResources().getString(R.string.running)));

	}

	public void connect(String name) {
		ConnectTask connectionTask = new ConnectTask();
		connectionTask.execute(new String[] { name });
	}

	public void disconnect(String serverName) {
		Server s = this.getServer(serverName);

		if (s != null && s.getServerInfo() != null) {
			if (s.getClient().isConnected()) {
				s.getClient().disconnect();
			}
			for (Channel c : s.getChannels()) {
				s.removeChannel(c.getName());
			}
			this.serverDisconnected(s,
					getResources().getString(R.string.requested));
		}
	}

	public void addServer(ServerPreferences prefs) {
		Server server = new Server();
		Client client = this.clientManager.createClient(prefs);

		client.getListenerManager().addListener(this.listener);
		client.setServerPreferences(prefs);
		server.setName(prefs.getName());
		server.setServerInfo(client.getServerInfo());
		server.setClient(client);

		this.servers.add(server);
		this.serverMap.put(server.getName(), server);
	}

	public void renameServer(String from, String to) {
		Server server = this.serverMap.remove(from);
		if (server != null)
			server.setName(to);
		this.serverMap.put(to, server);
	}

	public void deleteServer(String name) {
		Server server = this.serverMap.get(name);
		if (server.getClient().isConnected()) {
			this.disconnect(name);
		}
		this.serverMap.remove(name);
		this.servers.remove(this.servers.indexOf(server));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Compatibility
	@Override
	public void onStart(Intent i, int dunno) {
		this.serviceStart();
	}

	// Service started.
	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
		this.serviceStart();
		return Service.START_STICKY;
	}

	// Called if startService is called and the Service is already running. No
	// connect code here
	private void serviceStart() {

	}

	public void stopService() {
		// IRC Cleanup
		for (Server s : servers) {
			s.getClient().getListenerManager().removeListener(listener);
			this.disconnect(s.getName());
		}
		this.stopForeground(true);
		this.stopSelf();
	}

	// Load preferences.
	public ArrayList<ServerPreferences> loadPreferences() {
		ArrayList<ServerPreferences> preferences = new ArrayList<ServerPreferences>();
		SharedPreferences rawPreferences = this.getSharedPreferences("servers",
				Context.MODE_PRIVATE);

		// decent arrays r 4 scrubs, id rather smoke weed
		int serverCount = rawPreferences.getInt("server_count", 0);
		for (int i = 0; i < serverCount; i++) {
			ServerPreferences preference = new ServerPreferences();
			preference.loadFromSharedPreferences(rawPreferences, i);
			preferences.add(preference);
		}

		return preferences;
	}

	private class ConnectTask extends AsyncTask<String, Void, Boolean> {
		private Server server;
		private Client client;
		private ServerPreferences preferences;

		protected Boolean doInBackground(String... arguments) {
			if (arguments.length < 1) {
				return false;
			}
			this.server = serverMap.get(arguments[0]);
			this.client = this.server.getClient();
			this.preferences = this.client.getServerPreferences();

			// Attempt to connect to the server.
			boolean connected = false;
			ServerPreferences.Host host = this.preferences.getHost();

			try {
				// Emit 'channel joined' event for the server tab.
				if (connectedEventListener != null) {
					channelJoined(server, null);
				}

				if (host.isSSL()) {
					if (host.verifySSL()) {
						this.client.connect(host.getHostname(), host.getPort(),
								host.getPassword(),
								SSLSocketFactory.getDefault());
					} else {
						this.client.connect(host.getHostname(), host.getPort(),
								host.getPassword(), new UtilSSLSocketFactory()
										.trustAllCertificates());
					}
				} else {
					this.client.connect(host.getHostname(), host.getPort(),
							host.getPassword());
				}

				connected = true;
			} catch (Exception ex) {
				statusMessageReceived(server, Server.createError(Html
						.fromHtml(getResources().getString(
								R.string.couldnotconnectserver)
								+ " <strong>" + ex.getMessage() + "</strong>")));
				// Leave failed server in list for error-logging purposes (and
				// since ChannelList adapter will still want it)
				return false;
			}

			return connected;
		}

		protected void onPostExecute(Boolean succesful) {
			if (succesful) {
				// Automatically join channels after connecting (Afterwards so
				// that server list is ready)
				for (String channel : this.preferences.getAutoChannels()) {
					this.client.joinChannel(channel);
				}
			}
		}
	}

	// Requires server name because lolsubclassing
	@SuppressWarnings({ "deprecation" })
	@SuppressLint("NewApi")
	public void showMentionNotification(ChannelMessage message,
			Channel channel, String serverName) {
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		notificationIntent.putExtra("server", serverName);
		notificationIntent.putExtra("channel", channel.getChannelInfo()
				.getName());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		this.notificationBuilder = new Notification.Builder(
				getApplicationContext());
		this.notificationBuilder.setSmallIcon(R.drawable.notification);
		this.notificationBuilder.setAutoCancel(true);
		this.notificationBuilder.setContentTitle(getResources().getString(
				R.string.app_name));
		this.notificationBuilder.setContentText(message.getContent());
		this.notificationBuilder.setWhen(System.currentTimeMillis());
		this.notificationBuilder.setContentIntent(contentIntent);
		this.notificationBuilder.setLights(Color.argb(200, 255, 150, 50), 300,
				6000);
		this.notificationBuilder.setDefaults(Notification.DEFAULT_SOUND
				| Notification.DEFAULT_VIBRATE);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= 16) {
			this.notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
			this.notificationBuilder
					.setContentText(getResources()
							.getString(R.string.mentionby)
							+ " "
							+ message.getNickname()
							+ ": "
							+ message.getContent());
			Notification n = new Notification.BigTextStyle(
					this.notificationBuilder).bigText(message.getContent())
					.build();
			mNotificationManager.notify((int) Math.random(), n);
		} else {
			mNotificationManager.notify((int) Math.random(),
					this.notificationBuilder.getNotification());
		}
	}

	public void updateNotificationWithNoNew() {
		try {
			this.updateNotification(currentNotificationIcon,
					currentNotificationString);
		} catch (Exception ex) {
			Log.e("UGUU~!", "Failed to update main notification");
		}
	}

	private int currentNotificationIcon;
	private String currentNotificationString;

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public Notification updateNotification(int icon, String message) {
		this.currentNotificationIcon = icon;
		this.currentNotificationString = message;
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		this.notificationBuilder = new Notification.Builder(
				getApplicationContext());
		this.notificationBuilder.setSmallIcon(icon);
		this.notificationBuilder.setAutoCancel(false);
		this.notificationBuilder.setContentTitle(getResources().getString(
				R.string.app_name));
		this.notificationBuilder.setContentText(message);
		this.notificationBuilder.setWhen(0);
		this.notificationBuilder.setContentIntent(contentIntent);

		Notification n = null;
		if (Build.VERSION.SDK_INT >= 16) {
			this.notificationBuilder.setPriority(Notification.PRIORITY_LOW);
			int chancount = 0;
			for (Server s : this.servers) {
				chancount += s.getChannels().size();
			}
			this.notificationBuilder.setContentText(message + " (" + chancount
					+ " " + getResources().getString(R.string.channels) + ")");
			InboxStyle i = new Notification.InboxStyle(this.notificationBuilder);
			for (Server s : this.servers) {
				i.addLine("-" + s.getName());
				for (Channel c : s.getChannels()) {
					String chanString;
					if (c.getUnreadMessageCount() > 0)
						chanString = " " + c.getName() + " ("
								+ c.getUnreadMessageCount() + ")";
					else
						chanString = " " + c.getName();
					i.addLine(chanString);
				}
			}
			i.setSummaryText(chancount + " "
					+ getResources().getString(R.string.channels));
			n = i.build();

		} else
			n = this.notificationBuilder.getNotification();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, n);
		return n;
	}

	public Query createQuery(Client client, String peer) {
		Server server = this.getServer(client.getServerPreferences().getName());

		Query query = new Query();
		query.setServer(server);
		query.setChannelInfo(client.getChannel(peer));
		server.addChannel(query);

		this.channelJoined(query, client.getNick());
		return query;
	}

	public void channelMessageReceived(Channel channel, GenericMessage message,
			boolean active) {
		MessageParser.parseSpecial(message);
		if (this.connectedEventListener == null) {
			channel.addMessage(message);
		} else
			this.connectedEventListener.channelMessageReceived(channel,
					message, active);
		this.updateNotificationWithNoNew();
	}

	public void statusMessageReceived(Channel channel, GenericMessage message) {
		message.isChannelNotificationType(true);
		if (this.connectedEventListener == null) {
			channel.addMessage(message);
		} else
			this.connectedEventListener.statusMessageReceived(channel, message);
	}

	public void queryMessageReceived(Query query, GenericMessage message,
			boolean active) {
		MessageParser.parseSpecial(message);

		if (this.connectedEventListener == null) {
			query.addMessage(message);
		} else
			this.connectedEventListener.queryMessageReceived(query, message,
					active);
	}

	public Server getServer(String name) {
		return this.serverMap.get(name);
	}

	public ArrayList<Server> getServers() {
		return this.servers;
	}

	// Returns the position the channel was in the server
	public int removeChannel(Channel channel) {
		int pos = -1;
		for (int i = 0; i < channel.getServer().getChannels().size(); i++) {
			Channel c = channel.getServer().getChannels().get(i);
			if (c.getName().equals(channel.getName())) {
				pos = i;
				break;
			}
		}
		// next channel to show
		if (pos > 0)
			pos--;
		else if (channel.getServer().getChannels().size() > pos + 1)
			pos++;
		this.getServer(channel.getServer().getName()).removeChannel(
				channel.getName());
		return pos - 1;
	}

	public void partChannel(Channel channel) {
		channel.getServer().getClient().partChannel(channel.getChannelInfo());
		this.channelParted(channel, channel.getServer().getClient().getNick());
	}

	public void channelJoined(Channel channel, String nickname) {
		this.connectedEventListener.channelJoined(channel, nickname);
		this.updateNotificationWithNoNew();
	}

	public void channelParted(Channel channel, String nickname) {
		this.connectedEventListener.channelParted(channel, nickname);
		this.updateNotificationWithNoNew();
	}

	public void networkQuit(Collection<Channel> commonChannels, String nickname) {
		this.connectedEventListener.networkQuit(commonChannels, nickname);
	}

	public void serverConnected(Server server) {
		// Don't update for startup of non-autoconnect servers
		// if (constantNotification != null)
		// XXX this is no longer done?
		this.updateNotification(R.drawable.ic_launcher, getResources()
				.getString(R.string.connected));
		this.connectedEventListener.serverConnected(server);
	}

	public void serverDisconnected(Server server, String error) {
		this.connectedEventListener.serverDisconnected(server, error);
		// If all servers are disconnected (connection loss) change icon
		boolean hasConnectedServer = false;
		for (Server s : this.getServers()) {
			if (s.getClient().isConnected()) {
				hasConnectedServer = true;
				break;
			}
		}
		if (!hasConnectedServer)
			this.updateNotification(R.drawable.ic_launcher_red, getResources()
					.getString(R.string.disconnected));
	}

	public void nickChanged(Collection<Channel> commonChannels, String from,
			String to) {
		this.connectedEventListener.nickChanged(commonChannels, from, to);
	}

	public void isAppActive(boolean active) {
		this.appActive = active;
	}

	public boolean isAppActive() {
		return this.appActive;
	}

	public boolean serverNameExists(String name) {
		return this.serverMap.containsKey(name);
	}

}
