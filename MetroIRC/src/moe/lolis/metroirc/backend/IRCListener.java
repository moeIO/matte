package moe.lolis.metroirc.backend;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import moe.lolis.metroirc.R;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.MessageParser;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerMessage;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.MotdEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.events.TopicEvent;

import android.text.Html;
import android.text.SpannedString;

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
		this.service.serverConnected(server);
	}

	public void onDisconnect(DisconnectEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		// Disconnect seems to give us no reason, ability to send it anyway
		this.service.serverDisconnected(server, this.service.getString(R.string.disconnectederror));
	}

	public void onMotd(MotdEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());

		for (String s : event.getMotd().split("\n")) {
			ServerMessage message = new ServerMessage();
			message.setContent(SpannedString.valueOf(s));
			message.setTime(new Date());
			// XXX: I don't like this, new runnable for each MOTD line
			this.service.statusMessageReceived(server, message);
		}
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
					String msg = "<strong>" + event.getUser().getNick() + "</strong> (" + event.getUser().getLogin() + "@"
							+ event.getUser().getHostmask() + ") " + this.service.getString(R.string.hasquit);
					if (event.getReason().length() > 0) {
						msg += " (<em>" + event.getReason() + "</em>)";
					}

					message = new ChannelMessage();
					message.setNickname("<--");
					message.setContent(Html.fromHtml(msg));
					message.setTime(new Date());
				}
				// ch.addMessage(message);
				this.service.statusMessageReceived(ch, message);
			}
		}

		this.service.networkQuit(commonChannels, event.getUser().getNick());
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
		message.setContent(Html.fromHtml("<strong>" + event.getUser().getNick() + "</strong> (" + event.getUser().getLogin() + "@"
				+ event.getUser().getHostmask() + ") " + this.service.getString(R.string.hasjoined) + " " + event.getChannel().getName()));
		message.setTime(new Date());
		// channel.addMessage(message);
		this.service.statusMessageReceived(channel, message);
		this.service.channelJoined(channel, event.getUser().getNick());
	}

	public void onPart(PartEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname("<--");
		message.setContent(Html.fromHtml("<strong>" + event.getUser().getNick() + "</strong> (" + event.getUser().getLogin() + "@"
				+ event.getUser().getHostmask() + ") " + this.service.getString(R.string.hasleft) + " " + event.getChannel().getName() + " (<em>"
				+ event.getReason() + "</em>)"));
		message.setTime(new Date());
		// channel.addMessage(message);
		this.service.statusMessageReceived(channel, message);
		this.service.channelParted(channel, event.getUser().getNick());
	}

	public void onMessage(MessageEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname(event.getUser().getNick());
		message.setContent(Html.fromHtml(MessageParser.parseToHTML(event.getMessage())));
		message.setTime(new Date());
		// channel.addMessage(message);

		if (channel.isActive()) {
			this.service.activeChannelMessageReceived(channel, message);
		} else {
			channel.incrementUnreadMessages();
			this.service.inactiveChannelMessageReceived(channel, message);
		}
		if (message.getContent().toString().toLowerCase().contains(event.getBot().getNick().toLowerCase())) {
			message.isHighlighted(true);
			if (!this.service.isAppActive() || !channel.isActive()) {
				this.service.showMentionNotification(message, channel, event.getBot().getServerPreferences().getName());
			}
		} else {
			message.isHighlighted(false);
		}
	}

	public void onNotice(NoticeEvent<Client> event) {

	}

	public void onNickChange(NickChangeEvent<Client> event) {
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
					message.setNickname("--");
					message.setContent(Html.fromHtml("<strong>" + event.getOldNick() + "</strong> " + this.service.getString(R.string.nowknownas)
							+ " <strong>" + event.getNewNick() + "</strong>"));
					message.setTime(new Date());
				}
				// ch.addMessage(message);
				this.service.statusMessageReceived(ch, message);
			}
		}

		this.service.nickChanged(commonChannels, event.getOldNick(), event.getNewNick());
	}

	public void onTopic(TopicEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		Channel channel = server.getChannel(event.getChannel().getName());

		ChannelMessage message = new ChannelMessage();
		message.setNickname("--");
		message.setTime(new Date());
		if (event.isChanged()) {
			message.setContent(Html.fromHtml(event.getUser().getNick() + " " + this.service.getString(R.string.changedtopic) + ": <strong>"
					+ MessageParser.parseToHTML(event.getTopic()) + "</strong>"));
		} else {
			message.setContent(Html.fromHtml(event.getChannel().getName() + this.service.getString(R.string.stopicis) + ": <strong>"
					+ MessageParser.parseToHTML(event.getTopic()) + "</strong>"));
		}

		this.service.statusMessageReceived(channel, message);
	}

	public void onServerResponse(ServerResponseEvent<Client> event) {
		Server server = this.service.getServer(event.getBot().getServerPreferences().getName());
		String response = event.getResponse();
		if (response == null || response.isEmpty()) {
			return;
		}

		// Not *that* raw...
		response = response.trim();
		if (response.startsWith(event.getBot().getNick())) {
			response = response.substring(event.getBot().getNick().length(), response.length()).trim();
		}
		if (response.startsWith("*")) {
			response = response.substring(1, response.length()).trim();
		}
		if (response.startsWith(":")) {
			response = response.substring(1, response.length()).trim();
		}

		ServerMessage message = new ServerMessage();
		message.setContent(Html.fromHtml(MessageParser.parseToHTML(response)));
		message.setTime(new Date());
		// server.addMessage(message);

		this.service.statusMessageReceived(server, message);
	}

}
