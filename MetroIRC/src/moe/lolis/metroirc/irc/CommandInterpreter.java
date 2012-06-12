package moe.lolis.metroirc.irc;

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

		// HUGE FUCKLOAD LIST OF COMMANDS INCOMING
		// PREPARE YOUR ANUS
		if ((parts[0].equalsIgnoreCase("join") || parts[0].equalsIgnoreCase("j")) && parts.length > 1) {
			String[] channels = parts[1].split(",");
			String[] passwords = null;
			if (parts.length > 2 && parts[2].split(",").length == channels.length) {
				passwords = parts[2].split(",");
			}

			for (int i = 0; i < channels.length; i++) {
				if (!this.looksLikeChannel(channels[i])) {
					// Send message.
					this.service.activeChannelMessageReceived(this.activity.getCurrentChannel(),
							this.activity.getCurrentChannel().createError("Invalid channel name: " + channels[i]));
					continue;
				}

				if (passwords != null) {
					client.joinChannel(channels[i], passwords[i]);
				} else {
					client.joinChannel(channels[i]);
				}
			}
		} else if (parts[0].equalsIgnoreCase("leave") || parts[0].equalsIgnoreCase("part")) {
			String[] channels = { this.activity.getCurrentChannel().getChannelInfo().getName() };
			if (parts.length == 1) {
				// /part
				client.partChannel(this.activity.getCurrentChannel().getChannelInfo());
			} else {
				if (!this.looksLikeChannel(parts[1])) {
					// /part $reason
					client.partChannel(this.activity.getCurrentChannel().getChannelInfo(), parts[1]);
				} else {
					if (parts.length < 3) {
						// /part $channel
						org.pircbotx.Channel channel = server.getChannel(parts[1]).getChannelInfo();
						client.partChannel(channel);
					} else {
						// /part $channel $reason
						org.pircbotx.Channel channel = server.getChannel(parts[1]).getChannelInfo();
						client.partChannel(channel, parts[2]);
					}
				}
			}
		} else if ((parts[0].equalsIgnoreCase("nick") || parts[0].equalsIgnoreCase("nickname")) && parts.length > 1) {
			if (!client.userExists(parts[1])) {
				client.changeNick(parts[1]);
			} else {
				this.service.activeChannelMessageReceived(this.activity.getCurrentChannel(),
						this.activity.getCurrentChannel().createError("Nickname is already in use: " + parts[1]));
			}
		} else if ((parts[0].equalsIgnoreCase("whois") || parts[0].equalsIgnoreCase("who")) && parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				client.sendRawLine("WHOIS " + parts[i]);
			}
	 	} else if (parts[0].equalsIgnoreCase("mode") && parts.length > 1) {
	 		Channel channel;
	 		int start;
	 		if (this.looksLikeChannel(parts[1])) {
	 			channel = server.getChannel(parts[1]);
	 			start = 2;
	 		} else {
	 			channel = this.activity.getCurrentChannel();
	 			start = 1;
	 		}
	 		
	 		client.setMode(channel.getChannelInfo(), message.substring("mode".length() + 1, message.length()));
	 	} else if (parts[0].equalsIgnoreCase("ctcp") && parts.length > 2) {
	 		if (this.looksLikeChannel(parts[1])) {
	 			client.sendCTCPCommand(server.getChannel(parts[1]).getChannelInfo(), parts[2]);
	 		} else {
	 			client.sendCTCPCommand(parts[1], parts[2]);
	 		}
	 	}
	}

	private boolean looksLikeChannel(String subject) {
		return subject != null && (subject.startsWith("#") || subject.startsWith("&") || subject.startsWith("!") || subject.startsWith("+"));
	}
}
