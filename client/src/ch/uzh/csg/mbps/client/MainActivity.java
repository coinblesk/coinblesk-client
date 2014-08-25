package ch.uzh.csg.mbps.client;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.navigation.DrawerItemClickListener;
import ch.uzh.csg.mbps.client.payment.AbstractPaymentActivity;
import ch.uzh.csg.mbps.client.request.MainActivityRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.security.KeyHandler;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.client.util.HistoryTransactionFormatter;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.PaymentResponse;
import ch.uzh.csg.mbps.model.AbstractHistory;
import ch.uzh.csg.mbps.model.HistoryPayInTransaction;
import ch.uzh.csg.mbps.model.HistoryPayOutTransaction;
import ch.uzh.csg.mbps.model.HistoryTransaction;
import ch.uzh.csg.mbps.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.mbps.responseobject.MainRequestObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;
import ch.uzh.csg.mbps.util.Converter;
import ch.uzh.csg.paymentlib.IPaymentEventHandler;
import ch.uzh.csg.paymentlib.IServerResponseListener;
import ch.uzh.csg.paymentlib.IUserPromptAnswer;
import ch.uzh.csg.paymentlib.IUserPromptPaymentRequest;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.PaymentRequestHandler;
import ch.uzh.csg.paymentlib.container.ServerInfos;
import ch.uzh.csg.paymentlib.container.UserInfos;
import ch.uzh.csg.paymentlib.messages.PaymentError;

/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends AbstractPaymentActivity {
	private String[] mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private Button createNewTransactionBtn;
	private RequestTask<TransferObject, MainRequestObject> getMainActivityValues;
	private PopupWindow popupWindow;
	public static Boolean isFirstTime;
	AnimationDrawable nfcActivityAnimation;
	
	private NfcAdapter nfcAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setScreenOrientation();

		initializeDrawer();
		initializeGui();

		initClickListener();

		initializeNFC();
	}

	@Override
	public void onResume(){
		super.onResume();
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalanceBTC(), getApplicationContext());
		checkOnlineModeAndProceed();
		invalidateOptionsMenu();

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	/**
	 * Initializes the navigation drawer.
	 * 
	 * @see http://developer.android.com/training/implementing-navigation/nav-drawer.html
	 */
	private void initializeDrawer() {
		mDrawerItems = getResources().getStringArray(R.array.drawerItems_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_activity_main);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerItems));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mTitle = mDrawerTitle = getTitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_activity_main);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}

	private void initializeGui() {
		// display red action bar when running on testserver
		if(Constants.BASE_URI.contains("clone")) {
			ActionBar bar = getActionBar();
			bar.setBackgroundDrawable(new ColorDrawable(Color.RED));
		}
		createNewTransactionBtn = (Button) findViewById(R.id.createNewTransactionButton);

		//create animated nfc activity image
		ImageView nfcActivity = (ImageView) findViewById(R.id.mainActivity_nfcIcon);
		nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
		nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
		nfcActivityAnimation.start();

		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalanceBTC(), getApplicationContext());
	}

	private void initClickListener() {
		createNewTransactionBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleAsyncTask();
				launchActivity(MainActivity.this, ChoosePaymentActivity.class);
			}
		});
	}

	private void handleAsyncTask(){
		if(ClientController.isOnline() && !getMainActivityValues.equals(AsyncTask.Status.FINISHED)){
			getMainActivityValues.cancel(true);
		}
	}

	private void checkOnlineModeAndProceed() {
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.mainActivity_balanceCHF));
		if (ClientController.isOnline()) {
			launchRequest();
		} else {
			createNewTransactionBtn.setEnabled(false);
			createNewTransactionBtn.setTextColor(Color.LTGRAY);
		}
		showFirstTimeInformation();
	}

	private void launchRequest() {
		showLoadingProgressDialog();
		getMainActivityValues = new MainActivityRequestTask(new IAsyncTaskCompleteListener<MainRequestObject>() {
			@Override
			public void onTaskComplete(MainRequestObject response) {
				TextView lastTransactionsTitle = (TextView) findViewById(R.id.mainActivity_lastTransactionsTitle);
				String s = String.format(getResources().getString(R.string.lastFewTransactionsTitle), getNumberOfLastTransactions());
				lastTransactionsTitle.setText(s);
				if(response.isSuccessful()) {
					dismissProgressDialog();
					exchangeRate = response.getExchangeRate();
					ArrayList<AbstractHistory> transactions = extractLastFewTransactions(response.getGetHistoryTransferObject());
					ClientController.getStorageHandler().setUserBalance(response.getBalanceBTC());
					//update gui
					if(! transactions.isEmpty()){
						lastTransactionsTitle.setVisibility(View.VISIBLE);
						createHistoryViews(transactions);
					}
					CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalanceBTC(), getApplicationContext());
					CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.mainActivity_balanceCHF), exchangeRate, ClientController.getStorageHandler().getUserAccount().getBalanceBTC());
					TextView balanceTv = (TextView) findViewById(R.id.mainActivity_balanceCHF);
					balanceTv.append(" (1 BTC = " + CurrencyFormatter.formatChf(exchangeRate) + " CHF)");
					//renew Session Timeout Countdown
					if(ClientController.isOnline()){
						startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
					}
				} else if (response.getMessage() != null && (response.getMessage().equals(Constants.CONNECTION_ERROR) || response.getMessage().equals(Constants.REST_CLIENT_ERROR))) {
					dismissProgressDialog();
					launchOfflineMode(getApplicationContext());
					invalidateOptionsMenu();
					displayResponse(getResources().getString(R.string.no_connection_server));
					lastTransactionsTitle.setVisibility(View.INVISIBLE);
				} 
				
			}
		}, new TransferObject(), new MainRequestObject());
		getMainActivityValues.execute();
	}

	/**
	 * Sort HistoryTransactions received from server according to timestamp and
	 * return.
	 * 
	 * @param GetHistoryTransferObject
	 *            hto which includes {@link HistoryTransaction}s,
	 *            {@link HistoryPayInTransaction}s and
	 *            {@link HistoryPayOutTransaction}s
	 * @return ArrayList<AbstractHistory>
	 */
	private ArrayList<AbstractHistory> extractLastFewTransactions(GetHistoryTransferObject hto) {
		List<HistoryTransaction> transactionHistory = hto.getTransactionHistory();
		List<HistoryPayInTransaction> payInTransactionHistory = hto.getPayInTransactionHistory();
		List<HistoryPayOutTransaction> payOutTransactionHistory = hto.getPayOutTransactionHistory();

		ArrayList<AbstractHistory> history = new ArrayList<AbstractHistory>();
		history.addAll(transactionHistory);
		history.addAll(payInTransactionHistory);
		history.addAll(payOutTransactionHistory);
		Collections.sort(history, Collections.reverseOrder(new CustomComparator()));

		return history;
	}

	private class CustomComparator implements Comparator<AbstractHistory> {
		public int compare(AbstractHistory o1, AbstractHistory o2) {
			return o1.getTimestamp().compareTo(o2.getTimestamp());
		}
	}

	private int getNumberOfLastTransactions(){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String value =  sharedPref.getString("numberOfLastTransactions", "3");
		return Integer.parseInt(value);
	}

	private void createHistoryViews(ArrayList<AbstractHistory> history) {
		LinearLayout linearLayout = (LinearLayout)findViewById(R.id.mainActivity_history);
		linearLayout.removeAllViews();
		for(int i = 0; i < getNumberOfLastTransactions(); i++){
			if(i<history.size()){
				TextView tView = new TextView(getApplicationContext());
				tView.setGravity(Gravity.LEFT);
				tView.setTextColor(Color.BLACK);
				int drawable = getImage(history.get(i));
				final int historyFilterValue = getHistoryCode(history.get(i));
				tView.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
				tView.setText(HistoryTransactionFormatter.formatHistoryTransaction(history.get(i), getApplicationContext()));
				tView.setClickable(true);
				if(i % 2 == 0){
					tView.setBackgroundColor(Color.LTGRAY);
				}
				tView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						handleAsyncTask();
						Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
						Bundle b = new Bundle();
						b.putInt("filter", historyFilterValue);
						intent.putExtras(b);
						startActivity(intent);
					}
				});
				linearLayout.addView(tView);
			}
		}
	}

	private int getImage(AbstractHistory history) {
		if(history instanceof HistoryTransaction){
			if(((HistoryTransaction) history).getSeller().equals(ClientController.getStorageHandler().getUserAccount().getUsername())){
				return R.drawable.ic_receive_payment;
			}else{
				return R.drawable.ic_pay_payment;
			}
		}else if(history instanceof HistoryPayInTransaction){
			return R.drawable.ic_pay_in;
		}else if(history instanceof HistoryPayOutTransaction){
			return R.drawable.ic_pay_out;
		}
		return 0;
	}

	private int getHistoryCode(AbstractHistory history) {
		if(history instanceof HistoryTransaction){
			if(((HistoryTransaction) history).getSeller().equals(ClientController.getStorageHandler().getUserAccount().getUsername())){
				return 0;
			}else{
				return 0;
			}
		}else if(history instanceof HistoryPayInTransaction){
			return 1;
		}else if(history instanceof HistoryPayOutTransaction){
			return 2;
		}
		return 0;
	}

	/**
	 * Show first time information dialog if application is started for the
	 * first time to inform user about NFC handling.
	 */
	private void showFirstTimeInformation(){	
		if(isFirstTime == null){
			SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
			boolean defaultValue = true;
			isFirstTime = sharedPref.getBoolean(getString(R.string.sharedPreferences_isFirstTime), defaultValue);
		}

		if (isFirstTime){
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup group = (ViewGroup) findViewById(R.id.nfc_instruction_popup);
			final View layout = inflater.inflate(R.layout.activity_popup_nfc_instructions, group);
			popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

			layout.post(new Runnable() {
				public void run() {
					popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
				}
			});

			final Button closeBtn = (Button) layout.findViewById(R.id.nfc_instruction_close_button);
			closeBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					popupWindow.dismiss();
				}
			});

			isFirstTime = false;

			//write to shared preferences
			SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean(getString(R.string.sharedPreferences_isFirstTime), false);
			editor.commit();
		}
	}

	/**
	 * Initializes NFC adapter and user payment information.
	 */
	private void initializeNFC() {
		nfcAdapter = createAdapter(MainActivity.this);
		if (nfcAdapter == null) {
			return;
		}
		
		//disable android beam (touch to beam screen)
		nfcAdapter.setNdefPushMessage(null, this, this);
		
		try {
			PublicKey publicKeyServer = KeyHandler.decodePublicKey(ClientController.getStorageHandler().getServerPublicKey().getPublicKey());
			final ServerInfos serverInfos = new ServerInfos(publicKeyServer);
			PrivateKey privateKey = KeyHandler.decodePrivateKey(ClientController.getStorageHandler().getKeyPair().getPrivateKey());
			final UserInfos userInfos = new UserInfos(ClientController.getStorageHandler().getUserAccount().getUsername(), privateKey, PKIAlgorithm.DEFAULT, ClientController.getStorageHandler().getKeyPair().getKeyNumber());
			new PaymentRequestHandler(this, eventHandler, userInfos, serverInfos, userPrompt, persistencyHandler);
		} catch (Exception e) {
			displayResponse(getResources().getString(R.string.error_nfc_initializing));
		}
	}

	/**
	 * Handler responsible for managing events received by NFC Payment Library.
	 */
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
						showDialog(getResources().getString(R.string.transaction_rejected_payer), false);
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
						dismissNfcInProgressDialog();
						showDialog(getResources().getString(R.string.error_transaction_failed), false);
						break;
					case INIT_FAILED:
						//ignore
						break;
					default:
						break;
					}
				}
				resetStates();
				break;
			case FORWARD_TO_SERVER:
				break;
			case SUCCESS:
				showSuccessDialog(object, paymentAccepted);
				resetStates();
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
	 * Prompts user if an incoming payment request shall be accepted or not.
	 */
	private IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

		public boolean isPaymentAccepted() {
			return paymentAccepted;
		}

		public void promptUserPaymentRequest(String username, Currency currency, long amount, IUserPromptAnswer answer) {
			if(checkAutoAccept(username, amount)) {
				paymentAccepted = true;
				answer.acceptPayment();
				showNfcProgressDialog(false);
			}
			else {
				showCustomDialog(username, currency, amount, answer);
			}
		}
	};

	/**
	 * Checks if auto accepting of payments are enabled for given username and amount.
	 * 
	 * @param username
	 * @param amount
	 * @return boolean if transaction shall be autoaccepted
	 */
	private boolean checkAutoAccept(String username, long amount){
		if (exchangeRate != null){
			BigDecimal amountChf = CurrencyViewHandler.getAmountInCHF(exchangeRate, Converter.getBigDecimalFromLong(amount));
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			boolean isAutoAcceptEnabled = sharedPref.getBoolean("auto_accept", false);
			if(isAutoAcceptEnabled && ClientController.getStorageHandler().isTrustedContact(username)){
				String value =  sharedPref.getString("auto_accept_amount", "0");
				int limit = Integer.parseInt(value);
				if (amountChf.compareTo(new BigDecimal(limit)) <= 0)
					return true;
			}
			return false;
		}
		else {
			return false;
		}
	}

	/**
	 * Opens dialog to prompt user if he wants to accept or reject an incoming payment request.
	 * 
	 * @param username
	 * @param currency
	 * @param amount
	 * @param answer2
	 */
	private void showCustomDialog(String username, Currency currency, long amount, final IUserPromptAnswer answer2) {
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup group = (ViewGroup) findViewById(R.id.pay_payment_popup);
		final View layout = inflater.inflate(R.layout.pay_payment_popup, group);
		popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

		final TextView receiverTv = (TextView) layout.findViewById(R.id.payPayment_username);
		receiverTv.setText(username);
		final TextView amountTv = (TextView) layout.findViewById(R.id.payPayment_amountBTC);
		amountTv.setText(CurrencyViewHandler.formatBTCAsString(Converter.getBigDecimalFromLong(amount), getApplicationContext()));

		if(exchangeRate != null){
			final TextView amountChfTv = (TextView) layout.findViewById(R.id.payPayment_amountCHF);
			amountChfTv.setText(CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, Converter.getBigDecimalFromLong(amount)));
			final TextView exchangeRateTv = (TextView) layout.findViewById(R.id.payPayment_exchangeRateValue);
			CurrencyViewHandler.setExchangeRateView(exchangeRate, exchangeRateTv);
			final TextView balanceTvChf = (TextView) layout.findViewById(R.id.payPayment_balanceCHF);
			balanceTvChf.setText(CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, ClientController.getStorageHandler().getUserAccount().getBalanceBTC()));
		}
		final TextView balanceTvBtc = (TextView) layout.findViewById(R.id.payPayment_balanceBTC);
		balanceTvBtc.setText(CurrencyViewHandler.formatBTCAsString(ClientController.getStorageHandler().getUserAccount().getBalanceBTC(), getApplicationContext()));

		layout.post(new Runnable() {
			public void run() {
				dismissNfcInProgressDialog();
				popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
			}
		});

		final Button rejectButton = (Button) layout.findViewById(R.id.payPayment_reject);
		rejectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				paymentAccepted = false;
				showNfcProgressDialog(false);
				answer2.rejectPayment();
				popupWindow.dismiss();
			}
		});

		final Button acceptButton = (Button) layout.findViewById(R.id.payPayment_accept);
		acceptButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				paymentAccepted = true;
				answer2.acceptPayment();
				popupWindow.dismiss();
				showNfcProgressDialog(false);
			}
		});
	}

	protected void refreshActivity() {
		this.recreate();
	}

	/**
	 * Shows a dialog indicating if transaction was successful or not.
	 * @param object (object with {@link PaymentResponse})
	 * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
	 */
	private void showSuccessDialog(Object object, boolean isSending) {
		dismissNfcInProgressDialog();
		String answer;
		if (object == null || !(object instanceof PaymentResponse)) {
			answer = getResources().getString(R.string.error_transaction_failed);
			showDialog(answer, false);
		} else {
			PaymentResponse pr = (PaymentResponse) object;
			BigDecimal amountBtc = Converter.getBigDecimalFromLong(pr.getAmount());
			BigDecimal balance = ClientController.getStorageHandler().getUserAccount().getBalanceBTC();
			String chfValue = "";
			if(exchangeRate != null) {
				chfValue = " (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")";
			}

			if(isSending){
				ClientController.getStorageHandler().getUserAccount().setBalanceBTC(balance.subtract(amountBtc));
				ClientController.getStorageHandler().addAddressBookEntry(pr.getUsernamePayee());
				answer = String.format(getResources().getString(R.string.payment_notification_success_payer),
						CurrencyViewHandler.formatBTCAsString(amountBtc, this) + chfValue,
						pr.getUsernamePayee());
			}
			else {
				ClientController.getStorageHandler().getUserAccount().setBalanceBTC(balance.add(amountBtc));
				ClientController.getStorageHandler().addAddressBookEntry(pr.getUsernamePayer());
				answer = String.format(getResources().getString(R.string.payment_notification_success_payee),
						CurrencyViewHandler.formatBTCAsString(amountBtc, this) + chfValue,
						pr.getUsernamePayer());
			}
			showDialog(answer, true);
		}
	}

}
