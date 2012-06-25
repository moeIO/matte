package com.moeio.matte.backend;

import java.util.Collection;

import com.moeio.matte.irc.Channel;
import com.moeio.matte.irc.GenericMessage;
import com.moeio.matte.irc.Query;
import com.moeio.matte.irc.Server;

/*
 * Represents an object that receives events from the MoeService
 */
public interface ServiceEventListener {

	/*
	 * Called when an IRC message is received by the service. Passes the channel
	 * the message belonged to.
	 */
	public void channelMessageReceived(Channel channel, GenericMessage message, boolean active);
	
	/*
	 * Called when a private message is received by the service. Passes the query
	 * the message belongs to.
	 */
	public void queryMessageReceived(Query query, GenericMessage message, boolean active);

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
