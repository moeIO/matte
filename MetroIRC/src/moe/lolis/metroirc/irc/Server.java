package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.HashMap;

public class Server {
	private org.pircbotx.ServerInfo serverInfo;
	private HashMap<String, Channel> channels;
	private String name;
	private ArrayList<ServerMessage> messages;
	private Client client;

	public Server() {
		this.channels = new HashMap<String, Channel>();
		this.messages = new ArrayList<ServerMessage>();
		this.name = "Unnamed";
	}

	public void setServerInfo(org.pircbotx.ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	public org.pircbotx.ServerInfo getServerInfo() {
		return this.serverInfo;
	}

	public void addChannel(Channel channel) {
		this.channels.put(channel.getChannelInfo().getName(), channel);
	}

	public Channel getChannel(String name) {
		return this.channels.get(name);
	}

	public ArrayList<Channel> getChannels() {
		return new ArrayList<Channel>(this.channels.values());
	}

	public void removeChannel(Channel channel) {
		this.channels.remove(channel);
	}

	public void setChannels(ArrayList<Channel> channels) {
		this.channels = new HashMap<String, Channel>();
		for (Channel channel : channels) {
			this.addChannel(channel);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void addMessage(ServerMessage message) {
		this.messages.add(message);
	}

	public ArrayList<ServerMessage> getMessages() {
		return this.messages;
	}

	public void removeMessage(ServerMessage message) {
		this.messages.remove(message);
	}

	public void setMessages(ArrayList<ServerMessage> messages) {
		this.messages = messages;
	}

	public void sendRawCommand(String rawCommand) {
		this.getServerInfo().getBot().sendRawLine(rawCommand);
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Client getClient() {
		return this.client;
	}
}
