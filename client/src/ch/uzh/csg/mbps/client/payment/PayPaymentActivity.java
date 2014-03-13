package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.Transaction;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This is the UI to accept a payment - i.e. to be the buyer in a transaction.
 */
public class PayPaymentActivity extends AbstractPaymentActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private BigDecimal exchangeRate;
	private MenuItem menuWarning;
	
	private TextView resultTextView;
	private TextView resultTextViewCHF;
	private TextView sellerUsername;
	
	private Button acceptBtn;
	private Button rejectBtn;
	
	private boolean acceptPaymentAutomatically = false;
	private Switch acceptPaymentSwitch;
	
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
		
		acceptPaymentSwitch = (Switch) findViewById(R.id.payPayment_switch);
		acceptPaymentSwitch.setChecked(false);
		
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), ClientController.getUser().getBalance(), getApplicationContext());
		
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.pay_payment, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuWarning = menu.findItem(R.id.action_warning);
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public void invalidateOptionsMenu() {
		if (menuWarning != null) {
			if (ClientController.isOnline()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
	}
	
	private void initClickListener() {

		rejectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disableButtons();
				getHCEService().setUserDecision(false);
			}
		});

		acceptBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disableButtons();
				getHCEService().setUserDecision(true);
			}
		});

		acceptPaymentSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
					acceptPaymentAutomatically = true;
				else
					acceptPaymentAutomatically = false;
			}
		});
	}
	
	private void checkOnlineModeAndProceed() {
		if (ClientController.isOnline()) {
			launchRequest();
		} else {
			CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), ClientController.getUser().getBalance(), getApplicationContext());
			CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.payPayment_balanceCHF));
		}
	}

	@Override
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
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.payPayment_balanceCHF), exchangeRate, ClientController.getUser().getBalance());
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		} else {
			exchangeRate = BigDecimal.ZERO;
		}
	}
	
	@Override
	protected void updateGUI(Transaction tx) {
		CurrencyViewHandler.setBTC(resultTextView, tx.getAmount(), getApplicationContext());
		
		if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
			resultTextViewCHF.setText("- CHF");
		} else {
			CurrencyViewHandler.setToCHF(resultTextViewCHF, exchangeRate, tx.getAmount());
		}
		sellerUsername.setText(tx.getSellerUsername());
		
		if (acceptPaymentAutomatically) {
			getHCEService().setUserDecision(true);
			disableButtons();
		} else {
			acceptBtn.setEnabled(true);
			rejectBtn.setEnabled(true);
		}
	}
	
	@Override
	protected void resetGUI() {
		CurrencyViewHandler.clearTextView(resultTextView);
		CurrencyViewHandler.clearTextView(resultTextViewCHF);
		CurrencyViewHandler.clearTextView(sellerUsername);
		
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.payPayment_balanceBTC), ClientController.getUser().getBalance(), getApplicationContext());
		if (ClientController.isOnline()) {
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.payPayment_balanceCHF), exchangeRate, ClientController.getUser().getBalance());
		}
		
		disableButtons();
	}

	private void disableButtons() {
		acceptBtn.setEnabled(false);
		rejectBtn.setEnabled(false);
	}
	
}
