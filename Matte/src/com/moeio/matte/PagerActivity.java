package com.moeio.matte;

import java.util.Collection;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.moeio.matte.backend.IRCService;
import com.moeio.matte.backend.ServiceEventListener;
import com.moeio.matte.irc.Channel;
import com.moeio.matte.irc.CommandInterpreter;
import com.moeio.matte.irc.GenericMessage;
import com.moeio.matte.irc.Query;
import com.moeio.matte.irc.Server;
import com.viewpagerindicator.TitlePageIndicator;

public class PagerActivity extends FragmentActivity implements OnClickListener,
		OnEditorActionListener, IRCActivity, ServiceEventListener {
	private PagerActivity activity;
	// Layout elements
	private FragmentPageAdapter mAdapter;
	private ViewPager mPager;
	private ImageButton sendButton;
	private EditText sendText;
	private TitlePageIndicator titleIndicator;
	// IRC back-end.
	private IRCService moeService;
	private CommandInterpreter commandInterpreter;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		// Called when activity connects to the service.
		public void onServiceConnected(ComponentName className, IBinder service) {
			moeService = ((IRCService.IRCBinder) service).getService();
			moeService.connectedEventListener = activity;
			if (moeService != null)
				moeService.isAppActive(true);

			mAdapter = new FragmentPageAdapter(getSupportFragmentManager(),
					moeService);

			mPager = (ViewPager) findViewById(R.id.pager);
			mPager.setAdapter(mAdapter);
			// Bind the title indicator to the adapter
			titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
			titleIndicator.setViewPager(mPager);
			titleIndicator.setTextColor(0xAA000000);
			titleIndicator.setSelectedColor(0xFF000000);
		}

		public void onServiceDisconnected(ComponentName arg0) {
			if (moeService != null)
				moeService.isAppActive(false);
			moeService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		Intent serviceIntent = new Intent(this, IRCService.class);
		this.startService(serviceIntent);

		// Set up UI
		setContentView(R.layout.pager_layout);
		this.sendButton = (ImageButton) this.findViewById(R.id.sendButton);
		this.sendButton.setOnClickListener(this);
		this.sendText = (EditText) this.findViewById(R.id.sendText);
		this.sendText.setOnEditorActionListener(this);
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
		Intent servIntent = new Intent(this.getApplicationContext(),
				IRCService.class);
		this.bindService(servIntent, this.serviceConnection,
				Context.BIND_AUTO_CREATE);
		if (titleIndicator != null)
			titleIndicator.notifyDataSetChanged();
		super.onResume();
	}

	public void onClick(View v) {
		// Send button clicked
		if (v.getId() == this.sendButton.getId()) {
			this.sendMessage();
		}
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			this.sendMessage();
		}
		if (actionId == EditorInfo.IME_NULL
				&& event.getAction() == KeyEvent.ACTION_DOWN
				&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			this.sendMessage();
			// Explicitly state event is handled.
			return true;
		}
		return false;
	}

	private void sendMessage() {
		if (this.commandInterpreter == null) {
			this.commandInterpreter = new CommandInterpreter(this.moeService,
					this);
		}

		String message = this.sendText.getText().toString();
		if (!this.commandInterpreter.isCommand(message)) {
			if (message.length() >= 2 && message.substring(0, 2).equals("//")) {
				message = message.substring(1, message.length());
			}
			message = "/msg " + message;
		}
		this.commandInterpreter.interpret(message);
		// Update UI because sent message does not come in as an
		// event
		this.mAdapter.notifyDataSetChanged();
		this.sendText.setText("");
	}

	private void updateCurrentFragment() {
		this.mAdapter.updateFragment(this.mPager.getCurrentItem());
	}

	public Channel getCurrentChannel() {
		return mAdapter.getChannel(mPager.getCurrentItem());
	}

	public void channelMessageReceived(Channel channel, GenericMessage message,
			boolean active) {
		final Channel chan = channel;
		final GenericMessage mess = message;
		final boolean act = active;
		this.runOnUiThread(new Runnable() {
			public void run() {
				chan.addMessage(mess);
				if (act) {
					activity.updateCurrentFragment();
				} else {
					if (titleIndicator != null)
						titleIndicator.notifyDataSetChanged();
					// TODO: Notify channel list that unread count has changed
				}
			}
		});

	}

	public void queryMessageReceived(Query query, GenericMessage message,
			boolean active) {
		final Query q = query;
		final GenericMessage m = message;
		final boolean a = active;
		// Update the message list
		this.runOnUiThread(new Runnable() {
			public void run() {
				q.addMessage(m);
				if (a) {
					activity.updateCurrentFragment();
				} else if (!a) {
					// TODO: Notify channel list that unread count has changed
				}
			}
		});

	}

	public void statusMessageReceived(Channel channel, GenericMessage message) {
		final Channel chan = channel;
		final GenericMessage mess = message;
		// See above.
		this.runOnUiThread(new Runnable() {
			public void run() {
				chan.addMessage(mess);
				activity.updateCurrentFragment();
			}
		});

	}

	public void nickChanged(Collection<Channel> commonChannels, String from,
			String to) {
		if (commonChannels.contains(this.getCurrentChannel())) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					activity.updateCurrentFragment();
				}
			});
		}

	}

	public void channelJoined(Channel channel, String nickname) {
		final Channel chan = channel;
		this.runOnUiThread(new Runnable() {
			public void run() {
				// Expand newest server entry
				// TODO:
				// expandAllServerGroups();
				activity.updateCurrentFragment();
				// setCurrentChannelView(chan);
			}
		});

		// Switch to the new channel when it is joined
		// Accept 0-length nick for time when server is connecting and has no
		// nick
		if (nickname == null
				|| nickname.equals(channel.getServer().getClient().getNick())) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					// TODO:
					// setCurrentChannelView(chan);
					// Expand newest server entry
					// expandAllServerGroups();
				}
			});
		}

	}

	public void channelParted(Channel channel, String nickname) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				activity.updateCurrentFragment();
			}
		});

		if (nickname.equals(channel.getServer().getClient().getNick())) {
			// Remove channel and switch to another
			final Channel chan = channel;
			this.runOnUiThread(new Runnable() {
				public void run() {
					int nextPos = moeService.removeChannel(chan);
					activity.updateCurrentFragment();
					// channelAdapter.notifyDataSetChanged();
					// TODO:
					/*
					 * if (nextPos > -1)
					 * activity.setCurrentChannelView(chan.getServer()
					 * .getChannels().get(nextPos)); else
					 * activity.setCurrentChannelView(chan.getServer());
					 */
				}
			});
		}
	}

	public void networkQuit(Collection<Channel> commonChannels, String nickname) {
		if (commonChannels.contains(this.getCurrentChannel())) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					activity.updateCurrentFragment();
				}
			});
		}
	}

	public void serverConnected(Server server) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				// TODO update channel listing
				/*
				 * if (channelAdapter != null) {
				 * channelAdapter.notifyDataSetChanged(); }
				 */
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
					GenericMessage e = Server.createError(SpannableString
							.valueOf(err));
					serv.addMessage(e);
					for (Channel channel : serv.getChannels()) {
						channel.addMessage(e);
					}

					activity.updateCurrentFragment();
					// TODO update channel listing
					/*
					 * if (channelAdapter != null)
					 * channelAdapter.notifyDataSetChanged();
					 * 
					 * }
					 */
				}
			});
		}

	}
}
