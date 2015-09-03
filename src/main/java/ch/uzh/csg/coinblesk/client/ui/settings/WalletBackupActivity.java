package ch.uzh.csg.coinblesk.client.ui.settings;

import android.content.ComponentName;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.Mailer;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

/**
 * This class is the view that informs the user about the involved parties to
 * build this application.
 */
public class WalletBackupActivity extends BaseActivity {

    private final static Logger LOGGER = LoggerFactory.getLogger(WalletBackupActivity.class);

    private TextView mTextWalletSeed;
    private ProgressBar mProgressBar;
    private Button mEmailRefundTxButton;
    private Button mRestoreWalletButton;
    private EditText mSeedEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_backup);
        setScreenOrientation();

        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_mnemonic);
        mTextWalletSeed = (TextView) findViewById(R.id.settings_walletBackupSeed);
        mEmailRefundTxButton = (Button) findViewById(R.id.walletBackup_emailRefundTxButton);
        mRestoreWalletButton = (Button) findViewById(R.id.walletBackup_restoreWalletButton);
        mSeedEditText = (EditText) findViewById(R.id.walletRestoreSeed);

        initClickListeners();
    }

    private void initClickListeners() {
        initRefundTxButtonListener();
        initRestoreWalletClickListener();
    }

    private void initRestoreWalletClickListener() {
        mRestoreWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ConnectionCheck.isNetworkAvailable(WalletBackupActivity.this)) {
                    Toast.makeText(WalletBackupActivity.this, getString(R.string.establish_internet_connection), Toast.LENGTH_LONG).show();
                    return;
                }
                String seed = mSeedEditText.getText().toString();
                if (!BitcoinUtils.validMnemonic(seed)) {
                    Toast.makeText(WalletBackupActivity.this, getString(R.string.seed_not_valid), Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    showLoadingProgressDialog();

                    // send bitcoins of this wallet to the restored wallet
                    if (getWalletService().getUnconfirmedBalance().signum() > 1) {
                        String address = BitcoinUtils.getP2SHAddressFromSeed(seed, getCoinBleskApplication().getStorageHandler().getServerWatchingKey(), getCoinBleskApplication().getStorageHandler().getBitcoinNet());
                        getWalletService().createPayment(address, getWalletService().getMaxSendAmount());
                    }

                    // now restore the wallet
                    Service service = getWalletService().restoreWalletFromSeed(getCoinBleskApplication().getStorageHandler(), seed, Constants.EARLIEST_COINBLESK_KEY);
                    service.addListener(new Service.Listener() {
                        @Override
                        public void running() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dismissProgressDialog();
                                }
                            });
                        }

                        @Override
                        public void failed(Service.State from, Throwable failure) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dismissProgressDialog();
                                    Toast.makeText(WalletBackupActivity.this, getString(R.string.settings_walletBackupRestoreFailedToast), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }, Executors.newSingleThreadExecutor());
                } catch (Exception e) {
                    LOGGER.error("Wallet restore failed", e);
                    Toast.makeText(WalletBackupActivity.this, getString(R.string.settings_walletBackupRestoreFailedToast), Toast.LENGTH_LONG).show();
                }
            }
        });
        mRestoreWalletButton.setEnabled(true);
    }

    private void initRefundTxButtonListener() {
        mEmailRefundTxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long refundTxValidBlock = getCoinBleskApplication().getStorageHandler().getRefundTxValidBlock();
                String base64Tx = getCoinBleskApplication().getStorageHandler().getRefundTx();

                if (refundTxValidBlock == RefundTx.NO_REFUND_TX_VALID_BLOCK || null == base64Tx) {
                    Toast.makeText(WalletBackupActivity.this, R.string.refundTxMail_noRefundTxYet, Toast.LENGTH_LONG);
                    return;
                }

                // send email
                String hexTx = BitcoinUtils.base64TxToHex(base64Tx);
                String username = getCoinBleskApplication().getStorageHandler().getUsername();
                String refundTxValid = BitcoinUtils.refundTxValidBlockToFriendlyString(refundTxValidBlock - getWalletService().getCurrentBlockHeight(), WalletBackupActivity.this);
                String subject = getString(R.string.refundTxMail_subject);

                StringBuilder message = new StringBuilder();
                message.append(String.format(getString(R.string.refundTxMail_salutation), username));
                message.append("\n\n");
                message.append(String.format(getString(R.string.refundTxMail_explanation), refundTxValid));
                message.append("\n\n");
                message.append(hexTx);
                message.append("\n\n");
                message.append(getString(R.string.refundTxMail_ending));

                Mailer mailer = new Mailer();
                mailer.setMessage(message.toString());
                mailer.setSubject(subject);

                mailer.sendEmail(WalletBackupActivity.this);

            }
        });
        mEmailRefundTxButton.setEnabled(true);

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);


        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return getWalletService().getMnemonicSeed();
            }

            @Override
            protected void onPostExecute(String result) {
                mProgressBar.setVisibility(View.GONE);
                mTextWalletSeed.setText(result);
                initRefundTxButtonListener();
            }
        };

        task.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

}
