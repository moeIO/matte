package moe.lolis.metroirc;

import java.util.ArrayList;

public class Channel {
	public org.pircbotx.Channel channel;
	public ArrayList<ChannelMessage> messages;

	public Channel() {
		messages = new ArrayList<ChannelMessage>();
	}
}
