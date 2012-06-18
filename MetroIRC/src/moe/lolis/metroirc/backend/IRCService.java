package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;

import moe.lolis.metroirc.ChannelActivity;
import moe.lolis.metroirc.R;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.ClientManager;
import moe.lolis.metroirc.irc.GenericMessage;
import moe.lolis.metroirc.irc.MessageParser;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerPreferences;

import org.pircbotx.UtilSSLSocketFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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

	private Notification constantNotification;
	private static final int CONSTANT_FOREGROUND_ID = 2;

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
		this.clientManager = new ClientManager();
		this.listener = new IRCListener(this);

	}

	public void connect(ServerPreferences serverPrefs) {
		ConnectTask connectionTask = new ConnectTask();
		connectionTask.execute(new ServerPreferences[] { serverPrefs });
	}

	public void disconnect(String serverName) {
		Server s = this.getServer(serverName);
		if (s != null) {
			if (s.getServerInfo().getBot().isConnected())
				s.getServerInfo().getBot().disconnect();
			this.serverMap.remove(serverName);
			this.servers.remove(s);
		}
	}

	public void addDisconnectedServer(ServerPreferences prefs) {
		Server s = new Server();
		Client c = IRCService.this.clientManager.createClient(prefs);
		c.getListenerManager().addListener(IRCService.this.listener);
		s.setClient(c);
		s.getClient().setServerPreferences(prefs);
		s.setName(prefs.getName());
		this.servers.add(s);
		this.serverMap.put(s.getName(), s);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Service started.
	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
		// Load preferences from configuration.
		ArrayList<ServerPreferences> preferences = this.loadPreferences();

		for (ServerPreferences serverPrefs : preferences) {
			if (serverPrefs.isAutoConnected()) {
				this.connect(serverPrefs);
			} else {
				this.addDisconnectedServer(serverPrefs);
			}
		}
		super.onCreate();

		// Notification for foreground sevice
		NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

		// XXX: Using the deprecated API to support <3.0 (Need to switch to new
		// API + compat package)
		int icon = moe.lolis.metroirc.R.drawable.ic_launcher;
		this.constantNotification = new Notification(icon, "", 0);

		Context context = getApplicationContext();
		CharSequence contentTitle = "MetroIRC";
		CharSequence contentText = "Running";
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		this.constantNotification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		this.constantNotification.flags = Notification.FLAG_ONGOING_EVENT;

		this.startForeground(CONSTANT_FOREGROUND_ID, constantNotification);
		return Service.START_STICKY;
	}

	public void stopService() {
		// IRC Cleanup
		for (Server s : servers) {
			s.getClient().getServerPreferences().saveToSharedPreferences(getApplicationContext().getSharedPreferences("servers", Context.MODE_PRIVATE));
			s.getClient().getListenerManager().removeListener(listener);
			this.disconnect(s.getName());
		}
		// TODO store connected channels
		this.stopForeground(true);
		this.stopSelf();
	}

	// Load preferences.
	public ArrayList<ServerPreferences> loadPreferences() {
		ArrayList<ServerPreferences> preferences = new ArrayList<ServerPreferences>();
		SharedPreferences rawPreferences = this.getSharedPreferences("servers", Context.MODE_PRIVATE);

		// decent arrays r 4 scrubs, id rather smoke weed
		int serverCount = rawPreferences.getInt("server_count", 0);
		for (int i = 0; i < serverCount; i++) {
			ServerPreferences preference = new ServerPreferences();
			preference.loadFromSharedPreferences(rawPreferences, i);
			preferences.add(preference);
		}

		return preferences;
	}

	private class ConnectTask extends AsyncTask<ServerPreferences, Void, Boolean> {
		private Client client;
		ServerPreferences preferences;

		protected Boolean doInBackground(ServerPreferences... arguments) {
			if (arguments.length < 1) {
				return false;
			}
			this.preferences = arguments[0];

			this.client = IRCService.this.clientManager.createClient(this.preferences);
			this.client.getListenerManager().addListener(IRCService.this.listener);

			// Attempt to connect to the server.
			boolean connected = false;
			ServerPreferences.Host host = this.preferences.getHost();
			Server server = new Server();
			try {

				server.setName(this.preferences.getName());
				server.setServerInfo(this.client.getServerInfo());
				server.setClient(this.client);
				IRCService.this.servers.add(server);
				IRCService.this.serverMap.put(this.preferences.getName(), server);
				// If activity is already bound, get it to switch to the new
				// server tab, otherwise it will switch tab once it has bound on
				// it's own
				if (IRCService.this.connectedEventListener != null)
					IRCService.this.channelJoined(server, null);

				if (host.isSSL()) {
					if (host.verifySSL()) {
						this.client.connect(host.getHostname(), host.getPort(), host.getPassword(), SSLSocketFactory.getDefault());
					} else {
						this.client
								.connect(host.getHostname(), host.getPort(), host.getPassword(), new UtilSSLSocketFactory().trustAllCertificates());
					}
				} else {
					this.client.connect(host.getHostname(), host.getPort(), host.getPassword());
				}

				connected = true;
			} catch (Exception ex) {
				Log.e("IRC Connection failed", ex.getMessage());
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
	public void showMentionNotification(ChannelMessage message, Channel channel, String serverName) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.notification;
		CharSequence tickerText = message.getContent();
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "Mention by " + message.getNickname();
		CharSequence contentText = message.getContent();
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		notificationIntent.putExtra("server", serverName);
		notificationIntent.putExtra("channel", channel.getChannelInfo().getName());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		final int NOTIFICATION_ID = 1;
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void updateNotification(int icon, String message) {
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		constantNotification.icon = icon;
		constantNotification.when = System.currentTimeMillis();
		constantNotification.setLatestEventInfo(context, "MetroIRC", message, contentIntent);

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.notify(CONSTANT_FOREGROUND_ID, constantNotification);
	}

	public void activeChannelMessageReceived(Channel channel, GenericMessage message) {
		MessageParser.parseMessage(message);
		if (this.connectedEventListener == null) {
			channel.addMessage(message);
		} else
			this.connectedEventListener.activeChannelMessageReceived(channel, message);
	}

	public void inactiveChannelMessageReceived(Channel channel, GenericMessage message) {
		if (this.connectedEventListener == null) {
			channel.addMessage(message);
		} else
			this.connectedEventListener.inactiveChannelMessageReceived(channel, message);
	}

	public void statusMessageReceived(Channel channel, GenericMessage message) {
		message.isChannelNotificationType(true);
		if (this.connectedEventListener == null) {
			channel.addMessage(message);
		} else
			this.connectedEventListener.statusMessageReceived(channel, message);
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
		this.getServer(channel.getServer().getName()).removeChannel(channel.getName());
		return pos - 1;
	}

	public void partChannel(Channel channel) {
		channel.getServer().getClient().partChannel(channel.getChannelInfo());
		this.channelParted(channel, channel.getServer().getClient().getNick());
	}

	public void channelJoined(Channel channel, String nickname) {
		this.connectedEventListener.channelJoined(channel, nickname);
	}

	public void channelParted(Channel channel, String nickname) {
		this.connectedEventListener.channelParted(channel, nickname);
	}

	public void networkQuit(Collection<Channel> commonChannels, String nickname) {
		this.connectedEventListener.networkQuit(commonChannels, nickname);
	}

	public void serverConnected(Server server) {
		//Don't update for startup of non-autoconnect servers
		if (constantNotification != null)
			this.updateNotification(R.drawable.ic_launcher, "Connected");
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
			this.updateNotification(R.drawable.ic_launcher_red, "Disconnected");
	}

	public void nickChanged(Collection<Channel> commonChannels, String from, String to) {
		this.connectedEventListener.nickChanged(commonChannels, from, to);
	}

	public void isAppActive(boolean active) {
		this.appActive = active;
	}

	public boolean isAppActive() {
		return this.appActive;
	}

}
