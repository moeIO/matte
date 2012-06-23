package com.moeio.matte;

import java.io.InputStream;
import java.net.URL;

import com.moeio.matte.irc.GenericMessage;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.widget.TextView;

public class AsyncYoutubeLoad extends AsyncTask<Object, Void, Boolean> {
	private TextView view;
	private GenericMessage message;
	private SpannableString spanToSet;

	@Override
	protected Boolean doInBackground(Object... params) {
		this.view = (TextView) params[0];
		this.message = (GenericMessage) params[1];

		this.spanToSet = new SpannableString(" \n" + this.message.getContent().toString());
		Drawable d = null;
		try {
			InputStream is = (InputStream) new URL("http://img.youtube.com/vi/" + this.message.getEmbeddedYoutube() + "/hqdefault.jpg").getContent();
			d = Drawable.createFromStream(is, "src name");
		} catch (Exception e) {
				// No-one cares
				return false;
		}
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
		this.spanToSet.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

		return true;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			try {
				if (this.view != null)
					this.view.setText(this.spanToSet);
			} catch (Exception ex) {
				// Whatever
			}
		}
	}

}