package ch.uzh.csg.coinblesk.client.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SendRequestReceiver extends BroadcastReceiver {

    public static final String ACTION = "ch.uzh.coinblesk.SEND_REQUEST";

    private SendRequest activeSendRequest;

    public SendRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activeSendRequest = SendRequest.fromIntent(intent);
    }

    public SendRequest getActiveSendRequest() {
        return activeSendRequest;
    }

    public void inactivateSendRequest() {
        activeSendRequest = null;
    }

    public boolean hasActiveSendRequest() {
        return null != activeSendRequest;
    }

}
