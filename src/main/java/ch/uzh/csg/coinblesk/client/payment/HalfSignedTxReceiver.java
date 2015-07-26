package ch.uzh.csg.coinblesk.client.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Preconditions;

import java.io.UnsupportedEncodingException;

import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;

/**
 * Quick and dirty broadcast receiver for half signed transactions.
 */
public class HalfSignedTxReceiver extends BroadcastReceiver {

    public static final String ACTION = "ch.uzh.coinblesk.HALF_SIGNED_TX";

    private final static String extraKey = "sigReq";

    public interface HalfSignedTxListener {
        void onTxReceived(byte[] serverSigRequest);
    }

    private HalfSignedTxListener listener;

    public HalfSignedTxReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String sigReqJson = intent.getExtras().getString(extraKey, null);
        Preconditions.checkNotNull(sigReqJson, "Received half signed tx broadcast but transaction is null");

        try {
            if(listener != null) listener.onTxReceived(sigReqJson.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setListener(HalfSignedTxListener listener) {
        this.listener = listener;
    }

    public static Intent createIntent(ServerSignatureRequestTransferObject sigReq) {
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.putExtra(extraKey, sigReq.toJson());
        return intent;
    }

}
