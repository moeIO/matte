package moe.lolis.metroirc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import moe.lolis.metroirc.backend.IRCService;
import moe.lolis.metroirc.backend.ServiceEventListener;
import moe.lolis.metroirc.irc.Channel;
import moe.lolis.metroirc.irc.Client;
import moe.lolis.metroirc.irc.CommandInterpreter;
import moe.lolis.metroirc.irc.GenericMessage;
import moe.lolis.metroirc.irc.Server;
import moe.lolis.metroirc.irc.ServerPreferences;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ChannelInfoEvent;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.SpannedString;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

public class ChannelActivity extends ListActivity implements ServiceEventListener, OnClickListener, OnEditorActionListener, OnChildClickListener,
		OnGroupClickListener {
	// Layout stuff.
	private ChannelActivity activity;
	private LayoutInflater inflater;
	private MessageAdapter adapter;
	private ChannelListAdapter channelAdapter;

	private static boolean isStarted;
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
	private int[] possibleNickColours = { R.color.nickcolor0, R.color.nickcolor1, R.color.nickcolor2, R.color.nickcolor3, R.color.nickcolor4,
			R.color.nickcolor5, R.color.nickcolor6, R.color.nickcolor7, R.color.nickcolor8, R.color.nickcolor9, R.color.nickcolor10,
			R.color.nickcolor11, R.color.nickcolor12, R.color.nickcolor13, R.color.nickcolor14, R.color.nickcolor15 };
	private HashMap<String, Integer> nickColours;

	private static final int CONTEXTMENU_SERVEROPTIONS = 0;
	private static final int CONTEXTMENU_CHANNELOPTIONS = 1;
	private static final int CONTEXTMENU_USERLIST = 2;

	private static final int SERVEROPTIONS_CONNECT = 0;
	private static final int SERVEROPTIONS_DISCONNECT = 1;
	private static final int SERVEROPTIONS_EDIT = 2;
	private static final int SERVEROPTIONS_DELETE = 3;

	private static final int CHANNELOPTIONS_PART = 1;

	private View fakeUserListView;
	private ArrayList<User> contextUserList;

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
		this.fakeUserListView = this.findViewById(R.id.fakeView);
		this.registerForContextMenu(this.fakeUserListView);
		contextUserList = new ArrayList<User>();

		this.nickColours = new HashMap<String, Integer>();
		// Add predefined 'special' nicknames.
		this.nickColours.put("!", this.activity.getResources().getColor(R.color.nickcolor4));
		this.nickColours.put("<--", this.activity.getResources().getColor(R.color.nickcolor5));
		this.nickColours.put("-->", this.activity.getResources().getColor(R.color.nickcolor5));

		this.getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		// Show first run if needed

		new AlertDialog.Builder(this).setView(getLayoutInflater().inflate(R.layout.firstrun, null))
				.setTitle(getResources().getString(R.string.welcome0)).setPositiveButton("Don't tell me again", new Dialog.OnClickListener() {
					public void onClick(DialogInterface d, int which) {
						// Do nothing here.
					}
				}).show();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_channel, menu);
		return true;
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
		case R.id.usersOption:
			fakeUserListView.showContextMenu();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		// Send button clicked
		if (v.getId() == this.sendButton.getId()) {
			this.sendMessage();
		} else if (v.getId() == this.settingsButton.getId()) {
			Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
			this.startActivity(settingsIntent);
		} else if (v.getId() == this.addServerButton.getId()) {
			// Show add server dialog
			this.showServerEditDialog(null);
		} else if (v.getId() == this.quitButton.getId()) {
			this.moeService.stopService();
			this.finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Override back to prevent killing
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (channelList.getVisibility() == View.VISIBLE) {
				channelList.setVisibility(View.GONE);
			} else {
				moveTaskToBack(true);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (channelList.getVisibility() == View.VISIBLE) {
				channelList.setVisibility(View.GONE);
			} else if (channelList.getVisibility() == View.GONE) {
				channelList.setVisibility(View.VISIBLE);
				expandAllServerGroups();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showServerEditDialog(Server existingServer) {
		final Server server = existingServer;
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
				final TextView nameView = (TextView) dialogView.findViewById(R.id.addServer_name);
				final TextView hostView = (TextView) dialogView.findViewById(R.id.addServer_host);
				final TextView portView = (TextView) dialogView.findViewById(R.id.addServer_port);
				final TextView passwordView = (TextView) dialogView.findViewById(R.id.addServer_password);
				final CheckBox ssl = (CheckBox) dialogView.findViewById(R.id.addServer_ssl);
				final CheckBox verifyssl = (CheckBox) dialogView.findViewById(R.id.addServer_verifyssl);
				final TextView nickName = (TextView) dialogView.findViewById(R.id.addServer_nicknames);
				final TextView usernameView = (TextView) dialogView.findViewById(R.id.addServer_username);
				final TextView realnameView = (TextView) dialogView.findViewById(R.id.addServer_realname);
				final TextView autoconnectCommands = (TextView) dialogView.findViewById(R.id.addServer_autoconnectcommands);
				final CheckBox autoconnect = (CheckBox) dialogView.findViewById(R.id.addServer_connectatstartup);
				final CheckBox log = (CheckBox) dialogView.findViewById(R.id.addServer_log);

				String n = null;
				if (server != null)
					n = server.getClient().getServerPreferences().getName();
				final String originalServerName = n;
				if (server != null) {
					ServerPreferences prefs = server.getClient().getServerPreferences();
					nameView.setText(prefs.getName());
					hostView.setText(prefs.getHost().getHostname());
					portView.setText(String.valueOf(prefs.getHost().getPort()));
					passwordView.setText(prefs.getHost().getPassword());
					ssl.setChecked(prefs.getHost().isSSL());
					verifyssl.setChecked(prefs.getHost().verifySSL());
					String nicks = "";
					for (int i = 0; i < prefs.getNicknames().size(); i++) {
						if (i != 0)
							nicks += ",";
						nicks += prefs.getNicknames().get(i);
					}
					nickName.setText(nicks);
					usernameView.setText(prefs.getUsername());
					realnameView.setText(prefs.getRealname());
					String commands = "";
					for (int i = 0; i < prefs.getAutoCommands().size(); i++) {
						if (i != 0)
							commands += "\n";
						commands += prefs.getAutoCommands().get(i);
					}
					autoconnectCommands.setText(commands);
					autoconnect.setChecked(prefs.isAutoConnected());
					log.setChecked(prefs.isLogged());
				}
				b.setOnClickListener(new View.OnClickListener() {

					public void onClick(View view) {
						SharedPreferences rawPreferences = activity.getSharedPreferences("servers", Context.MODE_PRIVATE);
						ServerPreferences prefs = new ServerPreferences();
						ServerPreferences.Host host = prefs.new Host();
						prefs.setHost(host);

						boolean success = true;
						if (nameView.getText().length() == 0) {
							success = false;
							AlertDialog.Builder b = new AlertDialog.Builder(activity);
							b.setMessage("You need to enter a server name");
							b.setPositiveButton("OK", null);
							b.show();
						} else {
							if (!nameView.getText().toString().equals(originalServerName)
									&& ServerPreferences.serverNameExists(rawPreferences, nameView.getText().toString())) {
								success = false;
								AlertDialog.Builder b = new AlertDialog.Builder(activity);
								b.setMessage("A server with this name already exists");
								b.setPositiveButton("OK", null);
								b.show();
							} else
								prefs.setName(nameView.getText().toString());
						}

						if (hostView.getText().length() == 0) {
							host.setHostname("irc.lolipower.org");
						} else {
							host.setHostname(hostView.getText().toString());
						}

						if (portView.getText().length() == 0) {
							host.setPort(6667);
						} else {
							host.setPort(Integer.parseInt(portView.getText().toString()));
						}

						if (passwordView.getText().length() == 0) {
							host.setPassword(null);
						} else {
							host.setPassword(passwordView.getText().toString());
						}

						host.isSSL(ssl.isChecked());
						host.verifySSL(verifyssl.isChecked());

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

						if (usernameView.getText().length() == 0) {
							prefs.setUsername("MetroIRCUser");
						} else {
							prefs.setUsername(usernameView.getText().toString());
						}

						if (realnameView.getText().length() == 0) {
							prefs.setRealname("MetroIRCUser");
						} else {
							prefs.setRealname(realnameView.getText().toString());
						}

						if (autoconnectCommands.getText().length() > 0) {
							ArrayList<String> commands = new ArrayList<String>();
							for (String c : autoconnectCommands.getText().toString().split("\n"))
								commands.add(c);
							prefs.setAutoCommands(commands);
						}

						prefs.isAutoConnected(autoconnect.isChecked());
						prefs.isLogged(log.isChecked());

						if (success) {
							prefs.saveToSharedPreferences(rawPreferences);
							String newName = prefs.getName();

							moeService.renameServer(originalServerName, newName);
							if (server != null && server.getServerInfo().getBot().isConnected()) {
								moeService.disconnect(newName);
							}
							moeService.connect(newName);

							d.dismiss();
						}
					}
				});
			}
		});
		d.show();
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			this.sendMessage();
		}
		if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			this.sendMessage();
			// Explicitly state event is handled.
			return true;
		}
		return false;
	}

	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
		// Force groups to stay expanded
		parent.expandGroup(groupPosition);
		Channel newChannel = moeService.getServers().get(groupPosition);
		setCurrentChannelView(newChannel);
		hideChannelList();
		return true;
	}

	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Channel newChannel = moeService.getServers().get(groupPosition).getChannels().get(childPosition);
		setCurrentChannelView(newChannel);
		hideChannelList();
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v == this.expandableChannelList) {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
			int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			Server server = moeService.getServers().get(groupPosition);
			if (v.getId() == expandableChannelList.getId()) {
				int selectionType = ExpandableListView.getPackedPositionType(info.packedPosition);
				switch (selectionType) {
				case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
					menu.setHeaderTitle("Server Options");
					if (server.getClient().isConnected())
						menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_DISCONNECT, 0, "Disconnect");
					else
						menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_CONNECT, 0, "Connect");
					menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_EDIT, 1, "Edit");
					menu.add(CONTEXTMENU_SERVEROPTIONS, SERVEROPTIONS_DELETE, 2, "Delete");
					break;
				case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
					menu.setHeaderTitle("Channel Options");
					menu.add(CONTEXTMENU_CHANNELOPTIONS, CHANNELOPTIONS_PART, 0, "Part");
				}
			}
		} else if (v == this.getListView()) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			this.startActionMode(new ActionModeCallback(currentChannel.getMessages().get(info.position)));
		} else if (v == this.fakeUserListView) {
			if (currentChannel != null) {
				menu.setHeaderTitle("Users");
				contextUserList.clear();
				org.pircbotx.Channel info = currentChannel.getChannelInfo();
				if (info != null) {
					Iterator<User> i = info.getUsers().iterator();
					int in = 0;
					while (i.hasNext()) {
						User u = i.next();
						contextUserList.add(u);
						menu.add(CONTEXTMENU_USERLIST, 0, in, u.getNick());
						in++;
					}
					// TODO: sort nick list
				}
			}
		}
	}

	private class ActionModeCallback implements ActionMode.Callback {
		private GenericMessage message;

		public ActionModeCallback(GenericMessage m) {
			super();
			message = m;
		}

		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_messagelongpress, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			if (menuItem.getTitle().equals("Copy")) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				String s = message.getContent().toString();
				if (message.getNickname().length() > 0)
					s = message.getNickname() + ": " + s;
				ClipData clip = ClipData.newPlainText("quote", s);
				clipboard.setPrimaryClip(clip);
				actionMode.finish();
			}
			return false;
		}

		public void onDestroyActionMode(ActionMode actionMode) {

			// a.overviewFragment.stopEdit();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int groupPosition;
		int childPosition;
		switch (item.getGroupId()) {
		case CONTEXTMENU_SERVEROPTIONS:
			groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
			Server server = moeService.getServers().get(groupPosition);
			switch (item.getItemId()) {
			case SERVEROPTIONS_DISCONNECT:
				moeService.disconnect(server.getName());
				break;
			case SERVEROPTIONS_CONNECT:
				moeService.connect(server.getName());
				break;
			case SERVEROPTIONS_EDIT:
				this.showServerEditDialog(server);
				break;
			case SERVEROPTIONS_DELETE:
				if (currentChannel.getServer().getName().equals(server.getName())) {
					activity.adapter.setMessages(new ArrayList<GenericMessage>());
					activity.setTitle("");
				}
				moeService.disconnect(server.getName());
				server.getClient().getServerPreferences().deleteFromSharedPreferences(activity.getSharedPreferences("servers", Context.MODE_PRIVATE));
				activity.channelAdapter.notifyDataSetChanged();
				break;
			}
			break;
		case CONTEXTMENU_CHANNELOPTIONS:
			groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
			switch (item.getItemId()) {
			case CHANNELOPTIONS_PART:
				moeService.partChannel(moeService.getServers().get(groupPosition).getChannels().get(childPosition));
				break;
			}
			break;
		case CONTEXTMENU_USERLIST:
			// Do nothing for user click for now
			break;
		}

		return super.onContextItemSelected(item);
	}

	/*
	 * Helper classes.
	 */

	// Adapter that handles the message list
	private class MessageAdapter extends ArrayAdapter<GenericMessage> {

		private ArrayList<GenericMessage> items;

		public MessageAdapter(Context context, int textViewResourceId, ArrayList<GenericMessage> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		public void setMessages(ArrayList<GenericMessage> newList) {
			this.notifyDataSetInvalidated();
			this.items = newList;
			this.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return this.items != null ? items.size() : 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channel_message_row, null);
			}

			GenericMessage message = this.items.get(position);
			TextView name = (TextView) convertView.findViewById(R.id.channelMessageName);
			TextView content = (TextView) convertView.findViewById(R.id.channelMessageContent);
			if (message.isChannelNotificationType())
				content.setTextColor(activity.getResources().getColor(R.color.channelNotification));
			else
				content.setTextColor(activity.getResources().getColor(R.color.channelNormal));
			content.setText(message.getContent());
			if (message.getEmbeddedYoutube() != null) {
				// TODO: Toggle
				if (false) {
					AsyncYoutubeLoad videoPreviewLoader = new AsyncYoutubeLoad();
					videoPreviewLoader.execute(new Object[] { content, message });
				}
			}
			name.setText(message.getNickname());
			name.setTextColor(getNickColour(message.getNickname()));
			if (message.isHighlighted())
				convertView.setBackgroundColor(highlightCellColour);
			else
				convertView.setBackgroundDrawable(null);
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
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.channellist_channel, null);
			}
			Server s = servers.get(groupPosition);
			if (s.getChannels().size() > 0) {
				Channel c = s.getChannels().get(childPosition);
				TextView name = (TextView) convertView.findViewById(R.id.name);
				TextView messages = (TextView) convertView.findViewById(R.id.unreadCount);
				name.setText(c.getChannelInfo().getName());

				if (c.getUnreadMessageCount() > 0) {
					messages.setText("(" + String.valueOf(c.getUnreadMessageCount()) + ")");
				} else {
					messages.setText("");
				}
			}
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
			activity.registerForContextMenu(activity.getListView());
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

			if (!isStarted) {
				isStarted = true;
				// Right, we can't switch to the server tab in the server
				// connection task since due to a race, the task beats this
				// binding and the activity reference is null. Therefore the
				// best we can do is switch to the latest server in the server
				// list at the end of binding being completed
				// ==
				// Works reliably from what I see, and shouldn't cause any
				// issues if binding out-races the connection task.
				if (moeService.getServers().size() > 0)
					activity.channelJoined(moeService.getServers().get(moeService.getServers().size() - 1), null);
				channelAdapter.notifyDataSetChanged();
			}
		}

		// Called when the activity disconnects from the service in error.
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

	public void activeChannelMessageReceived(Channel channel, GenericMessage message) {
		final Channel chan = channel;
		final GenericMessage mess = message;
		// Update the message list
		this.runOnUiThread(new Runnable() {
			public void run() {
				chan.addMessage(mess);
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});
	}

	public void inactiveChannelMessageReceived(Channel channel, GenericMessage message) {
		final Channel chan = channel;
		final GenericMessage mess = message;
		// Update the channel list for unread counts
		this.runOnUiThread(new Runnable() {
			public void run() {
				chan.addMessage(mess);
				if (channelAdapter != null)
					channelAdapter.notifyDataSetChanged();
			}
		});
	}

	// Non-user message, channel notifications etc
	public void statusMessageReceived(Channel channel, GenericMessage message) {
		final Channel chan = channel;
		final GenericMessage mess = message;
		// See above.
		this.runOnUiThread(new Runnable() {
			public void run() {
				chan.addMessage(mess);
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});
	}

	public void channelJoined(Channel channel, String nickname) {
		final Channel chan = channel;
		this.runOnUiThread(new Runnable() {
			public void run() {
				// Expand newest server entry
				expandAllServerGroups();
				if (adapter != null)
					adapter.notifyDataSetChanged();
				setCurrentChannelView(chan);
			}
		});

		// Switch to the new channel when it is joined
		// Accept 0-length nick for time when server is connecting and has no
		// nick
		if (nickname == null || nickname.equals(channel.getServer().getClient().getNick())) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					setCurrentChannelView(chan);
					// Expand newest server entry
					expandAllServerGroups();
				}
			});
		}
	}

	public void channelParted(Channel channel, String nickname) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});

		if (nickname.equals(channel.getServer().getClient().getNick())) {
			// Remove channel and switch to another
			final Channel chan = channel;
			this.runOnUiThread(new Runnable() {
				public void run() {
					int nextPos = moeService.removeChannel(chan);
					if (adapter != null) {
						adapter.notifyDataSetChanged();
						channelAdapter.notifyDataSetChanged();
					}
					if (nextPos > -1)
						activity.setCurrentChannelView(chan.getServer().getChannels().get(nextPos));
					else
						activity.setCurrentChannelView(chan.getServer());
					// TODO remove channel from auto list
				}
			});
		}
	}

	public void networkQuit(Collection<Channel> commonChannels, String nickname) {
		if (commonChannels.contains(this.currentChannel)) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					if (adapter != null)
						adapter.notifyDataSetChanged();
				}
			});
		}
	}

	public void nickChanged(Collection<Channel> commonChannels, String from, String to) {
		if (commonChannels.contains(this.currentChannel)) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
	}

	public void serverConnected(Server server) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (channelAdapter != null) {
					channelAdapter.notifyDataSetChanged();
				}
			}
		});
	}

	public void serverDisconnected(Server server, String error) {
		if (server != null) {
			// Hurr durr I am Java and I require final
			final String err = error;
			final Server serv = server;

			this.runOnUiThread(new Runnable() {
				public void run() {
					serv.addMessage(Server.createError(SpannableString.valueOf(err)));
					for (Channel channel : serv.getChannels()) {
						channel.addMessage(Channel.createError(SpannableString.valueOf(err)));
					}

					if (adapter != null) {
						adapter.notifyDataSetChanged();
						if (channelAdapter != null)
							channelAdapter.notifyDataSetChanged();
					}
				}
			});
		}
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
		this.activity.setTitle(channel.getName());

		// Update the sidebar.
		this.channelAdapter.notifyDataSetChanged();
		// Set list adapter to be the messages of the connected channel,
		// TODO: Re-creating the adapter every time may be inefficient
		if (this.activity.adapter == null) {
			this.activity.adapter = new MessageAdapter(getApplicationContext(), R.layout.channel_message_row, channel.getMessages());
			this.activity.setListAdapter(this.activity.adapter);
		} else
			this.activity.adapter.setMessages(channel.getMessages());

		if (channel instanceof Server)
			activity.sendText.setHint(R.string.server_hint);
		else
			activity.sendText.setHint(R.string.channel_hint);
		// this.activity.setListAdapter(this.activity.adapter);
		// this.activity.adapter.notifyDataSetChanged();
	}

	private void hideChannelList() {
		this.channelList.setVisibility(View.GONE);
	}

	private void sendMessage() {
		if (this.commandInterpreter == null) {
			this.commandInterpreter = new CommandInterpreter(this.moeService, this);
		}

		String message = this.sendText.getText().toString();
		if (this.commandInterpreter.isCommand(message)) {
			this.commandInterpreter.interpret(message);
			this.sendText.setText("");
		} else {
			if (message.length() >= 2 && message.substring(0, 2).equals("//")) {
				message = message.substring(1, message.length());
			}
			if (this.currentChannel != null) {
				if (message.length() > 0) {
					if (this.currentChannel instanceof Server) {
						this.currentChannel.addMessage(Channel.createError(SpannedString
								.valueOf("Can't send messages to a server, it will never respond. :(")));
					} else {
						this.currentChannel.sendMessage(message);
					}
					this.sendText.setText("");
					// Update UI because sent message does not come in as an
					// event
					this.adapter.notifyDataSetChanged();
				}
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

	public void setNickColour(String nick, int colour) {
		this.nickColours.put(nick, colour);
	}

	private int generateNickColour(String nick) {
		int hash = 0;
		for (byte b : nick.getBytes()) {
			hash += b;
		}

		return this.activity.getResources().getColor(this.possibleNickColours[hash % this.possibleNickColours.length]);
	}

}
