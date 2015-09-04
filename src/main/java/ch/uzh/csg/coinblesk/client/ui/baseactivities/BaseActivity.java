package ch.uzh.csg.coinblesk.client.ui.baseactivities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.PublicKey;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.ui.fragments.CustomDialogFragment;
import ch.uzh.csg.coinblesk.client.ui.payment.PaymentActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

/**
 * The class BaseActivity is the abstract base class for all
 * activity-contexts. This class inherits from fragmentActivity and extends the
 * activity with some common functions, which are used by almost all activities.
 * This class determines if the application is used by a smart-phone or by a
 * tablet and sets the orientation appropriately.
 */
public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(BaseActivity.class);
    
    private MenuItem menuWarning;
    private MenuItem offlineMode;
    private ProgressDialog progressDialog;
    private boolean destroyed = false;

    private WalletService walletService = null;
    private boolean isBound = false;

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        LOGGER.debug("{} connected to the wallet service.", name.toShortString());
        final WalletService.LocalBinder binder = (WalletService.LocalBinder) service;
        walletService = binder.getService();
        walletStatus(true);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        LOGGER.debug("{} disconnected from the wallet service.", name.toShortString());
        walletService = null;
        walletStatus(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startWalletService();
    }

    private void startWalletService() {
        final Intent serviceIntent = new Intent(this, WalletService.class);
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    //get notified if the wallet is ready
    protected void walletStatus(boolean connected) {}


    public WalletService getWalletService() {
        return walletService;
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissProgressDialog();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        if(isBound) {
            unbindService(this);
            isBound = false;
        }
    }


    /**
     * Starts the progress dialog. As long as the dialog is running other touch
     * actions are ignored.
     */
    public void showLoadingProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
        }

        progressDialog.setMessage(getResources().getString(R.string.loading_progress_dialog));
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.show();
            }
        });
    }

    /**
     * Closes the progress dialog.
     */
    public void dismissProgressDialog() {
        if (progressDialog != null && !destroyed) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Starts a new activity. The method is called only for guiding to another
     * activity.
     * 
     * @param <T>
     *            generic placeholder for the passed parameter
     * @param activity
     *            this parameter represents the activity in which the client is
     *            or was before going to the next activity
     * @param classActvity
     *            The class of the activity which will be started.
     */
    public <T> void launchActivity(Activity activity, Class<T> classActvity) {
        Intent intent = new Intent(activity, classActvity);
        startActivity(intent);
    }

    /**
     * This method is a placeholder for the toast but much simpler to use.
     * 
     * @param response
     *            The message which is shown in a toast call.
     */
    public void displayResponse(String response) {
        Toast.makeText(this, response, Toast.LENGTH_LONG).show();
    }

    /**
     * Checks the resolution of the device and determines its orientation.
     * Smart-phones are in portrait and tablets in landscape view. This method
     * is called by every activity. Disables the rotation function of the
     * screen.
     */
    public void setScreenOrientation() {
        if (isTablet()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    protected void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Shows a customized dialog fragment {@link CustomDialogFragment} with a
     * title, message and an icon for success or fail. Notifies about finished
     * action. The dialog disables other touch actions.
     * 
     * @param title
     *            The title of the notification
     * @param icon
     *            The success or fail symbol are shown. The icons integer value
     *            is passed.
     * @param message
     *            The content of the actions message.
     * @return Returns dialog fragment that contains title, icon, and message
     */
    public CustomDialogFragment showDialog(String title, int icon, String message) {
        FragmentManager fragmentManager = getFragmentManager();
        CustomDialogFragment dialogFR = CustomDialogFragment.create(title, icon, message);
        dialogFR.setCancelable(false);

        try {
            dialogFR.show(fragmentManager, "CustomDialog");
        } catch (Exception e) {
        }

        return dialogFR;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        offlineMode = menu.findItem(R.id.menu_offlineMode);

        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void invalidateOptionsMenu() {
        if (menuWarning != null) {
            if (ConnectionCheck.isNetworkAvailable(this)) {
                menuWarning.setVisible(false);
                offlineMode.setVisible(false);
            } else {
                menuWarning.setVisible(true);
                offlineMode.setVisible(true);
            }
        }
    }

    public CoinBleskApplication getCoinBleskApplication() {
        return (CoinBleskApplication) getApplication();
    }

    public boolean isTablet() {
        return !getResources().getBoolean(R.bool.small_device);
    }

    public void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
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
    protected void checkAccept(final long satoshis, final String receiver, PublicKey remotePubKey, final PaymentActivity.UserPaymentConfirmation confirmation) {
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
            BigDecimal autoAcceptAmount = new BigDecimal(PreferenceManager.getDefaultSharedPreferences(BaseActivity.this).getString("auto_accept_amount", "0"));
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

    private AlertDialog d = null;

    public void showNFCDialog(boolean bt) {
        if(bt) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder alert = new AlertDialog.Builder(BaseActivity.this);
                alert.setTitle("Hold to NFC");
                alert.setMessage("Hold to NFC");

                alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        d = null;
                    }
                });

                d = alert.show();
            }
        });

    }

    public void hideNFCDialog() {
        if (d != null) {
            d.dismiss();
            d = null;
        }
    }

    private void showConfirmationDialog(final BigDecimal amount, final String user, final PaymentActivity.UserPaymentConfirmation confirmation) {

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {

                String amountString;
                if (response.isSuccessful()) {
                    BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                    amountString = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amount, BaseActivity.this);
                } else {
                    amountString = amount.toString();
                }

                String message = String.format(getString(R.string.sendPayment_dialog_message), amountString, user);

                final AlertDialog.Builder alert = new AlertDialog.Builder(BaseActivity.this);
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
     * Saves transaction meta data and address book entry after a successful payment
     * @param isSending true if the user SENT bitcoins, false if the user received bitcoins
     * @param signedTx the fully signed, raw bitcoin transaction
     * @param remoteUser the username of the other party
     * @param remotePubKey the public key of the other party
     * @param bitcoinAddress the bitcoin address of the other party
     */
    protected void postPayment(final boolean isSending, final byte[] signedTx, final String remoteUser, final PublicKey remotePubKey, final String bitcoinAddress) {

        // add transaction to the wallet, save transaction metadata and address book entry in the background....
        AsyncTask<Void, Void, Void> postPaymentTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                BitcoinNet bitcoinNet = getWalletService().getBitcoinNet();

                // tx meta data
                String txId = BitcoinUtils.getTxHash(signedTx, bitcoinNet);
                TransactionMetaData txMetaData = getCoinBleskApplication().getStorageHandler().getTransactionMetaData(txId);
                txMetaData = txMetaData != null ? txMetaData : new TransactionMetaData(txId);
                txMetaData.setReceiver(isSending ? remoteUser : TransactionMetaData.SELF);
                txMetaData.setSender(isSending ? TransactionMetaData.SELF : remoteUser);
                txMetaData.setType(isSending ? TransactionMetaData.TransactionType.COINBLESK_PAY_OUT : TransactionMetaData.TransactionType.COINBLESK_PAY_IN);
                getCoinBleskApplication().getStorageHandler().saveTransactionMetaData(txMetaData);
                LOGGER.debug("Saved transaction meta data");

                // save (or update)  user in address book
                AddressBookEntry entry = getCoinBleskApplication().getStorageHandler().getAddressBookEntry(remotePubKey);
                entry = entry != null ? entry : new AddressBookEntry(remotePubKey);
                entry.setName(remoteUser);
                entry.setBitcoinAddress(bitcoinAddress);
                getCoinBleskApplication().getStorageHandler().saveAddressBookEntry(entry);
                LOGGER.debug("Saved address book entry");

                getWalletService().commitAndBroadcastTx(signedTx);
                LOGGER.debug("added transaction to the wallet");

                return null;
            }
        };
        postPaymentTask.execute();
    }

}
