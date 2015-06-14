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

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;

public class RestoreOrNewActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreOrNewActivity.class);

    private Button mRestoreWalletButton;
    private Button mCreateNewWalletButton;
    private EditText mBackupPhraseField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_or_new);

        mRestoreWalletButton = (Button) findViewById(R.id.restoreOrNew_button_restoreWallet);
        mCreateNewWalletButton = (Button) findViewById(R.id.restoreOrNew_button_createNewWallet);
        mBackupPhraseField = (EditText) findViewById(R.id.restoreOrNew_edit_passphrase);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        initClickListener();
    }

    private void initClickListener() {

        final BitcoinNet bitcoinNet = ClientController.getStorageHandler().getBitcoinNet();
        final String serverWatchingKey = ClientController.getStorageHandler().getWatchingKey();
        final long creationTime = ClientController.getStorageHandler().getUserAccount().getCreationDate();

        mRestoreWalletButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String backupPhrase = mBackupPhraseField.getText().toString();

                if(BitcoinUtils.validMnemonic(backupPhrase)) {

                    showLoadingProgressDialog();

                    AsyncTask<Void, Void, Void> startWalletTask = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                showLoadingProgressDialog();
                                Service service = getWalletService().restoreWalletFromSeed(bitcoinNet, serverWatchingKey, backupPhrase, creationTime);
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

        mRestoreWalletButton.setEnabled(true);
        mCreateNewWalletButton.setEnabled(true);
    }

    private void startMainActivity() {
        Intent intent = new Intent(RestoreOrNewActivity.this, MainActivity.class);
        startActivity(intent);
    }

}
