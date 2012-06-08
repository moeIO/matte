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
		Client client = (Client) this.activity.getCurrentChannel().getChannelInfo().getBot();
		Server server = this.activity.getCurrentChannel().getServer();

		// HUGE FUCKLOAD LIST OF COMMANDS INCOMING
		// PREPARE YOUR ANUS
		if (parts[0].equals("join") && parts.length > 1) {
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
		} else if (parts[0].equals("leave") || parts[0].equals("part")) {
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
		}
	}

	private boolean looksLikeChannel(String subject) {
		return subject != null && (subject.startsWith("#") || subject.startsWith("&") || subject.startsWith("!") || subject.startsWith("+"));
	}
}
