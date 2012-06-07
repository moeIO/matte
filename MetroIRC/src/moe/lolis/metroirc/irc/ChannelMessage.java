package moe.lolis.metroirc.irc;

import java.util.Date;

public class ChannelMessage {
	private String nickname;
	private String content;
	private Date time;
	private boolean isHighlight;
	
	public void setNickname(String nickname) { this.nickname = nickname; }
	public String getNickname() { return this.nickname; }
	public void setContent(String content) { this.content = content; }
	public String getContent() { return this.content; }
	public void setTime(Date time) { this.time = time; }
	public Date getTime() { return this.time; }
	public void setisHighlighted(boolean highlighted){this.isHighlight=highlighted;}
	public boolean isHighlighted(){return isHighlight;}
}