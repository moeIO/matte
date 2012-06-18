package moe.lolis.metroirc.irc;

import java.util.ArrayList;

import android.content.SharedPreferences;

public class ServerPreferences {

	// Host abstraction.
	public class Host {
		private String hostname;
		private int port;
		private boolean SSL = false;
		private boolean verifySSL = false;
		private String password;

		public Host() {
		}

		public Host(String hostname, int port, boolean ssl, String password) {
			this.hostname = hostname;
			this.port = port;
			this.SSL = ssl;
			this.password = password;
		}

		// Getters/setters ahoy.
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		public String getHostname() {
			return this.hostname;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getPort() {
			return this.port;
		}

		public void isSSL(boolean ssl) {
			this.SSL = ssl;
		}

		public boolean isSSL() {
			return this.SSL;
		}

		public void verifySSL(boolean verifySSL) {
			this.verifySSL = verifySSL;
		}

		public boolean verifySSL() {
			return this.verifySSL;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPassword() {
			return this.password;
		}
	}

	// User-friendly name.
	private String name;
	// Our spot in the SharedPreferences, if applicable.
	private int preferenceSpot = -1;

	// List of all nicknames to attempt.
	private ArrayList<String> nicknames = new ArrayList<String>();
	private String username;
	private String realname;
	private Host host;

	// List of channels to automatically join.
	private ArrayList<String> autoChannels = new ArrayList<String>();
	// List of commands to automatically execute.
	private ArrayList<String> autoCommands = new ArrayList<String>();

	private boolean autoConnect = false;
	private boolean doLog = false;

	public ServerPreferences() {
		this.nicknames = new ArrayList<String>();
		this.autoChannels = new ArrayList<String>();
		this.autoCommands = new ArrayList<String>();
	}

	// Boilerplate getter/setters.
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void addNickname(String nickname) {
		this.nicknames.add(nickname);
	}

	public ArrayList<String> getNicknames() {
		return this.nicknames;
	}

	public void setNicknames(ArrayList<String> nicknames) {
		this.nicknames = nicknames;
	}

	public void removeNickname(String nickname) {
		this.nicknames.remove(nickname);
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return this.username;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public String getRealname() {
		return this.realname;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public void setHost(String hostname, int port, boolean ssl, String password) {
		Host host = new Host();
		host.setHostname(hostname);
		host.setPort(port);
		host.isSSL(ssl);
		host.setPassword(password);
		this.setHost(host);
	}

	public Host getHost() {
		return this.host;
	}

	public void addAutoChannel(String channel) {
		this.autoChannels.add(channel);
	}

	public ArrayList<String> getAutoChannels() {
		return this.autoChannels;
	}

	public void setAutoChannels(ArrayList<String> autoChannels) {
		this.autoChannels = autoChannels;
	}

	public void removeAutoChannel(String channel) {
		this.autoChannels.remove(channel);
	}

	public void addAutoCommand(String command) {
		this.autoCommands.add(command);
	}

	public ArrayList<String> getAutoCommands() {
		return this.autoCommands;
	}

	public void setAutoCommands(ArrayList<String> autoCommands) {
		this.autoCommands = autoCommands;
	}

	public void removeAutoCommand(String command) {
		this.autoCommands.remove(command);
	}

	public boolean isAutoConnected() {
		return this.autoConnect;
	}

	public void isAutoConnected(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}

	public boolean isLogged() {
		return this.doLog;
	}

	public void isLogged(boolean doLog) {
		this.doLog = doLog;
	}

	// Load from SharedPreferences.
	public void loadFromSharedPreferences(SharedPreferences sharedPreferences, int i) {
		String prefix = "server_" + i + "_";

		this.setName(sharedPreferences.getString(prefix + "name", ""));
		int nickCount = sharedPreferences.getInt(prefix + "nick_count", 0);
		for (int j = 0; j < nickCount; j++) {
			this.addNickname(sharedPreferences.getString(prefix + "nick_" + j, "JohnDoe"));
		}
		this.setUsername(sharedPreferences.getString(prefix + "user", "johndoe"));
		this.setRealname(sharedPreferences.getString(prefix + "realname", "John Doe"));

		Host host = new Host();
		host.setHostname(sharedPreferences.getString(prefix + "host_hostname", null));
		host.setPort(sharedPreferences.getInt(prefix + "host_port", 6667));
		host.isSSL(sharedPreferences.getBoolean(prefix + "host_ssl", false));
		host.verifySSL(sharedPreferences.getBoolean(prefix + "host_verify_ssl", false));
		host.setPassword(sharedPreferences.getString(prefix + "host_password", null));
		this.setHost(host);

		// double fake arraying {MLG}[N0OBj3CT$]
		int autoChannelCount = sharedPreferences.getInt(prefix + "auto_channel_count", 0);
		for (int j = 0; j < autoChannelCount; j++) {
			this.addAutoChannel(sharedPreferences.getString(prefix + "auto_channel_" + j, ""));
		}
		int autoCommandCount = sharedPreferences.getInt(prefix + "auto_command_count", 0);
		for (int j = 0; j < autoCommandCount; j++) {
			this.addAutoCommand(sharedPreferences.getString(prefix + "auto_command_" + j, ""));
		}
		this.isAutoConnected(sharedPreferences.getBoolean(prefix + "auto_connect", false));
		this.isLogged(sharedPreferences.getBoolean(prefix + "log", false));

		this.preferenceSpot = i;
	}

	// Save the preference to SharedPreferences.
	public void saveToSharedPreferences(SharedPreferences sharedPreferences) {
		// If we're already in the preferences, use our existing spot for
		// updating.
		// Else, create a new spot.
		String prefix = "server_";
		SharedPreferences.Editor editor = sharedPreferences.edit();
		if (this.preferenceSpot >= 0) {
			prefix += this.preferenceSpot + "_";
		} else {
			int count = sharedPreferences.getInt("server_count", 0);
			editor.putInt("server_count", count + 1);
			prefix += count + "_";
		}

		editor.putString(prefix + "name", this.getName());
		editor.putInt(prefix + "nick_count", this.nicknames.size());
		for (int i = 0; i < this.nicknames.size(); i++) {
			editor.putString(prefix + "nick_" + i, this.nicknames.get(i));
		}
		editor.putString(prefix + "username", this.getUsername());
		editor.putString(prefix + "realname", this.getRealname());

		editor.putString(prefix + "host_hostname", this.host.getHostname());
		editor.putInt(prefix + "host_port", this.host.getPort());
		editor.putBoolean(prefix + "host_ssl", this.host.isSSL());
		editor.putBoolean(prefix + "host_verify_ssl", this.host.verifySSL());
		editor.putString(prefix + "host_password", this.host.getPassword());

		editor.putInt(prefix + "auto_channel_count", this.autoChannels.size());
		for (int i = 0; i < this.autoChannels.size(); i++) {
			editor.putString(prefix + "auto_channel_" + i, this.autoChannels.get(i));
		}
		editor.putInt(prefix + "auto_command_count", this.autoCommands.size());
		for (int i = 0; i < this.autoCommands.size(); i++) {
			editor.putString(prefix + "auto_command_" + i, this.autoCommands.get(i));
		}

		editor.putBoolean(prefix + "auto_connect", this.isAutoConnected());
		editor.putBoolean(prefix + "log", this.isLogged());
		editor.commit();
	}

	public void deleteFromSharedPreferences(SharedPreferences sharedPreferences) {
		SharedPreferences.Editor editor = sharedPreferences.edit();

		int serverCount = sharedPreferences.getInt("server_count", 0);

		// Move all servers back one spot.
		for (int i = this.preferenceSpot; i < serverCount - 1; i++) {
			this.loadFromSharedPreferences(sharedPreferences, i + 1);
			this.preferenceSpot = i;
			this.saveToSharedPreferences(sharedPreferences);
		}

		// Delete the last server.
		String prefix = "server_" + serverCount + "_";
		editor.remove(prefix + "name");
		editor.remove(prefix + "username");
		editor.remove(prefix + "realname");
		editor.remove(prefix + "host_hostname");
		editor.remove(prefix + "host_port");
		editor.remove(prefix + "host_ssl");
		editor.remove(prefix + "host_verify_ssl");
		editor.remove(prefix + "host_password");
		editor.remove(prefix + "auto_connect");
		editor.remove(prefix + "log");

		int nickCount = sharedPreferences.getInt(prefix + "nick_count", 0);
		for (int i = 0; i < nickCount; i++) {
			editor.remove(prefix + "nick" + i);
		}
		editor.remove(prefix + "nick_count");
		int autoChannelCount = sharedPreferences.getInt(prefix + "auto_channel_count", 0);
		for (int i = 0; i < autoChannelCount; i++) {
			editor.remove(prefix + "auto_channel_" + i);
		}
		editor.remove(prefix + "auto_channel_count");
		int autoCommandCount = sharedPreferences.getInt(prefix + "auto_command_count", 0);
		for (int i = 0; i < autoCommandCount; i++) {
			editor.remove(prefix + "auto_command_" + i);
		}
		editor.remove(prefix + "auto_command_count");

		// Decrement the server count.
		editor.putInt("server_count", serverCount - 1);
		editor.commit();
	}

	public static boolean serverNameExists(SharedPreferences sharedPreferences, String name) {
		// TODO Implement
		return false;
	}
}
