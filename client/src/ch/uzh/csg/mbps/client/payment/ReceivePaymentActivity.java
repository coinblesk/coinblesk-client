package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PrivateKey;
import java.security.PublicKey;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
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
import ch.uzh.csg.mbps.client.request.TransactionRequestTask;
import ch.uzh.csg.mbps.client.security.KeyHandler;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.DecoderFactory;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.PaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerPaymentRequest;
import ch.uzh.csg.mbps.customserialization.ServerPaymentResponse;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;
import ch.uzh.csg.mbps.util.Converter;
import ch.uzh.csg.paymentlib.IPaymentEventHandler;
import ch.uzh.csg.paymentlib.IServerResponseListener;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.PaymentRequestInitializer;
import ch.uzh.csg.paymentlib.PaymentRequestInitializer.PaymentType;
import ch.uzh.csg.paymentlib.container.PaymentInfos;
import ch.uzh.csg.paymentlib.container.ServerInfos;
import ch.uzh.csg.paymentlib.container.UserInfos;
import ch.uzh.csg.paymentlib.messages.PaymentError;

/**
 * This is the UI to receive a payment - i.e. to be the seller in a transaction or to actively send bitcoins by NFC.
 */
public class ReceivePaymentActivity extends AbstractPaymentActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private String[] strings = { "CHF", "BTC" };
	protected CalculatorDialog newFragment;
	protected static BigDecimal amountBTC = BigDecimal.ZERO;
	protected static BigDecimal inputUnitValue = BigDecimal.ZERO;
	private BigDecimal exchangeRate;
	private EditText receiveAmount;
	private TextView descriptionOfInputUnit;
	private SharedPreferences settings;
	AnimationDrawable nfcActivityAnimation;
	private boolean isSendingMode;

	private MenuItem menuWarning;
	private MenuItem offlineMode;
	private MenuItem sessionCountdownMenuItem;
	private MenuItem sessionRefreshMenuItem;
	private TextView sessionCountdown;
	private CountDownTimer timer;

	private NfcAdapter nfcAdapter;
	private IServerResponseListener responseListener;
	private PaymentRequestInitializer paymentRequestInitializer;
	private boolean serverResponseSuccessful = false;


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

		Spinner spinner = (Spinner) findViewById(R.id.receivePayment_currencySpinner);
		spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, strings));
		spinner.setOnItemSelectedListener(spinnerListener);
		spinner.setSelection(0);

		refreshCurrencyTextViews();

		//adapt view for actively sending instead of requesting bitcoins
		Intent myIntent = getIntent(); // gets the previously created intent
		isSendingMode = myIntent.getBooleanExtra("isSend", false); 
		if(isSendingMode){
			TextView title = (TextView) findViewById(R.id.receivePayment_title);
			title.setText(getResources().getString(R.string.sendPayment_title));
			ImageView logo = (ImageView) findViewById(R.id.receivePayment_logo);
			logo.setImageResource(R.drawable.ic_pay_payment_big);
			getActionBar().setTitle(getResources().getString(R.string.title_activity_send_payment));			
		}
		else{

		}
	}

	@Override
	public void onPause() {
		if (paymentRequestInitializer != null){
			paymentRequestInitializer.disable();
			paymentRequestInitializer = null;
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		checkOnlineModeAndProceed();
		invalidateOptionsMenu();
		super.onResume();
	}

	private void checkOnlineModeAndProceed() {
		if (ClientController.isOnline()) {
			launchRequest();
		} else {
			launchOfflineMode(getApplicationContext());
		}
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

	protected void startTimer(long duration, long interval) {
		if(timer != null){
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
	 * Launches request to update exchange rate.
	 */
	protected void launchRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
			getExchangeRate.execute();
		}
	}

	public void onTaskComplete(CustomResponseObject response) {
		dismissProgressDialog();
		dismissNfcInProgressDialog();
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.receivePayment_exchangeRate));	
		if (response.getType() == Type.EXCHANGE_RATE) {
			if (response.isSuccessful()) {
				//renew Session Timeout Countdown
				if(ClientController.isOnline()){
					startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
				}
				exchangeRate = new BigDecimal(response.getMessage());
				CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.receivePayment_exchangeRate));
				BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalance();
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_balance), balance, getBaseContext());
				TextView balanceTv = (TextView) findViewById(R.id.receivePayment_balance);
				balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
			} else if(response.getMessage().equals(Constants.REST_CLIENT_ERROR)){
				displayResponse(getResources().getString(R.string.no_connection_server));
				finish();
				launchActivity(this, MainActivity.class);
			}
		} else if (response.getType() == Type.CREATE_TRANSACTION && response.isSuccessful()) {

			byte[] serverPaymentResponseBytes = response.getServerPaymentResponse();
			ServerPaymentResponse serverPaymentResponse = null;
			try {
				serverPaymentResponse = DecoderFactory.decode(ServerPaymentResponse.class, serverPaymentResponseBytes);
			} catch (Exception e) {
				displayResponse(getResources().getString(R.string.error_transaction_failed));
				return;
			}
			responseListener.onServerResponse(serverPaymentResponse);
		} else if (response.getMessage() != null && (response.getMessage().equals(Constants.CONNECTION_ERROR) || response.getMessage().equals(Constants.REST_CLIENT_ERROR))) {
			displayResponse(getResources().getString(R.string.no_connection_server));
			finish();
			launchActivity(this, MainActivity.class);
		}
		else {
			displayResponse(response.getMessage());
		}
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
				amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
				inputUnitValue = CurrencyViewHandler.getAmountInCHF(exchangeRate, amountBTC);
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

				if(amountBTC.compareTo(BigDecimal.ZERO) > 0) {
					PaymentInfos paymentInfos;
					try {
						paymentInfos = new PaymentInfos(Currency.BTC, Converter.getLongFromBigDecimal(amountBTC), Currency.CHF, Converter.getLongFromBigDecimal(inputUnitValue));
						initializeNFC(paymentInfos);
					} catch (Exception e) {
						displayResponse(getResources().getString(R.string.unexcepted_error));
					}
					showNfcInstructions();
				}

			}
		});
	}

	private void showNfcInstructions(){
		findViewById(R.id.receivePayment_establishNfcConnectionInfo).setVisibility(View.VISIBLE);;
		ImageView nfcActivity = (ImageView) findViewById(R.id.receivePayment_nfcIcon);
		nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
		nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
		nfcActivityAnimation.start();
	}

	protected void refreshActivity() {
		receiveAmount.setText("");
		this.recreate();
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


	private void initializeNFC(PaymentInfos paymentInfos) throws Exception {
		PublicKey publicKeyServer = KeyHandler.decodePublicKey(ClientController.getStorageHandler().getServerPublicKey().getPublicKey());
		final ServerInfos serverInfos = new ServerInfos(publicKeyServer);
		PrivateKey privateKey = ch.uzh.csg.mbps.client.security.KeyHandler.decodePrivateKey(ClientController.getStorageHandler().getKeyPair().getPrivateKey());
		final UserInfos userInfos = new UserInfos(ClientController.getStorageHandler().getUserAccount().getUsername(), privateKey, PKIAlgorithm.DEFAULT, ClientController.getStorageHandler().getKeyPair().getKeyNumber());

		nfcAdapter = createAdapter(ReceivePaymentActivity.this);
		if (nfcAdapter == null) {
			return;
		}

		if(isSendingMode){
			try {
				if (paymentRequestInitializer == null) {
					paymentRequestInitializer = new PaymentRequestInitializer(ReceivePaymentActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, persistencyHandler, PaymentType.SEND_PAYMENT);
				} else {
					paymentRequestInitializer.setPaymentInfos(paymentInfos);
				}
			} catch (Exception e) {
				displayResponse(getResources().getString(R.string.unexcepted_error));
				launchActivity(ReceivePaymentActivity.this, MainActivity.class);
			}
		}
		else {
			try {
				if (paymentRequestInitializer == null) {
					paymentRequestInitializer = new PaymentRequestInitializer(ReceivePaymentActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, persistencyHandler, PaymentType.REQUEST_PAYMENT);
				} else {
					paymentRequestInitializer.setPaymentInfos(paymentInfos);
				}
			} catch (Exception e) {
				displayResponse(getResources().getString(R.string.unexcepted_error));
				launchActivity(ReceivePaymentActivity.this, MainActivity.class);
			}
		}
	}

	private IPaymentEventHandler eventHandler = new IPaymentEventHandler() {
		public void handleMessage(PaymentEvent event, Object object, IServerResponseListener caller) {

			switch (event) {
			case ERROR:
				PaymentError err = (PaymentError) object;
				if (err != null){
					switch (err) {
					case DUPLICATE_REQUEST:
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.transaction_duplicate_error), false);
						break;
					case NO_SERVER_RESPONSE:
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.error_transaction_failed), false);
						break;
					case PAYER_REFUSED:
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.transaction_rejected), false);
						break;
					case REQUESTS_NOT_IDENTIC:
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.transaction_server_rejected), false);
						break;
					case SERVER_REFUSED:
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.transaction_server_rejected), false);
						break;
					case UNEXPECTED_ERROR:
						if (!serverResponseSuccessful) {
							dismissNfcInProgressDialog();
							showDialog(getResources().getString(R.string.error_transaction_failed), false);
						}
						break;
					case INIT_FAILED:
						//ignore
						break;
					default:
						break;
					}
					resetStates();
				}
				break;
			case FORWARD_TO_SERVER:
				try {
					showNfcProgressDialog(true);
					ServerPaymentRequest serverPaymentRequest = DecoderFactory.decode(ServerPaymentRequest.class, (byte[]) object);
					responseListener = caller;
					launchTransactionRequest(serverPaymentRequest);
				} catch (Exception e) {
					displayResponse(getResources().getString(R.string.unexcepted_error));
				}
				break;
			case SUCCESS:
				serverResponseSuccessful = true;
				showSuccessDialog(object, isSendingMode);
				break;
			case INITIALIZED:
				showNfcProgressDialog(true);
				break;
			default:
				break;
			}
		}
	};

	/**
	 * Launches request to send a new transaction to the server.
	 * 
	 * @param serverPaymentRequest object with transaction details
	 */
	private void launchTransactionRequest(ServerPaymentRequest serverPaymentRequest) {
		if (ClientController.isOnline()) {
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

	/**
	 * Shows a dialog indicating if transaction was successful or not.
	 * 
	 * @param object (object with {@link PaymentResponse})
	 * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
	 */
	private void showSuccessDialog(Object object, boolean isSending) {
		dismissNfcInProgressDialog();
		String answer;
		if (object == null || !(object instanceof PaymentResponse)) {
			answer = getResources().getString(R.string.error_transaction_failed);
		}
		else {
			PaymentResponse pr = (PaymentResponse) object;
			BigDecimal amountBtc = Converter.getBigDecimalFromLong(pr.getAmount());

			if(isSending){
				ClientController.getStorageHandler().addAddressBookEntry(pr.getUsernamePayee());
				answer = String.format(getResources().getString(R.string.payment_notification_success_payer),
						CurrencyViewHandler.formatBTCAsString(amountBtc, this) + " (" +CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")",
						pr.getUsernamePayee());
			}
			else {
				ClientController.getStorageHandler().addAddressBookEntry(pr.getUsernamePayer());
				answer = String.format(getResources().getString(R.string.payment_notification_success_payee),
						CurrencyViewHandler.formatBTCAsString(amountBtc, this) + " (" +CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")",
						pr.getUsernamePayer());
			}
		}
		showDialog(answer, true);
	}
}
