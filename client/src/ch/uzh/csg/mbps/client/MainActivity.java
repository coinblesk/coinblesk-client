package ch.uzh.csg.mbps.client;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
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
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;
import ch.uzh.csg.mbps.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.paymentlib.IPaymentEventHandler;
import ch.uzh.csg.paymentlib.IServerResponseListener;
import ch.uzh.csg.paymentlib.IUserPromptAnswer;
import ch.uzh.csg.paymentlib.IUserPromptPaymentRequest;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.PaymentRequestHandler;
import ch.uzh.csg.paymentlib.container.ServerInfos;
import ch.uzh.csg.paymentlib.container.UserInfos;
import ch.uzh.csg.paymentlib.messages.PaymentError;
import ch.uzh.csg.paymentlib.persistency.IPersistencyHandler;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends AbstractLoginActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{

	private String[] mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	
	private Button createNewTransactionBtn;
	public static BigDecimal exchangeRate;
	private RequestTask getMainActivityValues;
	private PopupWindow popupWindow;
	public static Boolean isFirstTime;
	AnimationDrawable nfcActivityAnimation;
	
	private boolean paymentAccepted = false;
	private AlertDialog userPromptDialog;
	private static final String TAG = "##NFC## MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setScreenOrientation();

		initializeDrawer();
		initializeGui();

		initClickListener();
		
		//TODO simon: handle exceptions
		try {
			initializeNFC();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalance(), getApplicationContext());
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
		createNewTransactionBtn = (Button) findViewById(R.id.createNewTransactionButton);

		//create animated nfc activity image
		ImageView nfcActivity = (ImageView) findViewById(R.id.mainActivity_nfcIcon);
		nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
		nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
		nfcActivityAnimation.start();

		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalance(), getApplicationContext());
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
		getMainActivityValues = new MainActivityRequestTask(this);
		getMainActivityValues.execute();
	}

	public void onTaskComplete(CustomResponseObject response) {
		TextView lastTransactionsTitle = (TextView) findViewById(R.id.mainActivity_lastTransactionsTitle);
		String s = String.format(getResources().getString(R.string.lastFewTransactionsTitle), getNumberOfLastTransactions());
		lastTransactionsTitle.setText(s);
		if(response.getType() == Type.MAIN_ACTIVITY && response.isSuccessful()) {
			dismissProgressDialog();
			exchangeRate = new BigDecimal(response.getMessage());
			ArrayList<AbstractHistory> transactions = extractLastFewTransactions(response.getGetHistoryTO());
			ClientController.getStorageHandler().setUserBalance(new BigDecimal(response.getBalance()));
			//update gui
			if(! transactions.isEmpty()){
				lastTransactionsTitle.setVisibility(View.VISIBLE);
				createHistoryViews(transactions);
			}
			CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getStorageHandler().getUserAccount().getBalance(), getApplicationContext());
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.mainActivity_balanceCHF), exchangeRate, ClientController.getStorageHandler().getUserAccount().getBalance());
			TextView balanceTv = (TextView) findViewById(R.id.mainActivity_balanceCHF);
			balanceTv.append(" (1 BTC = " + CurrencyFormatter.formatChf(exchangeRate) + " CHF)");
			//renew Session Timeout Countdown
			if(ClientController.isOnline()){
				startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
			}
		} else if (response.getMessage() != null && (response.getMessage().equals(Constants.CONNECTION_ERROR) || response.getMessage().equals(Constants.REST_CLIENT_ERROR))) {
			dismissProgressDialog();
			reload(getIntent());
			lastTransactionsTitle.setVisibility(View.INVISIBLE);
			invalidateOptionsMenu();
			displayResponse(response.getMessage());
		} else {
			super.onTaskComplete(response, getApplicationContext());
		}
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
		ArrayList<HistoryTransaction> transactionHistory = hto.getTransactionHistory();
		ArrayList<HistoryPayInTransaction> payInTransactionHistory = hto.getPayInTransactionHistory();
		ArrayList<HistoryPayOutTransaction> payOutTransactionHistory = hto.getPayOutTransactionHistory();

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
	
	
	//TODO simon: refactor NFC stuff
	private void initializeNFC() throws Exception{
		final NfcAdapter adapter = createAdapter(MainActivity.this);
		
		PublicKey publicKeyServer = KeyHandler.decodePublicKey(ClientController.getStorageHandler().getServerPublicKey((byte) 1).getPublicKey());
		final ServerInfos serverInfos = new ServerInfos(publicKeyServer);
		PrivateKey privateKey = ch.uzh.csg.mbps.client.security.KeyHandler.decodePrivateKey(ClientController.getStorageHandler().getKeyPair().getPrivateKey());
		final UserInfos userInfos = new UserInfos(ClientController.getStorageHandler().getUserAccount().getUsername(), privateKey, PKIAlgorithm.DEFAULT, ClientController.getStorageHandler().getKeyPair().getKeyNumber());
		new PaymentRequestHandler(this, eventHandler, userInfos, serverInfos, userPrompt, persistencyHandler);
	}
	
	private IPaymentEventHandler eventHandler = new IPaymentEventHandler() {
		public void handleMessage(PaymentEvent event, Object object, IServerResponseListener caller) {
			Log.i(TAG, "evt2:" + event + " obj:" + object);

			if (userPromptDialog != null && userPromptDialog.isShowing()) {
				userPromptDialog.dismiss();
			}
			
			switch (event) {
			case ERROR:
				if (object == PaymentError.PAYER_REFUSED) {
				}
				if (object == PaymentError.NO_SERVER_RESPONSE) {
					//TODO simon: display message
				}
				break;
			case FORWARD_TO_SERVER:
				break;
			case SUCCESS:
				showSuccessDialog(object);
				break;
			case INITIALIZED:
				break;
			default:
				break;
			}
			resetStates();
		}
	};
	
	//TODO: simon: what for?
	private IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

//		@Override
		public boolean isPaymentAccepted() {
			Log.i(TAG, "payment accepted: "+paymentAccepted);
			return paymentAccepted;
		}

//		@Override
        public void promptUserPaymentRequest(String username, Currency currency, long amount, IUserPromptAnswer answer) {
			Log.i(TAG, "user " + username + " wants " + amount);
			showCustomDialog(username, currency, amount, answer);
        }
		
	};
	
	//TODO simon: adapt
	private void showSuccessDialog(Object object) {
		String msg;
		if (object == null) {
			msg = "object is null";
		} else if (!(object instanceof PaymentResponse)) {
			msg = "object is not instance of PaymentResponse";
		} else {
			PaymentResponse pr = (PaymentResponse) object;
			msg = "payed "+pr.getAmount() +" "+pr.getCurrency().getCurrencyCode()+" to "+pr.getUsernamePayee();
		}
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Payment Success!")
			.setMessage(msg)
			.setCancelable(true)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		
		runOnUiThread(new Runnable() {
		    public void run() {
		    	AlertDialog alert = builder.create();
				alert.show();
		    }
		});
		
		resetStates();
	}
	
	private void resetStates() {
		paymentAccepted = false;
	}
	
	//TODO simon: change to popup with receivepayment design
	private void showCustomDialog(String username, Currency currency, long amount, final IUserPromptAnswer answer2) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Incoming Payment Request")
			.setMessage("Do you want to pay "+amount+" "+currency.getCurrencyCode()+" to "+username+"?")
			.setCancelable(false)
			.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   paymentAccepted = true;
		               answer2.acceptPayment();
		           }
		       })
		     .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   paymentAccepted = false;
		               answer2.rejectPayment();
		               refreshActivity();
		           }
		       });
		
		runOnUiThread(new Runnable() {
		    public void run() {
		    	userPromptDialog = builder.create();
				userPromptDialog.show();
		    }
		});
    }
	
	//TODO jeton: add to xml
	private IPersistencyHandler persistencyHandler = new IPersistencyHandler() {

//		@Override
		public PersistedPaymentRequest getPersistedPaymentRequest(String username, Currency currency, long amount) {
			Log.i(TAG, "getPersistedPaymentRequest");
			return null;
		}

//		@Override
		public void delete(PersistedPaymentRequest paymentRequest) {
			Log.i(TAG, "delete");
		}

//		@Override
		public void add(PersistedPaymentRequest paymentRequest) {
			Log.i(TAG, "add");
		}
		
	};
	/**
	 * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
	 * null and open up NFC settings.
	 * 
	 * @param context
	 * @return
	 */
	private NfcAdapter createAdapter(Context context) {
		NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());
		return nfcAdapter;
	}
	
	private void refreshActivity() {
		this.recreate();
	}

}
