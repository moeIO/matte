package moe.lolis.metroirc.irc;

import java.util.Date;

public class ChannelMessage implements GenericMessage{
	private String nickname;
	private String content;
	private Date time;
	private boolean highlight;
	private boolean isChannelNotificationType;
	
	public void setNickname(String nickname) { this.nickname = nickname; }
	public String getNickname() { return this.nickname; }
	public void setContent(String content) { this.content = content; }
	public String getContent() { return this.content; }
	public void setTime(Date time) { this.time = time; }
	public Date getTime() { return this.time; }
	public void isHighlighted(boolean highlighted) { this.highlight = highlighted; }
	public boolean isHighlighted() { return this.highlight; }
	public void isChannelNotificationType(boolean val){this.isChannelNotificationType=val;}
	public boolean isChannelNotificationType() { return this.isChannelNotificationType;}
}