package ch.uzh.csg.mbps.client.payment.nfc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.AbstractPaymentActivity;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;
import ch.uzh.csg.mbps.client.payment.nfc.transceiver.ExternalNfcTransceiver;
import ch.uzh.csg.mbps.client.payment.nfc.transceiver.NfcMessageReassembler;
import ch.uzh.csg.mbps.client.payment.nfc.transceiver.NfcMessageSplitter;
import ch.uzh.csg.mbps.client.payment.nfc.transceiver.SequenceNumberGenerator;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.model.Transaction;

/**
 * This is the card emulation service. In order to be notified by the Android OS
 * when a NFC contact happens, one has to register the APDU in the manifest.xml
 * with a specific AID (application id).
 * 
 * On each new NFC contact, the Android OS instantiates a new Object of this
 * class. Therefore, some class members need to be static.
 * 
 * Depending on the NFC controller of the NfcTransceiver initiating the
 * communication, this HostApduService needs or does not need to fragment the
 * returning messages.
 * 
 * This version of the CustomHostApduService allows resuming sessions, but is
 * unfortunately not running 100% reliably.
 */
public class CustomHostApduService extends HostApduService {
	public static final byte RESUME_PROTOCOL = 0x01;
	public static final byte START_PROTOCOL = 0x02;

	private static Handler guiHandler;
	private static BuyerRole role;

	private static AbstractPaymentActivity activity;
	private final static boolean IS_SELLER = false;

	private static boolean responsePostponed = false;
	private static boolean userTookDecision = false;
	private static boolean transactionAccepted = false;

	private final NfcMessageSplitter messageSplitter = new NfcMessageSplitter(ExternalNfcTransceiver.MAX_WRITE_LENGTH);
	private NfcMessageReassembler messageReassembler = new NfcMessageReassembler();
	private ArrayList<NfcMessage> nfcMessages = null;
	private int index = 0;

	private int lastSequenceNumberReceived = 0;
	
	private static final String TAG = "CustomHostApduService";
	
	private TearDownTask current;
	private final Timer timer = new Timer();

	/*
	 * The empty constructor is needed by android to instantiate the service.
	 * Handler and BuyerRole are therefore static.
	 */
	public CustomHostApduService() {
		if (activity != null) {
			activity.setHCEService(this);
		} else {
			Log.e(TAG, "cannot start service, as activity was not set yet.");
		}
	}

	public static void init(Handler guiHandler, AbstractPaymentActivity activity) {
		CustomHostApduService.guiHandler = guiHandler;
		CustomHostApduService.activity = activity;

		role = null;
		responsePostponed = userTookDecision = transactionAccepted = false;
		SequenceNumberGenerator.resetSequenceNumber();
	}

	@Override
	public void onDeactivated(int reason) {
		Log.d(TAG, "deactivated due to " + (reason == HostApduService.DEACTIVATION_LINK_LOSS ? "link loss" : "deselected") + "("+reason+")");
		// postpone tear down for a moment
		synchronized (timer) {
			if (current == null && role !=null && !role.isStateEnd()) {
				Log.d(TAG, "schedule shutdown");
				current = new TearDownTask();
				timer.schedule(current, Constants.RESUME_TIMEOUT);
			}
		}
	}

	private class TearDownTask extends TimerTask {
		@Override
		public void run() {
			synchronized (timer) {
				if (activity != null && current != null && role !=null && !role.isStateEnd()) {
					Message m = CommUtils.Message.PAYMENT_ERROR_UNEXPECTED;
					protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
					reset();
				} else {
					Log.d(TAG, "no can do schedule shutdown");
				}
			}
		}
	}
	
	private void cancelShutdownTimer() {
	    synchronized (timer) {
			if (current != null) {
				current.cancel();
				current = null;
			}
		}
    }

	@Override
	public byte[] processCommandApdu(byte[] bytes, Bundle extras) {
		cancelShutdownTimer();

		if (activity == null) {
			Log.e(TAG, "The user is not in the \"pay-activity\" but tries to establish a NFC connection.");
			return null;
		}
		
		if (selectAidApduResume(bytes)) {
			SequenceNumberGenerator.resetSequenceNumber();
			lastSequenceNumberReceived = 0;
			Log.e(TAG, "resume selected: "+Arrays.toString(bytes));
			return new NfcMessage(NfcMessage.AID_SELECTED, SequenceNumberGenerator.getNextSequenceNumber(), new byte[]{RESUME_PROTOCOL}).getData();
		} else if (selectAidApdu(bytes)) {
			SequenceNumberGenerator.resetSequenceNumber();
			lastSequenceNumberReceived = 0;
			Log.d(TAG, "AID selected");
			
			if (role == null) {
				role = new BuyerRole();	
			} else if (role.isResume()){
				Log.e(TAG, "send to resume protocol");
				return new NfcMessage(NfcMessage.AID_SELECTED, SequenceNumberGenerator.getNextSequenceNumber(), new byte[]{RESUME_PROTOCOL}).getData();
			}
			return new NfcMessage(NfcMessage.AID_SELECTED, SequenceNumberGenerator.getNextSequenceNumber(), new byte[]{START_PROTOCOL}).getData();
		} else if(selectedReadBinary(bytes)) {
			return new byte[1];
		} else if (role != null) {
			NfcMessage incoming = new NfcMessage(bytes);
			Log.d(TAG, "rcv msg: "+incoming);
			NfcMessage reply = handleNfcMessage(incoming);
			if (reply == null) {
				Log.d(TAG, "ignore msg with length " + bytes.length);
				return null;
			}
			
			Log.e(TAG, "returning msg: "+reply);
			
			if( reply.getData() == null) {
				Log.d(TAG, "ret nzll");
				return null;
			} else {
				Log.d(TAG, "ret data:"+Arrays.toString(reply.getData()));
				return reply.getData();
			}
		} else {
			Log.e(TAG, "We already shutdown");
			return null;
		}
	}

	private boolean selectAidApduResume(byte[] bytes) {
		Log.e(TAG, "RESUME? "+ Arrays.toString(bytes));
		if( bytes.length >= 2 && bytes[0] == Constants.CLA_INS_P1_P2[0] && bytes[1] == Constants.CLA_INS_P1_P2[1]) {
			return bytes[bytes.length-2] == 0x12;
		}
		return false;
    }

	private boolean selectedReadBinary(byte[] bytes) {
	    return Arrays.equals(bytes, Constants.READ_BINARY);
    }

	private NfcMessage handleNfcMessage(NfcMessage nfcMessage) {
		/*
		 * Because java uses signed bytes, we can not use a byte to count from
		 * 0-255. We have to store the sequence number as an integer instead.
		 */
		int sqNrOfMessage = (int) nfcMessage.getSequenceNumber() & 0xFF;
		if (sqNrOfMessage != lastSequenceNumberReceived+1) {
			// drop this message, since it has already been processed
			Log.d(TAG, "sequence number expecting: "+ (lastSequenceNumberReceived+1) + " received: "+sqNrOfMessage);
			return null;
		}

		lastSequenceNumberReceived = sqNrOfMessage;

		byte status = (byte) (nfcMessage.getStatus());

		switch (status) {
		case NfcMessage.HAS_MORE_FRAGMENTS:
			Log.d(TAG, "has more fragments");
			messageReassembler.handleReassembly(nfcMessage);
			return new NfcMessage(NfcMessage.WAITING_FOR_NEXT, SequenceNumberGenerator.getNextSequenceNumber(), null);
		case NfcMessage.DEFAULT:
			if (responsePostponed) {
				Log.e(TAG, "in here "+responsePostponed);
				if (!userTookDecision) {
					Log.d(TAG, "waiting for user");
					return new NfcMessage(NfcMessage.WAIT_FOR_ANSWER, SequenceNumberGenerator.getNextSequenceNumber(), null);
				} else {
					Log.d(TAG, "user decided");
					Message m = transactionAccepted ? CommUtils.Message.PAYMENT_ACCEPT_PRESSED : CommUtils.Message.PAYMENT_REJECT_PRESSED;
					PaymentMessage pm = role.handle(new PaymentMessage(PaymentMessage.PROCEED, m.toString().getBytes()), protocolHandler);
					responsePostponed = userTookDecision = transactionAccepted = false;
					return returnResponse(pm);
				}
			}
			
			messageReassembler.handleReassembly(nfcMessage);
			
			if (nfcMessage.getPayload() != null && nfcMessage.getPayload().length == 0) {
				Message m = CommUtils.Message.PAYMENT_ERROR_UNEXPECTED;
				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
				reset();	
				return null;
			}

			Log.d(TAG, "handle default");
			
			Log.e(TAG, Arrays.toString(messageReassembler.getData()));
			
			PaymentMessage pm = role.handle(new PaymentMessage(messageReassembler.getData()), protocolHandler);
			messageReassembler.clear();
			if (pm == null) {
				Log.d(TAG, "seller has encountered an error and told us to abort");
				return new NfcMessage(NfcMessage.RESPONSE_ABORTED, SequenceNumberGenerator.getNextSequenceNumber(), null);
			} else if (pm.getStatus() == PaymentMessage.WAIT_WITH_ANSWER) {
				Log.d(TAG, "wait for answer");
				responsePostponed = true;
				return new NfcMessage(NfcMessage.WAIT_FOR_ANSWER, SequenceNumberGenerator.getNextSequenceNumber(), null);
			} else {
				return returnResponse(pm);
			}
		case NfcMessage.WAITING_FOR_NEXT:
			if (nfcMessages != null && !nfcMessages.isEmpty() && index < nfcMessages.size()) {
				NfcMessage toSend = nfcMessages.get(index);
				if (toSend.getStatus() != NfcMessage.HAS_MORE_FRAGMENTS) {
					index = 0;
					nfcMessages = null;
				}

				byte status1 = (byte) (toSend.getStatus() | NfcMessage.RESPONSE);
				toSend.setStatus(status1);
				index++;
				Log.d(TAG, "send next");
				return toSend;
			} else {
				Log.e(TAG, "waiting for next - else statement");
				return null;
			}
		default:
			Log.d(TAG, "some kind of keep alive message received, return empty NfcMessage");
			Log.e(TAG, Arrays.toString(nfcMessage.getData()));
			return new NfcMessage(NfcMessage.RESPONSE, SequenceNumberGenerator.getNextSequenceNumber(), null);
		}
	}

	private NfcMessage returnResponse(PaymentMessage pm) {
		nfcMessages = messageSplitter.getFragments(pm.getData());
		if (nfcMessages.size() == 1) {
			NfcMessage nfcMessage = nfcMessages.get(0);
			nfcMessages = null;
			nfcMessage.setStatus(NfcMessage.RESPONSE);
			return nfcMessage;
		} else {
			byte status1;
			if (index == nfcMessages.size()-1) {
				status1 = (byte) (NfcMessage.RESPONSE | NfcMessage.DEFAULT);
			} else {
				status1 = (byte) (NfcMessage.RESPONSE | NfcMessage.HAS_MORE_FRAGMENTS);
			}
			nfcMessages.get(index).setStatus(status1);
			return nfcMessages.get(index++);
		}
	}

	public void setUserDecision(boolean transactionAccepted) {
		userTookDecision = true;
		CustomHostApduService.transactionAccepted = transactionAccepted;
	}

	private boolean selectAidApdu(byte[] bytes) {
		return bytes.length >= 2 && bytes[0] == Constants.CLA_INS_P1_P2[0] && bytes[1] == Constants.CLA_INS_P1_P2[1];
	}

	private void reset() {
		Log.d(TAG, "shutdown");
		SequenceNumberGenerator.resetSequenceNumber();
		lastSequenceNumberReceived = 0;
		messageReassembler.clear();
		role = null;
		responsePostponed = false;
	}

	@SuppressLint("HandlerLeak")
	private final Handler protocolHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CommUtils.PAYMENT_LAYER_CATEGORY:
				if (msg.arg1 == CommUtils.Message.PAYMENT_UPDATE_GUI_AMOUNT.getCode()) {
					Log.d(TAG, "PAYMENT_UPDATE_GUI_AMOUNT");
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_ERROR_UNEXPECTED.getCode() || msg.arg1 == CommUtils.Message.PAYMENT_OTHERDEVICE_UNEXPECTED.getCode()) {
					Log.e(TAG, "PAYMENT_ERROR_UNEXPECTED");
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
					reset();
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_SUCCESS_BUYER.getCode()) {
					Log.d(TAG, "PAYMENT_SUCCESS_BUYER");
					Transaction tx = (Transaction) msg.obj;
					ClientController.updateUserAfterTransaction(IS_SELLER, tx, activity.getApplicationContext());
					String s = String.format(CommUtils.Message.PAYMENT_SUCCESS_BUYER.getMessage(), CurrencyFormatter.formatBTC(tx.getAmount()), tx.getSellerUsername());
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, s).sendToTarget();
					reset();
				} else if (msg.arg1 == CommUtils.Message.PAYMENT_ERROR_BUYER_REJECTED.getCode()) {
					Log.e(TAG, "PAYMENT_ERROR_BUYER_REJECTED");
					guiHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
					reset();
				}
				break;
			}
		}

	};

}
