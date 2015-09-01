package ch.uzh.csg.coinblesk.client.payment;

import java.math.BigDecimal;
import java.security.PublicKey;

/**
 * Created by rvoellmy on 7/30/15.
 */
public class NfcPaymentListener {

    /**
     * Called when the NFC payment is successfully finished
     * @param amount
     * @param senderPubKey
     * @param senderUserName
     */
    public void onPaymentReceived(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
    }

    /**
     * Called when the payment was successfully sent
     * @param amount
     * @param senderPubKey
     * @param senderUserName
     */
    public void onPaymentSent(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {

    }

    /**
     * Called if the NFC payment failed.
     * @param msg
     */
    public void onPaymentError(String msg) {
    }

    /**
     * Called when the other party rejected the payment
     * @param user
     */
    public void onPaymentRejected(String user) {

    }

}
