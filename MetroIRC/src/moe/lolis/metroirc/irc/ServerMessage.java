package moe.lolis.metroirc.irc;

import java.util.Date;
import android.text.Spanned;

public class ServerMessage implements GenericMessage {
	private Spanned content;
	private Date time;


	public void setContent(Spanned content) {
		this.content = content;
	}

	public Spanned getContent() {
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
		return "";
	}

	public void isHighlighted(boolean highlighted) {
		// Ignore
	}

	public boolean isHighlighted() {
		return false;
	}

	public void isChannelNotificationType(boolean val) {
		// Ignore
	}

	public boolean isChannelNotificationType() {
		return true;
	}

}
