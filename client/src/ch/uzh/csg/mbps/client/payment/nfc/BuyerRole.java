package ch.uzh.csg.mbps.client.payment.nfc;

import java.security.PublicKey;
import java.security.SignedObject;

import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.TransactionHandler;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.customserialization.security.KeyHandler;
import ch.uzh.csg.mbps.model.Transaction;

/**
 * This class handles the messages coming from the seller and returns the
 * appropriate messages according to the transaction protocol. Furthermore, this
 * class defines the transaction protocol for the buyer side.
 */
public class BuyerRole {
	
	private static final String TAG = "BuyerRole";
	
	private enum State {
		START,
		WAIT_FOR_USER_CONFIRMATION,
		WAIT_FOR_TRANSACTION_CONFIRMATION,
		END;
	}
	
	private State state; 
	
	private Transaction transactionRequest;
	
	public BuyerRole() {
		this.state = State.START;
		Log.d(TAG, "init buyer role");
	}
	
	/**
	 * A transaction protocol message, i.e. {@link PaymentMessage}, is processed
	 * according to the protocol. The protocolHandler is there for the case that
	 * the UI needs to be updated.
	 * 
	 * @param pm
	 *            the protocol message coming from the seller
	 * @param protocolHandler
	 *            the handler to update the UI asynchronously
	 * @return a protocol message to be passed to the seller - may return null
	 */
	public PaymentMessage handle(PaymentMessage pm, Handler protocolHandler) {
		switch (pm.getStatus()) {
		case PaymentMessage.PROCEED:
			return proceed(pm.getPayload(), protocolHandler);
		case PaymentMessage.ERROR:
			// it is not important which error message we choose, since the
			// message which was received will be propagated!
			handleUnexpectedError(pm, protocolHandler);
		}
		Log.e(TAG, "payment message status is not as expected!");
		return null;
	}

	

	private PaymentMessage proceed(byte[] bytes, Handler protocolHandler) {
		//TODO jeton: refactor
//		switch (state) {
//		case START:
//			Log.d(TAG, "Start payment");
//			//receiving payment request from seller
//			try {
//				transactionRequest = Serializer.deserialize(bytes);
//				Message m = CommUtils.Message.PAYMENT_UPDATE_GUI_AMOUNT;
//				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, transactionRequest).sendToTarget();
//				
//				state = State.WAIT_FOR_USER_CONFIRMATION;
//				return new PaymentMessage(PaymentMessage.WAIT_WITH_ANSWER, null);
//			} catch (Exception e) {
//				return handleUnexpectedError(protocolHandler, e);
//			}
//		case WAIT_FOR_USER_CONFIRMATION:
//			String s = new String(bytes);
//			Log.d(TAG, "wait for user confirmation: " + s);
//			if (s.equals(CommUtils.Message.PAYMENT_ACCEPT_PRESSED.toString())) {
//				try {
//					SignedObject signedTransaction = TransactionHandler.signPayment(transactionRequest);
//					state = State.WAIT_FOR_TRANSACTION_CONFIRMATION;
//					return new PaymentMessage(PaymentMessage.PROCEED, Serializer.serialize(signedTransaction));
//				} catch (Exception e) {
//					return handleUnexpectedError(protocolHandler, e);
//				}
//			} else {
//				state = State.END;
//				Message m = CommUtils.Message.PAYMENT_ERROR_BUYER_REJECTED;
//				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, transactionRequest).sendToTarget();
//				
//				String s1 = String.format(CommUtils.Message.PAYMENT_ERROR_BUYER_REJECTED.getMessage(), ClientController.getUser().getUsername(), CurrencyFormatter.formatBTC(transactionRequest.getAmount()));
//				return new PaymentMessage(PaymentMessage.ERROR, s1.getBytes());
//			}
//		case WAIT_FOR_TRANSACTION_CONFIRMATION:
//			Log.d(TAG, "wait for transaction confirmation");
//			try {
//				SignedObject signedForBuyer = Serializer.deserialize(bytes);
//				PublicKey serverPublicKey = KeyHandler.decodePublicKey(ClientController.getServerPublicKey());
//				
//				boolean verified = KeyHandler.verifyObject(signedForBuyer, serverPublicKey);
//				Transaction tx = KeyHandler.retrieveTransaction(signedForBuyer);
//				
//				if (verified && tx.equals(transactionRequest)) {
//					state = State.END;
//					Message m = CommUtils.Message.PAYMENT_SUCCESS_BUYER;
//					protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, transactionRequest).sendToTarget();
//					System.err.println("send ACK back!!");
//					return new PaymentMessage(PaymentMessage.PROCEED, "ACK".getBytes());
//				} else {
//					return handleUnexpectedError(protocolHandler, new RuntimeException("error in WAIT_FOR_TRANSACTION_CONFIRMATION"));
//				}
//			} catch (Exception e) {
//				return handleUnexpectedError(protocolHandler, e);
//			}
//		case END:
//			Log.d(TAG, "We finished, nothing to do here.");
//			break;
//		}
		return null;
	}
	
	private PaymentMessage handleUnexpectedError(Handler protocolHandler, Exception e) {
		Log.e(TAG, "handleUnexpectedError1", e);
		state = State.END;
		Message m = CommUtils.Message.PAYMENT_ERROR_UNEXPECTED;
		protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
		return new PaymentMessage(PaymentMessage.ERROR, m.getMessage().getBytes());
	}
	
	private void handleUnexpectedError(PaymentMessage pm, Handler protocolHandler) {
	    String message = new String(pm.getPayload());
	    Log.e(TAG, "handleUnexpectedError2: " + message);
	    state = State.END;
	    Message m = CommUtils.Message.PAYMENT_OTHERDEVICE_UNEXPECTED;
	    protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, message).sendToTarget();
    }
	
	public boolean isStateEnd() {
		return state == State.END;
	}
	
	public boolean isResume() {
		return state == State.WAIT_FOR_USER_CONFIRMATION;
	}
	
}
