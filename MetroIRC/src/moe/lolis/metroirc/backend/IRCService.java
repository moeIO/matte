package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import org.pircbotx.MultiBotManager;
import org.pircbotx.PircBotX;
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
	private MultiBotManager botManager;
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
		servers = new HashMap<String, Server>();
		this.botManager = new MultiBotManager("MetroIRC");

		// TODO: get server preferences from config file.
		// For now, use dummy data.
		ArrayList<ServerPreferences> prefs = new ArrayList<ServerPreferences>();
		ServerPreferences p = new ServerPreferences();
		p.setName("Rizon");
		p.addNickname("MoeBot");
		p.addNickname("MoeBot_");
		p.addNickname("MoeBot__");
		p.setUsername("moebot");
		p.setRealname("MetroIRC");
		p.addHost(p.new Host("irc.lolipower.org", 6697, true, null));
		p.addAutoChannel("#metroirc");
		p.isAutoConnected(true);
		prefs.add(p);

		this.listener = new IRCListener(this);
		this.connectionTask = new ConnectTask();

		for (ServerPreferences serverPrefs : prefs) {
			if (serverPrefs.isAutoConnected()) {
				this.connectionTask
						.execute(new ServerPreferences[] { serverPrefs });
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
	private class ConnectTask extends
			AsyncTask<ServerPreferences, Void, Boolean> {
		private PircBotX bot;
		ServerPreferences preferences;

		protected Boolean doInBackground(ServerPreferences... arguments) {
			if (arguments.length < 1) {
				return false;
			}
			preferences = arguments[0];

			this.bot = botManager.createBot(preferences.getHosts().get(0)
					.getHostname());
			this.bot.setName(preferences.getNicknames().get(0));
			this.bot.setLogin(preferences.getUsername());
			this.bot.setAutoNickChange(true);
			this.bot.setAutoSplitMessage(true);
			this.bot.getListenerManager().addListener(listener);

			// Attempt to connect to the server
			try {
				this.bot.connect(preferences.getHosts().get(0).getHostname());
			} catch (Exception ex) {
				Log.e("whoops", ex.getMessage());
				return false;
			}

			return true;
		}

		protected void onPostExecute(Boolean succesful) {
			if (succesful) {
				Server server = new Server();
				servers.put(this.bot.getServerInfo().getNetwork(), server);

				// Automatically join channels after connecting (Afterwards so
				// that server list is ready)
				for (String channel : preferences.getAutoChannels()) {
					this.bot.joinChannel(channel);
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
