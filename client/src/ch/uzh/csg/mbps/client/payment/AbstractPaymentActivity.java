package ch.uzh.csg.mbps.client.payment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.payment.nfc.CustomHostApduService;
import ch.uzh.csg.mbps.client.payment.nfc.transceiver.NfcTransceiver;
import ch.uzh.csg.mbps.client.util.CustomDialogFragment;
import ch.uzh.csg.mbps.model.Transaction;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class AbstractPaymentActivity extends AbstractAsyncActivity {
	private NfcAdapter nfcAdapter = null;
	protected boolean isSeller;
	
	private NfcTransceiver nfcTransceiver = null;
	private CustomHostApduService hceService;
	private boolean hceServiceInitialized = false;
	private boolean requestedToActivateNfc = false;
	
	private CustomDialogFragment lastDialog = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			displayResponse(CommUtils.Message.ERROR_NO_NFC_MSG.getMessage());
			finish();
			return;
		} 
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		/*
		 * If nfc is not enabled, the user receives the prompt to enable nfc.
		 * Then, onStart is called again and the else-branch is called.
		 */
		if (!nfcAdapter.isEnabled()) {
			if (!requestedToActivateNfc) {
				displayResponse(CommUtils.Message.ACTIVATE_NFC_MSG.getMessage());
				startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
				requestedToActivateNfc = true;
			} else {
				displayResponse(CommUtils.Message.ERROR_NFC_NOT_ACTIVATED_MSG.getMessage());
				finish();
			}
		} else {
			/*
			 * The seller acts as the nfc reader, while the buyer acts as the
			 * emulated tag. This distinction is needed to enable the p2p nfc
			 * communicaiton.
			 */
			if (isSeller) {
				if (nfcTransceiver == null)
					nfcTransceiver = NfcTransceiver.getInstance(handler, this);
			} else {
				if (!hceServiceInitialized) {
					CustomHostApduService.init(handler, this);
					hceServiceInitialized = true;
				}
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (isSeller)
			nfcTransceiver.enable(this, nfcAdapter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (isSeller)
			nfcTransceiver.disable(this, nfcAdapter);
	}
	
	/**
	 * Reset the UI after a successful or failed transaction.
	 */
	protected abstract void resetGUI();
	
	/**
	 * Update the UI to show the transaction details before the transaction is
	 * agreed on to be executed.
	 * 
	 * @param tx
	 *            The transaction to be executed.
	 */
	protected abstract void updateGUI(Transaction tx);
	
	protected abstract void launchRequest();
	
	/**
	 * Register the {@link CustomHostApduService} to this class. Since the
	 * {@link CustomHostApduService} is instantiated by android on a NFC
	 * contact, this is the only way to have a reference to the object.
	 * 
	 * @param service
	 */
	public void setHCEService(CustomHostApduService service) {
		hceService = service;
	}
	
	/**
	 * Returns the reference to the {@link CustomHostApduService} object.
	 */
	public CustomHostApduService getHCEService() {
		return hceService;
	}
	
	private boolean showNfcLayerErrorDialog(int arg) {
		return (arg == CommUtils.Message.NFC_INIT_ERROR.getCode() || 
				arg == CommUtils.Message.NFC_WRITE_ERROR.getCode());
	}
	
	private boolean showPaymentLayerErrorDialog(int arg) {
		return (arg == CommUtils.Message.PAYMENT_ERROR_NO_AMOUNT_ENTERED.getCode() ||
				arg == CommUtils.Message.PAYMENT_ERROR_UNEXPECTED.getCode() ||
				arg == CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED.getCode() || 
				arg == CommUtils.Message.PAYMENT_OTHERDEVICE_UNEXPECTED.getCode());
	}
	
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
		
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CommUtils.NFC_LAYER_CATEGORY:
				if (showNfcLayerErrorDialog(msg.arg1)) {
					showDialogAndClearOlderOne(getResources().getString(R.string.payment_failure), getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()),(String) msg.obj);
					resetGUI();
				}
				break;
			case CommUtils.PAYMENT_LAYER_CATEGORY:
				if (showPaymentLayerErrorDialog(msg.arg1)) {
					showDialogAndClearOlderOne(getResources().getString(R.string.payment_failure), getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()),(String) msg.obj);
					resetGUI();
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_ERROR_BUYER_REJECTED.getCode()) {
					resetGUI();
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_UPDATE_GUI_AMOUNT.getCode()) {
					updateGUI((Transaction) msg.obj);
					break;
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_SELLER.getCode() || msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_BUYER.getCode()) {
					showDialogAndClearOlderOne(getResources().getString(R.string.payment_success), getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()), (String) msg.obj);
					resetGUI();
				}
				break;
			}
		}

	};
	
	private void showDialogAndClearOlderOne(String title, int icon,String message) {
		if (lastDialog != null) {
			lastDialog.dismiss();
		}

		lastDialog = showDialog(title, icon, message);
	}
	
}
