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
import android.text.Html;
import android.text.SpannedString;
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

<<<<<<< HEAD
	public void connect(String name) {
=======
	public void connect(ServerPreferences serverPrefs) {
		// if disconnected server of the same alias exists, remove it
		for (int i = 0; i < this.getServers().size(); i++) {
			Server s = this.servers.get(i);
			if (s.getName().equals(serverPrefs.getName())) {
				this.servers.remove(s);
				this.serverMap.remove(s.getName());
			}
		}
>>>>>>> b628d090aa2c17a3f4111eedfefbac534cfa608e
		ConnectTask connectionTask = new ConnectTask();
		connectionTask.execute(new String[] { name });
	}

	public void disconnect(String serverName) {
		Server s = this.getServer(serverName);
<<<<<<< HEAD
		if (s != null) {
			if (s.getServerInfo().getBot().isConnected()) {
=======
		if (s != null && s.getServerInfo() != null) {
			if (s.getServerInfo().getBot().isConnected())
>>>>>>> b628d090aa2c17a3f4111eedfefbac534cfa608e
				s.getServerInfo().getBot().disconnect();
			}
			this.serverMap.remove(serverName);
			this.servers.remove(s);
			this.addDisconnectedServer(s.getServer().getServer().getServer().getServer().getServer().getServer().getServer().getServer().getServer()
					.getServer().getServer().getServer().getServer().getClient().getServerPreferences());
			this.serverDisconnected(s, "Requested");

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
		server.setName(to);
		this.serverMap.put(to, server);
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
			this.addServer(serverPrefs);
			if (serverPrefs.isAutoConnected()) {
				this.connect(serverPrefs.getName());
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
			s.getClient().getServerPreferences()
					.saveToSharedPreferences(getApplicationContext().getSharedPreferences("servers", Context.MODE_PRIVATE));
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
						this.client.connect(host.getHostname(), host.getPort(), host.getPassword(), SSLSocketFactory.getDefault());
					} else {
						this.client.connect(host.getHostname(), host.getPort(), host.getPassword(), new UtilSSLSocketFactory().trustAllCertificates());
					}
				} else {
					this.client.connect(host.getHostname(), host.getPort(), host.getPassword());
				}

				connected = true;
			} catch (Exception ex) {
				Log.e("IRC Connection failed", ex.getMessage());
				
			    server.addError(Html.fromHtml("Could not connect to server: <strong>" + ex.getMessage() + "</strong>"));
			    statusMessageReceived(server, null);
				
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
		// Don't update for startup of non-autoconnect servers
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
