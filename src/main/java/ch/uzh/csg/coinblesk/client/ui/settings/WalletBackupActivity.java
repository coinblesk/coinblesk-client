package ch.uzh.csg.coinblesk.client.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;

/**
 * This class is the view that informs the user about the involved parties to
 * build this application.
 */
public class WalletBackupActivity extends WalletActivity {

    private TextView mTextWalletSeed;
    private ProgressBar mProgressBar;
    private Button mCopyButton;
    private Button mEmailRefundTxButton;
    private Handler mHandler;
    private String mnemonic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_backup);
        setScreenOrientation();

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_mnemonic);
        mTextWalletSeed = (TextView) findViewById(R.id.settings_walletBackupSeed);
        mCopyButton = (Button) findViewById(R.id.walletBackup_copyButton);
        mEmailRefundTxButton = (Button) findViewById(R.id.walletBackup_emailRefundTxButton);
        mHandler = new Handler();

        initClickListeners();
    }

    private void initClickListeners() {
        mCopyButton.setEnabled(true);
        mEmailRefundTxButton.setEnabled(true);
        mCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Wallet Backup", mnemonic);
                clipboard.setPrimaryClip(clip);
                displayResponse(getResources().getString(R.string.copy_clipboard));
            }
        });
        mEmailRefundTxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
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
                mnemonic = result;
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
