package moe.lolis.metroirc.backend;

import java.util.Date;

import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.Server;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;

public class IRCListener extends ListenerAdapter<Client> {
	private IRCService service;

	public IRCListener(IRCService service) {
		this.service = service;
	}

	public void onMessage(MessageEvent event) throws Exception {
		Server server = this.service.getServer(event.getBot().getServerInfo()
				.getNetwork());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname(event.getUser().getNick());
		message.setContent(event.getMessage());
		message.setTime(new Date());
		channel.addMessage(message);

		this.service.messageReceived(channel);
	}

	public void onJoin(JoinEvent event) {
		Server server = this.service.getServer(event.getBot().getServerInfo()
				.getNetwork());
		Channel channel = server.getChannel(event.getChannel().getName());
		if (channel == null) {
			// newly encountered channel
			Channel c = new Channel();
			c.setChannelInfo(event.getChannel());
			server.addChannel(c);
			channel = c;
		}
		this.service.channelJoined(channel);
	}

	public void onConnect(ConnectEvent event) {

	}
}
