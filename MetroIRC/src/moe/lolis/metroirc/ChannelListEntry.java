package moe.lolis.metroirc;

public class ChannelListEntry {
	enum Type{
		Server,Channel
	}
	Type type;
	String name;
}
