package moe.lolis.metroirc;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import moe.lolis.metroirc.backend.IRCService;
import moe.lolis.metroirc.backend.ServiceEventListener;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.ChannelMessage;
import moe.lolis.metroirc.irc.CommandInterpreter;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerPreferences;

public class ChannelActivity extends ListActivity implements ServiceEventListener, OnClickListener, OnEditorActionListener, OnChildClickListener,
		OnGroupClickListener {
	// Layout stuff.
	private ChannelActivity activity;
	private LayoutInflater inflater;
	private MessageAdapter adapter;
	private ChannelListAdapter channelAdapter;

	// IRC backend.
	private IRCService moeService;
	private CommandInterpreter commandInterpreter;
	private Channel currentChannel;

	// Layout elements.
	private LinearLayout channelList;
	private ExpandableListView expandableChannelList;
	private ImageButton sendButton;
	private EditText sendText;
	private Button settingsButton;
	private Button addServerButton;
	private Button quitButton;
	private int highlightCellColour;

	// Misc.
	private boolean gotoChannelOnServiceConnect;
	private String onServiceConnectChannel;
	private String onServiceConnectServer;
	private int[] possibleNickColours = {};
	private HashMap<String, Integer> nickColours;

	/*
	 * UI callbacks.
	 */

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.channel_layout);
		this.activity = this;
		this.inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// Prevent keyboard showing at startup
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Fire up the service. First service bind will be done in onResume()
		// which is called at the start.
		Intent serviceIntent = new Intent(this, IRCService.class);
		this.startService(serviceIntent);

		// Request action bar.
		ActionBar bar = this.getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		this.setTitle("MetroIRC");

		// Set up sidebar.
		ViewStub channelListContainer = (ViewStub) this.activity.findViewById(R.id.channelListStub);
		channelListContainer.inflate();

		// Set up main UI.
		this.sendButton = (ImageButton) this.findViewById(R.id.sendButton);
		this.sendButton.setOnClickListener(this);
		this.sendText = (EditText) this.findViewById(R.id.sendText);
		this.sendText.setOnEditorActionListener(this);
		this.addServerButton = (Button) this.findViewById(R.id.addServerButton);
		this.addServerButton.setOnClickListener(this);
		this.quitButton = (Button) this.findViewById(R.id.quitButton);
		this.quitButton.setOnClickListener(this);
		this.highlightCellColour = Color.rgb(182, 232, 243);

		this.getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

	}

	// When our activity is paused.
	public void onPause() {
		if (this.moeService != null)
			this.moeService.isAppActive(false);
		// Unbind the service.
		this.unbindService(this.serviceConnection);
		super.onPause();
	};

	// When our activity is resumed.
	public void onResume() {
		// Remove mentions/notifications
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		// Bind the service.
		Intent servIntent = new Intent(this.getApplicationContext(), IRCService.class);
		this.bindService(servIntent, this.serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}

	@Override
	public void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			String server = extras.getString("server");
			String channel = extras.getString("channel");
			if (server != null && channel != null) {
				// Set as values, for the goto to occur when the service
				// connects (shortly after onNewIntent in onResume)
				this.onServiceConnectChannel = channel;
				this.onServiceConnectServer = server;
				this.gotoChannelOnServiceConnect = true;
			}
		}
		super.onNewIntent(intent);
	}

	@Override
	public void onDestroy() {
		// Make current channel inactive.
		this.currentChannel.isActive(false);
		super.onDestroy();
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
				// In case returning to activity
				expandAllServerGroups();
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		// Send button clicked
		if (v.getId() == this.sendButton.getId()) {
			if (this.commandInterpreter == null) {
				this.commandInterpreter = new CommandInterpreter(this.moeService, this);
			}

			if (this.commandInterpreter.isCommand(this.sendText.getText().toString())) {
				this.commandInterpreter.interpret(this.sendText.getText().toString());
			} else {
				this.sendMessage();
			}
		} else if (v.getId() == this.settingsButton.getId()) {
			Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
			this.startActivity(settingsIntent);
		} else if (v.getId() == this.addServerButton.getId()) {
			// Show add server dialog
			final View dialogView = getLayoutInflater().inflate(R.layout.addserver_dialog, null);
			final AlertDialog d = new AlertDialog.Builder(this).setView(dialogView).setTitle("Add Server")
					.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
						public void onClick(DialogInterface d, int which) {
							// Do nothing here.
						}
					}).setNegativeButton(android.R.string.cancel, null).create();
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				public void onShow(DialogInterface dialog) {
					Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
					b.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							SharedPreferences rawPreferences = activity.getSharedPreferences("servers", Context.MODE_PRIVATE);
							ServerPreferences prefs = new ServerPreferences();
							ServerPreferences.Host host = prefs.new Host();
							prefs.setHost(host);

							boolean success = true;

							TextView nameView = (TextView) dialogView.findViewById(R.id.addServer_name);
							if (nameView.getText().length() == 0) {
								success = false;
								AlertDialog.Builder b = new AlertDialog.Builder(activity);
								b.setMessage("You need to enter a server name");
								b.setPositiveButton("OK", null);
								b.show();
							} else {
								if (ServerPreferences.serverNameExists(rawPreferences, nameView.getText().toString())) {
									success = false;
									AlertDialog.Builder b = new AlertDialog.Builder(activity);
									b.setMessage("A server with this name already exists");
									b.setPositiveButton("OK", null);
									b.show();
								} else
									prefs.setName(nameView.getText().toString());
							}

							TextView hostView = (TextView) dialogView.findViewById(R.id.addServer_host);
							if (hostView.getText().length() == 0) {
								host.setHostname("irc.lolipower.org");
							} else {
								host.setHostname(hostView.getText().toString());
							}

							TextView portView = (TextView) dialogView.findViewById(R.id.addServer_port);
							if (portView.getText().length() == 0) {
								host.setPort(6667);
							} else {
								host.setPort(Integer.parseInt(hostView.getText().toString()));
							}

							TextView passwordView = (TextView) dialogView.findViewById(R.id.addServer_password);
							if (passwordView.getText().length() == 0) {
								host.setPassword(null);
							} else {
								host.setPassword(passwordView.getText().toString());
							}

							CheckBox ssl = (CheckBox) dialogView.findViewById(R.id.addServer_ssl);
							host.isSSL(ssl.isChecked());

							CheckBox verifyssl = (CheckBox) dialogView.findViewById(R.id.addServer_verifyssl);
							host.isSSL(verifyssl.isChecked());

							TextView nickName = (TextView) dialogView.findViewById(R.id.addServer_nicknames);
							if (nickName.getText().length() == 0) {
								success = false;
								AlertDialog.Builder b = new AlertDialog.Builder(activity);
								b.setMessage("You need a nickname silly :(");
								b.setPositiveButton("OK", null);
								b.show();
							} else {
								ArrayList<String> nicks = new ArrayList<String>();
								for (String s : nickName.getText().toString().split(","))
									nicks.add(s);
								prefs.setNicknames(nicks);
							}

							TextView usernameView = (TextView) dialogView.findViewById(R.id.addServer_username);
							if (usernameView.getText().length() == 0) {
								prefs.setUsername("MetroIRCUser");
							} else {
								prefs.setUsername(usernameView.getText().toString());
							}

							TextView realnameView = (TextView) dialogView.findViewById(R.id.addServer_realname);
							if (realnameView.getText().length() == 0) {
								prefs.setRealname("MetroIRCUser");
							} else {
								prefs.setRealname(realnameView.getText().toString());
							}

							TextView autoconnectCommands = (TextView) dialogView.findViewById(R.id.addServer_autoconnectcommands);
							if (autoconnectCommands.getText().length() == 0) {
								prefs.setAutoCommands(null);
							} else {
								ArrayList<String> commands = new ArrayList<String>();
								for (String c : autoconnectCommands.getText().toString().split("\n"))
									commands.add(c);
								prefs.setAutoCommands(commands);
							}

							CheckBox autoconnect = (CheckBox) dialogView.findViewById(R.id.addServer_connectatstartup);
							prefs.isAutoConnected(autoconnect.isChecked());

							CheckBox log = (CheckBox) dialogView.findViewById(R.id.addServer_log);
							prefs.isLogged(log.isChecked());

							if (success) {
								prefs.saveToSharedPreferences(rawPreferences);
								moeService.connect(prefs);
								d.dismiss();
							}
						}
					});
				}
			});
			d.show();
		} else if (v.getId() == this.quitButton.getId()) {
			this.moeService.stopService();
			this.finish();
		}
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			this.sendMessage();
		}
		return false;
	}

	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
		// Force groups to stay expanded
		parent.expandGroup(groupPosition);
		return true;
	}

	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Channel newChannel = moeService.getServers().get(groupPosition).getChannels().get(childPosition);
		setCurrentChannelView(newChannel);
		hideChannelList();
		return false;
	}

	/*
	 * Helper classes.
	 */

	// Adapter that handles the message list
	private class MessageAdapter extends ArrayAdapter<ChannelMessage> {

		private ArrayList<ChannelMessage> items;

		public MessageAdapter(Context context, int textViewResourceId, ArrayList<ChannelMessage> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channel_message_row, null);
			}

			ChannelMessage message = this.items.get(position);
			TextView name = (TextView) convertView.findViewById(R.id.channelMessageName);
			TextView content = (TextView) convertView.findViewById(R.id.channelMessageContent);
			content.setText(message.getContent());
			name.setText(message.getNickname());
			// TODO: Set colour here.

			if (message.isHighlighted())
				convertView.setBackgroundColor(highlightCellColour);
			else
				convertView.setBackgroundColor(Color.TRANSPARENT);
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

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			Channel c = servers.get(groupPosition).getChannels().get(childPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channellist_channel, null);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			String text = c.getChannelInfo().getName();
			if (c.getUnreadMessageCount() > 0)
				text += "(" + String.valueOf(c.getUnreadMessageCount()) + ")";
			name.setText(text);
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

		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			Server s = servers.get(groupPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channellist_server, null);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			name.setText(s.getName());
			return convertView;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		// Called when activity connects to the service.
		public void onServiceConnected(ComponentName className, IBinder service) {
			moeService = ((IRCService.IRCBinder) service).getService();
			moeService.connectedEventListener = activity;
			activity.serviceConnected();
			if (moeService != null)
				moeService.isAppActive(true);

			activity.channelAdapter = new ChannelListAdapter(moeService.getServers());

			// Set adapter of newly inflated container
			LinearLayout channelListPanel = (LinearLayout) activity.findViewById(R.id.channelListPanel);
			expandableChannelList = (ExpandableListView) channelListPanel.findViewById(android.R.id.list);
			expandableChannelList.setAdapter(activity.channelAdapter);
			expandableChannelList.setOnChildClickListener(activity);
			expandableChannelList.setOnGroupClickListener(activity);
			activity.registerForContextMenu(expandableChannelList);
			channelList = (LinearLayout) activity.findViewById(R.id.channelList);
			// And hide it by default.
			hideChannelList();

			settingsButton = (Button) activity.findViewById(R.id.settingsButton);
			settingsButton.setOnClickListener(activity);

			// Goto certain channel
			if (gotoChannelOnServiceConnect) {
				gotoChannelOnServiceConnect = false;
				activity.setCurrentChannelView(moeService.getServer(onServiceConnectServer).getChannel(onServiceConnectChannel));
				onServiceConnectChannel = null;
				onServiceConnectServer = null;
			}
		}

		// Called when the activity disconnects from the service.
		public void onServiceDisconnected(ComponentName className) {
			if (moeService != null)
				moeService.isAppActive(false);
			moeService = null;
		}
	};

	public void serviceConnected() {

	}

	/*
	 * IRC service callbacks.
	 */

	public void activeChannelMessageReceived(Channel channel) {
		// Update the message list
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});
	}

	public void inactiveChannelMessageReceived(Channel channel) {
		// Update the channel list for unread counts
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (channelAdapter != null)
					channelAdapter.notifyDataSetChanged();
			}
		});
	}

	public void messageReceived(Server server) {
		// See above.
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});
	}

	// Switch to the new channel when it is joined
	public void channelJoined(Channel channel) {
		final Channel chan = channel;
		this.runOnUiThread(new Runnable() {
			public void run() {
				setCurrentChannelView(chan);
				// Expand newest server entry
				expandAllServerGroups();
			}
		});
	}

	/*
	 * UI helpers.
	 */

	private void expandAllServerGroups() {
		int count = this.channelAdapter.getGroupCount();
		for (int i = 0; i < count; i++)
			this.expandableChannelList.expandGroup(i);
	}

	private void setCurrentChannelView(Channel channel) {
		if (this.currentChannel != null)
			this.currentChannel.isActive(false);
		channel.isActive(true);
		this.currentChannel = channel;

		// Update the sidebar.
		this.channelAdapter.notifyDataSetChanged();
		this.activity.setTitle(channel.getChannelInfo().getName());
		// Set list adapter to be the messages of the connected channel,
		// TODO: Re-creating the adapter every time may be inefficient
		this.activity.adapter = new MessageAdapter(getApplicationContext(), R.layout.channel_message_row, channel.getMessages());
		this.activity.setListAdapter(this.activity.adapter);
	}

	private void hideChannelList() {
		this.channelList.setVisibility(View.GONE);
	}

	private void sendMessage() {
		if (this.currentChannel != null) {
			if (this.sendText.getText().length() > 0) {
				this.currentChannel.sendMessage(this.sendText.getText().toString());
				this.sendText.setText("");
				// Update UI because sent message does not come in as an
				// event
				this.adapter.notifyDataSetChanged();
			}
		}
	}

	/*
	 * Getters/setters.
	 */
	public Channel getCurrentChannel() {
		return this.currentChannel;
	}

	public int getNickColour(String nick) {
		if (!this.nickColours.containsKey(nick)) {
			int colour = this.generateNickColour(nick);
			this.nickColours.put(nick, colour);
			return colour;
		}
		return this.nickColours.get(nick);
	}

	private static final int CONTEXTMENU_SERVEROPTIONS = 0;

	private static final int SERVEROPTIONS_EDIT = 0;
	private static final int SERVEROPTIONS_DELETE = 1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		if (v.getId() == expandableChannelList.getId()) {
			int selectionType = ExpandableListView.getPackedPositionType(info.packedPosition);
			switch (selectionType) {
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				menu.setHeaderTitle("Server Options");
				menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_EDIT, 0, "Edit");
				menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_DELETE, 1, "Delete");
				break;
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		switch (item.getGroupId()) {
		case CONTEXTMENU_SERVEROPTIONS:
			int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Server server = moeService.getServers().get(groupPosition);
			switch (item.getItemId()) {
			case SERVEROPTIONS_EDIT:
				break;
			case SERVEROPTIONS_DELETE:
				moeService.disconnect(server.getName());
				server.getClient().getServerPreferences().deleteFromSharedPrefereces(activity.getSharedPreferences("servers", Context.MODE_PRIVATE));
				break;
			}
			break;
		}
		return super.onContextItemSelected(item);
	}

	public void setNickColour(String nick, int colour) {
		this.nickColours.put(nick, colour);
	}

	private int generateNickColour(String nick) {
		int hash = nick.hashCode();
		return this.possibleNickColours[hash % this.possibleNickColours.length];
	}

}
