package moe.lolis.metroirc.backend;

import java.util.Date;

import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerMessage;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

public class IRCListener extends ListenerAdapter<Client> {
	private IRCService service;

	public IRCListener(IRCService service) {
		this.service = service;
	}

	public void onConnect(ConnectEvent<Client> event) {

	}
	
	public void onMotd(MotdEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		
		for (String s : event.getMotd().split("\n")) {
			ServerMessage message = new ServerMessage();
			message.setContent(s);
			message.setTime(new Date());
			server.addMessage(message);
		}
		
		this.service.messageReceived(server);
	}
	
	public void onJoin(JoinEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		if (channel == null) {
			// Newly encountered channel.
			channel = new Channel();
			channel.setServer(server);
			channel.setChannelInfo(event.getChannel());
			server.addChannel(channel);
		}
		this.service.channelJoined(channel);
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
		if (message.getContent().toLowerCase().contains(event.getBot().getNick().toLowerCase())) {
			message.isHighlighted(true);
			if (!this.service.isAppActive() || !channel.isActive()) {
				this.service.showMentionNotification(message, channel, event.getBot().getServerPreferences().getName());
			}
		} else {
			message.isHighlighted(false);
		}
	}
	
	public void onServerResponse(ServerResponseEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		String response = event.getResponse();
		if (response == null || response.isEmpty()) {
			return;
		}
		
		// Not *that* raw...
		if (response.startsWith(":")) {
			response = response.substring(1, response.length() - 1);
		}
		
		ServerMessage message = new ServerMessage();
		message.setContent(response);
		message.setTime(new Date());
		server.addMessage(message);
		
		this.service.messageReceived(server);
	}

}
