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

	public void onMessage(MessageEvent<Client> event) throws Exception {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname(event.getUser().getNick());
		message.setContent(event.getMessage());
		message.setTime(new Date());
		channel.addMessage(message);

		if (channel.isActive()) {
			this.service.activeChannelMessageReceived(channel);
		} else {
			channel.incrementUnreadMessages();
			this.service.inactiveChannelMessageReceived(channel);
		}
		if (message.getContent().contains(event.getBot().getNick())) {
			message.setisHighlighted(true);
			if (!this.service.isAppActive() || !channel.isActive()) {
				this.service.showMentionNotification(message, channel, event.getBot().getServerPreferences().getName());
			}
		}
		else
		{
			message.setisHighlighted(false);
		}

	}

	public void onJoin(JoinEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		if (channel == null) {
			// Newly encountered channel.
			channel = new Channel();
			channel.setServerInfo(event.getBot().getServerInfo());
			channel.setChannelInfo(event.getChannel());
			server.addChannel(channel);
		}
		this.service.channelJoined(channel);
	}

	public void onConnect(ConnectEvent<Client> event) {

	}
}
