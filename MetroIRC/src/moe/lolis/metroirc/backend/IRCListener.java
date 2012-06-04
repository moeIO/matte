package moe.lolis.metroirc.backend;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.Server;

public class IRCListener extends ListenerAdapter<PircBotX> {
	private IRCService service;

	public IRCListener(IRCService service) {
		this.service = service;
	}

	public void onMessage(MessageEvent event) throws Exception {
		Server server = this.service.getServer(event.getBot().getServerInfo().getNetwork());
		Channel channel = server.getChannel(event.getChannel().getName());
		
		Channel.Message message = channel.new Message();
		message.setNickname(event.getUser().getNick());
		message.setContent(event.getMessage());
		message.setTime(new Date());
		channel.addMessage(message);
		
		this.service.messageReceived(channel);
	}
}
