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
        onPaymentFinish(true);
    }

    public void onPaymentError(String msg) {
        onPaymentFinish(false);
    }

    public void onPaymentFinish(boolean success) {
    }

}
