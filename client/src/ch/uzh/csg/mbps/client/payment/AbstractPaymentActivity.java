package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import ch.uzh.csg.mbps.client.AbstractLoginActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.paymentlib.persistency.IPersistencyHandler;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends AbstractLoginActivity {
	protected boolean isSeller;
	protected BigDecimal exchangeRate;
	private AlertDialog lastDialog = null;
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
	 * @param boolean isInProgress determines if NFC is already inProgress or
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

	protected void showDialog(String message, boolean isSuccessful) {
		if (lastDialog != null) {
			lastDialog.dismiss();
		}
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (isSuccessful) {
			builder.setTitle(getResources().getString(R.string.payment_success))
			.setIcon(getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()));
		}
		else {
			builder.setTitle(getResources().getString(R.string.payment_failure))
			.setIcon(getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()));
		}
		builder.setMessage(message);
		builder.setCancelable(true);
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
				lastDialog = alert;
			}
		});
	}

	protected void resetStates() {
		paymentAccepted = false;
	}

	//TODO jeton: add to xml
	protected IPersistencyHandler persistencyHandler = new IPersistencyHandler() {

		//			@Override
		public PersistedPaymentRequest getPersistedPaymentRequest(String username, Currency currency, long amount) {
			//				Log.i(TAG, "getPersistedPaymentRequest");
			return null;
		}

		//			@Override
		public void delete(PersistedPaymentRequest paymentRequest) {
			//				Log.i(TAG, "delete");
		}

		//			@Override
		public void add(PersistedPaymentRequest paymentRequest) {
			//				Log.i(TAG, "add");
		}

	};

	/**
	 * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
	 * null and open up NFC settings.
	 * 
	 * @param context
	 * @return
	 */
	protected NfcAdapter createAdapter(Context context) {
		final NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());
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
