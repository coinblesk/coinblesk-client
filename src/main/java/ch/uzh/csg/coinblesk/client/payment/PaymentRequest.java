package ch.uzh.csg.coinblesk.client.payment;

import android.content.Intent;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.Coin;

import java.math.BigDecimal;

/**
 * Created by rvoellmy on 7/23/15.
 */
public class PaymentRequest {

    public final long satoshi;
    public final String user;
    public final String address;

    public PaymentRequest(long satoshi, String user, String address) {
        this.satoshi = satoshi;
        this.user = user;
        this.address = address;
    }

    public long getSatoshi() {
        return satoshi;
    }

    public String getUser() {
        return user;
    }

    public String getAddress() {
        return address;
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction(PaymentRequestReceiver.ACTION);
        intent.putExtra("satoshi", satoshi);
        intent.putExtra("user", user);
        intent.putExtra("address", address);
        return intent;
    }

    public static PaymentRequest fromIntent(Intent intent) {
        String user = intent.getExtras().getString("user");
        long satoshi = intent.getLongExtra("satoshi", -1);
        Preconditions.checkState(satoshi > 0, "Amount cannot be negative. Forgot to put the extra in the intent?");
        String address = intent.getExtras().getString("address");
        return new PaymentRequest(satoshi, user, address);
    }

    public static PaymentRequest create(BigDecimal amount, String user, String address) {

        return new PaymentRequest(Coin.parseCoin(amount.toString()).getValue(), user, address);
    }

}
