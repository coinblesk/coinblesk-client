package ch.uzh.csg.coinblesk.client.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;

/**
 * Activity for selecting which kind of payment shall be initialized.
 */
public class ChoosePaymentActivity extends BaseActivity {
    private Button requestPaymentBtn;
    private Button sendPaymentBtn;
    private Button requestPaymentNoNfcBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_payment);
        setScreenOrientation();

        requestPaymentBtn = (Button) findViewById(R.id.choosePayment_requestPaymentBtn);
        sendPaymentBtn = (Button) findViewById(R.id.choosePayment_sendPaymentBtn);
        requestPaymentNoNfcBtn = (Button) findViewById(R.id.choosePayment_requestPaymentNoNfcBtn);

        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initClickListener();
        checkOnlineModeAndProceed();
    }

    private void checkOnlineModeAndProceed() {
        if (!ConnectionCheck.isNetworkAvailable(this)) {
            requestPaymentBtn.setEnabled(false);
            sendPaymentBtn.setEnabled(false);
            requestPaymentNoNfcBtn.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        invalidateOptionsMenu();
        return true;
    }

    private void initClickListener() {
        requestPaymentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ChoosePaymentActivity.this, ReceivePaymentActivity.class);
                intent.putExtra("isSend", false);
                startActivity(intent);
                finish();
            }
        });

        sendPaymentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ChoosePaymentActivity.this, ReceivePaymentActivity.class);
                intent.putExtra("isSend", true);
                startActivity(intent);
                finish();
            }
        });

        requestPaymentNoNfcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchActivity(ChoosePaymentActivity.this, SendPaymentActivity.class);
                finish();
            }
        });
    }
}
