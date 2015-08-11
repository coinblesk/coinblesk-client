package ch.uzh.csg.coinblesk.client.payment;

import java.math.BigDecimal;
import java.security.PublicKey;

/**
 * Created by rvoellmy on 7/30/15.
 */
public class NfcPaymentListener {

    public void onPaymentRequestSent(BigDecimal amount) {
    }

    public void onPaymentReceived(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
    }

    public void onPaymentSent(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
    }

    public void onPaymentSuccess(BigDecimal amount, PublicKey receiverPubKey, String receiverUserName) {
    }


    public void onPaymentError(String msg) {
        onPaymentFinish(false);
    }

    /**
     * Called when the NFC protocol finished, with or without error.
     * @param success true if the NFC protocol finished successfully. If one party rejected a payment,
     *                it still means the NFC protocol finished successfully. False if there was an error.
     */
    public void onPaymentFinish(boolean success) {
    }

    /**
     * This method is called when the counterparty rejected the payment request
     */
    public void onPaymentRejected() {}

}
