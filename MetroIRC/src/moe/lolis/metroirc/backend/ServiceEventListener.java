package moe.lolis.metroirc.backend;

import java.util.Collection;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.GenericMessage;
import moe.lolis.metroirc.irc.Server;

/*
 * Represents an object that receives events from the MoeService
 */
public interface ServiceEventListener {

	/*
	 * Called when an IRC message is received by the service. Passes the channel
	 * the message belonged to
	 */
	public void activeChannelMessageReceived(Channel channel, GenericMessage message);

	/*
	 * Called when an IRC message is received by the service on an inactive
	 * channel
	 */
	public void inactiveChannelMessageReceived(Channel channel, GenericMessage message);

	/*
	 * Called when a server message is received. Passes the server the message
	 * belongs to.
	 */
	public void statusMessageReceived(Channel channel, GenericMessage message);

	/*
	 * Called when a user changes their nickname.
	 */
	public void nickChanged(Collection<Channel> commonChannels, String from, String to);

	/*
	 * Called when a channel is joined (durr)
	 */
	public void channelJoined(Channel channel, String nickname);

	/*
	 * Called when a channel is parted.
	 */
	public void channelParted(Channel channel, String nickname);

	/*
	 * Called when a network is quit.
	 */
	public void networkQuit(Collection<Channel> commonChannels, String nickname);

	/*
	 * Called when a server is connected
	 */
	public void serverConnected(Server server);

	/*
	 * Called when a server is disconnected
	 */
	public void serverDisconnected(Server server, String error);
}
