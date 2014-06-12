package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.MainActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.TransactionRequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.customserialization.ServerPaymentRequest;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;
import ch.uzh.csg.mbps.util.Converter;

/**
 * This is the UI to receive a payment - i.e. to be the seller in a transaction or to actively send bitcoins by NFC.
 */
public class SendPaymentActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private MenuItem menuWarning;
	private String[] currencies = { "CHF", "BTC" };
	protected CalculatorDialog calculatorDialogFragment;
	protected static BigDecimal amountBTC = BigDecimal.ZERO;
	protected static BigDecimal inputUnitValue = BigDecimal.ZERO;
	private BigDecimal exchangeRate;
	private EditText sendAmount;
	private TextView descriptionOfInputUnit;
	private SharedPreferences settings;
	AnimationDrawable nfcActivityAnimation;
	private Button sendButton;
	private Button addressBookButton;
	public static EditText receiverUsernameEditText;

	protected static final String INPUT_UNIT_CHF = "CHF";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_payment);
		setScreenOrientation();

		Constants.inputUnit = INPUT_UNIT_CHF;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		exchangeRate = BigDecimal.ZERO;
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		launchRequest();
		setUpGui();
		refreshCurrencyTextViews();

	}

	@Override
	public void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.send_payment, menu);
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

	protected void launchRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
			getExchangeRate.execute();
		}
	}

	protected void launchTransactionRequest(ServerPaymentRequest serverPaymentRequest) {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			CreateTransactionTransferObject ctto = null;
			try {
				ctto = new CreateTransactionTransferObject(serverPaymentRequest);
			} catch (Exception e ) {
				displayResponse("Internal Error");
			}
			RequestTask transactionRequest = new TransactionRequestTask(this, ctto);
			transactionRequest.execute();
		}
	}

	public void onTaskComplete(CustomResponseObject response) {
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.sendPayment_exchangeRate));	
		if (response.isSuccessful()) {
			if(response.getType().equals(Type.EXCHANGE_RATE)){
				exchangeRate = new BigDecimal(response.getMessage());
				CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.sendPayment_exchangeRate));
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_balance), ClientController.getUser().getBalance(), getBaseContext());
				TextView balanceTv = (TextView) findViewById(R.id.sendPayment_balance);
				balanceTv.append(" (" + CurrencyViewHandler.amountInCHF(exchangeRate, ClientController.getUser().getBalance()) + ")");
			}
			else if (response.getType().equals(Type.OTHER)){
				if(response.isSuccessful()){
					String s = String.format(CommUtils.Message.PAYMENT_SUCCESS_BUYER.getMessage(), CurrencyFormatter.formatBTC(Converter.getBigDecimalFromLong(response.getServerPaymentResponse().getPaymentResponsePayer().getAmount())), response.getServerPaymentResponse().getPaymentResponsePayer().getUsernamePayee());
					showDialog(getResources().getString(R.string.payment_success), R.drawable.ic_payment_succeeded, s);
					try {
						ClientController.getStorageHandler().addAddressBookEntry(response.getServerPaymentResponse().getPaymentResponsePayer().getUsernamePayee());
					} catch (Exception e) {
						//do nothing
					}
				} else {
					showDialog(getResources().getString(R.string.payment_failure), R.drawable.ic_payment_failed, response.getMessage());
				}
			}
		} else if(response.getMessage().equals(Constants.REST_CLIENT_ERROR)){
			displayResponse(getResources().getString(R.string.no_connection_server));
			finish();
			launchActivity(this, MainActivity.class);
		}
		else{
			displayResponse(response.getMessage());
		}
		dismissProgressDialog();
	}

	private void refreshCurrencyTextViews() {
		amountBTC = BigDecimal.ZERO;
		if (Constants.inputUnit.equals(INPUT_UNIT_CHF)) {
			try {
				BigDecimal amountChf = CurrencyFormatter.getBigDecimalChf(sendAmount.getText().toString());
				inputUnitValue = amountChf;
				amountBTC = CurrencyViewHandler.getBitcoinExchangeValue(exchangeRate, amountChf);
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), amountBTC, getApplicationContext());
			} catch (NumberFormatException e) {
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), BigDecimal.ZERO, getApplicationContext());
			}
		} else {
			try{
				BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(sendAmount.getText().toString());
				inputUnitValue = BigDecimal.ZERO;
				amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
			} catch(NumberFormatException e) {
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
			}
		}

		//Check if the user defined a fee on the received amount of bitcoin.
		if(settings.getBoolean("include_fee", false)){
			String percentageStr = settings.getString("fee_amount", "pref_fee_amount");
			try{
				double percentage = 1 + ((double) Integer.valueOf(percentageStr))/100;
				amountBTC = amountBTC.multiply(new BigDecimal(percentage)).setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP) ;
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_BTCIncFee), amountBTC, getApplicationContext());
				((TextView)findViewById(R.id.sendPayment_BTCIncFeeText)).setText("(" + getResources().getString(R.string.receivePayment_fee) + " " +  percentageStr +"%)");				
			} catch(NumberFormatException e){
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_BTCIncFee), BigDecimal.ZERO, getApplicationContext());
			}
		}
	}

	private void openCalculatorDialog() {
		calculatorDialogFragment = new CalculatorDialog(this);
		calculatorDialogFragment.show();
		calculatorDialogFragment.setOnDismissListener(new OnDismissListener() {

			public void onDismiss(DialogInterface dialog) {
				sendAmount.setText(Constants.inputValueCalculator.toString());
				refreshCurrencyTextViews();
			}
		});
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
			label.setText(currencies[position]);

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

	private void setUpGui(){
		sendAmount = (EditText) findViewById(R.id.sendPayment_amountText);
		sendAmount.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openCalculatorDialog();			
			}
		});
		sendAmount.setFocusable(false);

		descriptionOfInputUnit = (TextView)findViewById(R.id.sendPayment_enterAmountIn);
		receiverUsernameEditText = (EditText)findViewById(R.id.sendPayment_enterReceiver);

		Spinner spinner = (Spinner) findViewById(R.id.sendPayment_currencySpinner);
		spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, currencies));
		spinner.setOnItemSelectedListener(spinnerListener);
		spinner.setSelection(0);

		sendButton = (Button)findViewById(R.id.sendPayment_sendButton);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				createTransaction();
			}
		});

		addressBookButton = (Button)findViewById(R.id.sendPayment_addressBookButton);
		addressBookButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AddressBookDialog dialog = new AddressBookDialog();
				dialog.show(getFragmentManager(), "sendPaymentActivity");
			}
		});

	}

	public void createTransaction(){
		//TODO simon: add PrivateKey
		//TODO: refactor, since no Transaction model class anymore

		//		PaymentRequest paymentRequestPayer = new PaymentRequest(pkiAlgorithm, keyNumber, ClientController.getUser().getUsername(), receiverUsernameEditText.getText().toString() , Currency.BTC, amountBTC, Constants.inputUnit  , inputUnitValue, new Date());
		//
		//		try {
		//			paymentRequestPayer.sign(privateKey);
		//			ServerPaymentRequest serverPaymentRequest = new ServerPaymentRequest(paymentRequestPayer);
		//			launchTransactionRequest(serverPaymentRequest);
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}
	}

	/**
	 * Creates a dialog which shows all entries from addressbook. Selected item
	 * will be written to SendPaymentActivity.receiverUsernameEditText.
	 * 
	 */
	public static class AddressBookDialog extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Set<String> receiverEntries = null;
			try {
				receiverEntries = ClientController.getStorageHandler().getAddressBook();
			} catch (Exception e1) {
				//do nothing
			}

			final CharSequence[] cs = receiverEntries.toArray(new CharSequence[receiverEntries.size()]);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.sendPayment_selectReceiver);

			ScrollView scrollView = new ScrollView(getActivity().getApplicationContext());
			LinearLayout linearLayout = new LinearLayout(getActivity().getApplicationContext());
			linearLayout.setOrientation(LinearLayout.VERTICAL);

			for(int i=0; i < cs.length; i++){
				final TextView entry = new TextView(getActivity().getApplicationContext());
				final String username = cs[i].toString();

				entry.setGravity(android.view.Gravity.CENTER_VERTICAL);
				entry.setPadding(0, 0, 0, 10);
				entry.setTextColor(Color.BLACK);
				entry.setText(username);
				try {
					if (ClientController.getStorageHandler().isTrustedContact(username)) {
						entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_starred), null,null,null);
					} else{
						entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_not_starred), null,null,null);
					}
				} catch (Exception e) {
					//do nothing
				}

				entry.setClickable(true);
				entry.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						SendPaymentActivity.receiverUsernameEditText.setText(username);
						dismiss();
					}
				});

				linearLayout.addView(entry);
			}
			scrollView.addView(linearLayout);
			builder.setView(scrollView);

			return builder.create();
		}
	}

}