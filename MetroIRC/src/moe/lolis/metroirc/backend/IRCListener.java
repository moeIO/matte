package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

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
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		for (String command : server.getClient().getServerPreferences().getAutoCommands())
			event.respond(command);
		// No need to switch to the server tab as it was changed to during
		// connection
	}

	public void onMotd(MotdEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());

		for (String s : event.getMotd().split("\n")) {
			ServerMessage message = new ServerMessage();
			message.setContent(s);
			message.setTime(new Date());
			// XXX I don't like this, new runnable for each MOTD
			this.service.messageReceived(server, message);
		}
	}

	public void onJoin(JoinEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		// We joined the channel.
		if (channel == null) {
			// Newly encountered channel.
			channel = new Channel();
			channel.setServer(server);
			channel.setChannelInfo(event.getChannel());
			server.addChannel(channel);
		}

		ChannelMessage message = new ChannelMessage();
		message.setNickname("-->");
		message.setContent(event.getUser().getNick() + " (" + event.getUser().getLogin() + "@" + event.getUser().getHostmask() + ") has joined "
				+ event.getChannel().getName());
		message.setTime(new Date());
		// channel.addMessage(message);
		this.service.messageReceived(channel, message);
		this.service.channelJoined(channel, event.getUser().getNick());
	}

	public void onPart(PartEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname("<--");
		message.setContent(event.getUser().getNick() + " (" + event.getUser().getLogin() + "@" + event.getUser().getHostmask() + ") has left "
				+ event.getChannel().getName() + " (" + event.getReason() + ")");
		message.setTime(new Date());
		// channel.addMessage(message);
		this.service.messageReceived(channel, message);
		this.service.channelParted(channel, event.getUser().getNick());
	}

	public void onQuit(QuitEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Client client = server.getClient();
		Set<org.pircbotx.Channel> ownChannels = client.getChannels();
		ArrayList<Channel> commonChannels = new ArrayList<Channel>();

		ChannelMessage message = null;

		for (org.pircbotx.Channel channel : event.getUser().getChannels()) {
			if (ownChannels.contains(channel)) {
				Channel ch = server.getChannel(channel.getName());
				commonChannels.add(ch);

				if (message == null) {
					message = new ChannelMessage();
					message.setNickname("<--");
					message.setContent(event.getUser().getNick() + " (" + event.getUser().getLogin() + "@" + event.getUser().getHostmask()
							+ ") has quit (" + event.getReason() + ")");
					message.setTime(new Date());
				}
				// ch.addMessage(message);
				this.service.messageReceived(server, message);
			}
		}

		this.service.networkQuit(commonChannels, event.getUser().getNick());
	}

	public void onMessage(MessageEvent<Client> event) throws Exception {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname(event.getUser().getNick());
		message.setContent(event.getMessage());
		message.setTime(new Date());
		// channel.addMessage(message);

		if (channel.isActive()) {
			this.service.activeChannelMessageReceived(channel, message);
		} else {
			channel.incrementUnreadMessages();
			this.service.inactiveChannelMessageReceived(channel, message);
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
		// server.addMessage(message);

		this.service.messageReceived(server, message);
	}

}
