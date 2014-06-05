package ch.uzh.csg.mbps.client.payment.nfc.transceiver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.AbstractPaymentActivity;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.CustomHostApduService;
import ch.uzh.csg.mbps.client.payment.nfc.SellerRole;
import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;

import com.acs.smartcard.Reader;

/**
 * This is the generic NfcTransceiver. It is the base class for the internal and
 * external transceivers. It is responsible for handling the incoming and
 * outgoing messages according to the implemented protocol.
 */
public abstract class NfcTransceiver {
	
	protected Handler guiHandler;
	protected SellerRole role;
	
	private NfcMessageReassembler msgReassembler;
	
	private Context context;
	private boolean isSeller = true;
	
	private int lastSequenceNumberReceived = 0;
	
	private static final String TAG = "NfcTransceiver";
	
	public NfcTransceiver(Handler handler, Context applicationContext) {
		this.guiHandler = handler;
		this.context = applicationContext;
		this.msgReassembler = new NfcMessageReassembler();
	}
	
	public static NfcTransceiver getInstance(Handler handler, Activity activity) {
		UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		Reader reader = new Reader(manager);
		
		boolean hasExternalReader = false;
		for (UsbDevice device : manager.getDeviceList().values()) {
			if (reader.isSupported(device)) {
				hasExternalReader = true;
				break;
			}
		}
		
		if (hasExternalReader)
			return new ExternalNfcTransceiver(handler, activity.getApplicationContext());
		else
			return new InternalNfcTransceiver(handler, activity.getApplicationContext());
	}
	
	public abstract void enable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter);
	
	public abstract void disable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter);

	public abstract void cancel();
	
	public abstract boolean write(NfcMessage nfcMessage) throws IllegalArgumentException;
	
	public abstract void write(PaymentMessage paymentMessage);
	
	private void reset(boolean reset) {
		SequenceNumberGenerator.resetSequenceNumber();
		lastSequenceNumberReceived = 0;
		this.role = null;
		cancel();
	}
	
	/**
	 * To initiate a NFC connection, the NFC reader sends a "SELECT AID" APDU to
	 * the emulated card. Android OS then instantiates the service which has
	 * this AID registered (see apduservice.xml).
	 */
	protected byte[] createSelectAidApdu() {
		byte[] temp = new byte[Constants.CLA_INS_P1_P2.length + Constants.AID_MBPS.length + 2];
		System.arraycopy(Constants.CLA_INS_P1_P2, 0, temp, 0, Constants.CLA_INS_P1_P2.length);
		temp[4] = (byte) Constants.AID_MBPS.length;
		System.arraycopy(Constants.AID_MBPS, 0, temp, 5, Constants.AID_MBPS.length);
		temp[temp.length - 1] = 3;
		return temp;
	}
	
	protected byte[] createSelectAidApduResume() {
		SequenceNumberGenerator.resetSequenceNumber();
		lastSequenceNumberReceived = 0;
		
		byte[] temp = new byte[Constants.CLA_INS_P1_P2.length + Constants.AID_MBPS_RESUME.length + 2];
		System.arraycopy(Constants.CLA_INS_P1_P2, 0, temp, 0, Constants.CLA_INS_P1_P2.length);
		temp[4] = (byte) Constants.AID_MBPS_RESUME.length;
		System.arraycopy(Constants.AID_MBPS_RESUME, 0, temp, 5, Constants.AID_MBPS_RESUME.length);
		temp[temp.length - 1] = 2;
		return temp;
	}
	
	//NFC reader sends an APDU with a "SELECT AID"
	protected void handleAidApduResponse(byte[] result) {
		SequenceNumberGenerator.resetSequenceNumber();
		
		NfcMessage msg = new NfcMessage(result);
		lastSequenceNumberReceived = msg.getSequenceNumber();
		
		if (msg.getStatus() == NfcMessage.AID_SELECTED) {
			// buyer selected the aid
			if (msg.getPayload()[0] == CustomHostApduService.RESUME_PROTOCOL) {
				Log.e(TAG, "resume protocol requested, sending waiting for next");
				write(new NfcMessage(NfcMessage.DEFAULT, SequenceNumberGenerator.getNextSequenceNumber(), null));
			} else {
				Log.d(TAG, "starting protocol");
				role.init(protocolHandler);
			}
		} else {
			// should not receive something else than AID_SELECTED msg
			Message m = CommUtils.Message.NFC_INIT_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
	}
	
	protected void handleAidApduResponseResume(byte[] result) {
		SequenceNumberGenerator.resetSequenceNumber();
		
		NfcMessage msg = new NfcMessage(result);
		lastSequenceNumberReceived = msg.getSequenceNumber();
		
		Log.e(TAG, "resume protocol requested, sending waiting for next");
	}
	
	@SuppressLint("HandlerLeak")
	protected final Handler protocolHandler = new Handler() {
		
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CommUtils.NFC_LAYER_CATEGORY:
				if (role == null) {
					Log.d(TAG, "role is null, not ready yet");
					break;
				}
				if (msg.arg1 == CommUtils.Message.NFC_INIT_ERROR.getCode()) {
					Message m = CommUtils.Message.NFC_INIT_ERROR;
					guiHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
					Log.e(TAG, "NFC_INIT_ERROR");
					reset(true);
				} else if (msg.arg1 == CommUtils.Message.NFC_WRITE_ERROR.getCode()) {
					Message m = CommUtils.Message.NFC_WRITE_ERROR;
					guiHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
					Log.e(TAG, "NFC_WRITE_ERROR");
					reset(true);
				} else if (msg.arg1 == CommUtils.Message.NFC_PASS_TO_UPPER_LAYER.getCode()) {
					role.handle((PaymentMessage) msg.obj, protocolHandler);
				}
				break;
			case CommUtils.PAYMENT_LAYER_CATEGORY:
				if (msg.arg1 == CommUtils.Message.PAYMENT_ERROR_NO_AMOUNT_ENTERED.getCode()
						|| msg.arg1 == CommUtils.Message.PAYMENT_ERROR_UNEXPECTED.getCode()
						|| msg.arg1 == CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED.getCode()) {
					
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
					String errMsg = (String) msg.obj;
					PaymentMessage pm = new PaymentMessage(PaymentMessage.ERROR, errMsg.getBytes());
					write(pm);
					Log.e(TAG, "PaymentMessage.ERROR");
					reset(true);
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_TRANSCEIVE_PACKET.getCode()) {
					PaymentMessage pm = (PaymentMessage) msg.obj;
					Log.d(TAG, "got PAYMENT_TRANSCEIVE_PACKET");
					write(pm);
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_SELLER.getCode()) {
					//TODO: refactor, since no Transaction model class anymore
//					Transaction tx = (Transaction) msg.obj;
//					ClientController.updateUserAfterTransaction(isSeller, tx, context);
//					String s1 = String.format(CommUtils.Message.PAYMENT_SUCCESS_SELLER.getMessage(), CurrencyFormatter.formatBTC(tx.getAmount()), tx.getBuyerUsername());
//					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, s1).sendToTarget();
					Log.d(TAG, "success");
					reset(true);
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_OTHERDEVICE_UNEXPECTED.getCode()) {
					
					String errMsg = (String) msg.obj;
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
					Log.e(TAG, "PaymentMessage.OTHER ERROR: "+errMsg);
					reset(true);
				}
				break;
			}
		}
		
	};
	
	protected synchronized void handleResponse(NfcMessage msg) {
		Log.e(TAG, msg.toString());
		
		if (msg.getStatus() == NfcMessage.RESPONSE_ABORTED) {
			Log.e(TAG, "msg status = response aborted");
		}
		
		/*
		 * Because java uses signed bytes, we can not use a byte to count from
		 * 0-255. We have to store the sequence number as an integer instead.
		 */
		int sqNrOfMessage = (int) msg.getSequenceNumber() & 0xFF;
		if (sqNrOfMessage != lastSequenceNumberReceived+1) {
			//drop this message, since it has already been processed
			Log.d(TAG, "sequence number expecting: "+ (lastSequenceNumberReceived+1) + " received: "+sqNrOfMessage);
			return;
		}
		
		lastSequenceNumberReceived = sqNrOfMessage;
		
		byte status = (byte) (msg.getStatus() & ~NfcMessage.HAS_MORE_FRAGMENTS);
		
		switch (status) {
		case NfcMessage.WAITING_FOR_NEXT:
			// send next part
			break;
		case NfcMessage.RESPONSE:
			byte header = (byte) (msg.getStatus() & NfcMessage.HAS_MORE_FRAGMENTS);
			if (header == NfcMessage.HAS_MORE_FRAGMENTS) {
				msgReassembler.handleReassembly(msg);
				Log.d(TAG, "need more fragments");
				write(new NfcMessage(NfcMessage.WAITING_FOR_NEXT, SequenceNumberGenerator.getNextSequenceNumber(), null));
			} else {
				Log.d(TAG, "buyer has send his response after all the fragments have been delivered");
				msgReassembler.handleReassembly(msg);
				Message m = CommUtils.Message.NFC_PASS_TO_UPPER_LAYER;
				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, new PaymentMessage(msgReassembler.getData())).sendToTarget();
				msgReassembler.clear();
			}
			break;
		case NfcMessage.RESPONSE_ABORTED:
			Log.d(TAG, "buyer has aborted the protocol as well, since we told him to do so");
			break;
		case NfcMessage.WAIT_FOR_ANSWER:
			try {
				Thread.sleep(50);
				Log.d(TAG, "WAIT_FOR_ANSWER");
				write(new PaymentMessage(PaymentMessage.PROCEED, null));
			} catch (InterruptedException e) {
				Message m1 = CommUtils.Message.NFC_WRITE_ERROR;
				protocolHandler.obtainMessage(m1.getCategory(), m1.getCode(), 0).sendToTarget();
			}
			break;
		}
	}
	
}
