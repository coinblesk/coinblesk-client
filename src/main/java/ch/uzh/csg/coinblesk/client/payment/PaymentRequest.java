package ch.uzh.csg.coinblesk.client.payment;

import android.content.Intent;

import com.google.common.base.Preconditions;

/**
 * Created by rvoellmy on 7/23/15.
 */
public class PaymentRequest {

    public static final String ACTION = PaymentRequest.class.getName();

    public final long satoshi;

    public PaymentRequest(long satoshi) {
        this.satoshi = satoshi;
    }

    public long getSatoshi() {
        return satoshi;
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.putExtra("satoshi", satoshi);
        return intent;
    }

    public static PaymentRequest fromIntent(Intent intent) {
        long satoshi = intent.getLongExtra("satoshi", -1);
        Preconditions.checkState(satoshi > 0, "Amount cannot be negative. Forgot to put the extra in the intent?");
        return new PaymentRequest(satoshi);
    }

}
