package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.Date;

import android.text.Spanned;
import android.text.SpannedString;

public class Channel {
	private org.pircbotx.Channel channelInfo;
	private Server server;
	protected ArrayList<GenericMessage> messages;
	protected int unreadMessages;
	protected boolean active;

	private static final int MAX_BUFFER_MESSAGES = 50;

	public Channel() {
		this.messages = new ArrayList<GenericMessage>(MAX_BUFFER_MESSAGES);
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

	public void addMessage(GenericMessage message) {
		if (messages.size() == MAX_BUFFER_MESSAGES) {
			this.logMessage(messages.get(0));
			this.messages.remove(0);
		}
		this.messages.add(message);
	}

	private void logMessage(GenericMessage message) {
		// TODO: Implement
	}

	public GenericMessage createError(Spanned error) {
		ChannelMessage message = new ChannelMessage();
		message.setNickname("!");
		message.setContent(error);
		message.setTime(new Date());
		return message;
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
		this.channelInfo.getBot().sendMessage(this.channelInfo, message);
		ChannelMessage m = new ChannelMessage();
		m.setTime(new Date());
		m.setNickname(this.getChannelInfo().getBot().getNick());
		m.setContent(SpannedString.valueOf(message));
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

	public String getName() {
		return getChannelInfo().getName();
	}
	
	public Client getClient()
	{
		return this.getServer().getClient();
	}
}
