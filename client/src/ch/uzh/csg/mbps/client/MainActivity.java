package ch.uzh.csg.mbps.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
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
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.InternalStorageXML;
import ch.uzh.csg.mbps.model.AbstractHistory;
import ch.uzh.csg.mbps.model.HistoryPayInTransaction;
import ch.uzh.csg.mbps.model.HistoryPayOutTransaction;
import ch.uzh.csg.mbps.model.HistoryTransaction;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.GetHistoryTransferObject;

/**
 * This class shows the main view of the user with the balance of the user's
 * account. The navigation to different views are handled from this class.
 */
public class MainActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{

	private String[] mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private MenuItem menuWarning;
	private Button createNewTransactionBtn;
	public static BigDecimal exchangeRate;
	private RequestTask getMainActivityValues;
	private PopupWindow popupWindow;
	public static Boolean isFirstTime;
	AnimationDrawable nfcActivityAnimation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setScreenOrientation();

		initializeDrawer();
		readServerPublicKey();
		initializeGui();

		initClickListener();
		checkOnlineModeAndProceed();
	}


	@Override
	public void onResume(){
		super.onResume();
		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getUser().getBalance(), getApplicationContext());
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuWarning = menu.findItem(R.id.action_warning);
		menuWarning.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			//TODO simon: put to superclass?
			public boolean onMenuItemClick(MenuItem item) {
				displayResponse(getResources().getString(R.string.optionMenu_offlineModeHint));
				return false;
			}
		});
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public void invalidateOptionsMenu() {
		if(menuWarning != null){
			if(ClientController.isOnline()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
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

		CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), ClientController.getUser().getBalance(), getApplicationContext());
	}

	private void readServerPublicKey() {
		try {
			String serverPK = InternalStorageXML.readPublicKeyFromFile(getApplicationContext());
			ClientController.setServerPublicKey(serverPK);
		} catch (Exception e) {
			displayResponse(getResources().getString(R.string.unexcepted_error));
		}
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
		}
		showFirstTimeInformation();
	}

	private void launchRequest() {
		showLoadingProgressDialog();
		getMainActivityValues = new MainActivityRequestTask(this);
		getMainActivityValues.execute();
	}

	public void onTaskComplete(CustomResponseObject response) {
		dismissProgressDialog();
		TextView lastTransactionsTitle = (TextView) findViewById(R.id.mainActivity_lastTransactionsTitle);
		if (response.isSuccessful()) {
			exchangeRate = new BigDecimal(response.getMessage());
			ClientController.getUser().setBalance(response.getReadAccountTO().getUserAccount().getBalance());
			ArrayList<AbstractHistory> transactions = extractLast5Transactions(response.getGetHistoryTO());

			//update gui
			if(! transactions.isEmpty()){
				lastTransactionsTitle.setVisibility(View.VISIBLE);
				createHistoryViews(transactions);
			}
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.mainActivity_balanceCHF), exchangeRate, ClientController.getUser().getBalance());
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			lastTransactionsTitle.setVisibility(View.INVISIBLE);
			invalidateOptionsMenu();
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
	private ArrayList<AbstractHistory> extractLast5Transactions(GetHistoryTransferObject hto) {
		ArrayList<HistoryTransaction> transactionHistory = hto.getTransactionHistory();
		ArrayList<HistoryPayInTransaction> payInTransactionHistory = hto.getPayInTransactionHistory();
		ArrayList<HistoryPayOutTransaction> payOutTransactionHistory = hto.getPayOutTransactionHistory();

		ArrayList<AbstractHistory> history = new ArrayList<AbstractHistory>();
		history.addAll(transactionHistory);
		history.addAll(payInTransactionHistory);
		history.addAll(payOutTransactionHistory);
		Collections.sort(history, new CustomComparator());

		return history;
	}

	private class CustomComparator implements Comparator<AbstractHistory> {
		public int compare(AbstractHistory o1, AbstractHistory o2) {
			return o1.getTimestamp().compareTo(o2.getTimestamp());
		}
	}

	private void createHistoryViews(ArrayList<AbstractHistory> history) {
		LinearLayout linearLayout = (LinearLayout)findViewById(R.id.mainActivity_history);
		linearLayout.removeAllViews();

		for(int i = history.size()-1;i>=history.size()-5;i--){
			TextView tView = new TextView(getApplicationContext());
			tView.setGravity(Gravity.LEFT);
			tView.setTextColor(Color.BLACK);
			int drawable = getImage(history.get(i));
			final int historyFilterValue = getHistoryCode(history.get(i));
			tView.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
			tView.setText(history.get(i).toString());
			tView.setClickable(true);
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

	private int getImage(AbstractHistory history) {
		if(history instanceof HistoryTransaction){
			if(((HistoryTransaction) history).getSeller().equals(ClientController.getUser().getUsername())){
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
			if(((HistoryTransaction) history).getSeller().equals(ClientController.getUser().getUsername())){
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

}
