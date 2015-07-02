package ch.uzh.csg.coinblesk.client.ui.payment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends WalletActivity {
	protected boolean isSeller;
	protected BigDecimal exchangeRate;
	private ProgressDialog progressDialog;
	private boolean destroyed = false;
	protected boolean paymentAccepted = false;
	
	/**
	 * Reset the UI after a successful or failed transaction.
	 */
	protected abstract void refreshActivity();

	/**
	 * Starts the NFC progress dialog. As long as the dialog is running other
	 * touch actions are ignored.
	 * 
	 * @param isInProgress determines if NFC is already inProgress or
	 *        needs to be established first
	 */
	protected void showNfcProgressDialog(final boolean isInProgress){
		runOnUiThread(new Runnable() {
			public void run() {
				getNfcInProgressDialog(isInProgress).show();
			}
		});
	}

	/**
	 * Closes the progress dialog. 
	 */
	public void dismissNfcInProgressDialog() {
		if (progressDialog != null && !destroyed) {
			progressDialog.dismiss();
		}
	}
	
	private ProgressDialog getNfcInProgressDialog(boolean isInProgress) {
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
		if(isInProgress){
			progressDialog.setMessage(getResources().getString(R.string.nfc_in_progress_dialog));
		}
		else {
			progressDialog.setMessage(getResources().getString(R.string.establishNfcConnectionInfo));
		}

		return progressDialog;
	}

	/**
	 * Displays a custom dialog with a given message and an image indicating if task was successful or not.
	 * 
	 * @param message to be displayed to the user
	 * @param isSuccessful boolean to indicate if task was successful
	 */
	protected void showDialog(String message, boolean isSuccessful) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (isSuccessful) {
			playSound();
			builder.setTitle(getResources().getString(R.string.payment_success))
			.setIcon(getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()));
		}
		else {
			builder.setTitle(getResources().getString(R.string.payment_failure))
			.setIcon(getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()));
		}
		builder.setMessage(message);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				refreshActivity();
			}
		});

		runOnUiThread(new Runnable() {
			public void run() {
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	private void playSound() {
		try {
		    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		    r.play();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    
    }

	protected void resetStates() {
		paymentAccepted = false;
	}

	/**
	 * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
	 * null and open up NFC settings.
	 * 
	 * @param context
	 * @return
	 */
	protected NfcAdapter createAdapter(Context context) {
		NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());
		if (nfcAdapter == null) {
			showDialog(getResources().getString(R.string.nfc_enable_title), R.drawable.ic_alerts_and_states_warning, getResources().getString(R.string.nfc_no_adapter));
			return null;
		}
		if(nfcAdapter.isEnabled() == false){
			enableNFC(context);
		}
		return nfcAdapter;
	}
	
	private void enableNFC(final Context context) {
		final AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
		alertbox.setTitle(getResources().getString(R.string.nfc_enable_title));
		alertbox.setMessage(getResources().getString(R.string.nfc_enable_message));
		alertbox.setPositiveButton(getResources().getString(R.string.nfc_enable_button), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				} else {
					Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				}
			}
		});
		alertbox.setNegativeButton(getResources().getString(R.string.nfc_enable_button_abort), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertbox.show();
	}

}
