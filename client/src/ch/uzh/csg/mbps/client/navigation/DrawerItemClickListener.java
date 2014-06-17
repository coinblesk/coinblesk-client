package ch.uzh.csg.mbps.client.navigation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.AbstractLoginActivity;
import ch.uzh.csg.mbps.client.AddressBookActivity;
import ch.uzh.csg.mbps.client.ChoosePaymentActivity;
import ch.uzh.csg.mbps.client.HistoryActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.LoginActivity;
import ch.uzh.csg.mbps.client.MainActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.payment.PayInActivity;
import ch.uzh.csg.mbps.client.payment.PayOutActivity;
import ch.uzh.csg.mbps.client.profile.AccountProfileActivity;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SignOutRequestTask;
import ch.uzh.csg.mbps.client.settings.SettingsActivity;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class represents the navigation drawer. The methods from
 * {@link AbstractAsyncActivity} are not inherited but overridden.
 */
public class DrawerItemClickListener extends AbstractLoginActivity implements OnItemClickListener, IAsyncTaskCompleteListener<CustomResponseObject> {
	private View view;
	private ProgressDialog dialog;

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(parent, position);
	}

	/**
	 * Swaps fragments to guide through views from main view {@link MainActvity}
	 */
	private void selectItem(View view, int position) {
		this.view = view;
		switch (position) {
		case 0:
			// Profile
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), AccountProfileActivity.class));
			break;
		case 1:
			// Create New Transaction
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), ChoosePaymentActivity.class));
			break;
		case 2:
			// History
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), HistoryActivity.class));
			break;
		case 3:
			// Settings
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), SettingsActivity.class));
			break;
		case 4:
			// Pay In
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), PayInActivity.class));
			break;
		case 5:
			// Pay Out
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), PayOutActivity.class));
			break;
		case 6:
			// Address Book
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), AddressBookActivity.class));
			break;
		case 7:
			// Reconnect to Server
			launchSignInRequest();
			break;
		case 8:
			// Help
			MainActivity.isFirstTime = true;
			this.view.getContext().startActivity(new Intent(this.view.getContext().getApplicationContext(), MainActivity.class));
			break;
		case 9:
			// Sign Out
			launchSignOut();
			break;
		default:
			break;
		}
	}

	protected void launchSignInRequest() {
		if (!ClientController.isOnline()) {
			super.launchSignInRequest();
		} else {
			displayResponse(getContext().getResources().getString(R.string.already_connected_to_server));
		}
	}

	private void launchSignOut() {
		if (ClientController.isOnline()) {
			if(!TimeHandler.getInstance().determineIfLessThanFiveSecondsLeft()){
				launchSignOutRequest();
			}else{
				// Dismiss session
				TimeHandler.getInstance().terminateSession();
				updateClientControllerAndFinish();
				displayResponse(getContext().getResources().getString(R.string.session_expired));
			}
		} else {
			updateClientControllerAndFinish();
		}
	}

	private void launchSignOutRequest() {
		showLoadingProgressDialog();
		RequestTask signOut = new SignOutRequestTask(this);		
		signOut.execute();
	}

	public void onTaskComplete(CustomResponseObject response) {
		if (response.getType() == Type.LOGOUT) {
			TimeHandler.getInstance().terminateSession();
			updateClientControllerAndFinish();
		} else if (response.getMessage() != null && (response.getMessage().equals(Constants.CONNECTION_ERROR) || response.getMessage().equals(Constants.REST_CLIENT_ERROR))) {
			dismissProgressDialog();
			displayResponse(response.getMessage());
		}
		else
			super.onTaskComplete(response, getContext());
	}

	private void updateClientControllerAndFinish() {
		dismissProgressDialog();
		ClientController.clear();
		launchActivity(LoginActivity.class);
	}

	/**
	 * Starts a new activity. The method is called only for guiding to another
	 * activity.
	 * 
	 * @param <T>
	 *            generic placeholder for the passed parameter
	 * @param classActvity
	 *            The class of the activity which will be started.
	 */
	public <T> void launchActivity(Class<T> classActivity){
		Intent intent = new Intent(this.view.getContext().getApplicationContext(), classActivity);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.view.getContext().startActivity(intent);
		finish();
	}

	/**
	 * This method is a placeholder for the toast but much simpler to use. See
	 * {@link AbstractAsyncActivity}
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
