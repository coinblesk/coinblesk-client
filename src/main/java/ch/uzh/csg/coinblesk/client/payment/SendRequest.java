package ch.uzh.csg.coinblesk.client.payment;

import android.content.Intent;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.Coin;

import java.math.BigDecimal;


public class SendRequest {

    private final long satoshi;

    public SendRequest(long satoshi) {
        this.satoshi = satoshi;
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction(SendRequestReceiver.ACTION);
        intent.putExtra("satoshi", satoshi);
        return intent;
    }

    public static SendRequest fromIntent(Intent intent) {
        long satoshi = intent.getLongExtra("satoshi", -1);
        Preconditions.checkState(satoshi > 0, "Amount cannot be negative. Forgot to put the extra in the intent?");
        return new SendRequest(satoshi);
    }

    public static SendRequest create(BigDecimal amount) {
        return new SendRequest(Coin.parseCoin(amount.toString()).getValue());
    }

    public long getSatoshi() {
        return satoshi;
    }

}
