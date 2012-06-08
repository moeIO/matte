package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.Date;

public class Channel {
	private org.pircbotx.Channel channelInfo;
	private Server server;
	protected ArrayList<GenericMessage> messages;
	protected int unreadMessages;
	protected boolean active;
	

	public Channel() {
		this.messages = new ArrayList<GenericMessage>();
	}

	public void setChannelInfo(org.pircbotx.Channel channelInfo) {
		this.channelInfo = channelInfo;
	}

	public org.pircbotx.Channel getChannelInfo() {
		return this.channelInfo;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Server getServer() {
		return this.server;
	}

	public void addMessage(ChannelMessage message) {
		this.messages.add(message);
	}
	
	public void addError(String error) {
		ChannelMessage message = new ChannelMessage();
		message.setNickname("!");
		message.setContent(error);
		message.setTime(new Date());
		this.addMessage(message);
	}

	public ArrayList<GenericMessage> getMessages() {
		return this.messages;
	}

	public void removeMessage(ChannelMessage message) {
		this.messages.remove(message);
	}

	public void setMessages(ArrayList<GenericMessage> messages) {
		this.messages = messages;
	}

	public void sendMessage(String message) {
		channelInfo.getBot().sendMessage(channelInfo, message);
		ChannelMessage m = new ChannelMessage();
		m.setTime(new Date());
		m.setNickname(this.getChannelInfo().getBot().getNick());
		m.setContent(message);
		this.addMessage(m);
	}

	public void incrementUnreadMessages() {
		this.unreadMessages++;
	}

	public int getUnreadMessageCount() {
		return this.unreadMessages;
	}

	public void isActive(boolean active) {
		this.active = active;
		if (this.active) {
			this.unreadMessages = 0;
		}
	}

	public boolean isActive() {
		return this.active;
	}
	
	public String getName()
	{
		return getChannelInfo().getName();
	}
}
