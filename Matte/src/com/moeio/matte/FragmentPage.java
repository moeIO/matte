package com.moeio.matte;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moeio.matte.backend.IRCService;
import com.moeio.matte.irc.Channel;

public class FragmentPage extends ListFragment {
	private Channel channel;
	private IRCService service;

	static FragmentPage newInstance(int num, Channel channel, IRCService service) {
		FragmentPage f = new FragmentPage(channel, service);
		return f;
	}

	public FragmentPage(Channel channel, IRCService service) {
		this.channel = channel;
		this.service = service;
		this.channel.setFragment(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.pager_page, container, false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.setListAdapter(new MessageAdapter(getActivity(),
				R.layout.channel_message_row, channel.getMessages(),
				(LayoutInflater) getActivity().getSystemService(
						Context.LAYOUT_INFLATER_SERVICE)));
	}

	public void notifyAdapterChanged() {
		if (this.getListAdapter() != null)
			((MessageAdapter) this.getListAdapter()).notifyDataSetChanged();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		// Make sure that we are currently visible
		if (this.isVisible()) {
			// If we are becoming invisible, then...
			if (!isVisibleToUser) {
				this.channel.isActive(false);
			} else {
				this.channel.isActive(true);
				this.notifyAdapterChanged();
			}
		}
	}

}
