package moe.lolis.metroirc;

import java.util.ArrayList;
import java.util.Date;

import moe.lolis.metroirc.ChannelListEntry.Type;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChannelActivity extends ListActivity {
	LayoutInflater inflater;
	MessageAdapter adapter;
	ChannelListAdapter channelAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel_layout);
		// Request action bar (3.0+)
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		setTitle("#coolchannel");
		// Temporary fake list
		ArrayList<ChannelMessage> messages = new ArrayList<ChannelMessage>();
		ChannelMessage m = new ChannelMessage();
		m.time = new Date();
		m.text = "The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog..";
		messages.add(m);
		m = new ChannelMessage();
		m.time = new Date();
		m.text = "Blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah";
		messages.add(m);
		m = new ChannelMessage();
		m.time = new Date();
		m.text = "Guy is the coolest guy around etc etc different sized words etc different sized words etc different sized words.";
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);
		messages.add(m);

		// Set up list adapter
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		adapter = new MessageAdapter(getApplicationContext(),
				R.layout.channel_message_row, messages);
		setListAdapter(adapter);

		// Set up sidebar
		ViewStub channelListContainer = (ViewStub) findViewById(R.id.channelListStub);
		channelListContainer.inflate();

		// Some fake data
		ArrayList<ChannelListEntry> channels = new ArrayList<ChannelListEntry>();
		ChannelListEntry serv = new ChannelListEntry();
		serv.type = Type.Server;
		serv.name = "!Rizon";
		channels.add(serv);
		ChannelListEntry chan = new ChannelListEntry();
		chan.type = Type.Channel;
		chan.name = "#coolchannel";
		channels.add(chan);
		channelAdapter = new ChannelListAdapter(getApplicationContext(),
				R.layout.channel_message_row, channels);
		//Set adapter of newly inflated container
		LinearLayout channelList = (LinearLayout)findViewById(R.id.channelListPanel);
		((ListView) channelList.findViewById(android.R.id.list))
				.setAdapter(channelAdapter);
		//Hide that shit
		findViewById(R.id.channelList).setVisibility(View.GONE);
		
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
				convertView = inflater.inflate(R.layout.channel_message_row,
						null);
			}
			ChannelMessage message = items.get(position);
			TextView name = (TextView) convertView
					.findViewById(R.id.channelMessageName);
			TextView content = (TextView) convertView
					.findViewById(R.id.channelMessageContent);

			content.setText(message.text);

			return convertView;
		}
	}

	// Action bar button pressed
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Home button pressed in action bar
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
				if (entry.type == Type.Server) {
					convertView = inflater.inflate(R.layout.channellist_server,
							null);
				} else if (entry.type == Type.Channel) {
					convertView = inflater.inflate(
							R.layout.channellist_channel, null);
				}
			}

			TextView name = (TextView) convertView
					.findViewById(R.id.name);

			name.setText(entry.name);

			return convertView;
		}
	}

}