package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
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
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.TransactionRequestTask;
import ch.uzh.csg.mbps.client.security.KeyHandler;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.DecoderFactory;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.PaymentRequest;
import ch.uzh.csg.mbps.customserialization.PaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerPaymentRequest;
import ch.uzh.csg.mbps.customserialization.ServerPaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerResponseStatus;
import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;
import ch.uzh.csg.mbps.util.Converter;

/**
 * This is the UI to send a payment directly to a known receiver without the use of NFC communication.
 */
public class SendPaymentActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private String[] currencies = { "CHF", "BTC" };
	protected CalculatorDialog calculatorDialogFragment;
	protected static BigDecimal amountBTC = BigDecimal.ZERO;
	protected static BigDecimal amountCHF = BigDecimal.ZERO;
	private BigDecimal exchangeRate;
	private EditText sendAmount;
	private TextView descriptionOfInputUnit;
	AnimationDrawable nfcActivityAnimation;
	private Button sendButton;
	private Button addressBookButton;
	public static EditText receiverUsernameEditText;

	private MenuItem menuWarning;
	private MenuItem offlineMode;
	private MenuItem sessionCountdownMenuItem;
	private MenuItem sessionRefreshMenuItem;
	private TextView sessionCountdown;
	private CountDownTimer timer;

	protected static final String INPUT_UNIT_CHF = "CHF";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_payment);
		setScreenOrientation();

		Constants.inputUnit = INPUT_UNIT_CHF;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		exchangeRate = BigDecimal.ZERO;

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
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		initializeMenuItems(menu);
		invalidateOptionsMenu();
		return true;
	}

	protected void initializeMenuItems(Menu menu){
		menuWarning = menu.findItem(R.id.action_warning);
		offlineMode = menu.findItem(R.id.menu_offlineMode);
		TextView offlineModeTV = (TextView) offlineMode.getActionView();
		offlineModeTV.setText(getResources().getString(R.string.menu_offlineModeText));
		
		menuWarning.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchRequest();
				return false;
			}
		});
		//setup timer
		sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
		sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
		sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);
		sessionRefreshMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchRequest();
				return false;
			}
		});
	}

	@Override
	public void invalidateOptionsMenu() {
		if(menuWarning != null){
			if(ClientController.isOnline()) {
				menuWarning.setVisible(false);
				offlineMode.setVisible(false);
				sessionCountdownMenuItem.setVisible(true);
				sessionRefreshMenuItem.setVisible(true);
			} else {
				menuWarning.setVisible(true);
				offlineMode.setVisible(true);
				sessionCountdownMenuItem.setVisible(false);
				sessionRefreshMenuItem.setVisible(false);
			}
		}
	}
	
	/**
	 * Start Timer for Session Countdown in Options Menu.
	 * 
	 * @param duration Time left in milliseconds.
	 * @param interval Interval in which timer is updated in milliseconds.
	 */
	private void startTimer(long duration, long interval) {
		if (timer != null) {
			timer.cancel();
		}
		timer = new CountDownTimer(duration, interval) {
			@Override
			public void onFinish() {
				//Session Timeout is already handled by TimeHandler
			}
			@Override
			public void onTick(long millisecondsLeft) {
				int secondsLeft = (int) Math.round((millisecondsLeft / (double) 1000));
				sessionCountdown.setText(getResources().getString(R.string.menu_sessionCountdown) + " " + TimeHandler.getInstance().formatCountdown(secondsLeft));
			}
		};
		timer.start();
	}

	/**
	 * Launches request for updating Exchange Rate
	 */
	private void launchRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
			getExchangeRate.execute();
		}
	}

	private void launchTransactionRequest(ServerPaymentRequest serverPaymentRequest) {
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
		dismissProgressDialog();

		if (response.getType() == Type.CREATE_TRANSACTION) {
			if (!response.isSuccessful()) {
				displayResponse(response.getMessage());
			} else {
				//renew Session Timeout Countdown
				if(ClientController.isOnline()){
					startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
				}

				byte[] serverPaymentResponseBytes = response.getServerPaymentResponse();
				ServerPaymentResponse serverPaymentResponse = null;
				try {
					serverPaymentResponse = DecoderFactory.decode(ServerPaymentResponse.class, serverPaymentResponseBytes);
				} catch (Exception e) {
					displayResponse(getResources().getString(R.string.error_transaction_failed));
					return;
				}

				PaymentResponse paymentResponsePayer = serverPaymentResponse.getPaymentResponsePayer();
				//verification of server response not needed as no interaction with selling partner
				if (paymentResponsePayer.getStatus() == ServerResponseStatus.SUCCESS ) {
					//update textviews
					receiverUsernameEditText.setText("");
					sendAmount.setText("");
					refreshCurrencyTextViews();
					BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalance()
							.subtract(Converter.getBigDecimalFromLong(paymentResponsePayer.getAmount()));
					CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_balance), balance, getBaseContext());
					TextView balanceTv = (TextView) findViewById(R.id.sendPayment_balance);
					balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
					BigDecimal amountBtc = Converter.getBigDecimalFromLong(paymentResponsePayer.getAmount());
					
					String s = String.format(getResources().getString(R.string.payment_notification_success_payer),
							CurrencyViewHandler.formatBTCAsString(amountBtc, this) + " (" +CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")",
							paymentResponsePayer.getUsernamePayee());
					showDialog(getResources().getString(R.string.payment_success), R.drawable.ic_payment_succeeded, s);
					
					boolean saved = ClientController.getStorageHandler().addAddressBookEntry(serverPaymentResponse.getPaymentResponsePayer().getUsernamePayee());
					if (!saved) {
						displayResponse(getResources().getString(R.string.error_xmlSave_failed));
					}
				} else if (paymentResponsePayer.getStatus() == ServerResponseStatus.DUPLICATE_REQUEST) {
					showDialog(getResources().getString(R.string.payment_failure), R.drawable.ic_payment_failed, getResources().getString(R.string.transaction_duplicate_error));
				} else {
					showDialog(getResources().getString(R.string.payment_failure), R.drawable.ic_payment_failed, paymentResponsePayer.getReason());
				}
			}
		} else if (response.getType() == Type.EXCHANGE_RATE) {
			CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.sendPayment_exchangeRate));	
			if (response.isSuccessful()) {
				//renew Session Timeout Countdown
				if(ClientController.isOnline()){
					startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
				}
				exchangeRate = new BigDecimal(response.getMessage());
				CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.sendPayment_exchangeRate));
				BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalance();
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_balance), balance, getBaseContext());
				TextView balanceTv = (TextView) findViewById(R.id.sendPayment_balance);
				balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
			} else { //Server couldn't get exchange rate 
				displayResponse(getResources().getString(R.string.exchangeRate_error));
			}
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			displayResponse(getResources().getString(R.string.no_connection_server));
			finish();
			launchActivity(this, MainActivity.class);
		} else {
			displayResponse(response.getMessage());
		}
	}

	private void refreshCurrencyTextViews() {
		amountBTC = BigDecimal.ZERO;
		if (Constants.inputUnit.equals(INPUT_UNIT_CHF)) {
			try {
				amountCHF = CurrencyFormatter.getBigDecimalChf(sendAmount.getText().toString());
				amountBTC = CurrencyViewHandler.getBitcoinExchangeValue(exchangeRate, amountCHF);
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), amountBTC, getApplicationContext());
			} catch (NumberFormatException e) {
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), BigDecimal.ZERO, getApplicationContext());
			}
		} else {
			try{
				BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(sendAmount.getText().toString());
				amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
				amountCHF = CurrencyViewHandler.getAmountInCHF(exchangeRate, amountBTC);
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
			} catch(NumberFormatException e) {
				CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
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

		public MyAdapter(Context context, int textViewResourceId,
				String[] objects) {
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

	private void setUpGui() {
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

		sendButton = (Button) findViewById(R.id.sendPayment_sendButton);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// hide virtual keyboard
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				createTransaction();
			}
		});

		addressBookButton = (Button) findViewById(R.id.sendPayment_addressBookButton);
		addressBookButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AddressBookDialog dialog = new AddressBookDialog();
				dialog.show(getFragmentManager(), "sendPaymentActivity");
			}
		});

	}

	private void createTransaction(){
		CustomKeyPair ckp = ClientController.getStorageHandler().getKeyPair();
		if (!receiverUsernameEditText.getText().toString().isEmpty() && !(amountBTC == null || amountBTC.compareTo(BigDecimal.ZERO) == 0)) {
			if(receiverUsernameEditText.getText().toString().equals(ClientController.getStorageHandler().getUserAccount().getUsername())){
				displayResponse(getResources().getString(R.string.sendPayment_error_user));
				return;
			}
			try {
				PaymentRequest paymentRequestPayer = new PaymentRequest(
						PKIAlgorithm.DEFAULT, 
						ckp.getKeyNumber(), 
						ClientController.getStorageHandler().getUserAccount().getUsername(), 
						receiverUsernameEditText.getText().toString(), 
						Currency.BTC, 
						Converter.getLongFromBigDecimal(amountBTC),
						Currency.CHF, 
						Converter.getLongFromBigDecimal(amountCHF), 
						System.currentTimeMillis());

				paymentRequestPayer.sign(KeyHandler.decodePrivateKey(ckp.getPrivateKey()));
				ServerPaymentRequest serverPaymentRequest = new ServerPaymentRequest(paymentRequestPayer);
				launchTransactionRequest(serverPaymentRequest);
			} catch (Exception e) {
				displayResponse(getResources().getString(R.string.sendPayment_error));
			}
		} else {
			displayResponse(getResources().getString(R.string.fill_necessary_fields));
		}
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
			receiverEntries = ClientController.getStorageHandler().getAddressBook();

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
				if (ClientController.getStorageHandler().isTrustedContact(username)) {
					entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_starred), null,null,null);
				} else{
					entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_not_starred), null,null,null);
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
