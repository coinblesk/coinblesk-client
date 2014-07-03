package ch.uzh.csg.mbps.client.payment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import ch.uzh.csg.mbps.client.AbstractLoginActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.util.CustomDialogFragment;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends AbstractLoginActivity {
	protected boolean isSeller;
	
	private CustomDialogFragment lastDialog = null;
	private ProgressDialog progressDialog;
	private boolean destroyed = false;


	/**
	 * Reset the UI after a successful or failed transaction.
	 */
	protected abstract void refreshActivity();
		
	protected void showDialogAndClearOlderOne(String title, int icon,String message) {
		if (lastDialog != null) {
			lastDialog.dismiss();
		}

		lastDialog = showDialog(title, icon, message);
	}
	
	/**
	 * Starts the progress dialog. As long as the dialog is running other touch
	 * actions are ignored.
	 * @return 
	 */
	public ProgressDialog getNfcInProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
			progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
			        dialog.dismiss();
			        refreshActivity();
			    }
			});
			progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.animation_nfc_in_progress));
		}
		
		progressDialog.setMessage(getResources().getString(R.string.nfc_in_progress_dialog));

		return progressDialog;
	}

	/**
	 * Closes the progress dialog. 
	 */
	public void dismissNfcInProgressDialog() {
		if (progressDialog != null && !destroyed) {
			progressDialog.dismiss();
		}
	}
}
