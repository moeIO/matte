package moe.lolis.metroirc.irc;

import java.util.Date;

public abstract interface GenericMessage {
	public abstract void setNickname(String nickname);
	public abstract String getNickname();
	public abstract void setContent(String content) ;
	public abstract String getContent();
	public abstract void setTime(Date time);
	public abstract Date getTime();
	public abstract void isHighlighted(boolean highlighted);
	public abstract boolean isHighlighted();
	public abstract void isChannelNotificationType(boolean type);
	public abstract boolean isChannelNotificationType();
}
