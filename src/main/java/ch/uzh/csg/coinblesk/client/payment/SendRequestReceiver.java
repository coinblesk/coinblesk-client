package ch.uzh.csg.coinblesk.client.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendRequestReceiver extends BroadcastReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestReceiver.class);

    public static final String ACTION = "ch.uzh.coinblesk.SEND_REQUEST";

    private SendRequest activeSendRequest;

    public SendRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.debug("Received send request broadcast");
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
