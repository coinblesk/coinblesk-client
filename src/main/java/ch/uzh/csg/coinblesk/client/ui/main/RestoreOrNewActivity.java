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
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;

public class RestoreOrNewActivity extends BaseActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreOrNewActivity.class);

    private Button mRestoreWalletButton;
    private Button mCreateNewWalletButton;
    private EditText mBackupPhraseField;
    private EditText mUsernameField;

    private StorageHandler storageHandler;


    //TOOD: restore wallet form settings menu
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
                /*startMainActivity();*/
            }
        };

        startWalletTask.execute();
    }







}
