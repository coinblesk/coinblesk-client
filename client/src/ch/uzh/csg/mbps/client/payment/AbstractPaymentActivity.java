package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.NfcAdapter;
import ch.uzh.csg.mbps.client.AbstractLoginActivity;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.CustomDialogFragment;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.PaymentResponse;
import ch.uzh.csg.mbps.util.Converter;
import ch.uzh.csg.paymentlib.persistency.IPersistencyHandler;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends AbstractLoginActivity {
	protected boolean isSeller;
	protected BigDecimal exchangeRate;
	private CustomDialogFragment lastDialog = null;
	private ProgressDialog progressDialog;
	private boolean destroyed = false;
	protected boolean paymentAccepted = false;


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
	private ProgressDialog getNfcInProgressDialog() {
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
	
	protected void showNfcInProgressDialog(){
		runOnUiThread(new Runnable() {
			public void run() {
				getNfcInProgressDialog().show();
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
	
	protected void showDialog(String message, boolean isSuccessful) {
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
			}
		});

	}

	protected void resetStates() {
		paymentAccepted = false;
	}
	
	//TODO simon: check if works!
	/**
	 * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
	 * null and open up NFC settings.
	 * 
	 * @param context
	 * @return
	 */
	protected NfcAdapter createAdapter(Context context) {
		NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());
		return nfcAdapter;
	}
	
}
