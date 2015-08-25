package ch.uzh.csg.coinblesk.client.ui.payment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ch.uzh.csg.btlib.BTInitiatorSetup;
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
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.ui.history.HistoryActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.HistoryTransactionFormatter;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
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
    private ProgressDialog progressDialog;
    private boolean destroyed = false;
    protected boolean paymentAccepted = false;

    private NfcPaymentListener listener;
    private NfcInitiatorHandler handler;
    private NfcInitiatorSetup initiator;
    private BTInitiatorSetup btInitiator;
    private KeyPair localPair;

    private PaymentRequestReceiver paymentRequestReceiver;
    private HalfSignedTxReceiver halfSignedTxReceiver;


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

        try {
            listener = initNfcListener();
            handler = initializeNFC(listener);
            initiator = new NfcInitiatorSetup(handler, getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        localPair = getCoinBleskApplication().getStorageHandler().getKeyPair();
        btInitiator = new BTInitiatorSetup(handler, PaymentActivity.this,
                Utils.hashToUUID(localPair.getPublic().getEncoded()));

        checkNfc(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOGGER.debug("paymenta acitivity start");
        //this must be in start as the permission dialog would call resume/pause
        initiator.startInitiating(this);
    }



    @Override
    protected void onStop() {
        super.onStop();
        LOGGER.debug("paymenta acitivity stop");
        //this must be in start as the permission dialog would call resume/pause
        initiator.stopInitiating(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(paymentRequestReceiver);
        unregisterReceiver(halfSignedTxReceiver);
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
     * Initializes NFC adapter and receiver payment information.
     */
    protected NfcInitiatorHandler initializeNFC(final NfcPaymentListener listener) {

        return new NfcInitiatorHandler() {

            State current = State.INIT;
            KeyPair keyPair;
            PublicKey remotePubKey;
            byte[] halfSignedTx;
            int[] childNumbers;
            byte[] accountNumbers;
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
            public void setUUID(byte[] bytes) {
                try {
                    btInitiator.scanLeDevice(PaymentActivity.this, Utils.byteArrayToUUID(bytes, 0));
                    LOGGER.debug("initiate BT");
                } catch (Throwable t) {
                    t.printStackTrace();
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
                    accountNumbers = protocol.accountNumbers();
                    childNumbers = protocol.childNumbers();
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
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Sending payment request failed", e);
                                    listener.onPaymentError(e.getMessage());
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

                        ServerSignatureRequestTransferObject serverSigReq = new ServerSignatureRequestTransferObject();
                        serverSigReq.setPartialTx(Base64.encodeToString(halfSignedTx, Base64.NO_WRAP));
                        serverSigReq.setAccountNumbers(Bytes.asList(accountNumbers));
                        serverSigReq.setChildNumbers(Ints.asList(childNumbers));

                        LOGGER.debug("Sending signature request to server with half signed tx to server");

                        signTask = getCoinBleskApplication().getRequestFactory().payOutRequest(new RequestCompleteListener<SignedTxTransferObject>() {
                            @Override
                            public void onTaskComplete(SignedTxTransferObject response) {

                                byte[] responseMsg = new byte[0];

                                try {
                                    if (response.isSuccessful()) {
                                        LOGGER.debug("Received fully signed transaction from server.");
                                        final byte[] fullySignedTx = Base64.decode(response.getSignedTx(), Base64.NO_WRAP);

                                        // check the server's signature
                                        if (!getWalletService().isTxSignedByServer(fullySignedTx, accountNumbers, childNumbers)) {
                                            LOGGER.warn("Failed to verify server signature. Sending Server NOK to client");
                                            listener.onPaymentError("Invalid Server Signature");
                                            responseMsg = PaymentProtocol.serverNok().toBytes(keyPair.getPrivate());
                                            return;
                                        }

                                        listener.onPaymentReceived(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, user);
                                        LOGGER.debug("Sending server request OK to other client, with fully signed tx. The transaction is {} bytes in size.", fullySignedTx.length);
                                        responseMsg = PaymentProtocol.fullTransaction(fullySignedTx, accountNumbers, childNumbers).toBytes(keyPair.getPrivate());
                                        listener.onPaymentSuccess(BitcoinUtils.satoshiToBigDecimal(paymentRequest.getSatoshi()), remotePubKey, user);
                                        getWalletService().commitAndBroadcastTx(fullySignedTx);


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
                        }, serverSigReq, PaymentActivity.this);

                        signTask.execute();

                        return null;

                    default:
                        throw new RuntimeException("Should never be reached");
                }

            }
        };
    }

    abstract protected  NfcPaymentListener initNfcListener();

    private NfcPaymentListener initNfcListener1() {
        return new NfcPaymentListener() {

            private boolean paymentSent = false;
            private boolean paymentSuccess = false;

            @Override
            public void onPaymentFinish(boolean success) {

                paymentRequestReceiver.inactivatePaymentRequest();
                //dismissNfcInProgressDialog();

                // redraw the history
                createHistoryViews();

                if(paymentSent && !paymentSuccess) {
                    String msg = getString(R.string.error_no_confirmation);
                    showDialog(msg, false);
                }

                // reset state
                paymentSent = false;
                paymentSuccess = false;
            }

            @Override
            public void onPaymentReceived(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
                super.onPaymentReceived(amount, senderPubKey, senderUserName);
                showSuccessDialog(false, amount, senderUserName);
            }

            @Override
            public void onPaymentSent(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
                super.onPaymentSent(amount, senderPubKey, senderUserName);
                paymentSent = true;
            }

            @Override
            public void onPaymentSuccess(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
                super.onPaymentSent(amount, senderPubKey, senderUserName);
                showSuccessDialog(true, amount, senderUserName);
                paymentSuccess = true;
            }

            @Override
            public void onPaymentError(String msg) {
                showErrorDialog();
            }
        };
    }

    /**
     * Shows a dialog indicating if transaction was successful or not.
     *
     * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
     */
    private void showSuccessDialog(final boolean isSending, final BigDecimal amountBtc, final String user) {
        //dismissNfcInProgressDialog();

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                String amountString;
                if (response.isSuccessful()) {
                    BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    amountString = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amountBtc, PaymentActivity.this);
                } else {
                    amountString = CurrencyViewHandler.formatBTCAsString(amountBtc, PaymentActivity.this);
                }

                String msg;
                if (isSending) {
                    msg = String.format(getResources().getString(R.string.payment_notification_success_payer),
                            CurrencyViewHandler.formatBTCAsString(amountBtc, PaymentActivity.this) + amountString, user);
                } else {
                    msg = String.format(getResources().getString(R.string.payment_notification_success_payee),
                            CurrencyViewHandler.formatBTCAsString(amountBtc, PaymentActivity.this) + amountString, user);
                }
                showDialog(msg, true);
            }
        });


    }

    private void createHistoryViews() {

        AsyncTask<Void, Void, List<TransactionMetaData>> getTransactionHistoryTask = new AsyncTask<Void, Void, List<TransactionMetaData>>() {
            @Override
            protected List<TransactionMetaData> doInBackground(Void... params) {
                int numTransaction = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(PaymentActivity.this).getString("numberOfLastTransactions", "3"));
                return getWalletService().getTransactionHistory(numTransaction).getAllTransactions();
            }

            @Override
            protected void onPostExecute(final List<TransactionMetaData> history) {
                createHistoryViews(history, null);

                // create history with current exchange rate
                if (ConnectionCheck.isNetworkAvailable(PaymentActivity.this)) {
                    getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
                        @Override
                        public void onTaskComplete(ExchangeRateTransferObject response) {
                            if (response.isSuccessful()) {
                                BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                                createHistoryViews(history, exchangeRate);
                            }
                        }
                    });
                }
            }
        };
        getTransactionHistoryTask.execute();

    }

    private void createHistoryViews(List<TransactionMetaData> history, @Nullable BigDecimal exchangeRate) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mainActivity_history);
        linearLayout.removeAllViews();
        for (int i = 0; i < getNumberOfLastTransactions(); i++) {
            if (i < history.size()) {
                TextView tView = new TextView(getApplicationContext());
                tView.setGravity(Gravity.LEFT);
                tView.setTextColor(Color.DKGRAY);
                int drawable = getImage(history.get(i));
                tView.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
                tView.setText(HistoryTransactionFormatter.formatHistoryTransaction(history.get(i), exchangeRate, getApplicationContext()));
                tView.setClickable(true);
                if (i % 2 == 0) {
                    tView.setBackgroundColor(Color.LTGRAY);
                }
                tView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(PaymentActivity.this, HistoryActivity.class);
                        Bundle b = new Bundle();
                        intent.putExtras(b);
                        startActivity(intent);
                    }
                });
                linearLayout.addView(tView);
            }
        }
    }

    private int getImage(TransactionMetaData tx) {
        switch (tx.getType()) {
            case PAY_IN:
            case COINBLESK_PAY_IN:
                return R.drawable.ic_pay_in;
            case PAY_IN_UNVERIFIED:
                return R.drawable.ic_pay_in_un;
            case PAY_OUT:
            case COINBLESK_PAY_OUT:
                return R.drawable.ic_pay_out;
            default:
                throw new IllegalArgumentException("Unknown transaction type " + tx.getType());
        }
    }

    private int getNumberOfLastTransactions() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPref.getString("numberOfLastTransactions", "3");
        return Integer.parseInt(value);
    }

    private void showErrorDialog() {
        showDialog(getResources().getString(R.string.error_transaction_failed), false);
    }


}
