package moe.lolis.metroirc.irc;

import java.util.ArrayList;

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
}
