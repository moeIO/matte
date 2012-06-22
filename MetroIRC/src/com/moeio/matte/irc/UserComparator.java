package com.moeio.matte.irc;

import java.util.Comparator;
import org.pircbotx.User;

public class UserComparator implements Comparator<User> {
	private org.pircbotx.Channel channel;

	public UserComparator(com.moeio.matte.irc.Channel channel) {
		this.channel = channel.getChannelInfo();
	}
	
	public static String getPrefix(User user, Channel chan) {
		org.pircbotx.Channel channel = chan.getChannelInfo();
		if (user.isIrcop()) {
			return "!";
		} else if (channel.isOwner(user)) {
			return "~";
		} else if (channel.isSuperOp(user)) {
			return "&";
		} else if (channel.isOp(user)) {
			return "@";
		} else if (channel.isHalfOp(user)) {
			return "%";
		} else if (channel.hasVoice(user)) {
			return "+";
		}
		return "";
	}

	public int compare(User u1, User u2) {
		if (u1.isIrcop() && !u2.isIrcop())
			return -1;
		if (!u1.isIrcop() && u2.isIrcop())
			return 1;
		if (this.channel.isOwner(u1)) {
			if (this.channel.isOwner(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				return -1;
			}
		}
		if (this.channel.isSuperOp(u1)) {
			if (this.channel.isSuperOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (this.channel.isOwner(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (this.channel.isOp(u1)) {
			if (this.channel.isOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (this.channel.isOwner(u2)) {
					return 1;
				} else if (this.channel.isSuperOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (this.channel.isHalfOp(u1)) {
			if (this.channel.isHalfOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (this.channel.isOwner(u2)) {
					return 1;
				} else if (this.channel.isSuperOp(u2)) {
					return 1;
				} else if (this.channel.isOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (this.channel.hasVoice(u1)) {
			if (this.channel.hasVoice(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (this.channel.isOwner(u2)) {
					return 1;
				} else if (this.channel.isSuperOp(u2)) {
					return 1;
				} else if (this.channel.isOp(u2)) {
					return 1;
				} else if (this.channel.isHalfOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (this.channel.isOwner(u2)) {
			return 1;
		} else if (this.channel.isSuperOp(u2)) {
			return 1;
		} else if (this.channel.isOp(u2)) {
			return 1;
		} else if (this.channel.isHalfOp(u2)) {
			return 1;
		} else if (this.channel.hasVoice(u2)) {
			return 1;
		}
		if (u1.isAway() && !u2.isAway()) {
			return 1;
		} else if (!u1.isAway() && u2.isAway()) {
			return -1;
		} else {
			return u1.getNick().compareToIgnoreCase(u2.getNick());
		}

	}
}