package ch.uzh.csg.mbps.client;

import ch.uzh.csg.mbps.client.payment.PayPaymentActivity;
import ch.uzh.csg.mbps.client.payment.ReceivePaymentActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class ChoosePaymentActivity extends Activity {
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
				//				handleAsyncTask();
				launchActivity(ChoosePaymentActivity.this, ReceivePaymentActivity.class);
			}
		});

		sendPaymentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//				handleAsyncTask();
				launchActivity(ChoosePaymentActivity.this, PayPaymentActivity.class);
			}
		});

		requestPaymentNoNfcBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//				handleAsyncTask();
				launchActivity(ChoosePaymentActivity.this, HistoryActivity.class);
			}
		});
	}

	/**
	 * Launchs a new activity.
	 * @param activity
	 * @param classActvity
	 */
	private <T> void launchActivity(Activity activity, Class<T> classActvity) {
		Intent intent = new Intent(activity, classActvity);
		startActivity(intent);
	}
}
