package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.HashMap;

public class Server {
	private org.pircbotx.ServerInfo serverInfo;
	private HashMap<String, Channel> channels;
	private String name;
	
	public Server() {
		this.channels = new HashMap<String, Channel>();
		this.name = "Unnamed";
	}
	
	public void setServerInfo(org.pircbotx.ServerInfo serverInfo) { this.serverInfo = serverInfo; }
	public org.pircbotx.ServerInfo getServerInfo() { return this.serverInfo; }
	public void addChannel(Channel channel) { this.channels.put(channel.getChannelInfo().getName(), channel); }
	public Channel getChannel(String name) { return this.channels.get(name); }
	public ArrayList<Channel> getChannels() { return new ArrayList<Channel>(this.channels.values()); }
	public void removeChannel(Channel channel) { this.channels.remove(channel); }
	public void setChannels(ArrayList<Channel> channels) { 
		this.channels = new HashMap<String, Channel>();
		for(Channel channel : channels) {
			this.addChannel(channel);
		}
	}
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
}
