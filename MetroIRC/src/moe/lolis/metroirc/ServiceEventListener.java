package moe.lolis.metroirc;

/*
 * Represents an object that receives events from the MoeService
 */
public interface ServiceEventListener {

	/*
	 * Called when an IRC message is received by the service Passes the channel
	 * the message belonged to
	 */
	public void messageRecieved(Channel channel);
}
