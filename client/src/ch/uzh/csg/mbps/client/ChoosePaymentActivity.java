package ch.uzh.csg.mbps.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ch.uzh.csg.mbps.client.payment.ReceivePaymentActivity;
import ch.uzh.csg.mbps.client.util.ClientController;

public class ChoosePaymentActivity extends AbstractAsyncActivity {
	private MenuItem menuWarning;
	private Button requestPaymentBtn;
	private Button sendPaymentBtn;
	private Button requestPaymentNoNfcBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_payment);

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
		getMenuInflater().inflate(R.menu.choose_payment, menu);
		return true;
	}

	private void initClickListener() {

		requestPaymentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ChoosePaymentActivity.this, ReceivePaymentActivity.class);
				intent.putExtra("isSend", false);
				startActivity(intent);
			}
		});

		sendPaymentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ChoosePaymentActivity.this, ReceivePaymentActivity.class);
				intent.putExtra("isSend", true);
				startActivity(intent);
			}
		});

		requestPaymentNoNfcBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchActivity(ChoosePaymentActivity.this, SendPaymentActivity.class);
			}
		});
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuWarning = menu.findItem(R.id.action_warning);
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public void invalidateOptionsMenu() {
		if(menuWarning != null){
			if(ClientController.isOnline()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
	}
}
