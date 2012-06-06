package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.HashMap;
import javax.net.ssl.SSLSocketFactory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.pircbotx.UtilSSLSocketFactory;
import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.ClientManager;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerPreferences;

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
	private HashMap<String, Server> servers;

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
		this.servers = new HashMap<String, Server>();
		this.clientManager = new ClientManager();
		this.listener = new IRCListener(this);
		this.connectionTask = new ConnectTask();
		
		// Load preferences from configuration.
		ArrayList<ServerPreferences> preferences = this.loadPreferences();
		for (ServerPreferences serverPrefs : preferences) {
			if (serverPrefs.isAutoConnected()) {
				this.connectionTask.execute(new ServerPreferences[] { serverPrefs });
			}
		}
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Service started
	@Override
	public void onStart(Intent intent, int startId) {

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
			
			// double fake arraying {MLG}[N0OBj3CT$]
			int hostCount = rawPreferences.getInt(prefix + "host_count", 0);
			for (int j = 0; j < hostCount; j++) {
				String hostPrefix = prefix + "_host_" + j + "_";
				
				ServerPreferences.Host host = preference.new Host();
				host.setHostname(rawPreferences.getString(hostPrefix + "hostname" , null));
				host.setPort(rawPreferences.getInt(hostPrefix + "post", 6667));
				host.isSSL(rawPreferences.getBoolean(hostPrefix + "ssl", false));
				host.verifySSL(rawPreferences.getBoolean(hostPrefix + "verify_ssl", false));
				host.setPassword(rawPreferences.getString(hostPrefix + "password", null));
				preference.addHost(host);
			}
			
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
			preferences = arguments[0];

			this.client = clientManager.createClient(preferences);
			this.client.getListenerManager().addListener(listener);
			
			// Attempt to connect to the server.
			boolean connected = false;
			for (ServerPreferences.Host host : preferences.getHosts()) {
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
			}

			return connected;
		}

		protected void onPostExecute(Boolean succesful) {
			if (succesful) {
				Server server = new Server();
				servers.put(this.client.getServerInfo().getNetwork(), server);

				// Automatically join channels after connecting (Afterwards so
				// that server list is ready)
				for (String channel : preferences.getAutoChannels()) {
					this.client.joinChannel(channel);
				}
			}
		}
	}

	public void messageReceived(Channel channel) {
		this.connectedEventListener.messageReceived(channel);
	}

	public Server getServer(String name) {
		return this.servers.get(name);
	}

	public HashMap<String, Server> getServers() {
		return this.servers;
	}

	public void channelJoined(Channel channel) {
		this.connectedEventListener.channelJoined(channel);
	}

}
