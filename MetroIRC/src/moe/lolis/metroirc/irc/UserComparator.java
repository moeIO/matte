package moe.lolis.metroirc.irc;

import java.util.Comparator;
import org.pircbotx.Channel;
import org.pircbotx.User;

public class UserComparator implements Comparator<User> {
	private Channel channel;

	public UserComparator(moe.lolis.metroirc.irc.Channel channel) {
		this.channel = channel.getChannelInfo();
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