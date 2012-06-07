package moe.lolis.metroirc.irc;

import java.util.Date;

public class ServerMessage {
	private String content;
	private Date time;
	
	public void setContent(String content) { this.content = content; }
	public String getContent() { return this.content; }
	public void setTime(Date time) { this.time = time; }
	public Date getTime() { return this.time; }
}
