package com.moeio.matte.irc;

import java.util.Date;

import android.text.Html;
import android.text.SpannedString;

import com.moeio.matte.ChannelActivity;
import com.moeio.matte.R;
import com.moeio.matte.backend.IRCService;

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
		if ((command.equalsIgnoreCase("msg") || command.equalsIgnoreCase("message") || command.equalsIgnoreCase("privmsg")) && parts.length > 1) { 
			// Sanity checks.
			if (parts.length > 2 && this.looksLikeChannel(parts[1]) && !client.getChannelsNames().contains(parts[1])) {
				this.service.channelMessageReceived(this.activity.getCurrentChannel(),
						Channel.createError(SpannedString.valueOf(service.getResources().getString(R.string.notinchannel) + ": " + parts[1])), true);
				return;
			} else if (parts.length > 2 && !client.userExists(parts[1])) {
				this.service.channelMessageReceived(this.activity.getCurrentChannel(),
						Channel.createError(SpannedString.valueOf(service.getResources().getString(R.string.nosuchuser) + ": " + parts[1])), true);
				return;
			}
			
			ChannelMessage msg = new ChannelMessage();
			msg.setNickname(client.getNick());
			msg.setContent(Html.fromHtml(MessageParser.parseToHTML(message)));
			msg.setTime(new Date());
			
			Channel target = null;
			if (parts.length > 2) {
				// If it's a query and no window for it has been created, do that now.
				if (!this.looksLikeChannel(parts[1]) && server.getChannel(parts[1]) == null) {
					target = this.service.createQuery(client, parts[1]);
				} else {
					target = server.getChannel(parts[1]);
				}
				
				client.sendMessage(parts[1], message.substring(parts[0].length() + parts[1].length() + 2, message.length()));
				this.service.queryMessageReceived((Query) target, msg, true);
			} else if (this.activity.getCurrentChannel() != null) {
				target = this.activity.getCurrentChannel();
				if (target instanceof Server) {
					target.addMessage(Channel.createError(SpannedString.valueOf(this.service.getResources().getString(R.string.cantmessageserver))));
					return;
				}
				client.sendMessage(target.getChannelInfo(), message.substring(parts[0].length() + 1, message.length()));
				this.service.channelMessageReceived(target, msg, true);
			}
		} else if ((command.equalsIgnoreCase("join") || command.equalsIgnoreCase("j")) && parts.length > 1) {
			String[] channels = parts[1].split(",");
			String[] passwords = null;
			if (parts.length > 2 && parts[2].split(",").length == channels.length) {
				passwords = parts[2].split(",");
			}

			for (int i = 0; i < channels.length; i++) {
				if (!this.looksLikeChannel(channels[i])) {
					// Send message.
					this.service.channelMessageReceived(this.activity.getCurrentChannel(), Channel.createError(SpannedString.valueOf(service
							.getResources().getString(R.string.invalidchannel) + ": " + channels[i])), true);
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
		} else if (command.equalsIgnoreCase("query") && parts.length > 1) {
			this.service.createQuery(client, parts[1]);
			
			if (parts.length > 2) {
				this.interpret("/msg " + message.substring(command.length() + 1, message.length()));
			}
		} else if ((command.equalsIgnoreCase("nick") || command.equalsIgnoreCase("nickname")) && parts.length > 1) {
			if (!client.userExists(parts[1])) {
				client.changeNick(parts[1]);
			} else {
				this.service.channelMessageReceived(this.activity.getCurrentChannel(),
						Channel.createError(SpannedString.valueOf(service.getResources().getString(R.string.nickinuse) + ": " + parts[1])), true);
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
