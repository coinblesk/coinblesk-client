package ch.uzh.csg.coinblesk.client.ui.main;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.history.HistoryActivity;
import ch.uzh.csg.coinblesk.client.ui.navigation.DrawerItemClickListener;
import ch.uzh.csg.coinblesk.client.ui.payment.AbstractPaymentActivity;
import ch.uzh.csg.coinblesk.client.ui.payment.ChoosePaymentActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.formatter.HistoryTransactionFormatter;
import ch.uzh.csg.coinblesk.client.wallet.SyncProgress;
import ch.uzh.csg.coinblesk.client.wallet.TransactionObject;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.customserialization.PaymentResponse;
import ch.uzh.csg.coinblesk.util.Converter;
import ch.uzh.csg.paymentlib.IPaymentEventHandler;
import ch.uzh.csg.paymentlib.IServerResponseListener;
import ch.uzh.csg.paymentlib.IUserPromptAnswer;
import ch.uzh.csg.paymentlib.IUserPromptPaymentRequest;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.messages.PaymentError;

/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends AbstractPaymentActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private String[] mDrawerItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
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

        setContentView(R.layout.activity_main);
        setScreenOrientation();
        isPortrait = getResources().getBoolean(R.bool.portrait_only);

        initializeGui();
        initClickListener();
        initializeDrawer();
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
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_activity_main);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_activity_main);


        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                null,
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private void initiateProgressBar() {

        final int updateInterval = 3000; // == 1s
        final WalletService walletService = getWalletService();
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
                } else {

                    double progress = syncProgress.getProgress();

                    if(progress >= 0) {

                        // set progress
                        mProgressBar.setIndeterminate(false);
                        int progressPercentage = (int) (progress * mProgressBar.getMax()) + 1;
                        progressPercentage = Math.max(0, progressPercentage);
                        LOGGER.debug("Updating blockchain sync progress bar. current progress is {}%", progressPercentage);
                        mProgressBar.setProgress(progressPercentage);

                        // show sync status text
                        String syncText = String.format(getResources().getString(R.string.main_bitcoinSynchronizing),
                                String.format("%.1f", progress * 100));

                        mBlockchainSyncStatusText.setText(syncText);
                    }

                    handler.postDelayed(updateProgressTask, updateInterval);
                }
            }
        };

        handler.postDelayed(updateProgressTask, updateInterval);

    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    private void destroyProgressBar() {
        updateProgressTask.interrupt();
    }

    private void initializeGui() {
        // display red action bar when running on testserver
        if (Constants.BASE_URI_SSL.contains("clone")) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.RED));
        }
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
                handleAsyncTask();
                launchActivity(MainActivity.this, ChoosePaymentActivity.class);
            }
        });
    }

    private void handleAsyncTask() {
        throw new RuntimeException("Not yet implemented");
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        displayUserBalance();
        initiateProgressBar();
        createHistoryViews();
        initializeNFC();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
        destroyProgressBar();
    }

    private void displayUserBalance() {
        // display the user balance
        AsyncTask<Void, Void, BigDecimal> displayBalanceTaks = new AsyncTask<Void, Void, BigDecimal>() {
            @Override
            protected BigDecimal doInBackground(Void... params) {
                return getWalletService().getBalance();
            }

            @Override
            protected void onPostExecute(BigDecimal balance) {
                CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), balance, getApplicationContext());
            }
        };
        displayBalanceTaks.execute();
    }


    private int getNumberOfLastTransactions() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPref.getString("numberOfLastTransactions", "3");
        return Integer.parseInt(value);
    }

    private void createHistoryViews() {

        AsyncTask<Void, Void, List<TransactionObject>> getTransactionHistoryTask = new AsyncTask<Void, Void, List<TransactionObject>>() {
            @Override
            protected List<TransactionObject> doInBackground(Void... params) {
                return getWalletService().getTransactionHistory().getAllTransactions();
            }

            @Override
            protected void onPostExecute(List<TransactionObject> history) {
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mainActivity_history);
                linearLayout.removeAllViews();
                for (int i = 0; i < getNumberOfLastTransactions(); i++) {
                    if (i < history.size()) {
                        TextView tView = new TextView(getApplicationContext());
                        tView.setGravity(Gravity.LEFT);
                        tView.setTextColor(Color.DKGRAY);
                        int drawable = getImage(history.get(i));
                        tView.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
                        tView.setText(HistoryTransactionFormatter.formatHistoryTransaction(history.get(i), getApplicationContext()));
                        tView.setClickable(true);
                        if (i % 2 == 0) {
                            tView.setBackgroundColor(Color.LTGRAY);
                        }
                        tView.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                handleAsyncTask();
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
        };
        getTransactionHistoryTask.execute();

    }

    private int getImage(TransactionObject tx) {
        switch (tx.getType()) {
            case PAY_IN:
                return R.drawable.ic_pay_in;
            case PAY_IN_UNVERIFIED:
                return R.drawable.ic_pay_in_un;
            case PAY_OUT:
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
     * Initializes NFC adapter and user payment information.
     */
    private void initializeNFC() {

        // TODO
//        nfcAdapter = createAdapter(MainActivity.this);
//        if (nfcAdapter == null) {
//            return;
//        }
//
//        //disable android beam (touch to beam screen)
//        nfcAdapter.setNdefPushMessage(null, this, this);
//
//        try {
//            PublicKey publicKeyServer = KeyHandler.decodePublicKey(ClientController.getStorageHandler().getServerPublicKey().getPublicKey());
//            final ServerInfos serverInfos = new ServerInfos(publicKeyServer);
//            PrivateKey privateKey = KeyHandler.decodePrivateKey(ClientController.getStorageHandler().getKeyPair().getPrivateKey());
//            final UserInfos userInfos = new UserInfos(ClientController.getStorageHandler().getUserAccount().getUsername(), privateKey, PKIAlgorithm.DEFAULT, ClientController.getStorageHandler().getKeyPair().getKeyNumber());
//            new PaymentRequestHandler(this, eventHandler, userInfos, serverInfos, userPrompt, persistencyHandler);
//        } catch (Exception e) {
//            displayResponse(getResources().getString(R.string.error_nfc_initializing));
//        }
    }

    /**
     * Handler responsible for managing events received by NFC Payment Library.
     */
    private IPaymentEventHandler eventHandler = new IPaymentEventHandler() {
        public void handleMessage(PaymentEvent event, Object object, IServerResponseListener caller) {

            switch (event) {
                case ERROR:
                    PaymentError err = (PaymentError) object;
                    if (err != null) {
                        switch (err) {
                            case DUPLICATE_REQUEST:
                                dismissNfcInProgressDialog();
                                showDialog(getResources().getString(R.string.transaction_duplicate_error), false);
                                break;
                            case NO_SERVER_RESPONSE:
                                dismissNfcInProgressDialog();
                                showDialog(getResources().getString(R.string.error_transaction_failed), false);
                                break;
                            case PAYER_REFUSED:
                                dismissNfcInProgressDialog();
                                showDialog(getResources().getString(R.string.transaction_rejected_payer), false);
                                break;
                            case REQUESTS_NOT_IDENTIC:
                                dismissNfcInProgressDialog();
                                showDialog(getResources().getString(R.string.transaction_server_rejected), false);
                                break;
                            case SERVER_REFUSED:
                                dismissNfcInProgressDialog();
                                String errorMessage = err.getErrorCause();
                                if (errorMessage != null && errorMessage.equals("BALANCE")) {
                                    showDialog(getResources().getString(R.string.transaction_server_rejected_balance), false);
                                } else {
                                    showDialog(getResources().getString(R.string.transaction_server_rejected), false);
                                }
                                break;
                            case UNEXPECTED_ERROR:
                                //check if Mensa Tablet (with external reader) is connected, if yes ignore unexpected errors
                                if (Constants.IS_MENSA_MODE && !isPortrait) {
                                    break;
                                } else {
                                    dismissNfcInProgressDialog();
                                    showDialog(getResources().getString(R.string.error_transaction_failed), false);
                                }
                                break;
                            case INIT_FAILED:
                                //ignore
                                break;
                            default:
                                break;
                        }
                    }
                    resetStates();
                    break;
                case FORWARD_TO_SERVER:
                    break;
                case SUCCESS:
                    showSuccessDialog(object, paymentAccepted);
                    resetStates();
                    break;
                case INITIALIZED:
                    showNfcProgressDialog(true);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Prompts user if an incoming payment request shall be accepted or not.
     */
    private IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

        public boolean isPaymentAccepted() {
            return paymentAccepted;
        }

        public void promptUserPaymentRequest(String username, Currency currency, long amount, IUserPromptAnswer answer) {
            if (checkAutoAccept(username, amount)) {
                paymentAccepted = true;
                answer.acceptPayment();
                showNfcProgressDialog(false);
            } else {
                showCustomDialog(username, amount, answer);
            }
        }
    };

    /**
     * Checks if auto accepting of payments are enabled for given username and amount.
     *
     * @param username
     * @param amount
     * @return boolean if transaction shall be autoaccepted
     */
    private boolean checkAutoAccept(String username, long amount) {
        if (exchangeRate != null) {
            BigDecimal amountChf = CurrencyViewHandler.getAmountInCHF(exchangeRate, Converter.getBigDecimalFromLong(amount));
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isAutoAcceptEnabled = sharedPref.getBoolean("auto_accept", false);
            if (isAutoAcceptEnabled && getCoinBleskApplication().getStorageHandler().isTrustedContact(username)) {
                String value = sharedPref.getString("auto_accept_amount", "0");
                int limit = Integer.parseInt(value);
                if (amountChf.compareTo(new BigDecimal(limit)) <= 0)
                    return true;
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * Opens dialog to prompt user if he wants to accept or reject an incoming payment request.
     *
     * @param username
     * @param amount
     * @param answer2
     */
    private void showCustomDialog(String username, long amount, final IUserPromptAnswer answer2) {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup group = (ViewGroup) findViewById(R.id.pay_payment_popup);
        final View layout = inflater.inflate(R.layout.pay_payment_popup, group);
        popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        final TextView receiverTv = (TextView) layout.findViewById(R.id.payPayment_username);
        receiverTv.setText(username);
        final TextView amountTv = (TextView) layout.findViewById(R.id.payPayment_amountBTC);
        amountTv.setText(CurrencyViewHandler.formatBTCAsString(Converter.getBigDecimalFromLong(amount), getApplicationContext()));

        if (exchangeRate != null) {
            final TextView amountChfTv = (TextView) layout.findViewById(R.id.payPayment_amountCHF);
            amountChfTv.setText(CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, Converter.getBigDecimalFromLong(amount)));
            final TextView exchangeRateTv = (TextView) layout.findViewById(R.id.payPayment_exchangeRateValue);
            CurrencyViewHandler.setExchangeRateView(exchangeRate, exchangeRateTv);
            final TextView balanceTvChf = (TextView) layout.findViewById(R.id.payPayment_balanceCHF);
            balanceTvChf.setText(CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, getWalletService().getUnconfirmedBalance()));
        }
        final TextView balanceTvBtc = (TextView) layout.findViewById(R.id.payPayment_balanceBTC);
        balanceTvBtc.setText(CurrencyViewHandler.formatBTCAsString(getWalletService().getUnconfirmedBalance(), getApplicationContext()));

        layout.post(new Runnable() {
            public void run() {
                dismissNfcInProgressDialog();
                popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
            }
        });

        final Button rejectButton = (Button) layout.findViewById(R.id.payPayment_reject);
        rejectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                paymentAccepted = false;
                showNfcProgressDialog(false);
                answer2.rejectPayment();
                popupWindow.dismiss();
            }
        });

        final Button acceptButton = (Button) layout.findViewById(R.id.payPayment_accept);
        acceptButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                paymentAccepted = true;
                answer2.acceptPayment();
                popupWindow.dismiss();
                showNfcProgressDialog(false);
            }
        });
    }

    protected void refreshActivity() {
        this.recreate();
    }

    /**
     * Shows a dialog indicating if transaction was successful or not.
     *
     * @param object    (object with {@link PaymentResponse})
     * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
     */
    private void showSuccessDialog(Object object, boolean isSending) {
        dismissNfcInProgressDialog();
        String answer;
        if (object == null || !(object instanceof PaymentResponse)) {
            answer = getResources().getString(R.string.error_transaction_failed);
            showDialog(answer, false);
        } else {
            PaymentResponse pr = (PaymentResponse) object;
            BigDecimal amountBtc = Converter.getBigDecimalFromLong(pr.getAmount());
            BigDecimal balance = getWalletService().getBalance();
            String chfValue = "";
            if (exchangeRate != null) {
                chfValue = " (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")";
            }

            if (isSending) {
                getCoinBleskApplication().getStorageHandler().addAddressBookEntry(pr.getUsernamePayee());
                answer = String.format(getResources().getString(R.string.payment_notification_success_payer),
                        CurrencyViewHandler.formatBTCAsString(amountBtc, this) + chfValue,
                        pr.getUsernamePayee());
            } else {
                getCoinBleskApplication().getStorageHandler().addAddressBookEntry(pr.getUsernamePayer());
                answer = String.format(getResources().getString(R.string.payment_notification_success_payee),
                        CurrencyViewHandler.formatBTCAsString(amountBtc, this) + chfValue,
                        pr.getUsernamePayer());
            }
            showDialog(answer, true);
        }
    }

}
