package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

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
import android.text.method.DigitsKeyListener;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
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
import ch.uzh.csg.mbps.customserialization.exceptions.NotSignedException;
import ch.uzh.csg.mbps.responseobject.TransactionObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;
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
public class ReceivePaymentActivity extends AbstractPaymentActivity {
	private String[] strings;
	private String[] stringsNormal = { "CHF", "BTC" };
	private String[] stringsTablet = { "CHF", "Rp", "BTC" };

	protected CalculatorDialog newFragment;
	protected static BigDecimal amountBTC = BigDecimal.ZERO;
	protected static BigDecimal inputUnitValue = BigDecimal.ZERO;
	private BigDecimal exchangeRate;
	private String receiveAmount = "0";
	private TextView receiveAmountTextView;
	private EditText receiveAmountEditText;
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
	private static boolean isPortrait  = false;


	protected static final String INPUT_UNIT_CHF = "CHF";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		isSeller = true;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_payment);
		setScreenOrientation();
		if (getResources().getBoolean(R.bool.portrait_only)) {
			isPortrait = true;
			strings = stringsNormal;
		} else {
			strings = stringsTablet;
			isPortrait = false;
		}

		Constants.inputUnit = INPUT_UNIT_CHF;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		exchangeRate = BigDecimal.ZERO;
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		descriptionOfInputUnit = (TextView)findViewById(R.id.receivePayment_enterAmountIn);

		Spinner spinner = (Spinner) findViewById(R.id.receivePayment_currencySpinner);
		spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, strings));
		spinner.setOnItemSelectedListener(spinnerListener);
		spinner.setSelection(0);
		if(Constants.IS_MENSA_MODE && !isPortrait) {
			isRpInputMode = true;
			spinner.setSelection(1);
		}

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

		if (isPortrait) {
			receiveAmountEditText = (EditText) findViewById(R.id.receivePayment_amountText);
			receiveAmountEditText.setFocusable(false);
			receiveAmountEditText.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					openCalculatorDialog();			
				}
			});
		} else {
			receiveAmountTextView = (TextView) findViewById(R.id.receivePayment_amount);
			initializeCalculator();
		}
	}

	@Override
	public void onPause() {
		clearPaymentInfos();
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
			launchExchangeRateRequest();
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
				launchExchangeRateRequest();
				return false;
			}
		});

		//setup timer
		sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
		sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
		sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);
		sessionRefreshMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchExchangeRateRequest();
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
	 * Launches request for updating Exchange Rate
	 */
	public void launchExchangeRateRequest() {
		if (ClientController.isOnline()) {
			showLoadingProgressDialog();
			RequestTask<TransferObject, TransferObject> request = new ExchangeRateRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
				public void onTaskComplete(TransferObject response) {
					if (!response.isSuccessful()) {
						displayResponse(response.getMessage());
						return;
					}
					onTaskCompleteExchangeRate(response.getMessage());
				}
			}, new TransferObject(), new TransferObject());
			request.execute();
		}
	}
	
	private void onTaskCompleteExchangeRate(String exchangeRateNew) {
		dismissProgressDialog();
		dismissNfcInProgressDialog();
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.receivePayment_exchangeRate));
		//renew Session Timeout Countdown
		if(ClientController.isOnline()){
			startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
		}
		exchangeRate = new BigDecimal(exchangeRateNew);
		CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.receivePayment_exchangeRate));
		BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalanceBTC();
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_balance), balance, getBaseContext());
		TextView balanceTv = (TextView) findViewById(R.id.receivePayment_balance);
		balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
		//TODO: finish() on	REST_CLIENT_ERROR and launchActivity(this, MainActivity.class);?
	}

	private void refreshCurrencyTextViews() {
		amountBTC = BigDecimal.ZERO;
		if (Constants.inputUnit.equals(INPUT_UNIT_CHF)) {
			try {
				BigDecimal amountChf = CurrencyFormatter.getBigDecimalChf(receiveAmount);
				inputUnitValue = amountChf;
				amountBTC = CurrencyViewHandler.getBitcoinExchangeValue(exchangeRate, amountChf);
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_CHFinBTC), amountBTC, getApplicationContext());
			} catch (NumberFormatException e) {
				CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_CHFinBTC), BigDecimal.ZERO, getApplicationContext());
			}
		} else {
			try{
				BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(receiveAmount);
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
				initializePayment();
			}
		});
	}

	private void initializePayment() {
		receiveAmount = Constants.inputValueCalculator.toString();
		if (isPortrait) {
			receiveAmountEditText.setText(receiveAmount);
		} else {
			receiveAmountTextView.setText(receiveAmount);
		}
		refreshCurrencyTextViews();

		if (amountBTC.compareTo(BigDecimal.ZERO) > 0) {
			PaymentInfos paymentInfos;
			try {
				paymentInfos = new PaymentInfos(Currency.BTC,
						Converter.getLongFromBigDecimal(amountBTC),
						Currency.CHF,
						Converter.getLongFromBigDecimal(inputUnitValue));
				initializeNFC(paymentInfos);
			} catch (Exception e) {
				displayResponse(getResources().getString(
						R.string.unexcepted_error));
			}
			showNfcInstructions();
		} else {
			hideNfcInstructions();
		}
	}

	private void clearPaymentInfos() {
		if (paymentRequestInitializer != null){
			paymentRequestInitializer.disable();
			paymentRequestInitializer = null;
		}
		hideNfcInstructions();
		receiveAmount = "0";
		if (isPortrait) {
			receiveAmountEditText.setText(receiveAmount);
		} else {
			receiveAmountTextView.setText(receiveAmount);
		}
		refreshCurrencyTextViews();
	}

	private void showNfcInstructions(){
		findViewById(R.id.receivePayment_establishNfcConnectionInfo).setVisibility(View.VISIBLE);;
		ImageView nfcActivity = (ImageView) findViewById(R.id.receivePayment_nfcIcon);
		nfcActivity.setVisibility(View.VISIBLE);
		nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
		nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
		nfcActivityAnimation.start();
	}

	private void hideNfcInstructions() {
		findViewById(R.id.receivePayment_establishNfcConnectionInfo)
		.setVisibility(View.INVISIBLE);
		findViewById(R.id.receivePayment_nfcIcon).setVisibility(View.INVISIBLE);
	}

	protected void refreshActivity() {
		receiveAmount = "0";
		if (isPortrait)
			receiveAmountEditText.setText("");
		else {
			receiveAmountTextView.setText("");
			clearCalculator();
		}
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
			if(isPortrait){
				if (pos == 0) {
					Constants.inputUnit = INPUT_UNIT_CHF;
					isRpInputMode = false;
					adaptCalculatorToRpMode();
				} else {
					Constants.inputUnit = CurrencyViewHandler.getBitcoinUnit(getApplicationContext());
					isRpInputMode = false;
					adaptCalculatorToRpMode();
				}
			} else {
				if (pos == 0) {
					Constants.inputUnit = INPUT_UNIT_CHF;
					isRpInputMode = false;
					adaptCalculatorToRpMode();
				}
				else if (pos == 1) {
					Constants.inputUnit = INPUT_UNIT_CHF;
					isRpInputMode = true;
					adaptCalculatorToRpMode();

				} else {
					Constants.inputUnit = CurrencyViewHandler.getBitcoinUnit(getApplicationContext());
					isRpInputMode = false;
					adaptCalculatorToRpMode();
					disableMensaButtons();
				}
			}

			if(isPortrait) {
				descriptionOfInputUnit.setText(Constants.inputUnit);
			}
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

		//disable android beam (touch to beam screen)
		nfcAdapter.setNdefPushMessage(null, this, this);

		if(isSendingMode){
			try {
				if (paymentRequestInitializer != null) {
					paymentRequestInitializer.disable(); 
					paymentRequestInitializer = null;
				}
				paymentRequestInitializer = new PaymentRequestInitializer(ReceivePaymentActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, persistencyHandler, PaymentType.SEND_PAYMENT);
				paymentRequestInitializer.enableNfc();
			} catch (Exception e) {
				displayResponse(getResources().getString(R.string.unexcepted_error));
				launchActivity(ReceivePaymentActivity.this, MainActivity.class);
			}
		}
		else {
			try {
				if (paymentRequestInitializer != null) {
					paymentRequestInitializer.disable(); 
					paymentRequestInitializer = null;
				}
				paymentRequestInitializer = new PaymentRequestInitializer(ReceivePaymentActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, persistencyHandler, PaymentType.REQUEST_PAYMENT);
				paymentRequestInitializer.enableNfc();
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
						String errorMessage = err.getErrorCause();
						if (errorMessage != null && errorMessage.equals("BALANCE")){
							showDialog(getResources().getString(R.string.transaction_server_rejected_balance), false);
						} else {
							showDialog(getResources().getString(R.string.transaction_server_rejected), false);
						}
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
				if (paymentRequestInitializer != null)
					paymentRequestInitializer.disableNfc(); 
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
			showLoadingProgressDialog();
			TransactionObject tro = new TransactionObject();
			try {
	            tro.setServerPaymentResponse(serverPaymentRequest.encode());
            } catch (NotSignedException e) {
	            e.printStackTrace();
	            displayResponse(e.getMessage());
				return;
            }
			
			RequestTask<TransactionObject, TransactionObject> transactionRequest = new TransactionRequestTask(new IAsyncTaskCompleteListener<TransactionObject>() {
				public void onTaskComplete(TransactionObject response) {
					if (!response.isSuccessful()) {
						displayResponse(response.getMessage());
						return;
					}
					onTaskCompletTransaction(response.getServerPaymentResponse(), response.getBalanceBTC());
                }
			}, tro, new TransactionObject());
			transactionRequest.execute();
		}
	}
	
	private void onTaskCompletTransaction(byte[] serverPaymentResponseBytes, BigDecimal balance) {
		dismissProgressDialog();
		dismissNfcInProgressDialog();
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.receivePayment_exchangeRate));	
		
		ServerPaymentResponse serverPaymentResponse = null;
		try {
			serverPaymentResponse = DecoderFactory.decode(ServerPaymentResponse.class, serverPaymentResponseBytes);
		} catch (Exception e) {
			displayResponse(getResources().getString(R.string.error_transaction_failed));
			return;
		}
		if(balance != null) {
			ClientController.getStorageHandler().setUserBalance(balance);
		}
		responseListener.onServerResponse(serverPaymentResponse);
		
		//on error?
		//displayResponse(getResources().getString(R.string.no_connection_server));
		//finish();
		//launchActivity(this, MainActivity.class);
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

	//Tablet View, define more or adapt buttons for quickly entering fixed prices as in shops etc. here and in sw720dp\activity_receive_payment 

	private EditText calcDialogDisplay;
	private TextView enterTotal;
	private TextView allClear;
	private TextView seven;
	private TextView eight;
	private TextView nine;
	private TextView four;
	private TextView five;
	private TextView six;
	private TextView multiply;
	private TextView one;
	private TextView two;
	private TextView three;
	private TextView subtract;
	private TextView decimal;
	private TextView zero;
	private TextView equals;
	private TextView addition;

	private TextView list;

	private Button menu_student;
	private Button menu_employee;
	private Button menu_external;
	private Button drink;
	private Button coffee;

	private ArrayList<Float> mathVariables = new ArrayList<Float>();
	private float mathVariable1;
	private float mathVariable2;

	private int currentOperation = 0;
	private int nextOperation;
	private boolean wasEqualsBefore = false;
	private boolean isRpInputMode = false;

	private final static int ADD = 1;
	private final static int SUBTRACT = 2;
	private final static int MULTIPLY = 3;
	private final static int EQUALS = 5;

	private final static int CLEAR = 1;
	private final static int DONT_CLEAR = 0;
	private int clearCalcDisplay = 0;

	private BigDecimal mensaButtonAmount = BigDecimal.ZERO;

	private void initializeCalculator() {

		this.setTitle(getResources().getString(R.string.calcDialog_title));

		calcDialogDisplay = (EditText) findViewById(R.id.calc_dialog_display);
		enterTotal = (TextView) findViewById(R.id.enter_total);
		allClear = (TextView) findViewById(R.id.all_clear);
		seven = (TextView) findViewById(R.id.seven);
		eight = (TextView) findViewById(R.id.eight);
		nine = (TextView) findViewById(R.id.nine);
		four = (TextView) findViewById(R.id.four);
		five = (TextView) findViewById(R.id.five);
		six = (TextView) findViewById(R.id.six);
		multiply = (TextView) findViewById(R.id.multiply);
		one = (TextView) findViewById(R.id.one);
		two = (TextView) findViewById(R.id.two);
		three = (TextView) findViewById(R.id.three);
		subtract = (TextView) findViewById(R.id.subtract);
		decimal = (TextView) findViewById(R.id.decimal);
		zero = (TextView) findViewById(R.id.zero);
		equals = (TextView) findViewById(R.id.equals);
		addition = (TextView) findViewById(R.id.addition);
		list = (TextView) findViewById(R.id.receivePayment_list);


		calcDialogDisplay.setKeyListener(DigitsKeyListener.getInstance(true,
				true));

		initializeMensaButtons();

		if(isSendingMode || !Constants.IS_MENSA_MODE) {
			removeMensaButtons();
		}

		adaptCalculatorToRpMode();

		registerListeners();
	}

	private void adaptCalculatorToRpMode(){
		if(!isPortrait) {
			if(isRpInputMode)
				decimal.setText("00");
			else
				decimal.setText(".");
		}
	}

	private void registerListeners() {

		enterTotal.setText(getResources().getString(R.string.enter_total_tablet));
		enterTotal.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				enableMensaButtons();
				boolean wasEqualsBeforeTmp = wasEqualsBefore;
				if (!calcDialogDisplay.getText().toString().contentEquals("") && !wasEqualsBeforeTmp && !calcDialogDisplay.getText().toString().contentEquals("0.00")) {
					BigDecimal value = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) 
						value = value.divide(new BigDecimal("100"));
					list.append(value.toPlainString() + " \n" + "-----------" + " \n");
				}
				calcLogic(EQUALS);
				String calculatedValues = calcDialogDisplay.getText().toString();
				if (calculatedValues.length() == 0 || calculatedValues.contentEquals("")) {
					calculatedValues = "0.00";
				} 
				BigDecimal displayAmount = new BigDecimal(calculatedValues);

				if (isRpInputMode) {
					displayAmount = displayAmount.divide(new BigDecimal(100));
				}


				calculatedValues = displayAmount.add(mensaButtonAmount).toString();

				if(!wasEqualsBeforeTmp) {
					list.append("= " + calculatedValues + " \n" + "===========" + " \n");
				}

				scrollDown();

				try {
					if (Constants.inputUnit.equals(ReceivePaymentActivity.INPUT_UNIT_CHF)) {
						Constants.inputValueCalculator = CurrencyFormatter.getBigDecimalChf(calculatedValues);
					} else {
						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						String bitcoinUnit = settings.getString("bitcoin_list",	"");
						if (bitcoinUnit.equals(Constants.MILI_BTC)) {
							Constants.inputValueCalculator = new BigDecimal(calculatedValues).setScale(5, RoundingMode.HALF_UP);
						} else if (bitcoinUnit.equals(Constants.MICRO_BTC)) {
							Constants.inputValueCalculator = new BigDecimal(calculatedValues).setScale(2, RoundingMode.HALF_UP);
						} else {
							Constants.inputValueCalculator = CurrencyFormatter.getBigDecimalBtc(calculatedValues);
						}
					}
					initializePayment();
				} catch (Exception e) {
					Constants.inputValueCalculator = BigDecimal.ZERO;
				}
			}
		});

		allClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				enableMensaButtons();
				clearCalculator();
				clearPaymentInfos();
			}
		});

		seven.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");

				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("7");
			}
		});

		eight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");

				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("8");
			}
		});

		nine.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");

				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("9");
			}
		});

		four.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("4");
			}
		});

		five.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("5");
			}
		});

		six.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("6");
			}
		});

		multiply.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				try {
					disableMensaButtons();
					BigDecimal value = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) {
						value = value.divide(new BigDecimal(100));
					}
					list.append(value.toString() + " \n" + " * ");
					calcLogic(MULTIPLY);
				} catch (NumberFormatException e) {
					// do nothing
				}
			}
		});

		one.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("1");
			}
		});

		two.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("2");
			}
		});

		three.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("3");
			}
		});

		subtract.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					resetNfc();
					disableMensaButtons();
					BigDecimal value = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) {
						value = value.divide(new BigDecimal(100));
					}
					list.append(value.toString() + " \n" + " - ");
					calcLogic(SUBTRACT);
					scrollDown();
				} catch (NumberFormatException e) {
					//do nothing
				}
			}
		});

		decimal.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if(!isRpInputMode) {
					if (clearCalcDisplay == CLEAR) {
						calcDialogDisplay.setText("");
					}
					clearCalcDisplay = DONT_CLEAR;
					calcDialogDisplay.append(".");
				} else {
					calcDialogDisplay.append("00");
				}
			}
		});

		zero.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("0");
			}
		});

		equals.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					resetNfc();
					enableMensaButtons();
					BigDecimal value = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) {
						value = value.divide(new BigDecimal(100));
					}
					list.append(value.toString() + " \n" + "-----------" + " \n" + " = ");
					calcLogic(EQUALS);
					BigDecimal value2 = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) {
						value2 = value2.divide(new BigDecimal(100));
					}
					value2 = value2.add(mensaButtonAmount);
					list.append(value2.toString() + " \n" + "===========" + " \n");
					wasEqualsBefore = true;
					scrollDown();
				} catch (NumberFormatException e) {
					// do nothing
				}
			}
		});

		addition.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				try {
					resetNfc();
					BigDecimal value = new BigDecimal(calcDialogDisplay.getText().toString());
					if(isRpInputMode) {
						value = value.divide(new BigDecimal(100));
					}
					list.append(value.toPlainString() + " \n");
					calcLogic(ADD);
					calcDialogDisplay.setText("0.00");
					scrollDown();
				} catch (NumberFormatException e) {
					//do nothing
				}
			}
		});
	}

	/**
	 * Scrolls List TextView down to the bottom when adding text to TextView.
	 */
	private void scrollDown() {
		final ScrollView scroller = (ScrollView) findViewById(R.id.receivePayment_listScrollView);
		scroller.post(new Runnable() { 
			public void run() { 
				scroller.fullScroll(ScrollView.FOCUS_DOWN); 
			} 
		}); 
	}

	private void clearCalculator() {
		mensaButtonAmount = BigDecimal.ZERO;
		list.setText("");
		calcDialogDisplay.setText("");
		mathVariable1 = 0;
		mathVariable2 = 0;
		mathVariables.removeAll(mathVariables);
		currentOperation = 0;
		nextOperation = 0;

	}

	private void calcLogic(int operator) {
		try {
			mathVariables.add(Float.parseFloat(calcDialogDisplay.getText()
					.toString()));
		} catch (NumberFormatException e) {
			// do nothing
		}

		if (operator != EQUALS) {
			nextOperation = operator;
		} else if (operator == EQUALS) {
			nextOperation = 0;
		}

		switch (currentOperation) {
		case ADD:
			if (mathVariables.size() < 2) {
				break;
			}

			mathVariable1 = mathVariables.get(0);
			mathVariable2 = mathVariables.get(1);

			mathVariables.removeAll(mathVariables);

			mathVariables.add(mathVariable1 + mathVariable2);

			calcDialogDisplay.setText(String.valueOf(mathVariables.get(0)));
			break;
		case SUBTRACT:
			if (mathVariables.size() < 2) {
				break;
			}
			mathVariable1 = mathVariables.get(0);
			mathVariable2 = mathVariables.get(1);

			mathVariables.removeAll(mathVariables);

			mathVariables.add(mathVariable1 - mathVariable2);

			calcDialogDisplay.setText(String.valueOf(mathVariables.get(0)));
			break;
		case MULTIPLY:
			if (mathVariables.size() < 2) {
				break;
			}
			mathVariable1 = mathVariables.get(0);
			mathVariable2 = mathVariables.get(1);

			mathVariables.removeAll(mathVariables);

			mathVariables.add(mathVariable1 * mathVariable2);

			calcDialogDisplay.setText(String.valueOf(mathVariables.get(0)));
			break;
		}

		clearCalcDisplay = CLEAR;
		currentOperation = nextOperation;
		if (operator == EQUALS) {
			mathVariable1 = 0;
			mathVariable2 = 0;
			mathVariables.removeAll(mathVariables);
		}
		wasEqualsBefore = false;
	}

	private void addMensaButtonAmount(String amount) {
		try {
			BigDecimal value = new BigDecimal(amount);
			mensaButtonAmount = mensaButtonAmount.add(value);
			list.append(value.toString() + " \n");
			wasEqualsBefore = false;
			scrollDown();
		} catch (NumberFormatException e) {
			//do nothing
		}
	}

	private void initializeMensaButtons() {
		menu_student = (Button) findViewById(R.id.mensa_menu_student);
		menu_student.setVisibility(View.VISIBLE);
		menu_student.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				addMensaButtonAmount("5.40");
			}
		});

		menu_employee = (Button) findViewById(R.id.mensa_menu_employee);
		menu_employee.setVisibility(View.VISIBLE);
		menu_employee.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				addMensaButtonAmount("7.00");
			}
		});

		menu_external = (Button) findViewById(R.id.mensa_menu_external);
		menu_external.setVisibility(View.VISIBLE);
		menu_external.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				addMensaButtonAmount("10.50");
			}
		});

		drink = (Button) findViewById(R.id.mensa_drink);
		drink.setVisibility(View.VISIBLE);
		drink.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				addMensaButtonAmount("2.30");
			}
		});

		coffee = (Button) findViewById(R.id.mensa_coffe);
		coffee.setVisibility(View.VISIBLE);
		coffee.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resetNfc();
				addMensaButtonAmount("1.50");
			}
		});
	}

	private void removeMensaButtons() {
		menu_student.setEnabled(false);
		menu_student.setVisibility(View.INVISIBLE);
		menu_employee.setEnabled(false);
		menu_employee.setVisibility(View.INVISIBLE);
		menu_external.setEnabled(false);
		menu_external.setVisibility(View.INVISIBLE);
		coffee.setEnabled(false);
		coffee.setVisibility(View.INVISIBLE);
		drink.setEnabled(false);
		drink.setVisibility(View.INVISIBLE);
	}

	private void disableMensaButtons() {
		if(Constants.IS_MENSA_MODE){
			menu_student.setEnabled(false);
			menu_employee.setEnabled(false);
			menu_external.setEnabled(false);
			coffee.setEnabled(false);
			drink.setEnabled(false);
		}
	}

	private void enableMensaButtons() {
		if (Constants.IS_MENSA_MODE) {
			menu_student.setEnabled(true);
			menu_employee.setEnabled(true);
			menu_external.setEnabled(true);
			coffee.setEnabled(true);
			drink.setEnabled(true);
		}
	}

	private void resetNfc() {
		if (paymentRequestInitializer != null){
			paymentRequestInitializer.disable();
			paymentRequestInitializer = null;
			hideNfcInstructions();
		}
	}
}
