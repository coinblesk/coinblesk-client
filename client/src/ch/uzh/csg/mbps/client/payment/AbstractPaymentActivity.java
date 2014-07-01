package ch.uzh.csg.mbps.client.payment;

import android.app.ProgressDialog;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.util.CustomDialogFragment;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends AbstractAsyncActivity {
	protected boolean isSeller;
	
	private CustomDialogFragment lastDialog = null;
	private ProgressDialog progressDialog;
	private boolean destroyed = false;


	/**
	 * Reset the UI after a successful or failed transaction.
	 */
	protected abstract void refreshActivity();
	
	//TODO simon: delete
//	@SuppressLint("HandlerLeak")
//	private final Handler handler = new Handler() {
//		
//		@Override
//		public void handleMessage(android.os.Message msg) {
//			switch (msg.what) {
//			case CommUtils.NFC_LAYER_CATEGORY:
//				if (showNfcLayerErrorDialog(msg.arg1)) {
//					showDialogAndClearOlderOne(getResources().getString(R.string.payment_failure), getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()),(String) msg.obj);
//					resetGUI();
//				}
//				break;
//			case CommUtils.PAYMENT_LAYER_CATEGORY:
//				if (showPaymentLayerErrorDialog(msg.arg1)) {
//					showDialogAndClearOlderOne(getResources().getString(R.string.payment_failure), getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()),(String) msg.obj);
//					resetGUI();
//				} else if (msg.arg1 == CommUtils.Message.PAYMENT_ERROR_BUYER_REJECTED.getCode()) {
//					resetGUI();
//				} else if (msg.arg1 == CommUtils.Message.PAYMENT_UPDATE_GUI_AMOUNT.getCode()) {
//					//TODO: refactor, since no Transaction model class anymore
////					updateGUI((Transaction) msg.obj);
//					break;
//				} else if (msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_SELLER.getCode() || msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_BUYER.getCode()) {
//					showDialogAndClearOlderOne(getResources().getString(R.string.payment_success), getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()), (String) msg.obj);
//					resetGUI();
//				}
//				break;
//			}
//		}
//
//	};
	
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
