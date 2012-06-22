package com.moeio.matte.irc;

import java.nio.charset.Charset;
import java.util.HashMap;

import android.content.Context;

public class ClientManager {
	protected HashMap<String, Client> clients;
	private Context context;

	public ClientManager(Context context) {
		this.context = context;
		this.clients = new HashMap<String, Client>();
	}

	public Client createClient(ServerPreferences preferences) {
		Client client = new Client(context);
		client.loadFromPreferences(preferences);
		client.setAutoNickChange(true);
		client.setAutoSplitMessage(true);
		client.setEncoding(Charset.forName("UTF-8"));
		client.setMessageDelay(250);
		client.setSocketTimeout(5000);

		this.clients.put(preferences.getName(), client);
		return client;
	}

	public void addClient(String name, Client client) {
		this.clients.put(name, client);
	}

	public Client getClient(String name) {
		return this.clients.get(name);
	}

	public void removeClient(String name) {
		Client client = this.clients.get(name);
		client.disconnect();
		this.clients.remove(name);
	}

}
