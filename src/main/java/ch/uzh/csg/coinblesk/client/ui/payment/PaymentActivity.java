package ch.uzh.csg.coinblesk.client.ui.payment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicReference;

import ch.uzh.csg.coinblesk.JsonConverter;
import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;
import ch.uzh.csg.coinblesk.client.payment.HalfSignedTxReceiver;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequest;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequestReceiver;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;
import ch.uzh.csg.comm.NfcInitiatorHandler;
import ch.uzh.csg.comm.NfcResponseHandler;
import ch.uzh.csg.comm.ResponseLater;
import ch.uzh.csg.nfclib.NfcSetup;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class PaymentActivity extends WalletActivity {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentActivity.class);

    protected boolean isSeller;
    protected BigDecimal exchangeRate;
    private ProgressDialog progressDialog;
    private boolean destroyed = false;
    protected boolean paymentAccepted = false;

    private static NfcPaymentListener listener;
    protected static NfcSetup initiator;

    private PaymentRequestReceiver paymentRequestReceiver;
    private HalfSignedTxReceiver halfSignedTxReceiver;


    public class DefaultNfcListener extends NfcPaymentListener {
        @Override
        public void onPaymentFinish(boolean success) {
            super.onPaymentFinish(success);
            paymentRequestReceiver.inactivatePaymentRequest();
            dismissNfcInProgressDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register receivers
        this.paymentRequestReceiver = new PaymentRequestReceiver();
        registerReceiver(paymentRequestReceiver, new IntentFilter(PaymentRequestReceiver.ACTION));

        this.halfSignedTxReceiver = new HalfSignedTxReceiver();
        registerReceiver(halfSignedTxReceiver, new IntentFilter(HalfSignedTxReceiver.ACTION));

        checkNfc(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(paymentRequestReceiver);
        unregisterReceiver(halfSignedTxReceiver);
    }

    /**
     * Starts the NFC progress dialog. As long as the dialog is running other
     * touch actions are ignored.
     *
     * @param isInProgress determines if NFC is already inProgress or
     *                     needs to be established first
     */
    protected void showNfcProgressDialog(final boolean isInProgress) {
        runOnUiThread(new Runnable() {
            public void run() {
                getNfcInProgressDialog(isInProgress).show();
            }
        });
    }

    public void setNfcPaymentListener(NfcPaymentListener listener) {
        this.listener = listener;
    }

    /**
     * Closes the progress dialog.
     */
    public void dismissNfcInProgressDialog() {
        if (progressDialog != null && !destroyed) {
            progressDialog.dismiss();
        }
    }

    private ProgressDialog getNfcInProgressDialog(boolean isInProgress) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.animation_nfc_in_progress));
        }
        if (isInProgress) {
            progressDialog.setMessage(getResources().getString(R.string.nfc_in_progress_dialog));
        } else {
            progressDialog.setMessage(getResources().getString(R.string.establishNfcConnectionInfo));
        }

        return progressDialog;
    }

    /**
     * Displays a custom dialog with a given message and an image indicating if task was successful or not.
     *
     * @param message      to be displayed to the user
     * @param isSuccessful boolean to indicate if task was successful
     */
    protected void showDialog(String message, boolean isSuccessful) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (isSuccessful) {
            playSound();
            builder.setTitle(getResources().getString(R.string.payment_success))
                    .setIcon(getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()));
        } else {
            builder.setTitle(getResources().getString(R.string.payment_failure))
                    .setIcon(getResources().getIdentifier("ic_payment_failed", "drawable", getPackageName()));
        }
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
     * null and open up NFC settings.
     *
     * @param context
     * @return
     */
    protected void checkNfc(Context context) {
        NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (nfcAdapter == null) {
            showDialog(getResources().getString(R.string.nfc_enable_title), R.drawable.ic_alerts_and_states_warning, getResources().getString(R.string.nfc_no_adapter));
        }
        if (nfcAdapter.isEnabled() == false) {
            enableNFC(context);
        }
    }

    private void enableNFC(final Context context) {
        final AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle(getResources().getString(R.string.nfc_enable_title));
        alertbox.setMessage(getResources().getString(R.string.nfc_enable_message));
        alertbox.setPositiveButton(getResources().getString(R.string.nfc_enable_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
        alertbox.setNegativeButton(getResources().getString(R.string.nfc_enable_button_abort), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertbox.show();
    }


    enum State {
        INIT, FIRST, SECOND
    }


    /**
     * Initializes NFC adapter and user payment information.
     */
    protected void initializeNFC() throws Exception {

        initiator = new NfcSetup(new NfcInitiatorHandler() {

            State current = State.INIT;
            KeyPair keyPair = PaymentProtocol.generateKeys();
            PublicKey remotePubKey;
            byte[] halfSignedTx;
            RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> signTask;
            AtomicReference<byte[]> result = new AtomicReference<>();

            Thread t;
            AtomicReference<byte[]> result2 = new AtomicReference<>();

            private PaymentRequest paymentRequest;

            @Override
            public void handleFailed(String s) {
                LOGGER.error("NFC communication failed: {}", s);
                current = State.INIT;
            }

            @Override
            public void handleStatus(String s) {
                LOGGER.error("NFC communication status: {}", s);
            }

            @Override
            public void handleMessageReceived(byte[] bytes) throws Exception {
                //get 2nd message (maybe check amount here) -> send to server for check if valid, not spend, etc.
                //continue with 3rd message

                PaymentProtocol protocol = PaymentProtocol.fromBytesUnverified(bytes);

                LOGGER.debug("received nfc message {}, current state is {}", protocol.type().toString(), current.toString());

                if (protocol.type() == PaymentProtocol.Type.CONTACT_AND_PAYMENT_RESPONSE_OK) {
                    //this is the half signed transaction
                    remotePubKey = protocol.publicKey();
                    halfSignedTx = protocol.halfSignedTransaction();
                    current = State.FIRST;
                    return;
                }

                protocol = PaymentProtocol.fromBytes(bytes, remotePubKey);

                if (protocol.type() == PaymentProtocol.Type.PAYMENT_OK) {
                    LOGGER.info("NFC payment complete!");
                    current = State.INIT;
                }

            }

            @Override
            public boolean hasMoreMessages() {
                return current != State.SECOND;
            }

            @Override
            public byte[] nextMessage() throws Exception {
                //1st message: amount BTC, which user, to which address, public key -> request signed
                //1st message: which user, to which address, public key -> send signed

                //3rd message: server says ok, amount ok from server, signed
                // -> send ok with transaction (optional only signature), signed

                LOGGER.debug("Sending next NFC message, current state is {}", current);

                switch (current) {
                    case INIT:
                        if (result2.get() != null) {
                            current = State.FIRST;
                            return result2.get();
                        }
                        if (t != null) {
                            return null;
                        }
                        t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (paymentRequestReceiver.hasActivePaymentRequest()) {
                                        paymentRequest = paymentRequestReceiver.getActivePaymentRequest();

                                        String user = paymentRequest.getUser();
                                        long satoshi = paymentRequest.getSatoshi();
                                        String address = paymentRequest.getAddress();
                                        BitcoinNet bitcoinNet = getWalletService().getBitcoinNet();

                                        LOGGER.debug("Sending payment request for {} satoshi to user {} (address: {})", satoshi, user, address);

                                        byte[] retVal = PaymentProtocol.contactAndPaymentRequest(keyPair.getPublic(), user, new byte[6], satoshi, BitcoinUtils.addressToBytes(address, bitcoinNet)).toBytes(keyPair.getPrivate());
                                        result2.set(retVal);

                                        listener.onPaymentRequestSent(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()));
                                    } else {
                                        LOGGER.debug("No active payment request: Abort");
                                        listener.onPaymentError("No active payment request");
                                        result2.set(new byte[0]);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("No active payment request: Abort ", e);
                                    listener.onPaymentError("No active payment request");
                                    result2.set(new byte[0]);
                                }
                            }
                        });
                        t.start();
                        return null;

                    case FIRST:


                        if (result.get() != null) {
                            current = State.SECOND;
                            return result.get();
                        }

                        if (signTask != null) {
                            return null;
                        }

                        String sigReqJson = new String(halfSignedTx, "UTF-8");

                        LOGGER.debug("Sending signature request to server: {}", sigReqJson);

                        signTask = getCoinBleskApplication().getRequestFactory().payOutRequest(new RequestCompleteListener<SignedTxTransferObject>() {
                            @Override
                            public void onTaskComplete(SignedTxTransferObject response) {
                                try {
                                    byte[] signedTx;
                                    if (response.isSuccessful()) {
                                        LOGGER.debug("Received fully signed transaction from server.");
                                        signedTx = Base64.decode(response.getSignedTx(), Base64.NO_WRAP);
                                    } else {
                                        LOGGER.warn("Server didn't sign the transaction: {}", response.getMessage());
                                        signedTx = null;
                                    }
                                    byte[] responseMsg;

                                    if (signedTx != null) {
                                        listener.onPaymentReceived(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, paymentRequest.getUser());
                                        LOGGER.debug("Sending server request OK to other client, with fully signed tx. The transaction is {} bytes in size.", signedTx.length);
                                        responseMsg = PaymentProtocol.fromServerRequestOk(signedTx).toBytes(keyPair.getPrivate());

                                        listener.onPaymentSuccess(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, paymentRequest.getUser());
                                    } else {
                                        LOGGER.debug("Sending server request NOK to other client.");
                                        responseMsg = PaymentProtocol.fromServerRequestNok().toBytes(keyPair.getPrivate());

                                        listener.onPaymentError("Failed to get the signature from the server");

                                    }
                                    result.set(responseMsg);
                                } catch (Exception e) {
                                    LOGGER.error("Payment error: {}", e);
                                    listener.onPaymentError(e.getMessage());
                                }
                            }
                        }, JsonConverter.fromJson(sigReqJson, ServerSignatureRequestTransferObject.class), PaymentActivity.this);

                        signTask.execute();
                        return null;

                    default:
                        throw new RuntimeException("Should never be reached");
                }

            }

            @Override
            public boolean isFirst() {
                return false;
            }
        }, new NfcResponseHandler() {

            KeyPair keyPair = PaymentProtocol.generateKeys();

            // payment details
            private PublicKey remotePubKey;
            private long satoshis;
            private String user;

            @Override
            public byte[] handleMessageReceived(final byte[] bytes, final ResponseLater responseLater) throws Exception {
                //get 1st message request / get 1st message send
                //2nd return partially signed transaction
                // request: BTC from message, public key, signed, username
                // send: BTC entered by user, public key, signed, username
                //4th (optional check if signed by server), get ok, reply
                //5th reply ok, public key, signed
                System.err.println("time BB1: " + System.currentTimeMillis());

                //remotePubKey = protocol.publicKey();
                PaymentProtocol.Type type = PaymentProtocol.type(bytes);
                LOGGER.debug("received nfc message {}", type.toString());
                System.err.println("time BB2: " + System.currentTimeMillis());
                switch (type) {
                    case CONTACT_AND_PAYMENT_REQUEST:
                        System.err.println("time BB3: " + System.currentTimeMillis());


                        // check the payment request
                        // TODO

                        // create the half signed transaction
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final PaymentProtocol protocol = PaymentProtocol.fromBytes(bytes, null);
                                    satoshis = protocol.satoshis();
                                    byte[] btcAddress = protocol.sendTo();
                                    remotePubKey = protocol.publicKey();
                                    user = protocol.user();

                                    byte[] halfSignedTx = getWalletService().createNfcPayment(btcAddress, satoshis);

                                    LOGGER.debug("Sending partially signed transaction over NFC, total size of message is {} bytes", halfSignedTx.length);

                                    byte[] response = PaymentProtocol.contactAndPaymentResponseOk(halfSignedTx).toBytes(keyPair.getPrivate());
                                    responseLater.response(response);

                                    listener.onPaymentSent(BitcoinUtils.satoshiToBigDecimal(satoshis), protocol.publicKey(), protocol.user());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        System.err.println("time BB4: " + System.currentTimeMillis());
                        return null;
                    case FROM_SERVER_REQUEST_OK:

                        // get the fully signed tx, commit and broadcast it
                        AsyncTask<Void, Void, Void> commitAndBroadcastTx = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    final PaymentProtocol protocol = PaymentProtocol.fromBytes(bytes, null);
                                    byte[] signedTx = protocol.fullySignedTransaction();
                                    LOGGER.debug("Broadcasting fully signed transaction and commit it to wallet");
                                    getWalletService().commitAndBroadcastTx(signedTx);
                                } catch (Exception e) {
                                    LOGGER.error("Failed to commit and broadcast transaction: {}", e);
                                }
                                return null;
                            }
                        };
                        commitAndBroadcastTx.execute();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    byte[] data = PaymentProtocol.paymentOk().toBytes(keyPair.getPrivate());
                                    responseLater.response(data);
                                    listener.onPaymentSuccess(BitcoinUtils.satoshiToBigDecimal(satoshis), remotePubKey, user);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                        return null;
                }
                System.err.println("time BB5: " + System.currentTimeMillis());
                return new byte[0];
            }

            @Override
            public void handleFailed(String s) {
                LOGGER.error("NFC connection failed: {}", s);
            }

            @Override
            public void handleStatus(String s) {
                LOGGER.info("received NFC message: {}", s);
            }
        }, this);

    }


}
