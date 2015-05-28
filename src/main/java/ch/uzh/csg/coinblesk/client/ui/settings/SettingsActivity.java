package ch.uzh.csg.coinblesk.client.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.ClientController;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	private MenuItem menuWarning;
	protected static Context context;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);	
		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		getActionBar().setDisplayHomeAsUpEnabled(true);
		context = getApplicationContext();
		setupSimplePreferencesScreen();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if(key.equals("numberOfLastTransactions")) {
					int value = Integer.parseInt(sharedPreferences.getString("numberOfLastTransactions", "3"));
					if(value <= 0 || value > 5){
						Toast.makeText(context, "please enter number between 1 and 5", Toast.LENGTH_LONG).show();
						SharedPreferences.Editor prefEditor = prefs.edit();
				        prefEditor.putString("numberOfLastTransactions", "3");
				        prefEditor.commit();
					}
				}
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.offline_mode, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuWarning = menu.findItem(R.id.action_warning);
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public void invalidateOptionsMenu() {
		if (menuWarning != null) {
			if (ClientController.isConnectedToServer()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// Add all preferences for settings in one view
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPreferenceFragement()).commit();
	}

	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);
				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index]: null);
			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(),""));
	}

	@Override
	protected boolean isValidFragment(String fragmentName){
		return true;
	}

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference. This class represents the view for
	 * small devices.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	public static class SettingsPreferenceFragement extends PreferenceFragment{
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// Add 'general' preferences
			addPreferencesFromResource(R.xml.pref_general);
			// Add 'payment' preferences, and a corresponding header.
			PreferenceCategory paymentHeader = new PreferenceCategory(getActivity());
			paymentHeader.setTitle(R.string.pref_header_payments);
			getPreferenceScreen().addPreference(paymentHeader);
			addPreferencesFromResource(R.xml.pref_payment);
			bindPreferenceSummaryToValue(findPreference("bitcoin_list"));
			bindPreferenceSummaryToValue(findPreference("fee_amount"));
			bindPreferenceSummaryToValue(findPreference("auto_accept_amount"));

			// add wallet preferences
			PreferenceCategory walletHeader = new PreferenceCategory(getActivity());
			walletHeader.setTitle(R.string.pref_header_wallet);
			getPreferenceScreen().addPreference(walletHeader);
			addPreferencesFromResource(R.xml.pref_wallet);


		}
	}

	/**
	 * This class represents the view for tablets and larger devices.
	 * Here the value's are bind which are declared in the general part.
	 */
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);
			PreferenceCategory fakeHeader = new PreferenceCategory(getActivity());
			getPreferenceScreen().addPreference(fakeHeader);
			bindPreferenceSummaryToValue(findPreference("bitcoin_list"));
		}
	}

	/**
	 * This class represents the view for tablets and larger devices. Here the
	 * value's are bind which are declared in the payment part.
	 */
	public static class PaymentPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_payment);
			PreferenceCategory fakeHeader = new PreferenceCategory(getActivity());
			getPreferenceScreen().addPreference(fakeHeader);
			bindPreferenceSummaryToValue(findPreference("fee_amount"));
			bindPreferenceSummaryToValue(findPreference("auto_accept_amount"));
		}
	}

}
