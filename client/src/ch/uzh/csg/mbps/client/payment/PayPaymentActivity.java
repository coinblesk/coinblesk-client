package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This is the UI to accept a payment - i.e. to be the buyer in a transaction.
 */
public class PayPaymentActivity extends AbstractPaymentActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private BigDecimal exchangeRate;
	
	private TextView resultTextView;
	private TextView resultTextViewCHF;
	private TextView sellerUsername;
	
	private Button acceptBtn;
	private Button rejectBtn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		isSeller = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pay_payment);
		setScreenOrientation();
		
		resultTextView = (TextView) findViewById(R.id.payPayment_amountBTC);
		resultTextViewCHF = (TextView) findViewById(R.id.payPayment_amountCHF);
		
		acceptBtn = (Button) findViewById(R.id.payPayment_accept);
		rejectBtn = (Button) findViewById(R.id.payPayment_reject);
		sellerUsername = (TextView) findViewById(R.id.payPayment_username);
		
		acceptBtn.setEnabled(false);
		rejectBtn.setEnabled(false);
		
		
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), ClientController.getStorageHandler().getUserAccount().getBalance(), getApplicationContext());
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		initClickListener();
		checkOnlineModeAndProceed();
	}
    
    @Override
    public void onResume(){
    	super.onResume();
    	checkOnlineModeAndProceed();
    	invalidateOptionsMenu();
    }
	
	private void initClickListener() {

		rejectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disableButtons();
			}
		});

		acceptBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disableButtons();
			}
		});
	}
	
	private void checkOnlineModeAndProceed() {
		if (ClientController.isOnline()) {
			launchRequest();
		} else {
			CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), ClientController.getStorageHandler().getUserAccount().getBalance(), getApplicationContext());
			CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.payPayment_balanceCHF));
		}
	}

	protected void launchRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
			getExchangeRate.execute();
		}
	}
	
	public void onTaskComplete(CustomResponseObject response) {
		dismissProgressDialog();
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.payPayment_balanceCHF));
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.payPayment_exchangeRateValue));
		if (response.isSuccessful()) {
			exchangeRate = new BigDecimal(response.getMessage());
			CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.payPayment_exchangeRateValue));
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.payPayment_balanceCHF), exchangeRate, ClientController.getStorageHandler().getUserAccount().getBalance());
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		} else {
			exchangeRate = BigDecimal.ZERO;
		}
	}
	
	//TODO: refactor, since no Transaction model class anymore
//	@Override
//	protected void updateGUI(Transaction tx) {
//		CurrencyViewHandler.setBTC(resultTextView, tx.getAmount(), getApplicationContext());
//		
//		if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
//			resultTextViewCHF.setText("- CHF");
//		} else {
//			CurrencyViewHandler.setToCHF(resultTextViewCHF, exchangeRate, tx.getAmount());
//		}
//		sellerUsername.setText(tx.getSellerUsername());
//		
//		if (acceptPaymentAutomatically) {
//			getHCEService().setUserDecision(true);
//			disableButtons();
//		} else {
//			acceptBtn.setEnabled(true);
//			rejectBtn.setEnabled(true);
//		}
//	}
	
	@Override
	protected void refreshActivity() {
		CurrencyViewHandler.clearTextView(resultTextView);
		CurrencyViewHandler.clearTextView(resultTextViewCHF);
		CurrencyViewHandler.clearTextView(sellerUsername);
		
		BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalance();
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), balance, getApplicationContext());
		if (ClientController.isOnline()) {
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.payPayment_balanceCHF), exchangeRate, balance);
		}
		
		disableButtons();
	}

	private void disableButtons() {
		acceptBtn.setEnabled(false);
		rejectBtn.setEnabled(false);
	}
	
}
