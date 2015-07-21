package ch.uzh.csg.coinblesk.client.ui.navigation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.ui.adressbook.AddressBookActivity;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.ChoosePaymentActivity;
import ch.uzh.csg.coinblesk.client.ui.history.HistoryActivity;
import ch.uzh.csg.coinblesk.client.ui.main.MainActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.PayInActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.PayOutActivity;
import ch.uzh.csg.coinblesk.client.ui.settings.SettingsActivity;
import ch.uzh.csg.coinblesk.client.R;

/**
 * This class represents the navigation drawer. The methods from
 * {@link BaseActivity} are not inherited but overridden.
 */
public class DrawerItemClickListener extends WalletActivity implements OnItemClickListener {
	private View view;
	private ProgressDialog dialog;

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(parent, position);
	}

	/**
	 * Swaps fragments to guide through views from main view {@link MainActivity}
	 */
	private void selectItem(View view, int position) {
		this.view = view;
		switch (position) {
		case 0:
			// Create New Transaction
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), ChoosePaymentActivity.class));
			break;
		case 1:
			// History
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), HistoryActivity.class));
			break;
		case 2:
			// Settings
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), SettingsActivity.class));
			break;
		case 3:
			// Pay In
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), PayInActivity.class));
			break;
		case 4:
			// Pay Out
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), PayOutActivity.class));
			break;
		case 5:
			// Address Book
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), AddressBookActivity.class));
			break;
		case 6:
			// Help
			MainActivity.isFirstTime = true;
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), MainActivity.class));
			break;
		default:
			break;
		}
	}

	/**
	 * This method is a placeholder for the toast but much simpler to use. See
	 * {@link BaseActivity}
	 * 
	 * @param response
	 *            The message which is shown in a toast call.
	 */
	public void displayResponse(String response) {
		Toast.makeText(getContext(), response, Toast.LENGTH_LONG).show();
	}

	/**
	 * Starts the progress dialog. As long dialog is running other touch actions
	 * are ignored.
	 */
	public void showLoadingProgressDialog() {
		if (dialog == null) {
			dialog = new ProgressDialog(this.view.getRootView().getContext());
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
		}

		dialog.setMessage(view.getContext().getResources().getString(R.string.loading_progress_dialog));

		/*
		 * Runs an UI thread to show the 
		 * loading progress over another view. 
		 */
		runOnUiThread(new Runnable(){
			public void run() {
				dialog.show();
			}});    
	}

	@Override
	public void dismissProgressDialog() {
		try {
			if (dialog != null) {
				dialog.dismiss();
			} 
		} catch (Exception e) {}
	}

	private Context getContext(){
		return this.view.getContext();
	}
}
