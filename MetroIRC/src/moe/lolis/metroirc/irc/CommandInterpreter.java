package moe.lolis.metroirc.irc;

import android.text.SpannedString;
import moe.lolis.metroirc.ChannelActivity;
import moe.lolis.metroirc.backend.IRCService;

public class CommandInterpreter {
	private IRCService service;
	private ChannelActivity activity;

	public CommandInterpreter(IRCService service, ChannelActivity activity) {
		this.service = service;
		this.activity = activity;
	}

	public boolean isCommand(String message) {
		return message.startsWith("/") && !message.startsWith("//");
	}

	public void interpret(String message) {
		if (message.startsWith("/")) {
			message = message.substring(1, message.length());
		}

		String[] parts = message.split(" ");
		Client client = this.activity.getCurrentChannel().getClient();
		Server server = this.activity.getCurrentChannel().getServer();
		String command = parts[0];

		// HUGE FUCKLOAD LIST OF COMMANDS INCOMING
		// PREPARE YOUR ANUS
		if ((command.equalsIgnoreCase("join") || command.equalsIgnoreCase("j")) && parts.length > 1) {
			String[] channels = parts[1].split(",");
			String[] passwords = null;
			if (parts.length > 2 && parts[2].split(",").length == channels.length) {
				passwords = parts[2].split(",");
			}

			for (int i = 0; i < channels.length; i++) {
				if (!this.looksLikeChannel(channels[i])) {
					// Send message.
					this.service.activeChannelMessageReceived(this.activity.getCurrentChannel(),
							Channel.createError(SpannedString.valueOf("Invalid channel name: " + channels[i])));
					continue;
				}

				if (passwords != null) {
					client.joinChannel(channels[i], passwords[i]);
				} else {
					client.joinChannel(channels[i]);
				}
			}
		} else if (command.equalsIgnoreCase("leave") || command.equalsIgnoreCase("part")) {
			if (parts.length == 1) {
				// /part
				client.partChannel(this.activity.getCurrentChannel().getChannelInfo());
				this.service.channelParted(this.activity.getCurrentChannel(), client.getNick());
			} else {
				if (!this.looksLikeChannel(parts[1])) {
					// /part $reason
					client.partChannel(this.activity.getCurrentChannel().getChannelInfo(), parts[1]);
					this.service.channelParted(this.activity.getCurrentChannel(), client.getNick());
				} else {
					if (parts.length < 3) {
						// /part $channel
						Channel channel = server.getChannel(parts[1]);
						client.partChannel(channel.getChannelInfo());
						this.service.channelParted(channel, client.getNick());
					} else {
						// /part $channel $reason
						Channel channel = server.getChannel(parts[1]);
						client.partChannel(channel.getChannelInfo(), parts[2]);
						this.service.channelParted(channel, client.getNick());
					}
				}
			}
		} else if ((command.equalsIgnoreCase("nick") || command.equalsIgnoreCase("nickname")) && parts.length > 1) {
			if (!client.userExists(parts[1])) {
				client.changeNick(parts[1]);
			} else {
				this.service.activeChannelMessageReceived(this.activity.getCurrentChannel(),
						Channel.createError(SpannedString.valueOf("Nickname is already in use: " + parts[1])));
			}
		} else if ((command.equalsIgnoreCase("whois") || command.equalsIgnoreCase("who")) && parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				client.sendRawLine("WHOIS " + parts[i]);
			}
		} else if (command.equalsIgnoreCase("mode") && parts.length > 1) {
			Channel channel;
			if (this.looksLikeChannel(parts[1])) {
				channel = server.getChannel(parts[1]);
			} else {
				channel = this.activity.getCurrentChannel();
			}

			client.setMode(channel.getChannelInfo(), message.substring("mode".length() + 1, message.length()));
		} else if (command.equalsIgnoreCase("ctcp") && parts.length > 2) {
			if (this.looksLikeChannel(parts[1])) {
				client.sendCTCPCommand(server.getChannel(parts[1]).getChannelInfo(), parts[2]);
			} else {
				client.sendCTCPCommand(parts[1], parts[2]);
			}
		} else if ((command.equalsIgnoreCase("quote") || command.equalsIgnoreCase("raw")) && parts.length > 1) {
			client.sendRawLine(message.substring(parts[0].length() + 1, message.length()));
		}
	}

	private boolean looksLikeChannel(String subject) {
		return subject != null && (subject.startsWith("#") || subject.startsWith("&") || subject.startsWith("!") || subject.startsWith("+"));
	}
}
