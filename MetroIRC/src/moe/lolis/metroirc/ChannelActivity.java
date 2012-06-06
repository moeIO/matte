package moe.lolis.metroirc;

import java.util.ArrayList;

import moe.lolis.metroirc.backend.IRCService;
import moe.lolis.metroirc.backend.ServiceEventListener;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.Server;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView.OnEditorActionListener;

public class ChannelActivity extends ListActivity implements
		ServiceEventListener, OnClickListener, OnEditorActionListener,
		OnChildClickListener, OnGroupClickListener {
	private ChannelActivity activity;
	private LayoutInflater inflater;
	private MessageAdapter adapter;
	private ChannelListAdapter channelAdapter;

	private IRCService moeService;
	private Channel currentChannel;

	private LinearLayout channelList;

	private ImageButton sendButton;
	private EditText sendText;
	private Button settingsButton;
	private Button addServerButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.channel_layout);
		this.activity = this;
		this.inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// Prevent keyboard showing at startup
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Fire up the service. First service bind will be done in onResume()
		// which is called at the start.
		Intent serviceIntent = new Intent(this, IRCService.class);
		this.startService(serviceIntent);

		// Request action bar.
		ActionBar bar = this.getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		this.setTitle("MetroIRC");

		// Set up sidebar,
		ViewStub channelListContainer = (ViewStub) activity
				.findViewById(R.id.channelListStub);
		channelListContainer.inflate();

		sendButton = (ImageButton) this.findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);
		sendText = (EditText) this.findViewById(R.id.sendText);
		sendText.setOnEditorActionListener(this);
		addServerButton = (Button) this.findViewById(R.id.addServerButton);
		addServerButton.setOnClickListener(this);

		this.getListView().setTranscriptMode(
				ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
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
		Intent servIntent = new Intent(this.getApplicationContext(),
				IRCService.class);
		this.bindService(servIntent, serviceConnection,
				Context.BIND_AUTO_CREATE);
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
				convertView = inflater.inflate(R.layout.channel_message_row,
						null);
			}

			ChannelMessage message = items.get(position);
			TextView name = (TextView) convertView
					.findViewById(R.id.channelMessageName);
			TextView content = (TextView) convertView
					.findViewById(R.id.channelMessageContent);
			content.setText(message.getContent());
			name.setText(message.getNickname());

			return convertView;
		}
	}

	/*
	 * Sidebar
	 */
	// Adapter that handles the message list
	private class ChannelListAdapter extends BaseExpandableListAdapter {

		private ArrayList<Server> servers;

		public ChannelListAdapter(ArrayList<Server> servers) {
			super();
			this.servers = servers;
		}

		public Object getChild(int groupPos, int childPos) {
			return servers.get(groupPos).getChannels().get(childPos);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Channel c = servers.get(groupPosition).getChannels()
					.get(childPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channellist_channel,
						null);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			name.setText(c.getChannelInfo().getName());
			return convertView;
		}

		public int getChildrenCount(int groupPosition) {
			return servers.get(groupPosition).getChannels().size();
		}

		public Object getGroup(int groupPosition) {
			return servers.get(groupPosition);
		}

		public int getGroupCount() {
			return servers.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			Server s = servers.get(groupPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channellist_server,
						null);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			name.setText(s.getServerInfo().getNetwork());
			return convertView;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		// Force groups to stay expanded
		parent.expandGroup(groupPosition);
		return true;
	}

	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		Channel newChannel = moeService.getServers().get(groupPosition)
				.getChannels().get(childPosition);
		setCurrentChannelView(newChannel);
		hideChannelList();
		return false;
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		// Called when activity connects to the service.
		public void onServiceConnected(ComponentName className, IBinder service) {
			moeService = ((IRCService.IRCBinder) service).getService();
			moeService.connectedEventListener = activity;
			activity.serviceConnected();

			activity.channelAdapter = new ChannelListAdapter(
					moeService.getServers());

			// Set adapter of newly inflated container
			LinearLayout channelListPanel = (LinearLayout) activity
					.findViewById(R.id.channelListPanel);
			ExpandableListView expandableChannelList = (ExpandableListView) channelListPanel
					.findViewById(android.R.id.list);
			expandableChannelList.setAdapter(activity.channelAdapter);
			expandableChannelList.setOnChildClickListener(activity);
			expandableChannelList.setOnGroupClickListener(activity);
			channelList = (LinearLayout) activity
					.findViewById(R.id.channelList);
			// And hide it by default.
			hideChannelList();

			settingsButton = (Button) activity
					.findViewById(R.id.settingsButton);
			settingsButton.setOnClickListener(activity);
		}

		// Called when the activity disconnects from the service.
		public void onServiceDisconnected(ComponentName className) {
			moeService = null;
		}
	};

	public void serviceConnected() {

	}

	private void hideChannelList() {
		channelList.setVisibility(View.GONE);
	}

	public void messageReceived(Channel channel) {
		// Update the message list
		this.runOnUiThread(new Runnable() {
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

	private void setCurrentChannelView(Channel channel) {
		currentChannel = channel;
		// Update the sidebar
		channelAdapter.notifyDataSetChanged();
		activity.setTitle(channel.getChannelInfo().getName());
		// Set list adapter to be the messages of the connected channel,
		// TODO: Re-creating the adapter every time may be inefficient
		activity.adapter = new MessageAdapter(getApplicationContext(),
				R.layout.channel_message_row, channel.getMesages());
		activity.setListAdapter(activity.adapter);
	}

	// Switch to the new channel when it is joined
	public void channelJoined(Channel channel) {
		final Channel chan = channel;
		this.runOnUiThread(new Runnable() {
			public void run() {
				setCurrentChannelView(chan);
				// Expand newest server entry
				int count = channelAdapter.getGroupCount();
				for (int i = 0; i < count; i++)
					((ExpandableListView) channelList
							.findViewById(android.R.id.list)).expandGroup(i);
			}
		});
	}

	public void onClick(View v) {
		// Send button clicked
		if (v.getId() == sendButton.getId()) {
			this.sendMessage();
		} else if (v.getId() == settingsButton.getId()) {
			Intent settingsIntent = new Intent(getApplicationContext(),
					Preferences.class);
			this.startActivity(settingsIntent);
		} else if (v.getId() == addServerButton.getId()) {

		}
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			this.sendMessage();
		}
		return false;
	}

	private void sendMessage() {
		if (currentChannel != null) {
			if (sendText.getText().length() > 0) {
				currentChannel.sendMessage(sendText.getText().toString());
				sendText.setText("");
				// Update UI because sent message does not come in as an
				// event
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}