package moe.lolis.metroirc;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class IRCListener extends ListenerAdapter<PircBotX> {
	private MoeService service;

	public IRCListener(MoeService service) {
		this.service = service;
	}

	public void onMessage(MessageEvent event) throws Exception {
		ChannelMessage m = new ChannelMessage();
		m.text = event.getMessage();
		service.channel.messages.add(m);
		service.messageRecieved(service.channel);
	}
}
