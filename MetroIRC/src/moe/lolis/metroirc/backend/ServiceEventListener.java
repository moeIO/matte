package moe.lolis.metroirc.backend;

import moe.lolis.metroirc.irc.Channel;

/*
 * Represents an object that receives events from the MoeService
 */
public interface ServiceEventListener {

	/*
	 * Called when an IRC message is received by the service on the active channel
	 */
	public void activeChannelMessageReceived(Channel channel);
	
	/*
	 * Called when an IRC message is received by the service on an inactive channel
	 */
	public void inactiveChannelMessageReceived(Channel channel);

	/*
	 * Called when a channel is joined (durr)
	 */
	public void channelJoined(Channel channel);
}
