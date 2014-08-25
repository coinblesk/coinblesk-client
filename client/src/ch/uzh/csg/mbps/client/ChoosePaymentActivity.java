package ch.uzh.csg.mbps.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import ch.uzh.csg.mbps.client.payment.ReceivePaymentActivity;
import ch.uzh.csg.mbps.client.payment.SendPaymentActivity;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.TimeHandler;

/**
 * Activity for selecting which kind of payment shall be initialized.
 *
 */
public class ChoosePaymentActivity extends AbstractLoginActivity {
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

		initClickListener();
		checkOnlineModeAndProceed();
	}

	private void checkOnlineModeAndProceed() {
		if(!ClientController.isOnline()){
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
		initializeMenuItems(menu, getApplicationContext());
		
		//renew Session Timeout Countdown
		if(ClientController.isOnline()){
			startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
		}
		
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
