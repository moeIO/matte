package moe.lolis.metroirc.irc;

import java.util.ArrayList;
import java.util.Date;

public class Channel {
	public class Message {
		private String nickname;
		private String content;
		private Date time;
		
		public void setNickname(String nickname) { this.nickname = nickname; }
		public String getNickname() { return this.nickname; }
		public void setContent(String content) { this.content = content; }
		public String getContent() { return this.content; }
		public void setTime(Date time) { this.time = time; }
		public Date getTime() { return this.time; }
	}

	private org.pircbotx.Channel channelInfo;
	private ArrayList<Message> messages;

	public Channel() {
		this.messages = new ArrayList<Message>();
	}
	
	public void setChannelInfo(org.pircbotx.Channel channelInfo) { this.channelInfo = channelInfo; }
	public org.pircbotx.Channel getChannelInfo() { return this.channelInfo; }
	public void addMessage(Message message) { this.messages.add(message); }
	public ArrayList<Message> getMesages() { return this.messages; }
	public void removeMessage(Message message) { this.messages.remove(message); }
	public void setMessages(ArrayList<Message> messages) { this.messages = messages; }
}
