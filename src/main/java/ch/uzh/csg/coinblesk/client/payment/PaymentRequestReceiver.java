package ch.uzh.csg.coinblesk.client.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PaymentRequestReceiver extends BroadcastReceiver {

    public static final String ACTION = "ch.uzh.coinblesk.PAYMENT_REQUEST";

    private PaymentRequest activePaymentRequest;

    public PaymentRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activePaymentRequest = PaymentRequest.fromIntent(intent);
    }

    public PaymentRequest getActivePaymentRequest() {
        return activePaymentRequest;
    }

    public void inactivatePaymentRequest() {
        activePaymentRequest = null;
    }

    public boolean hasActivePaymentRequest() {
        return null != activePaymentRequest;
    }

}
