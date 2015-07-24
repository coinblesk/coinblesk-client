package ch.uzh.csg.coinblesk.client.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PaymentRequestReceiver extends BroadcastReceiver {

    private PaymentRequest activePaymentRequest;

    public PaymentRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activePaymentRequest = PaymentRequest.fromIntent(intent);
    }

    public PaymentRequest getActivePaymentRequest() {
        PaymentRequest activePaymentRequest = this.activePaymentRequest;
        this.activePaymentRequest = null;
        return activePaymentRequest;
    }

    public void inactivatePaymentRequest() {
        activePaymentRequest = null;
    }

    public boolean hasActivePaymentRequest() {
        return null != activePaymentRequest;
    }

}
