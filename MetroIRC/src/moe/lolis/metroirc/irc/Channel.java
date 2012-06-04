package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.Date;

public class Channel {
	private org.pircbotx.Channel channelInfo;
	private ArrayList<ChannelMessage> messages;

	public Channel() {
		this.messages = new ArrayList<ChannelMessage>();
	}
	
	public void setChannelInfo(org.pircbotx.Channel channelInfo) { this.channelInfo = channelInfo; }
	public org.pircbotx.Channel getChannelInfo() { return this.channelInfo; }
	public void addMessage(ChannelMessage message) { this.messages.add(message); }
	public ArrayList<ChannelMessage> getMesages() { return this.messages; }
	public void removeMessage(ChannelMessage message) { this.messages.remove(message); }
	public void setMessages(ArrayList<ChannelMessage> messages) { this.messages = messages; }
}
