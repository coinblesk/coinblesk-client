package ch.uzh.csg.coinblesk.client.ui.payment;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Pair;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.InsufficientMoneyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import ch.uzh.csg.btlib.BTInitiatorSetup;
import ch.uzh.csg.btlib.BTLEController;
import ch.uzh.csg.btlib.BTUtils;
import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;
import ch.uzh.csg.coinblesk.client.payment.HalfSignedTxReceiver;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequest;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequestReceiver;
import ch.uzh.csg.coinblesk.client.payment.SendRequest;
import ch.uzh.csg.coinblesk.client.payment.SendRequestReceiver;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.client.wallet.HalfSignedTransaction;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;
import ch.uzh.csg.comm.NfcInitiatorHandler;
import ch.uzh.csg.comm.Utils;
import ch.uzh.csg.nfclib.NfcInitiatorSetup;

/**
 * This is the abstract base class of the payment activities (receive and pay).
 * Common behavior is implemented here.
 */
public abstract class PaymentActivity extends BaseActivity {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentActivity.class);

    protected boolean isSeller;

    private NfcPaymentListener listener;
    protected PaymentNfcInitiatorHandler handler;
    protected NfcInitiatorSetup initiator;
    private BTInitiatorSetup btInitiator;

    protected PaymentRequestReceiver paymentRequestReceiver;
    protected SendRequestReceiver sendRequestReceiver;
    protected HalfSignedTxReceiver halfSignedTxReceiver;


    private interface TransactionSignedListener {
        void onTransactionSigned(byte[] fullySignedTx);
        void onFail();
    }

    public interface UserPaymentConfirmation {
        void onDecision(boolean accepted);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register receivers
        this.paymentRequestReceiver = new PaymentRequestReceiver();
        registerReceiver(paymentRequestReceiver, new IntentFilter(PaymentRequestReceiver.ACTION));
        this.sendRequestReceiver = new SendRequestReceiver();
        registerReceiver(sendRequestReceiver, new IntentFilter(SendRequestReceiver.ACTION));

        this.halfSignedTxReceiver = new HalfSignedTxReceiver();
        registerReceiver(halfSignedTxReceiver, new IntentFilter(HalfSignedTxReceiver.ACTION));

        try {
            listener = initNfcListener();
            handler = initializeNFC();
            initiator = new NfcInitiatorSetup(handler, getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        checkNfc(this);
        Pair<BluetoothManager,BluetoothAdapter> pair = BTUtils.checkBT(this);
        if(pair != null) {
            byte[] macAddress = BTUtils.btAddress(pair.second);
            btInitiator = BTInitiatorSetup.init(initiator.getNfcInitiator(), PaymentActivity.this, pair.second);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOGGER.debug("payment acitivity start");
        //this must be in start as the permission dialog would call resume/pause
        initiator.start(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        LOGGER.debug("payment acitivity stop");
        //this must be in start as the permission dialog would call resume/pause
        initiator.stop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(paymentRequestReceiver);
        unregisterReceiver(sendRequestReceiver);
        unregisterReceiver(halfSignedTxReceiver);
    }

    /**
     * Displays a custom dialog with a given message and an image indicating if task was successful or not.
     *
     * @param message      to be displayed to the receiver
     * @param isSuccessful boolean to indicate if task was successful
     */
    protected void showDialog(String message, boolean isSuccessful) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(PaymentActivity.this);
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
        INIT, FIRST_SENT, ADDRESS_RECEIVED, HALF_SIGNED_RECEIVED, SECOND
    }

    class PaymentNfcInitiatorHandler implements NfcInitiatorHandler {
        private State current = State.INIT;
        private KeyPair keyPair;
        private PublicKey remotePubKey;
        private byte[] halfSignedTx;
        int[] childNumbers;
        private byte[] accountNumbers;
        private String user;
        private byte[] remoteBtcAddr;
        private boolean signTaskStarted = false;

        private AtomicReference<byte[]> result = new AtomicReference<>();

        private Thread t;
        private AtomicReference<byte[]> result2 = new AtomicReference<>();

        private BTLEController btleController;
        private boolean nfcCommPresent = false;
        private boolean btCommPresent = false;

        private long performance = 0;

        private boolean isSending = false;

        private PaymentRequest paymentRequest;
        private SendRequest sendRequest;
        private byte[] currentUUID = null;

        {
            // initialize key pair
            KeyPair keyPair = getCoinBleskApplication().getStorageHandler().getKeyPair();
            if (keyPair == null) {
                this.keyPair = PaymentProtocol.generateKeys();
                getCoinBleskApplication().getStorageHandler().setKeyPair(this.keyPair);
            } else {
                this.keyPair = keyPair;
            }
        }

        @Override
        public void setUUID(final byte[] bytes) {
            if(btInitiator!=null && !btCommPresent && currentUUID!=null && !Arrays.equals(currentUUID, bytes)) {
                currentUUID = bytes;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        btInitiator.scanLeDevice(PaymentActivity.this, Utils.byteArrayToUUID(bytes, 0));
                        LOGGER.debug("initiate BT");
                    }
                }).start();
            }
            LOGGER.debug("setUUID done");
        }

        @Override
        public void btTagFound(BTLEController btleController) {
            LOGGER.debug("btleDiscovered");
            btCommPresent = true;
            this.btleController = btleController;
            if(!nfcCommPresent) {
                if(current == State.FIRST_SENT) {
                    btleController.startBTLE();
                }
            }
        }

        @Override
        public void btTagLost() {
            LOGGER.debug("bt tag lost, current");
            btCommPresent = false;
        }

        @Override
        public void nfcTagFound() {
            LOGGER.debug("tag found, current {}", current);
            nfcCommPresent = true;
        }

        @Override
        public void nfcTagLost() {
            LOGGER.debug("tag lost, check BT, current {}", current);
            nfcCommPresent = false;
            if(btleController != null && current == State.FIRST_SENT) {
                btleController.startBTLE();
            }
        }

        public void reset() {
            current = State.INIT;
            result.set(null);
            result2.set(null);
            t = null;
            signTaskStarted = false;
        }

        @Override
        public void handleFailed(String s) {
            //invalid sequence start over
            //initiator.getNfcInitiator().reset();
            reset();
            LOGGER.error("NFC communication failed: {}", s);
        }

        @Override
        public void handleStatus(String s) {
            LOGGER.error("NFC communication status: {}", s);
        }

        @Override
        public void handleMessageReceived(byte[] bytes) throws Exception {
            //get 2nd message (maybe check amount here) -> send to server for check if valid, not spend, etc.
            //continue with 3rd message

            // reset state to INIT if there is a new payment/send request
            if(current == State.SECOND && (sendRequestReceiver.hasActiveSendRequest() || paymentRequestReceiver.hasActivePaymentRequest())) {
                current = State.INIT;
            }

            PaymentProtocol protocol = PaymentProtocol.fromBytesUnverified(bytes);

            LOGGER.debug("received nfc message {}, current state is {}", protocol.type().toString(), current.toString());

            if (protocol.type() == PaymentProtocol.Type.PAYMENT_SEND_RESPONSE) {
                isSending = true;

                // bitcoin address to send the coins to
                remoteBtcAddr = protocol.sendTo();
                user = protocol.user();
                remotePubKey = protocol.publicKey();
                current = State.ADDRESS_RECEIVED;
                return;
            }

            if (protocol.type() == PaymentProtocol.Type.PAYMENT_REQUEST_RESPONSE) {

                isSending = false;

                result2.set(null);
                //this is the half signed transaction
                remotePubKey = protocol.publicKey();

                halfSignedTx = protocol.halfSignedTransaction();
                accountNumbers = protocol.accountNumbers();
                childNumbers = protocol.childNumbers();
                user = protocol.user();
                current = State.HALF_SIGNED_RECEIVED;
                return;
            }

            if (protocol.type() == PaymentProtocol.Type.PAYMENT_NOK) {
                // payment rejected by counterparty
                listener.onPaymentRejected(user);
                System.err.println("**PERFORMANCE, complete f " + (System.currentTimeMillis() - performance));
                return;
            }

            protocol = PaymentProtocol.fromBytes(bytes, remotePubKey);

            if (protocol.type() == PaymentProtocol.Type.PAYMENT_OK) {
                LOGGER.info("NFC payment complete!");
                current = State.SECOND;
                System.err.println("**PERFORMANCE, complete t " + (System.currentTimeMillis() - performance));

                if(isSending) {
                    listener.onPaymentSent(BitcoinUtils.satoshiToBigDecimal(sendRequest.getSatoshi()), remotePubKey, user);
                } else {
                    listener.onPaymentReceived(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, user);
                }

                paymentRequestReceiver.inactivatePaymentRequest();
                sendRequestReceiver.inactivateSendRequest();

                return;
            }

        }

        @Override
        public boolean hasMoreMessages() {
            return current != State.SECOND;
        }

        @Override
        public byte[] nextMessage() throws Exception {
            //1st message: amount BTC, which receiver, to which address, public key -> request signed
            //1st message: which receiver, to which address, public key -> send signed

            //3rd message: server says ok, amount ok from server, signed
            // -> send ok with transaction (optional only signature), signed

            // TODO: clean up results: set to null if state ended or after error

            LOGGER.debug("Sending next NFC message, current state is {}", current);

            switch (current) {
                case INIT:

                    if (result2.get() != null) {
                        byte[] retVal = result2.get();
                        current = State.FIRST_SENT;
                        LOGGER.debug("Set state to {}", current);
                        System.err.println("**PERFORMANCE, init done3: " + (System.currentTimeMillis() - performance));
                        return retVal;
                    }
                    if (t != null) {
                        return null;
                    }
                    performance = System.currentTimeMillis();
                    t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if (paymentRequestReceiver.hasActivePaymentRequest()) {
                                    // payment request
                                    paymentRequest = paymentRequestReceiver.getActivePaymentRequest();

                                    String user = paymentRequest.getUser();
                                    long satoshi = paymentRequest.getSatoshi();
                                    String address = paymentRequest.getAddress();
                                    BitcoinNet bitcoinNet = getWalletService().getBitcoinNet();

                                    LOGGER.debug("Sending payment request for {} satoshi to receiver {} (address: {})", satoshi, user, address);

                                    byte[] retVal = PaymentProtocol.paymentRequest(keyPair.getPublic(), user, new byte[6], satoshi, BitcoinUtils.addressToBytes(address, bitcoinNet)).toBytes(keyPair.getPrivate());
                                    result2.set(retVal);
                                    System.err.println("**PERFORMANCE, init done1: " + (System.currentTimeMillis() - performance));
                                } else if (sendRequestReceiver.hasActiveSendRequest()) {
                                    // send request
                                    sendRequest = sendRequestReceiver.getActiveSendRequest();
                                    LOGGER.debug("Active send request. Sending username to other device");
                                    byte[] retVal = PaymentProtocol.paymentSend(keyPair.getPublic(), getCoinBleskApplication().getStorageHandler().getUsername(), new byte[6]).toBytes(keyPair.getPrivate());
                                    result2.set(retVal);
                                    System.err.println("**PERFORMANCE, init done2: " + (System.currentTimeMillis() - performance));
                                } else {
                                    LOGGER.debug("No active payment request: Abort");
                                    listener.onPaymentError("No active payment request");
                                }
                            } catch (Exception e) {
                                LOGGER.error("Sending payment request failed", e);
                                listener.onPaymentError(e.getMessage());
                            }
                        }
                    });
                    t.start();

                    return null;
                case FIRST_SENT:
                    System.err.println("**PERFORMANCE, first send start: " + (System.currentTimeMillis() - performance));
                    //byte[] retVal = result2.get();
                    //LOGGER.debug("Set in first set");
                    //System.err.println("**PERFORMANCE, first send done: " + (System.currentTimeMillis() - performance));
                    //return new byte[0];
                    return null;
                case ADDRESS_RECEIVED:
                    System.err.println("**PERFORMANCE, address received start: " + (System.currentTimeMillis() - performance));
                    // received half signed transaction from other party

                    if (result.get() != null) {
                        current = State.SECOND;
                        LOGGER.debug("returning NFC response of size {}", result.get().length);
                        signTaskStarted = false;
                        System.err.println("**PERFORMANCE, address received done: " + (System.currentTimeMillis() - performance));
                        return result.get();
                    }

                    if(signTaskStarted) {
                        LOGGER.debug("Not ready yet... Waiting for signature of the server");
                        return null;
                    }

                    signTaskStarted = true;

                    AsyncTask<Void, Void, HalfSignedTransaction> createTxTask = new AsyncTask<Void, Void, HalfSignedTransaction>() {
                        @Override
                        protected HalfSignedTransaction doInBackground(Void... params) {
                            HalfSignedTransaction halfSignedTx = null;
                            try {
                                halfSignedTx = getWalletService().createNfcPayment(remoteBtcAddr, sendRequestReceiver.getActiveSendRequest().getSatoshi());
                            } catch (AddressFormatException e) {
                                listener.onPaymentError("Invalid Bitcoin address");
                            } catch (InsufficientMoneyException e) {
                                listener.onPaymentError("Not enough bitcoins");
                            }
                            return halfSignedTx;
                        }

                        @Override
                        protected void onPostExecute(final HalfSignedTransaction halfSignedTx) {
                            createPayment(halfSignedTx.getHalfSignedTx(), halfSignedTx.getAccountNumbers(), halfSignedTx.getChildNumbers(), new TransactionSignedListener() {

                                private byte[] responseMsg = null;

                                @Override
                                public void onTransactionSigned(final byte[] fullySignedTx) {
                                    try {
                                        responseMsg = PaymentProtocol.fullTransaction(fullySignedTx, halfSignedTx.getAccountNumbers(), halfSignedTx.getChildNumbers()).toBytes(keyPair.getPrivate());
                                        System.err.println("**PERFORMANCE, address received done2: " + (System.currentTimeMillis() - performance));
                                        result.set(responseMsg);
                                        SendRequest sendRequest = sendRequestReceiver.getActiveSendRequest();

                                        String remoteAddr = BitcoinUtils.getAddressFromScriptHash(remoteBtcAddr, getWalletService().getBitcoinNet());
                                        postPayment(isSending, fullySignedTx, user, remotePubKey, remoteAddr);

                                    } catch (Exception e) {
                                        listener.onPaymentError(e.getMessage());
                                    }
                                }

                                @Override
                                public void onFail() {
                                    try {
                                        responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                        result.set(responseMsg);
                                    } catch (Exception e) {
                                        LOGGER.error("Fail: ", e);
                                    }
                                }
                            });
                        }
                    };

                    createTxTask.execute();



                case HALF_SIGNED_RECEIVED:
                    System.err.println("**PERFORMANCE, half signed start: " + (System.currentTimeMillis() - performance));
                    // received half signed transaction from other party

                    if (result.get() != null) {
                        current = State.SECOND;
                        LOGGER.debug("returning NFC response of size {}", result.get().length);
                        signTaskStarted = false;
                        System.err.println("**PERFORMANCE, half signed done: " + (System.currentTimeMillis() - performance));
                        return result.get();
                    }

                    if(signTaskStarted) {
                        LOGGER.debug("Not ready yet...");
                        return null;
                    }

                    signTaskStarted = true;

                    createPayment(halfSignedTx, accountNumbers, childNumbers, new TransactionSignedListener() {

                        byte[] responseMsg = null;

                        @Override
                        public void onTransactionSigned(final byte[] fullySignedTx) {

                            try {
                                // we received the half signed transaction
                                // check the server's signature
                                if (!getWalletService().isTxSignedByServer(fullySignedTx, accountNumbers, childNumbers)) {
                                    LOGGER.warn("Failed to verify server signature. Sending Server NOK to client");
                                    listener.onPaymentError("Invalid Server Signature");
                                    responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                    return;
                                }

                                LOGGER.debug("Sending server request OK to other client, with fully signed tx. The transaction is {} bytes in size.", fullySignedTx.length);
                                responseMsg = PaymentProtocol.fullTransaction(fullySignedTx, accountNumbers, childNumbers).toBytes(keyPair.getPrivate());
                                System.err.println("**PERFORMANCE, half signed done2: " + (System.currentTimeMillis() - performance));
                                result.set(responseMsg);

                                String bitcoinAddress = BitcoinUtils.getSenderAddressFromP2SHTx(fullySignedTx, getWalletService().getBitcoinNet());

                                postPayment(isSending, fullySignedTx, user, remotePubKey, bitcoinAddress);

                            } catch (Exception e) {
                                listener.onPaymentError(e.getMessage());
                            }

                            result.set(responseMsg);
                        }

                        @Override
                        public void onFail() {
                            try {
                                responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                result.set(responseMsg);
                            } catch (Exception e) {
                                LOGGER.error("Fail: ", e);
                            }
                        }
                    });

                    return null;

                default:
                    throw new RuntimeException("Should never be reached");
            }

        }
    }
    /**
     * Initializes NFC adapter and receiver payment information.
     */
    protected PaymentNfcInitiatorHandler initializeNFC() {
        return new PaymentNfcInitiatorHandler();
    }


    private void createPayment(final byte[] halfSignedTx, final byte[] accountNumbers, final int[] childNumbers, final TransactionSignedListener signedListener) {

        ServerSignatureRequestTransferObject serverSigReq = new ServerSignatureRequestTransferObject();
        serverSigReq.setPartialTx(Base64.encodeToString(halfSignedTx, Base64.NO_WRAP));
        serverSigReq.setAccountNumbers(Bytes.asList(accountNumbers));
        serverSigReq.setChildNumbers(Ints.asList(childNumbers));

        LOGGER.debug("Sending signature request to server with half signed tx to server");

        RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> signTask;

        signTask = getCoinBleskApplication().getRequestFactory().payOutRequest(new RequestCompleteListener<SignedTxTransferObject>() {
            @Override
            public void onTaskComplete(SignedTxTransferObject response) {

                try {
                    if (response.isSuccessful()) {
                        LOGGER.debug("Received fully signed transaction from server.");
                        final byte[] fullySignedTx = Base64.decode(response.getSignedTx(), Base64.NO_WRAP);
                        signedListener.onTransactionSigned(fullySignedTx);
                    } else {
                        LOGGER.warn("Server didn't sign the transaction: {}", response.getMessage());
                        LOGGER.debug("Sending server request NOK to other client.");
                        signedListener.onFail();
                        listener.onPaymentError("Failed to get the signature from the server");
                    }

                } catch (Exception e) {
                    LOGGER.error("Payment error: {}", e);
                    signedListener.onFail();
                }
            }
        }, serverSigReq, PaymentActivity.this);

        signTask.execute();
    }

    abstract protected NfcPaymentListener initNfcListener();


}
