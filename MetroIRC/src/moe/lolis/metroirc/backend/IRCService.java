package moe.lolis.metroirc.backend;

import java.util.ArrayList;

import javax.net.ssl.SSLSocketFactory;

import moe.lolis.metroirc.ChannelActivity;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.ClientManager;
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
	public class IRCBinder extends Binder {
		public IRCService getService() {
			return IRCService.this;
		}
	}

	private final IBinder binder = new IRCBinder();
	public ServiceEventListener connectedEventListener;
	private IRCListener listener;
	private ConnectTask connectionTask;
	private ClientManager clientManager;
	private ArrayList<Server> servers;
	
	private Notification constantNotification;
	private static final int CONSTANT_ID = 1;

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
		this.clientManager = new ClientManager();
		this.listener = new IRCListener(this);
		this.connectionTask = new ConnectTask();
		
		// Load preferences from configuration.
		ArrayList<ServerPreferences> preferences = this.loadPreferences();
		// Guy-kun a shit - temporary test preferences until we've made an actual UI.
		ServerPreferences prefs = new ServerPreferences();
		prefs.setName("Rizon");
		prefs.addNickname("Metro-sama");
		prefs.addNickname("Metro-chan");
		prefs.setUsername("metroirc");
		prefs.setRealname("MetroIRC");
		prefs.setHost("irc.lolipower.org", 6697, true, null);
		prefs.addAutoChannel("#metroirc");
		prefs.addAutoChannel("#metroirc2");
		prefs.isAutoConnected(true);
		preferences.add(prefs);
		
		for (ServerPreferences serverPrefs : preferences) {
			if (serverPrefs.isAutoConnected()) {
				this.connectionTask.execute(new ServerPreferences[] { serverPrefs });
			}
		}
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		this.hideConnectedNotification();
		super.onDestroy();
	}

	// Service started.
	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
		return Service.START_STICKY;
	}
	
	// Load preferences.
	public ArrayList<ServerPreferences> loadPreferences() {
		ArrayList<ServerPreferences> preferences = new ArrayList<ServerPreferences>();
		SharedPreferences rawPreferences = this.getSharedPreferences("servers", Context.MODE_PRIVATE);
		
		// decent arrays r 4 scrubs, id rather smoke weed
		int serverCount = rawPreferences.getInt("server_count", 0);
		for (int i = 0; i < serverCount; i++) {
			ServerPreferences preference = new ServerPreferences();
			String prefix = "server_" + i + "_";
			
			preference.setName(rawPreferences.getString(prefix + "name", ""));
			int nickCount = rawPreferences.getInt(prefix + "nick_count", 0);
			for (int j = 0; j < nickCount; j++) {
				preference.addNickname(rawPreferences.getString(prefix + "nick_" + j, "JohnDoe"));
			}
			preference.setUsername(rawPreferences.getString(prefix + "user", "johndoe"));
			preference.setRealname(rawPreferences.getString(prefix + "realname", "John Doe"));
			
			ServerPreferences.Host host = preference.new Host();
			host.setHostname(rawPreferences.getString(prefix + "host_hostname" , null));
			host.setPort(rawPreferences.getInt(prefix + "host_port", 6667));
			host.isSSL(rawPreferences.getBoolean(prefix + "host_ssl", false));
			host.verifySSL(rawPreferences.getBoolean(prefix + "host_verify_ssl", false));
			host.setPassword(rawPreferences.getString(prefix + "host_password", null));
			preference.setHost(host);
						
			// double fake arraying {MLG}[N0OBj3CT$]
			int autoChannelCount = rawPreferences.getInt(prefix + "auto_channel_count", 0);
			for (int j = 0; j < autoChannelCount; j++) {
				preference.addAutoChannel(rawPreferences.getString(prefix + "auto_channel_" + j, ""));
			}
			int autoCommandCount = rawPreferences.getInt(prefix + "auto_command_count", 0);
			for (int j = 0; j < autoCommandCount; j++) {
				preference.addAutoCommand(rawPreferences.getString(prefix + "auto_command_" + j, ""));
			}
			preference.isAutoConnected(rawPreferences.getBoolean(prefix + "autoconnect", false));
			preference.isLogged(rawPreferences.getBoolean(prefix + "log", false));
			
			preferences.add(preference);
		}
		
		return preferences;
	}

	// Asynchronous connect (Can't have UI networking)
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
			try {
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
				Log.e("whoops", ex.getMessage());
				return false;
			}
			
			return connected;
		}

		protected void onPostExecute(Boolean succesful) {
			if (succesful) {
				IRCService.this.showConnectedNotification();
				Server server = new Server();
				server.setServerInfo(this.client.getServerInfo());
				IRCService.this.servers.add(server);

				// Automatically join channels after connecting (Afterwards so
				// that server list is ready)
				for (String channel : this.preferences.getAutoChannels()) {
					this.client.joinChannel(channel);
				}
			}
		}
	}
	
	private void showConnectedNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) this.getSystemService(ns);

		//XXX Using the deprecated API to support <3.0 (Need to switch to new API + compat package)
		int icon = moe.lolis.metroirc.R.drawable.ic_launcher;
		this.constantNotification = new Notification(icon, "MetroIRC connected",0);

		Context context = getApplicationContext();
		CharSequence contentTitle = "MetroIRC";
		CharSequence contentText = "Connected";
		Intent notificationIntent = new Intent(this, ChannelActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		this.constantNotification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		this.constantNotification.flags = Notification.FLAG_ONGOING_EVENT;
		notificationManager.notify(CONSTANT_ID, this.constantNotification);
	}
	
	private void hideConnectedNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) this.getSystemService(ns);
		notificationManager.cancel(CONSTANT_ID);
	}

	public void messageReceived(Channel channel) {
		this.connectedEventListener.messageReceived(channel);
	}

	public Server getServer(String name) {
		//XXX Mining, wahoo!
		for (Server s : servers) {
			if (s.getServerInfo().getNetwork().equals(name)) {
				return s;
			}
		}
		return null;
	}

	public ArrayList<Server> getServers() {
		return this.servers;
	}

	public void channelJoined(Channel channel) {
		this.connectedEventListener.channelJoined(channel);
	}

}
