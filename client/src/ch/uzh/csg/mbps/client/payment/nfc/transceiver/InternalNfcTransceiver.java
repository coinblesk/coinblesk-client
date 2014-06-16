package ch.uzh.csg.mbps.client.payment.nfc.transceiver;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.AbstractPaymentActivity;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.SellerRole;
import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;

/**
 * This class is responsible for initiating NFC connections with the emulated
 * host card service, and transmitting and receiving messages to/from the HCS.
 * This class is however only used, where the internal NFC sensor of the given
 * device is used, which is the normal case (has only been tested with Nexus 10,
 * 7, and 5).
 */
public class InternalNfcTransceiver extends NfcTransceiver implements ReaderCallback {

	/*
	 * NXP chip supports max 255 bytes (10 bytes is header of nfc protocol)
	 */
	private static final int MAX_WRITE_LENGTH = 245;

	private IsoDep isoDep;
	private NfcMessageSplitter messageSplitter;

	public InternalNfcTransceiver(Handler handler, Context applicationContext) {
		super(handler, applicationContext);
	}

	private static final String TAG = "InternalNfcTransceiver";

	@Override
	public void enable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter) {
		Bundle options = new Bundle();
		/*
		 * this will send a check presence message that needs to be handled by
		 * HostApduService! You may run into the issue with other cards as shown
		 * here: http://code.google.com/p/android/issues/detail?id=58773. For
		 * more details on the ISO spec see
		 * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816
		 * -4_6_basic_interindustry_commands.aspx#chap6_1
		 */
		nfcAdapter.enableReaderMode(abstractPaymentActivity, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options);
	}

	@Override
	public void disable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter) {
		nfcAdapter.disableReaderMode(abstractPaymentActivity);
	}

	@Override
	public void cancel() {
		Log.e(TAG, "cancel called");
		if (isoDep != null & isoDep.isConnected()) {
			try {
				isoDep.close();
			} catch (IOException e) {
				Log.e(TAG, "close failed", e);
			}
		}
	}

	@Override
	public boolean write(NfcMessage nfcMessage) throws IllegalArgumentException {
		if (isoDep.isConnected()) {
			if (nfcMessage.getData().length > isoDep.getMaxTransceiveLength())
				throw new IllegalArgumentException("The argument length exceeds the maximum capacity of " + isoDep.getMaxTransceiveLength() + " bytes.");

			try {
				Log.d(TAG, nfcMessage.toString());
				byte[] response = isoDep.transceive(nfcMessage.getData());
				Log.d(TAG, "got response " + response.length);
				if (response.length > 0) {
					handleResponse(new NfcMessage(response));
					return true;
				}
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}

		} else {
			Log.e(TAG, "not connected");
			// no need to distinguish between not connected and IOException, the same result for the user
			Message m = CommUtils.Message.NFC_WRITE_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
		return false;
	}

	@Override
	public void write(PaymentMessage paymentMessage) {
		synchronized (this) {
			List<NfcMessage> list = messageSplitter.getFragments(paymentMessage.getData());
			Log.d(TAG, "writing " + paymentMessage.getData().length + " / " + list.size());
			for (NfcMessage msg : list) {
				if (!write(msg)) {
					Log.e(TAG, "wirte abort");
					return;
				}
			}
		}
	}

	public void onTagDiscovered(Tag tag) {
		Log.d(TAG, "Tag discovered " + tag);
		if (this.role == null) {
			this.role = new SellerRole();
		}

		this.isoDep = IsoDep.get(tag);
		try {
			isoDep.connect();
			// isoDep.setTimeout(5000);
		} catch (IOException e) {
			Log.e(TAG, "cannot connect to isodep", e);
		}
		initNfc();
	}

	private boolean initNfc() {
		Log.d(TAG, "init NFC");
		long start = System.currentTimeMillis();
		int length = 0;

		for (int i = 0; i < 10; i++) {
			try {
				messageSplitter = new NfcMessageSplitter(MAX_WRITE_LENGTH);
				byte[] response = isoDep.transceive(createSelectAidApdu());
				handleAidApduResponse(response);
				return true;
			} catch (IOException e) {
				Log.d(TAG, "select AID failed", e);
			}

			long waitTime = 100 - (System.currentTimeMillis() - start);
			if (waitTime > 0) {
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					Log.d(TAG, "thread interrupted", e);
					return false;
				}
			}
		}

		if (length == 0) {
			Log.e(TAG, "init NFC failed, giving up");
			Message m = CommUtils.Message.NFC_INIT_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
		return false;
	}

	
}
