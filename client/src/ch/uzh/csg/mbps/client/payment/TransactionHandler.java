package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.security.SignedObject;

import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.TransactionRequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.customserialization.ServerPaymentResponse;
import ch.uzh.csg.mbps.model.Transaction;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This is a helper class to handle the transactions initialization and
 * execution on the client.
 */
public class TransactionHandler {
	
	private static final String TAG = "TransactionHandler";
	
	/**
	 * Returns a Transaction request, which contains only the information about
	 * the seller (i.e. the authenticated user). This Transaction object needs
	 * to be passed to the buyer so that he can fill in his information (e.g.
	 * transaction number).
	 * 
	 * @return a Transaction object containing only the information about the
	 *         seller
	 */
	public static Transaction requestPayment(){
		UserAccount user = ClientController.getUser();
		
		Transaction tx = new Transaction();
		tx.setAmount(ReceivePaymentActivity.amountBTC);
		tx.setSellerUsername(user.getUsername());
		tx.setTransactionNrSeller(user.getTransactionNumber());
		if (Constants.inputUnit.equals(ReceivePaymentActivity.INPUT_UNIT_CHF)) {
			tx.setInputCurrency(ReceivePaymentActivity.INPUT_UNIT_CHF);
			tx.setAmountInputCurrency(ReceivePaymentActivity.inputUnitValue);
		} else {
			tx.setInputCurrency("");
			tx.setAmountInputCurrency(BigDecimal.ZERO);
		}
		return tx;
	}
	
	/**
	 * Signs a Transaction object filled with the information about the seller
	 * with the seller's private key (i.e. the authenticated user) and returns a
	 * SingedObject.
	 * 
	 * @param tx
	 *            the Transaction to be signed
	 * @return
	 * @throws IllegalArgumentException
	 *             if the necessary fields are empty or null
	 * @throws Exception
	 */
	public static SignedObject signPayment(Transaction tx) throws IllegalArgumentException, Exception {
		//TODO jeton: this is not needed here! remove!
		if (tx.getAmount() == null || tx.getSellerUsername() == null || tx.getSellerUsername().isEmpty())
			throw new IllegalArgumentException();
		
		UserAccount user = ClientController.getUser();
		tx.setBuyerUsername(user.getUsername());
		tx.setTransactionNrBuyer(user.getTransactionNumber());
//		return KeyHandler.signTransaction(tx, user.getPrivateKey());
		
		return null;
	}
	
	/**
	 * Creates a Transaction object containing information about the buyer and
	 * the seller and transmits it to the server in order to execute the
	 * transaction. The responseHandler is notified when the server responses.
	 * 
	 * @param signedTransactionBuyer
	 *            the Transaction object which is signed by the buyer and
	 *            returned to the seller in order to be transmitted to the
	 *            server
	 * @param responseHandler
	 *            handles the response coming from the server (if the
	 *            transaction has been accepted or refused incl. error message)
	 * @return returns the complete Transaction object, containing both
	 *         information about the seller and the buyer
	 * @throws Exception
	 */
	public static Transaction newTransaction(SignedObject signedTransactionBuyer, Handler responseHandler) throws Exception {
		//TODO jeton: implement
//		Transaction transactionBuyer = KeyHandler.retrieveTransaction(signedTransactionBuyer);
//		
//		Transaction transactionSeller = requestPayment();
//		transactionSeller.setBuyerUsername(transactionBuyer.getBuyerUsername());
//		transactionSeller.setTransactionNrBuyer(transactionBuyer.getTransactionNrBuyer());
//		
//		SignedObject signedTransaction = KeyHandler.signTransaction(transactionSeller, ClientController.getUser().getPrivateKey());
//		CreateTransactionTransferObject ctto = new CreateTransactionTransferObject(signedTransactionBuyer, signedTransaction);
//		
//		responseListener = new ServerResponseListener(responseHandler);
//		//send Transaction to Server
//		launchRequest(ctto);
//		
//		return transactionSeller;
		return null;
	}

	private static void launchRequest(CreateTransactionTransferObject ctto) {
		RequestTask sendTransaction = new TransactionRequestTask(responseListener, ctto);
		sendTransaction.execute();
	}
	
	private static ServerResponseListener responseListener = null;
	
	private static class ServerResponseListener implements IAsyncTaskCompleteListener<CustomResponseObject> {
		
		private final long startTime = System.currentTimeMillis();
		private Handler responseHandler;
		
		private ServerResponseListener(Handler responseHandler) {
			this.responseHandler = responseHandler;
		}
		
		public void onTaskComplete(CustomResponseObject response) {
			try {
				ServerPaymentResponse serverPaymentResponse = response.getCreateTransactionTO().getServerPaymentResponse();
				
				PaymentMessage pm;
				if (response.isSuccessful())
					pm = new PaymentMessage(PaymentMessage.PROCEED, serverPaymentResponse.encode());
				else
					pm = new PaymentMessage(PaymentMessage.PROCEED, (CommUtils.Message.PAYMENT_ERROR_SERVER_REFUSED.getMessage()+response.getMessage()).getBytes());
				
				Message m = CommUtils.Message.NFC_PASS_TO_UPPER_LAYER;
				responseHandler.obtainMessage(m.getCategory(), m.getCode(), 0, pm).sendToTarget();
			} catch (Exception e) {
				Message m = CommUtils.Message.PAYMENT_ERROR_UNEXPECTED;
				responseHandler.obtainMessage(m.getCategory(), m.getCode(), 0, m.getMessage()).sendToTarget();
			} finally {
				Log.d(TAG, "Time from server: "+(System.currentTimeMillis()-startTime)+"ms");
			}
		}
	}

}
