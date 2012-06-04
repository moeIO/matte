package moe.lolis.metroirc;

import java.util.ArrayList;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import moe.lolis.metroirc.backend.IRCService;
import moe.lolis.metroirc.backend.ServiceEventListener;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;

public class ChannelActivity extends ListActivity implements
		ServiceEventListener {
	private ChannelActivity activity;
	private LayoutInflater inflater;
	private MessageAdapter adapter;
	private ChannelListAdapter channelAdapter;

	private IRCService moeService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.channel_layout);
		this.activity = this;
		
		// Fire up the service. First service bind will be done in onResume() 
		// which is called at the start.
		Intent serviceIntent = new Intent(this, IRCService.class);
		this.startService(serviceIntent);

		// Request action bar.
		ActionBar bar = this.getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		this.setTitle("#coolchannel");

		// Set up sidebar,
		ViewStub channelListContainer = (ViewStub) this.findViewById(R.id.channelListStub);
		channelListContainer.inflate();

		// Some fake data
		ArrayList<ChannelListEntry> channels = new ArrayList<ChannelListEntry>();
		ChannelListEntry serv = new ChannelListEntry();
		serv.type = ChannelListEntry.Type.Server;
		serv.name = "!Rizon";
		channels.add(serv);
		ChannelListEntry chan = new ChannelListEntry();
		chan.type = ChannelListEntry.Type.Channel;
		chan.name = "#coolchannel";
		channels.add(chan);
		this.channelAdapter = new ChannelListAdapter(getApplicationContext(), R.layout.channel_message_row, channels);

		// Set adapter of newly inflated container
		LinearLayout channelList = (LinearLayout) this.findViewById(R.id.channelListPanel);
		((ListView) channelList.findViewById(android.R.id.list)).setAdapter(this.channelAdapter);
		// And hide it by default.
		this.findViewById(R.id.channelList).setVisibility(View.GONE);
	}

	// When our activity is paused.
	public void onPause() {
		// Unbind the service.
		this.unbindService(serviceConnection);
		super.onPause();
	};

	// When our activity is resumed.
	public void onResume() {
		// Bind the service.
		Intent servIntent = new Intent(this.getApplicationContext(), IRCService.class);
		this.bindService(servIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}

	// Action bar button pressed
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Home button pressed in action bar, show channel list!
			View channelList = findViewById(R.id.channelList);
			if (channelList.getVisibility() == View.VISIBLE) {
				channelList.setVisibility(View.GONE);
			} else if (channelList.getVisibility() == View.GONE) {
				channelList.setVisibility(View.VISIBLE);
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Adapter that handles the message list
	private class MessageAdapter extends ArrayAdapter<ChannelMessage> {

		private ArrayList<ChannelMessage> items;

		public MessageAdapter(Context context, int textViewResourceId,
				ArrayList<ChannelMessage> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channel_message_row, null);
			}

			ChannelMessage message = items.get(position);
			TextView name = (TextView) convertView.findViewById(R.id.channelMessageName);
			TextView content = (TextView) convertView.findViewById(R.id.channelMessageContent);
			content.setText(message.getContent());

			return convertView;
		}
	}
	
	/*
	 * Sidebar
	 */
	// Adapter that handles the message list
	private class ChannelListAdapter extends ArrayAdapter<ChannelListEntry> {

		private ArrayList<ChannelListEntry> items;

		public ChannelListAdapter(Context context, int textViewResourceId,
				ArrayList<ChannelListEntry> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ChannelListEntry entry = items.get(position);
			if (convertView == null) {
				if (entry.type == ChannelListEntry.Type.Server) {
					convertView = inflater.inflate(R.layout.channellist_server, null);
				} else if (entry.type == ChannelListEntry.Type.Channel) {
					convertView = inflater.inflate(R.layout.channellist_channel, null);
				}
			}

			TextView name = (TextView) convertView.findViewById(R.id.name);
			name.setText(entry.name);

			return convertView;
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		// Called when activity connects to the service.
		public void onServiceConnected(ComponentName className, IBinder service) {
			moeService = ((IRCService.IRCBinder) service).getService();
			moeService.connectedEventListener = activity;
			activity.serviceConnected();
		}

		// Called when the activity disconnects from the service.
		public void onServiceDisconnected(ComponentName className) {
			moeService = null;
		}
	};

	public void serviceConnected() {
		// Set up list adapter,
		this.inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.adapter = new MessageAdapter(getApplicationContext(), R.layout.channel_message_row, moeService.channel.messages);
		this.setListAdapter(this.adapter);
	}

	public void messageReceived(Channel channel) {
		// Update the message list
		this.runOnUiThread(new Runnable() {
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

}