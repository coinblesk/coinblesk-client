package ch.uzh.csg.coinblesk.client.ui.main;

import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.util.concurrent.Service;

import org.bitcoinj.store.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.persistence.InternalStorage;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;

public class RestoreOrNewActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreOrNewActivity.class);

    private Button mRestoreWalletButton;
    private Button mCreateNewWalletButton;
    private EditText mBackupPhraseField;

    private InternalStorage storageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_or_new);

        mRestoreWalletButton = (Button) findViewById(R.id.restoreOrNew_button_restoreWallet);
        mCreateNewWalletButton = (Button) findViewById(R.id.restoreOrNew_button_createNewWallet);
        mBackupPhraseField = (EditText) findViewById(R.id.restoreOrNew_edit_passphrase);

        storageHandler = getCoinBleskApplication().getStorageHandler();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        initClickListener();
    }

    private void getSetupInfo(final IAsyncTaskCompleteListener<SetupRequestObject> cro) {
        getCoinBleskApplication().getRequestFactory().setupRequest(new IAsyncTaskCompleteListener<SetupRequestObject>() {
            @Override
            public void onTaskComplete(SetupRequestObject response) {
                storageHandler.setWatchingKey(response.getServerWatchingKey());
                storageHandler.setBitcoinNet(response.getBitcoinNet());
                cro.onTaskComplete(response);
            }
        }, this);
    }

    private void initRestoreWalletClickListener() {
        mRestoreWalletButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                getSetupInfo(new IAsyncTaskCompleteListener<SetupRequestObject>() {
                    @Override
                    public void onTaskComplete(final SetupRequestObject response) {

                        final String backupPhrase = mBackupPhraseField.getText().toString();
                        if (BitcoinUtils.validMnemonic(backupPhrase)) {
                            showLoadingProgressDialog();

                            AsyncTask<Void, Void, Void> startWalletTask = new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    try {
                                        showLoadingProgressDialog();
                                        Service service = getWalletService().restoreWalletFromSeed(response.getBitcoinNet(), response.getServerWatchingKey(), backupPhrase, 0L);
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

                        } else {
                            Animation shake = AnimationUtils.loadAnimation(RestoreOrNewActivity.this, R.anim.shake);
                            mBackupPhraseField.startAnimation(shake);
                            LOGGER.error("Failed to restore wallet with seed {}", backupPhrase);
                            displayResponse(getResources().getString(R.string.restoreOrCreate_toast_restoreFailed));
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
                LOGGER.debug("Setting up a new wallet...");
                getSetupInfo(new IAsyncTaskCompleteListener<SetupRequestObject>() {
                    @Override
                    public void onTaskComplete(SetupRequestObject response) {
                        startMainActivity();
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
        getWalletService().init(storageHandler.getBitcoinNet(), storageHandler.getWatchingKey());
        Intent intent = new Intent(RestoreOrNewActivity.this, MainActivity.class);
        startActivity(intent);
    }

}
