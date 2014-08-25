package ch.uzh.csg.mbps.client;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.CustomDialogFragment;

/**
 * The class AbstractAsyncActivity is the abstract base class for all
 * activity-contexts. This class inherits from fragmentActivity and extends the
 * activity with some common functions, which are used by almost all activities.
 * This class determines if the application is used by a smart-phone or by a
 * tablet and sets the orientation appropriately.
 */
public abstract class AbstractAsyncActivity extends FragmentActivity {
	private MenuItem menuWarning; 
    private MenuItem offlineMode; 
    
	private ProgressDialog progressDialog;
	private boolean destroyed = false;
	
	@Override
	protected void onPause(){
		super.onPause();
		dismissProgressDialog();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyed = true;
	}

	/**
	 * Starts the progress dialog. As long as the dialog is running other touch
	 * actions are ignored.
	 */
	public void showLoadingProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
		}
		
		progressDialog.setMessage(getResources().getString(R.string.loading_progress_dialog));
		runOnUiThread(new Runnable() {
			public void run() {
				progressDialog.show();
			}
		});
	}

	/**
	 * Closes the progress dialog. 
	 */
	public void dismissProgressDialog() {
		if (progressDialog != null && !destroyed) {
			progressDialog.dismiss();
		}
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
	
	/**
	 * Starts a new activity. The method is called only for guiding to another
	 * activity.
	 * 
	 * @param <T>
	 *            generic placeholder for the passed parameter
	 * @param activity
	 *            this parameter represents the activity in which the client is
	 *            or was before going to the next activity
	 * @param classActvity
	 *            The class of the activity which will be started.
	 */
	public <T> void launchActivity(Activity activity, Class<T> classActvity) {
		Intent intent = new Intent(activity, classActvity);
		startActivity(intent);
	}
	
	/**
	 * This method is a placeholder for the toast but much simpler to use.
	 * 
	 * @param response
	 *            The message which is shown in a toast call.
	 */
	public void displayResponse(String response) {
    	Toast.makeText(this, response, Toast.LENGTH_LONG).show();
	}

	/**
	 * Checks the resolution of the device and determines its orientation.
	 * Smart-phones are in portrait and tablets in landscape view. This method
	 * is called by every activity. Disables the rotation function of the
	 * screen.
	 */
	public void setScreenOrientation(){
		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	/**
	 * Reloads the current activity.
	 * 
	 * @param intent
	 *            the intent
	 */
	public void reload(Intent intent){
	    finish();
	    startActivity(intent);
	}
	
	/**
	 * Shows a customized dialog fragment {@link CustomDialogFragment} with a
	 * title, message and an icon for success or fail. Notifies about finished
	 * action. The dialog disables other touch actions.
	 * 
	 * @param title
	 *            The title of the notification
	 * @param icon
	 *            The success or fail symbol are shown. The icons integer value
	 *            is passed.
	 * @param message
	 *            The content of the actions message.
	 * @return Returns dialog fragment that contains title, icon, and message
	 */
	public CustomDialogFragment showDialog(String title, int icon ,String message) {
		FragmentManager fragmentManager = getFragmentManager();
		CustomDialogFragment dialogFR = CustomDialogFragment.create(title, icon ,message);
		dialogFR.setCancelable(false);
		
		try {
			dialogFR.show(fragmentManager, "CustomDialog");
		} catch (Exception e) { }
		
		return dialogFR;
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.offline_mode, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        offlineMode = menu.findItem(R.id.menu_offlineMode);
		TextView offlineModeTV = (TextView) offlineMode.getActionView();
		offlineModeTV.setText(getResources().getString(R.string.menu_offlineModeText));
		
        invalidateOptionsMenu();
        return true;
    }
    
	@Override
	public void invalidateOptionsMenu() {
		if (menuWarning != null) {
			if (ClientController.isOnline()) {
				menuWarning.setVisible(false);
				offlineMode.setVisible(false);
			} else {
				menuWarning.setVisible(true);
				offlineMode.setVisible(true);
			}
		}
	}
	
	
	
}