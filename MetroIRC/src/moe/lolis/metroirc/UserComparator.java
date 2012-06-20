package moe.lolis.metroirc;

import java.util.Comparator;

import moe.lolis.metroirc.irc.Channel;

import org.pircbotx.User;

public class UserComparator implements Comparator<User> {
	private Channel channel;

	public UserComparator(Channel channel) {
		this.channel = channel;
	}

	public int compare(User u1, User u2) {
		if (u1.isIrcop() && !u2.isIrcop())
			return -1;
		if (!u1.isIrcop() && u2.isIrcop())
			return 1;
		if (channel.getChannelInfo().isOwner(u1)) {
			if (channel.getChannelInfo().isOwner(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				return -1;
			}
		}
		if (channel.getChannelInfo().isSuperOp(u1)) {
			if (channel.getChannelInfo().isSuperOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (channel.getChannelInfo().isOwner(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (channel.getChannelInfo().isOp(u1)) {
			if (channel.getChannelInfo().isOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (channel.getChannelInfo().isOwner(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isSuperOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (channel.getChannelInfo().isHalfOp(u1)) {
			if (channel.getChannelInfo().isHalfOp(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (channel.getChannelInfo().isOwner(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isSuperOp(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (channel.getChannelInfo().hasVoice(u1)) {
			if (channel.getChannelInfo().hasVoice(u2)) {
				return u1.getNick().compareToIgnoreCase(u2.getNick());
			} else {
				if (channel.getChannelInfo().isOwner(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isSuperOp(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isOp(u2)) {
					return 1;
				} else if (channel.getChannelInfo().isHalfOp(u2)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		if (channel.getChannelInfo().isOwner(u2)) {
			return 1;
		} else if (channel.getChannelInfo().isSuperOp(u2)) {
			return 1;
		} else if (channel.getChannelInfo().isOp(u2)) {
			return 1;
		} else if (channel.getChannelInfo().isHalfOp(u2)) {
			return 1;
		} else if (channel.getChannelInfo().hasVoice(u2)) {
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