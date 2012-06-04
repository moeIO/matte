package moe.lolis.metroirc;

public class ChannelListEntry {
	public enum Type {
		Server,
		Channel
	}
	
	public Type type;
	public String name;
}
