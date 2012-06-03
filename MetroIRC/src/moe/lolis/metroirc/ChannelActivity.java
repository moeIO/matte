package moe.lolis.metroirc;

import java.util.ArrayList;
import java.util.Date;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ChannelActivity extends ListActivity {
	LayoutInflater inflater;
	MessageAdapter adapter;

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

		// Set up list adapter
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		adapter = new MessageAdapter(getApplicationContext(),
				R.layout.channel_message_row, messages);
		setListAdapter(adapter);

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
			View channelList = findViewById(R.id.channelListRow);
			if (channelList.getVisibility() == View.VISIBLE){
				channelList.setVisibility(View.GONE);
			}
			else if (channelList.getVisibility() == View.GONE){
				channelList.setVisibility(View.VISIBLE);
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}