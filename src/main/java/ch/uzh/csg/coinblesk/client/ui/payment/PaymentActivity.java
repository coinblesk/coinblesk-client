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
import android.preference.PreferenceManager;
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
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;
import ch.uzh.csg.coinblesk.client.payment.HalfSignedTxReceiver;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequest;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequestReceiver;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
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

    public interface UserPaymentConfirmation {
        void onDecision(boolean accepted);
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
     * @param message      to be displayed to the receiver
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

    private void checkAutoAccept(final long satoshis, final String receiver, PublicKey remotePubKey, final UserPaymentConfirmation confirmation) {
        // check whether to auto accept the payment or not
        AddressBookEntry entry = getCoinBleskApplication().getStorageHandler().getAddressBookEntry(remotePubKey);
        if(entry == null || !entry.isTrusted()) {
            showConfirmationDialog(BitcoinUtils.satoshiToBigDecimal(satoshis), receiver, confirmation);
        } else {
            getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
                @Override
                public void onTaskComplete(ExchangeRateTransferObject response) {
                    if (response.isSuccessful()) {
                        BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                        BigDecimal fiatAmount = BitcoinUtils.satoshiToBigDecimal(satoshis).multiply(exchangeRate);
                        int autoAcceptAmount = PreferenceManager.getDefaultSharedPreferences(PaymentActivity.this).getInt("auto_accept_amount", 0);
                        if (new BigDecimal(autoAcceptAmount).compareTo(fiatAmount) > 0) {
                            LOGGER.debug("Auto-accepting payment of {} {}. Auto accept amount is {} {}", fiatAmount, Constants.CURRENCY, autoAcceptAmount, Constants.CURRENCY);
                            confirmation.onDecision(true);
                        } else {
                            // ask the user
                            showConfirmationDialog(BitcoinUtils.satoshiToBigDecimal(satoshis), receiver, confirmation);
                        }
                    }
                }
            });
        }
    }

    private void showConfirmationDialog(final BigDecimal amount, final String user, final UserPaymentConfirmation confirmation) {

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {

                String amountString;
                if (response.isSuccessful()) {
                    BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    amountString = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amount, PaymentActivity.this);
                } else {
                    amountString = amount.toString();
                }

                String message = String.format(getString(R.string.sendPayment_dialog_message), amountString, user);

                final AlertDialog.Builder alert = new AlertDialog.Builder(PaymentActivity.this);
                alert.setTitle(getString(R.string.sendPayment_dialog_title));
                alert.setMessage(message);

                alert.setPositiveButton(getString(R.string.sendPayment_dialog_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                confirmation.onDecision(true);
                            }
                        }).start();
                    }
                });

                alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                confirmation.onDecision(false);
                            }
                        }).start();
                    }
                });

                alert.show();
            }
        });

    }


    enum State {
        INIT, FIRST, SECOND
    }

    /**
     * Initializes NFC adapter and receiver payment information.
     */
    protected void initializeNFC() throws Exception {

        initiator = new NfcSetup(new NfcInitiatorHandler() {

            State current = State.INIT;
            KeyPair keyPair;
            PublicKey remotePubKey;
            byte[] halfSignedTx;
            String user;
            RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> signTask;
            AtomicReference<byte[]> result = new AtomicReference<>();

            Thread t;
            AtomicReference<byte[]> result2 = new AtomicReference<>();

            private PaymentRequest paymentRequest;

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

                if (protocol.type() == PaymentProtocol.Type.PAYMENT_REQUEST_RESPONSE) {
                    //this is the half signed transaction
                    remotePubKey = protocol.publicKey();
                    halfSignedTx = protocol.halfSignedTransaction();
                    user = protocol.user();
                    current = State.FIRST;
                    return;
                }

                if (protocol.type() == PaymentProtocol.Type.PAYMENT_NOK) {
                    // payment rejected by counterparty
                    listener.onPaymentRejected();
                    listener.onPaymentFinish(true);
                }

                protocol = PaymentProtocol.fromBytes(bytes, remotePubKey);

                if (protocol.type() == PaymentProtocol.Type.PAYMENT_OK) {
                    LOGGER.info("NFC payment complete!");
                    current = State.INIT;
                    listener.onPaymentFinish(true);
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

                                        LOGGER.debug("Sending payment request for {} satoshi to receiver {} (address: {})", satoshi, user, address);

                                        byte[] retVal = PaymentProtocol.paymentRequest(keyPair.getPublic(), user, new byte[6], satoshi, BitcoinUtils.addressToBytes(address, bitcoinNet)).toBytes(keyPair.getPrivate());
                                        result2.set(retVal);

                                        listener.onPaymentRequestSent(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()));
                                    } else {
                                        LOGGER.debug("No active payment request: Abort");
                                        listener.onPaymentError("No active payment request");
                                        result2.set(new byte[0]);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Sending payment request failed", e);
                                    listener.onPaymentError(e.getMessage());
                                    result2.set(new byte[0]);
                                }
                            }
                        });
                        t.start();

                        return null;

                    case FIRST:

                        if (result.get() != null) {
                            current = State.SECOND;
                            LOGGER.debug("returning NFC response of size {}", result.get().length);
                            return result.get();
                        }

                        if (signTask != null) {
                            LOGGER.debug("Not yet ready, returning null");
                            return null;
                        }

                        String sigReqJson = new String(halfSignedTx, "UTF-8");

                        LOGGER.debug("Sending signature request to server: {}", sigReqJson);

                        signTask = getCoinBleskApplication().getRequestFactory().payOutRequest(new RequestCompleteListener<SignedTxTransferObject>() {
                            @Override
                            public void onTaskComplete(SignedTxTransferObject response) {

                                byte[] responseMsg = new byte[0];

                                try {
                                    if (response.isSuccessful()) {
                                        LOGGER.debug("Received fully signed transaction from server.");
                                        final byte[] fullySignedTx = Base64.decode(response.getSignedTx(), Base64.NO_WRAP);
                                        listener.onPaymentReceived(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, user);
                                        LOGGER.debug("Sending server request OK to other client, with fully signed tx. The transaction is {} bytes in size.", fullySignedTx.length);
                                        responseMsg = PaymentProtocol.fullTransaction(fullySignedTx, childNumbers).toBytes(keyPair.getPrivate());
                                        listener.onPaymentSuccess(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, user);
                                        getWalletService().commitAndBroadcastTx(fullySignedTx, true);


                                        // save transaction metadata and address book entry in the background....
                                        AsyncTask<Void, Void, Void> saveMetadataTask = new AsyncTask<Void, Void, Void>() {
                                            @Override
                                            protected Void doInBackground(Void... params) {
                                                BitcoinNet bitcoinNet = getWalletService().getBitcoinNet();
                                                // tx meta data
                                                String txId = BitcoinUtils.getTxHash(fullySignedTx, bitcoinNet);
                                                TransactionMetaData txMetaData = getCoinBleskApplication().getStorageHandler().getTransactionMetaData(txId);
                                                txMetaData = txMetaData != null ? txMetaData : new TransactionMetaData(txId);
                                                txMetaData.setReceiver("You");
                                                txMetaData.setSender(user);
                                                txMetaData.setType(TransactionMetaData.TransactionType.COINBLESK_PAY_IN);
                                                getCoinBleskApplication().getStorageHandler().saveTransactionMetaData(txMetaData);
                                                LOGGER.debug("Saved transaction meta data");

                                                // save (or update)  user in address book
                                                AddressBookEntry entry = getCoinBleskApplication().getStorageHandler().getAddressBookEntry(remotePubKey);
                                                entry = entry != null ? entry : new AddressBookEntry(remotePubKey);
                                                entry.setName(user);
                                                entry.setBitcoinAddress(BitcoinUtils.getSenderAddressFromP2SHTx(fullySignedTx, bitcoinNet));
                                                getCoinBleskApplication().getStorageHandler().saveAddressBookEntry(entry);
                                                LOGGER.debug("Saved address book entry");

                                                return null;
                                            }
                                        };
                                        saveMetadataTask.execute();

                                    } else {
                                        LOGGER.warn("Server didn't sign the transaction: {}", response.getMessage());
                                        LOGGER.debug("Sending server request NOK to other client.");
                                        responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                        listener.onPaymentError("Failed to get the signature from the server");
                                    }

                                } catch (Exception e) {
                                    LOGGER.error("Payment error: {}", e);
                                    listener.onPaymentError(e.getMessage());

                                    try {
                                        responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                        result.set(responseMsg);
                                    } catch (Exception e1) {
                                        LOGGER.error("NFC communication failed completely, was not able to send NOK to other client: ", e1);
                                        e1.printStackTrace();
                                    }

                                } finally {
                                    LOGGER.debug("setting NFC response ({} bytes)", responseMsg.length);
                                    result.set(responseMsg);
                                }
                            }
                        }, JsonConverter.fromJson(sigReqJson, ServerSignatureRequestTransferObject.class), PaymentActivity.this);

                        signTask.execute();

                        return null;

                    default:
                        throw new RuntimeException("Should never be reached");
                }

            }
        }, new NfcResponseHandler() {

            KeyPair keyPair;

            // payment details
            private PublicKey remotePubKey;
            private long satoshis;
            private String receiver;
            private byte[] btcAddress;

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
            public byte[] handleMessageReceived(final byte[] bytes, final ResponseLater responseLater) throws Exception {
                //get 1st message request / get 1st message send
                //2nd return partially signed transaction
                // request: BTC from message, public key, signed, username
                // send: BTC entered by receiver, public key, signed, username
                //4th (optional check if signed by server), get ok, reply
                //5th reply ok, public key, signed

                //remotePubKey = protocol.publicKey();
                PaymentProtocol.Type type = PaymentProtocol.type(bytes);
                LOGGER.debug("received nfc message {}", type.toString());

                switch (type) {
                    case PAYMENT_REQUEST:

                        // create the half signed transaction
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                PaymentProtocol protocol = null;
                                try {
                                    protocol = PaymentProtocol.fromBytes(bytes, null);
                                } catch (Exception e) {
                                    LOGGER.error("Fail: ", e);
                                    listener.onPaymentError("NFC communication failed");
                                    return;
                                }

                                satoshis = protocol.satoshis();
                                btcAddress = protocol.sendTo();
                                remotePubKey = protocol.publicKey();
                                receiver = protocol.user();

                                checkAutoAccept(satoshis, receiver, remotePubKey, new UserPaymentConfirmation() {
                                    @Override
                                    public void onDecision(boolean accepted) {
                                        try {
                                            if (accepted) {
                                                // the user accepted the payment
                                                byte[] halfSignedTx = getWalletService().createNfcPayment(btcAddress, satoshis);
                                                String username = getCoinBleskApplication().getStorageHandler().getUsername();
                                                LOGGER.debug("Sending partially signed transaction over NFC, total size of message is {} bytes", halfSignedTx.getHalfSignedTx().length);
                                                byte[] response = PaymentProtocol.paymentRequestResponse(keyPair.getPublic(), username, new byte[6], halfSignedTx.getHalfSignedTx(), halfSignedTx.getChildNumbers()).toBytes(keyPair.getPrivate());
                                                responseLater.response(response);
                                                listener.onPaymentSent(BitcoinUtils.satoshiToBigDecimal(satoshis), remotePubKey, receiver);
                                            } else {
                                                // user rejected the payment
                                                byte[] response = PaymentProtocol.paymentNok().toBytes(keyPair.getPrivate());
                                                responseLater.response(response);
                                                listener.onPaymentFinish(true);
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("Fail: ", e);
                                            listener.onPaymentError("NFC communication failed");
                                        }
                                    }
                                });

                            }
                        }).start();

                        return null;
                    case FULL_TRANSACTION:

                        // get the fully signed tx, commit and broadcast it
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    final PaymentProtocol protocol = PaymentProtocol.fromBytes(bytes, null);
                                    byte[] signedTx = protocol.fullySignedTransaction();
                                    LOGGER.debug("Broadcasting fully signed transaction and commit it to wallet");

                                    final String txId = getWalletService().commitAndBroadcastTx(signedTx, true);
                                    byte[] data = PaymentProtocol.paymentOk().toBytes(keyPair.getPrivate());
                                    responseLater.response(data);
                                    listener.onPaymentSuccess(BitcoinUtils.satoshiToBigDecimal(satoshis), remotePubKey, receiver);

                                    // save transaction metadata and address book entry in background....
                                    AsyncTask<Void, Void, Void> saveMetadataTask = new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            // save transaction metadata
                                            TransactionMetaData txMetaData = getCoinBleskApplication().getStorageHandler().getTransactionMetaData(txId);
                                            txMetaData = txMetaData != null ? txMetaData : new TransactionMetaData(txId);
                                            txMetaData.setReceiver(receiver);
                                            txMetaData.setSender("You");
                                            txMetaData.setType(TransactionMetaData.TransactionType.COINBLESK_PAY_OUT);
                                            getCoinBleskApplication().getStorageHandler().saveTransactionMetaData(txMetaData);
                                            LOGGER.debug("Saved transaction meta data");

                                            // save (or update)  user in address book
                                            AddressBookEntry entry = getCoinBleskApplication().getStorageHandler().getAddressBookEntry(remotePubKey);
                                            entry = entry != null ? entry : new AddressBookEntry(remotePubKey);
                                            entry.setName(receiver);
                                            entry.setBitcoinAddress(BitcoinUtils.getAddressFromScriptHash(btcAddress, getWalletService().getBitcoinNet()));
                                            getCoinBleskApplication().getStorageHandler().saveAddressBookEntry(entry);
                                            LOGGER.debug("Saved address book entry");

                                            listener.onPaymentFinish(true);

                                            return null;
                                        }
                                    };
                                    saveMetadataTask.execute();

                                } catch (Exception e) {
                                    LOGGER.error("Failed to commit and broadcast transaction", e);

                                }
                            }
                        }).start();

                        return null;

                    case SERVER_NOK:
                        LOGGER.debug("Received NOK from other client...");
                        listener.onPaymentError("The other client refused the payment.");

                }

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
