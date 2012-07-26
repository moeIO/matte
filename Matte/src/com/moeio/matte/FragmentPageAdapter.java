package com.moeio.matte;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.moeio.matte.backend.IRCService;
import com.moeio.matte.irc.Channel;
import com.moeio.matte.irc.Server;

public class FragmentPageAdapter extends FragmentPagerAdapter {
	private IRCService service;
	FragmentManager fragmentManager;

	public FragmentPageAdapter(FragmentManager fm, IRCService service) {
		super(fm);
		fragmentManager = fm;
		this.service = service;
	}

	@Override
	public Fragment getItem(int position) {
		int p = 0;
		for (Server s : service.getServers()) {
			if (p == position)
				return FragmentPage.newInstance(position, s, service);
			p++;
			for (Channel ch : s.getChannels()) {
				if (p == position)
					return FragmentPage.newInstance(position, ch, service);
				p++;
			}
		}
		Log.e("PageAdapter",
				"Tried to get page outside of channel/server bounds");
		return null;
	}

	public Channel getChannel(int position) {
		int p = 0;
		for (Server s : service.getServers()) {
			if (p == position)
				return s;
			p++;
			for (Channel ch : s.getChannels()) {
				if (p == position)
					return ch;
				p++;
			}
		}
		return null;
	}

	@Override
	public int getCount() {
		int c = 0;
		for (Server s : service.getServers()) {
			c++;
			for (Channel ch : s.getChannels())
				c++;
		}
		return c;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		Channel c = this.getChannel(position);
		if (c.getUnreadMessageCount() > 0)
			return c.getName() + " (" + c.getUnreadMessageCount() + ")";
		else
			return c.getName();
	}

	public void updateFragment(int position) {
		FragmentPage f = this.getChannel(position).getFragment();
		if (f != null)
			f.notifyAdapterChanged();
	}
}
