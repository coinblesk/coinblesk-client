package ch.uzh.csg.coinblesk.client.ui.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.storage.StorageHandlerListener;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.ui.history.HistoryActivity;
import ch.uzh.csg.coinblesk.client.ui.navigation.DrawerItemClickListener;
import ch.uzh.csg.coinblesk.client.ui.payment.ChoosePaymentActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.PaymentActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.HistoryTransactionFormatter;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.client.wallet.HalfSignedTransaction;
import ch.uzh.csg.coinblesk.client.wallet.SyncProgress;
import ch.uzh.csg.coinblesk.client.wallet.WalletListener;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;
import ch.uzh.csg.comm.NfcResponseHandler;
import ch.uzh.csg.comm.ResponseLater;
import ch.uzh.csg.nfclib.NfcResponderSetup;


/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends BaseActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private String[] mDrawerItems;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private ProgressBar mProgressBar;
    private CharSequence mTitle;

    private Button createNewTransactionBtn;
    private PopupWindow popupWindow;
    public static Boolean isFirstTime;
    AnimationDrawable nfcActivityAnimation;
    private boolean isPortrait = true;

    private NfcAdapter nfcAdapter;
    private Thread updateProgressTask;
    private TextView mBlockchainSyncStatusText;

    private NfcPaymentListener listener;
    private NfcResponderSetup responder;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoadingProgressDialog();
        setContentView(R.layout.activity_main);

        setScreenOrientation();
        isPortrait = getResources().getBoolean(R.bool.small_device);

        initializeDrawer();
        initializeGui();
        initClickListener();
        listener = initNfcListener();
        responder = initNfcResponder(listener);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        LOGGER.debug("service connected");

        getCoinBleskApplication().getStorageHandler().addStorageHandlerListener(new StorageHandlerListener() {
            @Override
            public void storageHandlerSet(StorageHandler storageHandler) {

                LOGGER.debug("we have stored the data");

                Service walletService = getWalletService().init(getCoinBleskApplication().getStorageHandler());
                walletService.awaitRunning();

                if (!getCoinBleskApplication().getStorageHandler().hasSentClientWatchingKey()) {
                    sendClientWatchingKey(new RequestCompleteListener<TransferObject>() {
                        @Override
                        public void onTaskComplete(TransferObject response) {
                            LOGGER.debug("sent client data");
                            if (response.isSuccessful()) {
                                getCoinBleskApplication().getStorageHandler().sentClientWatchingKey(true);
                            } else {
                                displayResponse(getString(R.string.establish_internet_connection));
                                finish();
                            }
                        }
                    });

                }

                displayUserBalance();
                initiateProgressBar();
                createHistoryViews();
                initTxListener();
                dismissProgressDialog();
            }

            @Override
            public void failed() {
                LOGGER.debug("sent client data failed");
                displayResponse(getString(R.string.establish_internet_connection));
                finish();
            }
        });
    }

    private void sendClientWatchingKey(final RequestCompleteListener<TransferObject> cro) {

        WatchingKeyTransferObject input = new WatchingKeyTransferObject();
        input.setWatchingKey(getWalletService().getWatchingKey());
        input.setBitcoinNet(getWalletService().getBitcoinNet());

        RequestTask<WatchingKeyTransferObject, TransferObject> task = getCoinBleskApplication().getRequestFactory().saveWatchingKeyRequest(new RequestCompleteListener<TransferObject>() {
            @Override
            public void onTaskComplete(TransferObject response) {
                cro.onTaskComplete(response);

            }
        }, input, this);

        task.execute();
    }

    @Override
    protected void walletStatus(boolean connected) {
        if(connected) {
            checkOnlineModeAndProceed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        responder.enable(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        responder.disable(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // do nothing...
        // prevents going back to the RestoreOrNewActivity from the home screen...
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Initializes the navigation drawer.
     *
     * @see 'http://developer.android.com/training/implementing-navigation/nav-drawer.html'
     */
    private void initializeDrawer() {

        mDrawerItems = getResources().getStringArray(R.array.drawerItems_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mTitle = mDrawerTitle = getTitle();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();

    }

    private void initiateProgressBar() {

        final WalletService walletService = getWalletService();

        if (walletService.getSyncProgress().isFinished()) {
            return;
        }

        final int updateInterval = 1000; // == 1s
        final Handler handler = new Handler();

        updateProgressTask = new Thread() {
            @Override
            public void run() {
                if (this.isInterrupted()) {
                    return;
                }

                SyncProgress syncProgress = walletService.getSyncProgress();

                if (syncProgress.isFinished()) {
                    mProgressBar.setVisibility(View.GONE);
                    mBlockchainSyncStatusText.setVisibility(View.GONE);
                    return;
                } else if(syncProgress.getProgress() <= 0) {
                    mProgressBar.setVisibility(View.GONE);
                    mBlockchainSyncStatusText.setVisibility(View.GONE);
                } else {

                    double progress = syncProgress.getProgress();

                    // make progress bar visible
                    mProgressBar.setVisibility(View.VISIBLE);
                    mBlockchainSyncStatusText.setVisibility(View.VISIBLE);

                    // set progress
                    int progressPercentage = (int) (progress * mProgressBar.getMax()) + 1;
                    progressPercentage = Math.max(0, progressPercentage);
                    LOGGER.debug("Updating blockchain sync progress bar. current progress is {}%", progressPercentage);

                    mProgressBar.setIndeterminate(false);
                    mProgressBar.setProgress(progressPercentage);

                    // show sync status text
                    String syncText = String.format(getResources().getString(R.string.main_bitcoinSynchronizing),
                            String.format("%.0f", progress * 100));

                    mBlockchainSyncStatusText.setText(syncText);
                }

                handler.postDelayed(updateProgressTask, updateInterval);
            }
        };

        handler.postDelayed(updateProgressTask, updateInterval);

    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    private void initializeGui() {

        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createNewTransactionBtn = (Button) findViewById(R.id.createNewTransactionButton);

        //create animated nfc activity image
        ImageView nfcActivity = (ImageView) findViewById(R.id.mainActivity_nfcIcon);
        nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
        nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
        nfcActivityAnimation.start();

        //show the loading spinner to indicate that the wallet is starting up
        mBlockchainSyncStatusText = (TextView) findViewById(R.id.mainActivityBlockchainSyncText);
        mBlockchainSyncStatusText.setText(getString(R.string.main_loadingWallet));
        mProgressBar = (ProgressBar) findViewById(R.id.mainActivityBlockchainSyncProgressBar);
        mProgressBar.setIndeterminate(true);

    }

    private void initClickListener() {
        createNewTransactionBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchActivity(MainActivity.this, ChoosePaymentActivity.class);
            }
        });
    }

    private void checkOnlineModeAndProceed() {
        CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.mainActivity_balanceCHF));
        if (!ConnectionCheck.isNetworkAvailable(this)) {
            createNewTransactionBtn.setEnabled(false);
            createNewTransactionBtn.setTextColor(Color.LTGRAY);
        }
        showFirstTimeInformation();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWalletService().removeBitcoinListener(this.getClass());
    }

    private void initTxListener() {
        getWalletService().addBitcoinListener(this.getClass(), new WalletListener() {
            @Override
            public void onWalletChange() {
                createHistoryViews();
                displayUserBalance();
            }
        });
    }

    private void displayUserBalance() {
        // display the user balance
        AsyncTask<Void, Void, BigDecimal> displayBalanceTaks = new AsyncTask<Void, Void, BigDecimal>() {
            @Override
            protected BigDecimal doInBackground(Void... params) {
                return getWalletService().getBalance();
            }

            @Override
            protected void onPostExecute(final BigDecimal balance) {
                CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), balance, getApplicationContext());
                setFiatBalance(balance);
            }
        };
        displayBalanceTaks.execute();
    }

    private void setFiatBalance(final BigDecimal btcBalance) {
        // display balance in fiat
        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                if (response.isSuccessful()) {
                    final BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    final TextView chfBalance = (TextView) findViewById(R.id.mainActivity_balanceCHF);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CurrencyViewHandler.clearTextView(chfBalance);
                            CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, btcBalance);
                        }
                    });
                }
            }
        });
    }

    private int getNumberOfLastTransactions() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPref.getString("numberOfLastTransactions", "3");
        return Integer.parseInt(value);
    }

    private void createHistoryViews() {

        AsyncTask<Void, Void, List<TransactionMetaData>> getTransactionHistoryTask = new AsyncTask<Void, Void, List<TransactionMetaData>>() {
            @Override
            protected List<TransactionMetaData> doInBackground(Void... params) {
                int numTransaction = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("numberOfLastTransactions", "3"));
                return getWalletService().getTransactionHistory(numTransaction).getAllTransactions();
            }

            @Override
            protected void onPostExecute(final List<TransactionMetaData> history) {
                createHistoryViews(history, null);

                // create history with current exchange rate
                if (ConnectionCheck.isNetworkAvailable(MainActivity.this)) {
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
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
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

    /**
     * Show first time information dialog if application is started for the
     * first time to inform user about NFC handling.
     */
    private void showFirstTimeInformation() {
        if (isFirstTime == null) {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            boolean defaultValue = true;
            isFirstTime = sharedPref.getBoolean(getString(R.string.sharedPreferences_isFirstTime), defaultValue);
        }

        if (isFirstTime) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup group = (ViewGroup) findViewById(R.id.nfc_instruction_popup);
            final View layout = inflater.inflate(R.layout.activity_popup_nfc_instructions, group);
            popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

            layout.post(new Runnable() {
                public void run() {
                    popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
                }
            });

            final Button closeBtn = (Button) layout.findViewById(R.id.nfc_instruction_close_button);
            closeBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });

            isFirstTime = false;

            //write to shared preferences
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.sharedPreferences_isFirstTime), false);
            editor.commit();
        }
    }

    /**
     * Check whether to accept or reject a payment. If the receiver is not trusted, the user
     * will be asked whether to send the payment or not
     *
     * @param satoshis     the amount in satoshis
     * @param receiver     the user name of the receiver
     * @param remotePubKey the public key of the receiver
     * @param confirmation callback for the decision
     */
    private void checkAccept(final long satoshis, final String receiver, PublicKey remotePubKey, final PaymentActivity.UserPaymentConfirmation confirmation) {
        // check whether to auto accept the payment or not
        BigDecimal amount = BitcoinUtils.satoshiToBigDecimal(satoshis);
        AddressBookEntry entry = getCoinBleskApplication().getStorageHandler().getAddressBookEntry(remotePubKey);
        if (entry == null || !entry.isTrusted()) {
            // for debugging
            if (entry == null) {
                LOGGER.debug("User {} was not found in the address book", receiver);
            } else {
                LOGGER.debug("User {} is not trusted", receiver);
            }
            showConfirmationDialog(amount, receiver, confirmation);
        } else {
            BigDecimal autoAcceptAmount = new BigDecimal(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("auto_accept_amount", "0"));
            if (autoAcceptAmount.compareTo(amount) > 0) {
                LOGGER.debug("Auto-accepting payment of {} BTC. Auto accept amount is {} BTC", amount, autoAcceptAmount);
                confirmation.onDecision(true);
            } else {
                LOGGER.debug("Amount of {} exceeds the auto-accept amount of {}. Asking user for confirmation.", amount, autoAcceptAmount);
                // ask the user
                showConfirmationDialog(BitcoinUtils.satoshiToBigDecimal(satoshis), receiver, confirmation);
            }
        }
    }

    private void showConfirmationDialog(final BigDecimal amount, final String user, final PaymentActivity.UserPaymentConfirmation confirmation) {

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {

                String amountString;
                if (response.isSuccessful()) {
                    BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    amountString = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amount, MainActivity.this);
                } else {
                    amountString = amount.toString();
                }

                String message = String.format(getString(R.string.sendPayment_dialog_message), amountString, user);

                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
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

    private NfcResponderSetup initNfcResponder(final NfcPaymentListener listener) {
        return new NfcResponderSetup(new NfcResponseHandler() {

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

                                checkAccept(satoshis, receiver, remotePubKey, new PaymentActivity.UserPaymentConfirmation() {
                                    @Override
                                    public void onDecision(boolean accepted) {
                                        try {
                                            if (accepted) {
                                                // the user accepted the payment
                                                HalfSignedTransaction halfSignedTx = getWalletService().createNfcPayment(btcAddress, satoshis);
                                                String username = getCoinBleskApplication().getStorageHandler().getUsername();
                                                LOGGER.debug("Sending partially signed transaction over NFC, total size of message is {} bytes", halfSignedTx.getHalfSignedTx().length);
                                                byte[] response = PaymentProtocol.paymentRequestResponse(keyPair.getPublic(), username, new byte[6], halfSignedTx.getHalfSignedTx(), halfSignedTx.getAccountNumbers(), halfSignedTx.getChildNumbers()).toBytes(keyPair.getPrivate());
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
                                    byte[] accountNumbers = protocol.accountNumbers();
                                    int[] childNumbers = protocol.childNumbers();

                                    if(!getWalletService().isTxSignedByServer(signedTx, accountNumbers, childNumbers)) {
                                        LOGGER.warn("transaction was not signed by the server. Aborting...");
                                        byte[] response = PaymentProtocol.paymentNok().toBytes(keyPair.getPrivate());
                                        responseLater.response(response);
                                        listener.onPaymentError("Could not verify server signature");
                                        listener.onPaymentFinish(false);
                                        return;
                                    }

                                    LOGGER.debug("Broadcasting fully signed transaction and commit it to wallet");
                                    final String txId = getWalletService().commitAndBroadcastTx(signedTx);


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
        });
    }


    private NfcPaymentListener initNfcListener() {
        return new NfcPaymentListener() {

            private boolean paymentSent = false;
            private boolean paymentSuccess = false;

            @Override
            public void onPaymentFinish(boolean success) {

                //paymentRequestReceiver.inactivatePaymentRequest();
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


    private void showErrorDialog() {
        showDialog(getResources().getString(R.string.error_transaction_failed), false);
    }


    /**
     * Shows a dialog indicating if transaction was successful or not.
     *
     * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
     */
    private void showSuccessDialog(final boolean isSending, final BigDecimal amountBtc, final String user) {
        dismissNfcInProgressDialog();

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                String amountString;
                if (response.isSuccessful()) {
                    BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    amountString = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amountBtc, MainActivity.this);
                } else {
                    amountString = CurrencyViewHandler.formatBTCAsString(amountBtc, MainActivity.this);
                }

                String msg;
                if (isSending) {
                    msg = String.format(getResources().getString(R.string.payment_notification_success_payer),
                            CurrencyViewHandler.formatBTCAsString(amountBtc, MainActivity.this) + amountString, user);
                } else {
                    msg = String.format(getResources().getString(R.string.payment_notification_success_payee),
                            CurrencyViewHandler.formatBTCAsString(amountBtc, MainActivity.this) + amountString, user);
                }
                showDialog(msg, true);
            }
        });


    }

    /**
     * Closes the progress dialog.
     */
    public void dismissNfcInProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
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

}
