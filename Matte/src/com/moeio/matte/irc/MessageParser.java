package com.moeio.matte.irc;

public class MessageParser {
	
	private static final int BOLD = 2;
	private static final int COLOUR = 3;
	private static final int PLAIN = 15;
	private static final int REVERSE = 22;
	private static final int UNDERLINE = 31;
	
	private static String[] colorCodesToHex = {
		"#000000", "#000085", "#208100", "#f91000",
		"#7e413f", "#7900ff", "#818100", "#ffff00",
		"#49ff00", "#2e8080", "#41ffff", "#0000ff",
		"#f700ff", "#808080", "#c0c0c0", "#ffffff"
	};

	public static void parseSpecial(GenericMessage message) {
		String text = message.getContent().toString();
		if (text.contains("youtube.com/watch")) {
			int loc = text.indexOf("v=");
			if (text.length() > loc + 12) {
				message.setEmbeddedYoutube(text.substring(loc+2, loc + 13));
			}
		}
	}
	
	public static int countDigits(int n) {
		return n == 0 ? 1 : (int) Math.floor(Math.log10(Math.abs(n))) + (n < 0 ? 1 : 0);
	}
	
	// Do not ask questions; we won't either.
	public static int parseIRCColour(String message, int offset) {
		int j = offset, k = 1, n = 0, ch = 0;
		
		do {
			ch = message.codePointAt(j);
			
			// IRC colours go up to 16, check if the value is between 0 and 9 in the ASCII-table.
			if (ch >= 0x30 && ch <= 0x39) {
				n *= k;
				n += (ch - 0x30);
				
				if (n > 16) {
					break;
				}
			} else {
				break;
			}
			
			k *= 10;
			j += Character.charCount(ch);
		} while (true);
		
		return n;
	}

	public static String parseToHTML(String message) {
		boolean has_colour = false, has_bold = false, has_underline = false, is_reversed = false;
		int foreground = 1;
		String html = "";
		
		int ch;
		for (int i = 0; i < message.length(); i += Character.charCount(ch)) {
			ch = message.codePointAt(i);

			// Bold
			if (ch == BOLD) {
				if (!has_bold) {
					html += "<strong>";
				} else {
					html += "</strong>";
				}
				has_bold = !has_bold;
			}
			// Colour
			else if (ch == COLOUR) {
				// Grab foreground colour.
				i += Character.charCount(ch);
				foreground = parseIRCColour(message, i);
				// Increment i by the number of characters in that digit in base-10.
				i += countDigits(foreground);
				// Convert foreground to an array index.
				foreground = Math.max(foreground - 1, 0);
				
				// If the next character is a comma (e.g. ^3,9) , parse but ignore it -- we can't do background colours.
				if (message.codePointAt(i + 1) == 0x2C) {
					i += 2;
					i += countDigits(parseIRCColour(message, i));
				} 
				
				html += "<font color='" + colorCodesToHex[foreground] + "'>";
				has_colour = true;
			}
			// Underline
			else if (ch == UNDERLINE) {
				if (!has_underline) {
					html += "<u>";
				} else {
					html += "</u>";
				}
				has_underline = !has_underline;
			}
			// Text reverse
			else if (ch == REVERSE) {
				if (has_colour) {
					html += "</font>";
					has_colour = false;
				}
				
				// Leet colour swapping algorithms
				foreground = (colorCodesToHex.length - 1) - foreground;
				
				html += "<font color='" + colorCodesToHex[foreground] + "'>";
				is_reversed = true;
			}
			// Reset
			else if (ch == PLAIN) {
				if (has_colour) {
					html += "</font>";
					has_colour = false;
					foreground = 1;
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
					html += "</font>";
					is_reversed = false; 
				}
			}
			// Regular character
			else {
				html += message.charAt(i);
			}
		}
		
		if (has_colour) {
			html += "</font>";
			has_colour = false;
			foreground = 1;
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
			html += "</font>";
			is_reversed = false; 
		}
		
		return html;
	}
		
	public static int getInt(String str) {
		@SuppressWarnings("unused")
		int i = 0, val = 0;
		
		return val;
	}
}
