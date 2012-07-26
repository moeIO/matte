package com.moeio.matte;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.moeio.matte.irc.GenericMessage;

public class MessageAdapter extends ArrayAdapter<GenericMessage> {

	private int[] possibleNickColours = { R.color.nickcolor0,
			R.color.nickcolor1, R.color.nickcolor2, R.color.nickcolor3,
			R.color.nickcolor4, R.color.nickcolor5, R.color.nickcolor6,
			R.color.nickcolor7, R.color.nickcolor8, R.color.nickcolor9,
			R.color.nickcolor10, R.color.nickcolor11, R.color.nickcolor12,
			R.color.nickcolor13, R.color.nickcolor14, R.color.nickcolor15 };
	private HashMap<String, Integer> nickColours;
	private int highlightCellColour;

	private ArrayList<GenericMessage> items;
	private Context context;
	private LayoutInflater inflater;

	public MessageAdapter(Context context, int textViewResourceId,
			ArrayList<GenericMessage> items, LayoutInflater inflater) {
		super(context, textViewResourceId, items);
		this.items = items;
		this.context = context;
		this.highlightCellColour = Color.rgb(182, 232, 243);
		this.nickColours = new HashMap<String, Integer>();
		// Add predefined 'special' nicknames.
		this.nickColours.put("!",
				this.context.getResources().getColor(R.color.nickcolor4));
		this.nickColours.put("<--",
				this.context.getResources().getColor(R.color.nickcolor5));
		this.nickColours.put("-->",
				this.context.getResources().getColor(R.color.nickcolor5));

		this.inflater = inflater;
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
		TextView name = (TextView) convertView
				.findViewById(R.id.channelMessageName);
		TextView content = (TextView) convertView
				.findViewById(R.id.channelMessageContent);
		if (message.isChannelNotificationType())
			content.setTextColor(context.getResources().getColor(
					R.color.channelNotification));
		else
			content.setTextColor(context.getResources().getColor(
					R.color.channelNormal));
		content.setText(message.getContent());
		/*
		 * if (message.getEmbeddedYoutube() != null) { if (false) {
		 * AsyncYoutubeLoad videoPreviewLoader = new AsyncYoutubeLoad();
		 * videoPreviewLoader.execute(new Object[] { content, message }); } }
		 */
		name.setText(message.getNickname());
		name.setTextColor(getNickColour(message.getNickname()));
		if (message.isHighlighted())
			convertView.setBackgroundColor(highlightCellColour);
		else
			convertView.setBackgroundDrawable(null);
		return convertView;
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

		return context.getResources()
				.getColor(
						this.possibleNickColours[hash
								% this.possibleNickColours.length]);
	}
}
