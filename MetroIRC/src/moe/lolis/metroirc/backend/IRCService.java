package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.HashMap;
import javax.net.ssl.SSLSocketFactory;

import android.app.Service;
import android.content.Intent;
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

		// TODO: get server preferences from config file.
		// For now, use dummy data.
		ArrayList<ServerPreferences> prefs = new ArrayList<ServerPreferences>();
		ServerPreferences p = new ServerPreferences();
		p.setName("Rizon");
		p.addNickname("JohnDoe");
		p.setUsername("johndo");
		p.setRealname("MetroIRC");
		p.addHost(p.new Host("irc.lolipower.org", 6697, true, null));
		p.addAutoChannel("#metroirc");
		p.isAutoConnected(true);
		prefs.add(p);

		this.listener = new IRCListener(this);
		this.connectionTask = new ConnectTask();

		for (ServerPreferences serverPrefs : prefs) {
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
