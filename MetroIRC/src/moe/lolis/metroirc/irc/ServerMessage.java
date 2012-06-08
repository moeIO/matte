package moe.lolis.metroirc.irc;

import java.util.Date;

public class ServerMessage implements GenericMessage {
	private String content;
	private Date time;
	private String serverName;

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return this.content;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Date getTime() {
		return this.time;
	}

	public void setNickname(String nickname) {
		// Ignore
	}

	public String getNickname() {
		return serverName;
	}

	public void isHighlighted(boolean highlighted) {
		// Ignore
	}

	public boolean isHighlighted() {
		return false;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerName() {
		return this.serverName;
	}
}
