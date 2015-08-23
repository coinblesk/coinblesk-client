package ch.uzh.csg.coinblesk.client.ui.settings;

import android.content.ComponentName;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.Mailer;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

/**
 * This class is the view that informs the user about the involved parties to
 * build this application.
 */
public class WalletBackupActivity extends BaseActivity {

    private TextView mTextWalletSeed;
    private ProgressBar mProgressBar;
    private Button mEmailRefundTxButton;

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

        initClickListeners();
    }

    private void initClickListeners() {
        mEmailRefundTxButton.setEnabled(true);
        mEmailRefundTxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long refundTxValidBlock = getCoinBleskApplication().getStorageHandler().getRefundTxValidBlock();
                String base64Tx = getCoinBleskApplication().getStorageHandler().getRefundTx();

                if(refundTxValidBlock == RefundTx.NO_REFUND_TX_VALID_BLOCK || null == base64Tx) {
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
                initClickListeners();
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
