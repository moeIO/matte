package moe.lolis.metroirc.irc;

import java.util.Date;

import android.text.Spanned;

public abstract interface GenericMessage {
	public abstract void setNickname(String nickname);

	public abstract String getNickname();

	public abstract void setContent(Spanned content);

	public abstract Spanned getContent();

	public abstract void setTime(Date time);

	public abstract Date getTime();

	public abstract void isHighlighted(boolean highlighted);

	public abstract boolean isHighlighted();

	public abstract void isChannelNotificationType(boolean type);

	public abstract boolean isChannelNotificationType();

	public abstract String getEmbeddedYoutube();

	public abstract void setEmbeddedYoutube(String id);
}
