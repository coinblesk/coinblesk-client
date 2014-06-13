package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.MainActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This is the UI to receive a payment - i.e. to be the seller in a transaction or to actively send bitcoins by NFC.
 */
public class ReceivePaymentActivity extends AbstractPaymentActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private MenuItem menuWarning;
	private String[] strings = { "CHF", "BTC" };
	protected CalculatorDialog newFragment;
	protected static BigDecimal amountBTC = BigDecimal.ZERO;
	protected static BigDecimal inputUnitValue = BigDecimal.ZERO;
	private BigDecimal exchangeRate;
	private EditText receiveAmount;
	private TextView descriptionOfInputUnit;
	private SharedPreferences settings;
	AnimationDrawable nfcActivityAnimation;
	
	protected static final String INPUT_UNIT_CHF = "CHF";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		isSeller = true;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_payment);
		setScreenOrientation();
		
		Constants.inputUnit = INPUT_UNIT_CHF;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		exchangeRate = BigDecimal.ZERO;
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		receiveAmount = (EditText) findViewById(R.id.receivePayment_amountText);
		receiveAmount.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openCalculatorDialog();			
			}
		});
		
		receiveAmount.setFocusable(false);
		
		descriptionOfInputUnit = (TextView)findViewById(R.id.receivePayment_enterAmountIn);
		launchRequest();

		Spinner spinner = (Spinner) findViewById(R.id.receivePayment_currencySpinner);
		spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, strings));
		spinner.setOnItemSelectedListener(spinnerListener);
		spinner.setSelection(0);
		
		refreshCurrencyTextViews();
		
		//adapt view for actively sending instead of requesting bitcoins
		Intent myIntent = getIntent(); // gets the previously created intent
		boolean isSend = myIntent.getBooleanExtra("isSend", false); 
		if(isSend){
			TextView title = (TextView) findViewById(R.id.receivePayment_title);
			title.setText(getResources().getString(R.string.sendPayment_title));
			ImageView logo = (ImageView) findViewById(R.id.receivePayment_logo);
			logo.setImageResource(R.drawable.ic_pay_payment_big);
			getActionBar().setTitle(getResources().getString(R.string.title_activity_send_payment));   
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.receive_payment, menu);
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

	@Override
	protected void launchRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
			getExchangeRate.execute();
		}
	}
	
	public void onTaskComplete(CustomResponseObject response) {
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.exchangeRate));	
		if (response.isSuccessful()) {
			exchangeRate = new BigDecimal(response.getMessage());
			CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.exchangeRate));
			BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalance();
			CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_balance), balance, getBaseContext());
			TextView balanceTv = (TextView) findViewById(R.id.receivePayment_balance);
			balanceTv.append(" (" + CurrencyViewHandler.amountInCHF(exchangeRate, balance) + ")");
		} else if(response.getMessage().equals(Constants.REST_CLIENT_ERROR)){
			displayResponse(getResources().getString(R.string.no_connection_server));
			finish();
			launchActivity(this, MainActivity.class);
		}
		dismissProgressDialog();
	}

	private void refreshCurrencyTextViews() {
		amountBTC = BigDecimal.ZERO;
		if (Constants.inputUnit.equals(INPUT_UNIT_CHF)) {
			try {
				BigDecimal amountChf = CurrencyFormatter.getBigDecimalChf(receiveAmount.getText().toString());
				inputUnitValue = amountChf;
				amountBTC = CurrencyViewHandler.getBitcoinExchangeValue(exchangeRate, amountChf);
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_CHFinBTC), amountBTC, getApplicationContext());
			} catch (NumberFormatException e) {
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_CHFinBTC), BigDecimal.ZERO, getApplicationContext());
			}
		} else {
			try{
				BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(receiveAmount.getText().toString());
				inputUnitValue = BigDecimal.ZERO;
				amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.receivePayment_CHFinBTC), exchangeRate, amountBTC);
			} catch(NumberFormatException e) {
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.receivePayment_CHFinBTC), exchangeRate, amountBTC);
			}
		}
		
		//Check if the user defined a fee on the received amount of bitcoin.
		if(settings.getBoolean("include_fee", false)){
			String percentageStr = settings.getString("fee_amount", "pref_fee_amount");
			try{
				double percentage = 1 + ((double) Integer.valueOf(percentageStr))/100;
				amountBTC = amountBTC.multiply(new BigDecimal(percentage)).setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP) ;
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_BTCIncFee), amountBTC, getApplicationContext());
				((TextView)findViewById(R.id.receivePayment_BTCIncFeeText)).setText("(" + getResources().getString(R.string.receivePayment_fee) + " " +  percentageStr +"%)");				
			} catch(NumberFormatException e){
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_BTCIncFee), BigDecimal.ZERO, getApplicationContext());
			}
		}
	}

	private void openCalculatorDialog() {
		newFragment = new CalculatorDialog(this);
		newFragment.show();
		newFragment.setOnDismissListener(new OnDismissListener() {
			
			public void onDismiss(DialogInterface dialog) {
				receiveAmount.setText(Constants.inputValueCalculator.toString());
				refreshCurrencyTextViews();
				
				//show nfc instructions
				findViewById(R.id.receivePayment_establishNfcConnectionInfo).setVisibility(View.VISIBLE);;
				//create animated nfc activity image
				ImageView nfcActivity = (ImageView) findViewById(R.id.receivePayment_nfcIcon);
				nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
				nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
				nfcActivityAnimation.start();
			}
		});
	}

	//TODO: refactor, since no Transaction model class anymore
//	@Override
//	protected void updateGUI(Transaction tx) {
//		//nothing to update on the seller side
//	}
	
	@Override
	protected void resetGUI() {
		receiveAmount.setText("");
		refreshCurrencyTextViews();
	}

	private class MyAdapter extends ArrayAdapter<String> {
		
		public MyAdapter(Context context, int textViewResourceId, String[] objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.spinner_currency, parent, false);
			TextView label = (TextView) row.findViewById(R.id.textView_currency);
			label.setText(strings[position]);

			return row;
		}
	}
	
	private OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {

		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			if (pos == 0)
				Constants.inputUnit = INPUT_UNIT_CHF;
			else
				Constants.inputUnit = CurrencyViewHandler.getBitcoinUnit(getApplicationContext());
			
			descriptionOfInputUnit.setText(Constants.inputUnit);
			refreshCurrencyTextViews();
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
		
	};
	
}
