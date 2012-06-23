package com.moeio.matte;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.view.View;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar bar = this.getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			final PreferenceFragment fragment = this;
			Preference gridview = findPreference("aboutPreference");
			gridview.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference pref) {
					// About screen
					final View dialogView = fragment.getActivity().getLayoutInflater().inflate(R.layout.about_dialog, null);
					new AlertDialog.Builder(fragment.getActivity()).setView(dialogView).setTitle(getResources().getString(R.string.about))
							.setPositiveButton(android.R.string.ok, null).show();
					return false;
				}
			});

		}
	}

	// Action bar button pressed
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
		}
		return true;
	}

}
