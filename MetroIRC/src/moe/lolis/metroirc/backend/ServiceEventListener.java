package moe.lolis.metroirc.backend;

import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.Server;

/*
 * Represents an object that receives events from the MoeService
 */
public interface ServiceEventListener {

	/*
	 * Called when an IRC message is received by the service. Passes the channel
	 * the message belonged to
	 */
	public void activeChannelMessageReceived(Channel channel);
	
	/*
	 * Called when an IRC message is received by the service on an inactive channel
	 */
	public void inactiveChannelMessageReceived(Channel channel);

	/*
	 * Called when a server message is received. Passes the server the message
	 * belongs to.
	 */
	public void messageReceived(Server server);
	
	/*
	 * Called when a channel is joined (durr)
	 */
	public void channelJoined(Channel channel);
}
