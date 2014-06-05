package ch.uzh.csg.mbps.client.payment.nfc;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.security.SignedObject;
import java.util.Arrays;

import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.TransactionHandler;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.customserialization.security.KeyHandler;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;

/**
 * This class handles the messages coming from the buyer and returns the
 * appropriate messages according to the transaction protocol. Furthermore, this
 * class defines the transaction protocol for the seller side.
 */
public class SellerRole {

	private enum State {
		START,
		WAIT_FOR_PAYMENT_CONFIRMATION,
		WAIT_FOR_TRANSACTION_CONFIRMATION,
		WAIT_FOR_TRANSACTION_ACKNOWLEDGEMENT,
		END;
	}
	
	private State state;
	
	//TODO: refactor, since no Transaction model class anymore
//	private Transaction transactionRequest;
	
	private static final String TAG = "SellerRole";
	
	public SellerRole() {
		this.state = State.START;
		Log.d(TAG, "init seller role");
	}
	
	/**
	 * This is the starting point of the NFC transaction protocol. It
	 * initializes creating the first protocol message and sending it to the
	 * client.
	 * 
	 * @param protocolHandler
	 *            the handler to update the UI asynchronously
	 */
	public void init(Handler protocolHandler) {
		handle(new PaymentMessage(PaymentMessage.PROCEED, null), protocolHandler);
	}
	
	/**
	 * A transaction protocol message, i.e. {@link PaymentMessage}, is processed
	 * according to the protocol. The protocolHandler is there for the case that
	 * the UI needs to be updated.
	 * 
	 * @param pm
	 *            the protocol message coming from the buyer
	 * @param protocolHandler
	 *            the handler to update the UI asynchronously
	 */
	public synchronized void handle(PaymentMessage pm, Handler protocolHandler) {
		switch (pm.getStatus()) {
		case PaymentMessage.PROCEED:
			proceed(pm.getPayload(), protocolHandler);
			break;
		case PaymentMessage.ERROR:
			Log.e(TAG, "PaymentMessage.ERROR: it is not important which error message we choose, since the message which was received will be propagated");
			Message m = CommUtils.Message.PAYMENT_OTHERDEVICE_UNEXPECTED;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, new String(pm.getPayload())).sendToTarget();
			break;
		}
	}

	private void proceed(byte[] bytes, Handler protocolHandler) {
		//TODO jeton: refactor
//		switch (state) {
//		case START:
//			try {
//				// send payment request to buyer
//				Transaction tx = TransactionHandler.requestPayment();
//				if (tx.getAmount().compareTo(BigDecimal.ZERO) == 0) {
//					Log.e(TAG, "amount is 0"); 
//					// the seller has not entered an amount or entered 0
//					state = State.END;
//					Message m = CommUtils.Message.PAYMENT_ERROR_NO_AMOUNT_ENTERED;
//					protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
//				} else {
//					Log.d(TAG, "wait for payment confirmation");
//					state = State.WAIT_FOR_PAYMENT_CONFIRMATION;
//					PaymentMessage pm = new PaymentMessage(PaymentMessage.PROCEED, Serializer.serialize(tx));
//					Message m = CommUtils.Message.PAYMENT_TRANSCEIVE_PACKET;
//					protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, pm).sendToTarget();
//				}
//			} catch (Exception e) {
//				Log.e(TAG, "exception in START", e);
//				handleUnexpectedError(protocolHandler);
//			}
//			break;
//		case WAIT_FOR_PAYMENT_CONFIRMATION:
//			try {
//				SignedObject signedObjectFromByteArray = Serializer.deserialize(bytes);
//				Log.d(TAG, "sign own Payment and do Server Request (get signed transaction confirmation back)"); 
//				transactionRequest = TransactionHandler.newTransaction(signedObjectFromByteArray, protocolHandler);
//				state = State.WAIT_FOR_TRANSACTION_CONFIRMATION;
//				
//			} catch (Exception e) {
//				Log.e(TAG, "exception in WAIT_FOR_PAYMENT_CONFIRMATION", e);
//				handleUnexpectedError(protocolHandler);
//			}
//			break;
//		case WAIT_FOR_TRANSACTION_CONFIRMATION:
//			Log.d(TAG, "getting the response from the server");
//			String s = new String(bytes);
//			if (s.startsWith(CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED.getMessage())) {
//				Log.e(TAG, "server refused payment");
//				state = State.END;
//				Message m = CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED;
//				String errMsg = s.substring(CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED.getMessage().length());
//				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, errMsg).sendToTarget();
//				break;
//			}
//			
//			try {
//				CreateTransactionTransferObject ctto = Serializer.deserialize(bytes);
//				
//				SignedObject signedForSeller = ctto.getSellerSignedObject();
//				PublicKey serverPublicKey = KeyHandler.decodePublicKey(ClientController.getServerPublicKey());
//				
//				boolean verified = KeyHandler.verifyObject(signedForSeller, serverPublicKey);
//				Transaction tx = KeyHandler.retrieveTransaction(signedForSeller);
//				
//				if (verified && tx.equals(transactionRequest)) {
//					state = State.WAIT_FOR_TRANSACTION_ACKNOWLEDGEMENT;
//					
//					PaymentMessage pm = new PaymentMessage(PaymentMessage.PROCEED, Serializer.serialize(ctto.getBuyerSignedObject()));
//					Message m = CommUtils.Message.PAYMENT_TRANSCEIVE_PACKET;
//					protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, pm).sendToTarget();
//					
//					//TOOD: not sure if needed at all. 
//					CatchAckMsgLostTask checkerTask = new CatchAckMsgLostTask(protocolHandler);
//					Thread t = new Thread(checkerTask);
//					t.start();
//					Log.d(TAG, "verified");
//				} else {
//					Log.e(TAG, "verification failed");
//					handleUnexpectedError(protocolHandler);
//				}
//			} catch (Exception e) {
//				Log.e(TAG, "exception in WAIT_FOR_TRANSACTION_CONFIRMATION", e);
//				handleUnexpectedError(protocolHandler);
//			}
//			break;
//		case WAIT_FOR_TRANSACTION_ACKNOWLEDGEMENT:
//			//from buyer
//			Log.d(TAG, "got ack "+ bytes.length);
//			if(Arrays.equals(bytes, "ACK".getBytes())) {
//				state = State.END;
//				Message m = CommUtils.Message.PAYMENT_SUCCESS_SELLER;
//				protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, transactionRequest).sendToTarget();
//			}
//			break;
//		case END:
//			Log.d(TAG, "nothing to do, move along");
//			break;
//		}
	}
	
	private void handleUnexpectedError(Handler protocolHandler) {
		Log.e(TAG, "handleUnexpectedError");
		state = State.END;
		Message m = CommUtils.Message.PAYMENT_ERROR_UNEXPECTED;
		protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
	}
	
	public boolean isStateEnd() {
		return state == State.END;
	}
	
	/**
	 * This task is responsible for terminating the protocol if the ACK message
	 * from the buyer gets lost. In this case, the protocol has terminated
	 * successfully anyways.
	 */
	private class CatchAckMsgLostTask implements Runnable {
		private long timeConfirmationSend;
		private Handler protocolHandler;
		
		protected CatchAckMsgLostTask(Handler protocolHandler) {
			this.protocolHandler = protocolHandler;
			timeConfirmationSend = System.currentTimeMillis();
		}
		
		public void run() {
			while (true) {
				if ((System.currentTimeMillis() - timeConfirmationSend) < Constants.BUYER_ACK_TIMEOUT) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						sendMsgAndEnd();
						break;
					}
				}  else {
					if (state == State.WAIT_FOR_TRANSACTION_ACKNOWLEDGEMENT) {
						sendMsgAndEnd();
					}
					
					break;
				}
			}
		}
		
		private void sendMsgAndEnd() {
			Log.d(TAG, "late send success");
			state = State.END;
			Message m = CommUtils.Message.PAYMENT_SUCCESS_SELLER;
			//TODO: refactor, since no Transaction model class anymore
//			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0, transactionRequest).sendToTarget();
		}
	}
	
}
