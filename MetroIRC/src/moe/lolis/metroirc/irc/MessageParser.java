package moe.lolis.metroirc.irc;


public class MessageParser {

	public static void parseMessage(GenericMessage message) {
		String text = message.getContent().toString();
		if (text.contains("youtube.com/watch")) {
			int loc = text.indexOf("v=");
			if (text.length() > loc + 12) {
				message.setEmbeddedYoutube(text.substring(loc+2, loc + 13));
			}
		}
	}

	public static String parseToHTML(String message) {
		return message;
	}
}
