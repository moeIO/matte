package moe.lolis.metroirc.irc;

public class MessageParser {
	
	private static final int BOLD = 2;
	private static final int COLOUR = 3;
	private static final int PLAIN = 15;
	private static final int REVERSE = 22;
	private static final int UNDERLINE = 31;
	
	private static String[] colorCodesToHex = {
		"#ffffff", "#000000"
	};

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
		boolean has_colour = false, has_bold = false, has_underline = false, is_reversed = false;
		int foreground = 1, background = 0;
		String html = "";
		
		int ch;
		for (int i = 0; i < message.length(); i += Character.charCount(ch)) {
			ch = message.codePointAt(i);

			// Bold
			if (ch == BOLD && !has_bold) {
				html += "<strong>";
				has_bold = true;
			}
			// Colour
			else if (ch == COLOUR) {
				// TODO: Grab colour values.
				html += "<font color='" + colorCodesToHex[foreground] + "'>";
				has_colour = true;
			}
			// Underline
			else if (ch == UNDERLINE && !has_underline) {
				html += "<u>";
				has_underline = true;
			}
			// Text reverse
			else if (ch == REVERSE) {
				if (has_colour) {
					html += "</font>";
					has_colour = false;
				}
				
				// Leet colour swapping algorithms
				foreground += background;
				background = foreground - background;
				foreground = foreground - background;
				
				html += "<font color='" + colorCodesToHex[foreground] + "'>";
				is_reversed = true;
			}
			// Reset
			else if (ch == PLAIN) {
				if (has_colour) {
					html += "</span>";
					has_colour = false;
					foreground = 1;
					background = 0;
				}
				if (has_underline) {
					html += "</u>";
					has_underline = false;
				}
				if (has_bold) {
					html += "</strong>";
					has_bold = false;
				}
				if (is_reversed) {
					html += "</span>";
					is_reversed = false; 
				}
			}
			// Regular character
			else {
				html += message.charAt(i);
			}
		}
		
		if (has_colour) {
			html += "</span>";
			has_colour = false;
			foreground = 1;
			background = 0;
		}
		if (has_underline) {
			html += "</span>";
			has_underline = false;
		}
		if (has_bold) {
			html += "</strong>";
			has_bold = false;
		}
		if (is_reversed) {
			html += "</span>";
			is_reversed = false; 
		}
		
		return html;
	}
		
	public static int getInt(String str) {
		int i = 0, val = 0;
		
		return val;
	}
}
