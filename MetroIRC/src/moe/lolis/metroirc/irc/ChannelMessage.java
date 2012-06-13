package moe.lolis.metroirc.irc;

import java.util.Date;
import android.text.Spanned;

public class ChannelMessage implements GenericMessage {
	private String nickname;
	private Spanned content;
	private Date time;
	private boolean highlight;
	private boolean isChannelNotificationType;

	public ChannelMessage(){
		
	}
	public ChannelMessage(String nickname, Spanned content) {
		this.nickname = nickname;
		this.content = content;
		this.time = new Date();
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getNickname() {
		return this.nickname;
	}

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

	public void isHighlighted(boolean highlighted) {
		this.highlight = highlighted;
	}

	public boolean isHighlighted() {
		return this.highlight;
	}

	public void isChannelNotificationType(boolean val) {
		this.isChannelNotificationType = val;
	}

	public boolean isChannelNotificationType() {
		return this.isChannelNotificationType;
	}
}