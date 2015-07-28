package ch.uzh.csg.coinblesk.client.ui.main;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.util.concurrent.Service;

import org.bitcoinj.store.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.persistence.StorageHandler;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;

public class RestoreOrNewActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreOrNewActivity.class);

    private Button mRestoreWalletButton;
    private Button mCreateNewWalletButton;
    private EditText mBackupPhraseField;
    private EditText mUsernameField;

    private StorageHandler storageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_or_new);

        mRestoreWalletButton = (Button) findViewById(R.id.restoreOrNew_button_restoreWallet);
        mCreateNewWalletButton = (Button) findViewById(R.id.restoreOrNew_button_createNewWallet);
        mBackupPhraseField = (EditText) findViewById(R.id.restoreOrNew_editText_passphrase);
        mBackupPhraseField = (EditText) findViewById(R.id.restoreOrNew_editText_username);

        storageHandler = getCoinBleskApplication().getStorageHandler();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        initClickListener();
    }

    private void getSetupInfo(final RequestCompleteListener<SetupRequestObject> cro) {
        showLoadingProgressDialog();

        RequestTask<TransferObject, SetupRequestObject> task = getCoinBleskApplication().getRequestFactory().setupRequest(new RequestCompleteListener<SetupRequestObject>() {
            @Override
            public void onTaskComplete(SetupRequestObject response) {
                if (response.isSuccessful()) {
                    storageHandler.setWatchingKey(response.getServerWatchingKey());
                    storageHandler.setBitcoinNet(response.getBitcoinNet());
                    cro.onTaskComplete(response);
                } else {
                    displayResponse(getString(R.string.establish_internet_connection));
                    dismissProgressDialog();
                }
            }
        }, this);

        task.execute();
    }

    private void restoreWallet(final String mnemonic) {
        AsyncTask<Void, Void, Void> startWalletTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Service service = getWalletService().restoreWalletFromSeed(getCoinBleskApplication().getStorageHandler(), mnemonic, Constants.EARLIEST_COINBLESK_KEY);
                    service.awaitRunning();
                } catch (UnreadableWalletException e) {
                    LOGGER.error("Wallet setup failed: {}", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                startMainActivity();
            }
        };

        startWalletTask.execute();
    }

    private void setupWalletAndSaveWatchingKey() {

        AsyncTask<Void, Void, Void> startWalletTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Service service = getWalletService().init(storageHandler);
                service.awaitRunning();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                saveWatchingKey();
            }
        };

        startWalletTask.executeOnExecutor(Executors.newCachedThreadPool());
    }

    private void saveWatchingKey() {

        WatchingKeyTransferObject input = new WatchingKeyTransferObject();
        input.setWatchingKey(getWalletService().getWatchingKey());
        input.setBitcoinNet(getWalletService().getBitcoinNet());

        RequestTask<WatchingKeyTransferObject, TransferObject> task = getCoinBleskApplication().getRequestFactory().saveWatchingKeyRequest(new RequestCompleteListener<TransferObject>() {
            @Override
            public void onTaskComplete(TransferObject response) {
                if (response.isSuccessful()) {
                    startMainActivity();
                } else {
                    // reset the setup info in the local storage, or else, the next time the user starts the app,
                    // the app will go to the main activity, but will not be able to spend bitcoins, because
                    // the server doesn't have the clients watching key.
                    storageHandler.clear();

                    displayResponse(getString(R.string.establish_internet_connection));
                    dismissProgressDialog();
                }
            }
        }, input, this);

        task.execute();
    }

    private void initRestoreWalletClickListener() {
        mRestoreWalletButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (isUsernameMissing()) {
                    return;
                }

                getSetupInfo(new RequestCompleteListener<SetupRequestObject>() {
                    @Override
                    public void onTaskComplete(final SetupRequestObject response) {

                        final String mnemonic = mBackupPhraseField.getText().toString().toLowerCase();
                        if (BitcoinUtils.validMnemonic(mnemonic)) {
                            restoreWallet(mnemonic);
                        } else {
                            Animation shake = AnimationUtils.loadAnimation(RestoreOrNewActivity.this, R.anim.shake);
                            mBackupPhraseField.startAnimation(shake);
                            LOGGER.error("Invalid mnemonic seed: {}", mnemonic);
                            displayResponse(getResources().getString(R.string.restoreOrCreate_toast_restoreFailed));
                            dismissProgressDialog();
                        }
                    }
                });
            }
        });
    }

    private void initCreateNewWalletClickListener() {
        mCreateNewWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isUsernameMissing()) {
                    return;
                }

                LOGGER.debug("Setting up a new wallet...");
                getSetupInfo(new RequestCompleteListener<SetupRequestObject>() {
                    @Override
                    public void onTaskComplete(SetupRequestObject response) {
                        setupWalletAndSaveWatchingKey();
                    }
                });
            }
        });
    }

    private void initClickListener() {
        initCreateNewWalletClickListener();
        initRestoreWalletClickListener();

        mRestoreWalletButton.setEnabled(true);
        mCreateNewWalletButton.setEnabled(true);
    }

    private void startMainActivity() {

        // save the username before starting the main activity
        String username = mUsernameField.getText().toString();
        getCoinBleskApplication().getStorageHandler().setUsername(username);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("username", username).apply();

        // init the wallet service and start the main activity
        getWalletService().init(getCoinBleskApplication().getStorageHandler());
        Intent intent = new Intent(RestoreOrNewActivity.this, MainActivity.class);
        dismissProgressDialog();
        startActivity(intent);
        finish();
    }

    private boolean isUsernameMissing() {
        // check if username is set
        if(mUsernameField.getText().toString().isEmpty()) {
            displayResponse(getString(R.string.restoreOrNew_usernameMissing_toast));
            return true;
        }
        return false;
    }

}
