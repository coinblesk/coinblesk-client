package ch.uzh.csg.coinblesk.client.ui.main;

import android.content.ComponentName;
import android.content.Context;
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
import android.provider.Settings;
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
import android.widget.Toast;

import com.google.common.util.concurrent.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.Executors;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.storage.StorageHandlerCallback;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.ui.history.HistoryActivity;
import ch.uzh.csg.coinblesk.client.ui.navigation.DrawerItemClickListener;
import ch.uzh.csg.coinblesk.client.ui.payment.ChoosePaymentActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.PaymentActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.HistoryTransactionFormatter;
import ch.uzh.csg.coinblesk.client.wallet.SyncProgress;
import ch.uzh.csg.coinblesk.client.wallet.WalletListener;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;


/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends PaymentActivity {

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if this is a new installation
        if(!getCoinBleskApplication().getStorageHandler().hasUserData()) {
            //its new, so create a wallet and set a default username
            final String androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            //TODO: make this setting available in the menu
            getCoinBleskApplication().getStorageHandler().setUsername("Coinblesk User " + androidId);
            showLoadingProgressDialog();
            requestSetup(new RequestCompleteListener<SetupRequestObject>() {
                @Override
                public void onTaskComplete(SetupRequestObject response) {
                    if (response.isSuccessful()) {
                        LOGGER.debug("set bitcoinnet and serverwatching key");
                        getCoinBleskApplication().getStorageHandler().setBitcoinNetAndServerWatchingKey(response.getBitcoinNet(), response.getServerWatchingKey());
                        getCoinBleskApplication().getStorageHandler().setStorageFailed(false);
                    } else {
                        getCoinBleskApplication().getStorageHandler().setStorageFailed(true);
                    }
                    //now we continue in onServiceConnected, where we dismiss the loading dialog
                }
            });
        } else {
            getCoinBleskApplication().getStorageHandler().setStorageFailed(false);
        }

        setContentView(R.layout.activity_main);

        setScreenOrientation();
        isPortrait = getResources().getBoolean(R.bool.small_device);

        initializeDrawer();
        initializeGui();
        initClickListener();

        try {
            initializeNFC();
            initNfcListener();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        LOGGER.debug("service connected");

        getCoinBleskApplication().getStorageHandler().setStorageHandlerCallback(new StorageHandlerCallback() {
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


    private void requestSetup(final RequestCompleteListener<SetupRequestObject> cro) {

        RequestTask<TransferObject, SetupRequestObject> task = getCoinBleskApplication().getRequestFactory().setupRequest(new RequestCompleteListener<SetupRequestObject>() {
            @Override
            public void onTaskComplete(SetupRequestObject response) {
                cro.onTaskComplete(response);
            }
        }, this);
        task.execute();
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
    public void onResume() {
        super.onResume();
        if (walletConnected()) {
            checkOnlineModeAndProceed();
        }
        invalidateOptionsMenu();
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
                return getWalletService().getTransactionHistory(10).getAllTransactions();
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

    private void initNfcListener() {
        setNfcPaymentListener(new DefaultNfcListener() {

            private boolean paymentSent = false;
            private boolean paymentSuccess = false;

            @Override
            public void onPaymentFinish(boolean success) {
                super.onPaymentFinish(success);

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
        });
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

}
