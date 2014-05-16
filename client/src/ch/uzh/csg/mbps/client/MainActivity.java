package ch.uzh.csg.mbps.client;

import java.math.BigDecimal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.navigation.DrawerItemClickListener;
import ch.uzh.csg.mbps.client.payment.PayPaymentActivity;
import ch.uzh.csg.mbps.client.payment.ReceivePaymentActivity;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.InternalStorageXML;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

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
    private BigDecimal balance;
    private Button receivePaymentBtn;
    private Button payPaymentBtn;
    private Button historyBtn;
    public static BigDecimal exchangeRate;
    private RequestTask getExchangeRate;
    private PopupWindow popupWindow;
    private Boolean isFirstTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setScreenOrientation();
        
        this.balance = ClientController.getUser().getBalance();
        
        initializeDrawer();
        readServerPublicKey();
        
        receivePaymentBtn = (Button) findViewById(R.id.receiveButton);
        payPaymentBtn = (Button) findViewById(R.id.payButton);
        historyBtn = (Button) findViewById(R.id.historyButton);
        
        CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), balance, getApplicationContext());
        initClickListener();
        checkOnlineModeAndProceed();
                
    }

	@Override
    public void onResume(){
    	super.onResume();
    	CurrencyViewHandler.setBTC((TextView) findViewById(R.id.mainActivityTextViewBTCs), balance, getApplicationContext());
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
    
    private void readServerPublicKey() {
		try {
			String serverPK = InternalStorageXML.readPublicKeyFromFile(getApplicationContext());
			ClientController.setServerPublicKey(serverPK);
		} catch (Exception e) {
			displayResponse(getResources().getString(R.string.unexcepted_error));
		}
	}
    
    private void initClickListener() {

		receivePaymentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleAsyncTask();
				launchActivity(MainActivity.this, ReceivePaymentActivity.class);
			}
		});

		payPaymentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleAsyncTask();
				launchActivity(MainActivity.this, PayPaymentActivity.class);
			}
		});

		historyBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleAsyncTask();
				launchActivity(MainActivity.this, HistoryActivity.class);
			}
		});
		
	}

    private void handleAsyncTask(){
    	if(ClientController.isOnline() && !getExchangeRate.equals(AsyncTask.Status.FINISHED)){
    		getExchangeRate.cancel(true);
    	}
    }
    
	private void checkOnlineModeAndProceed() {
		CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.mainActivity_balanceCHF));
		if (ClientController.isOnline()) {
			launchRequest();
		} else {
			receivePaymentBtn.setEnabled(false);
			showPopupWindow();
		}
	}
    
    private void launchRequest() {
    	getExchangeRate = new ExchangeRateRequestTask(this);
    	getExchangeRate.execute();
	}
    
    public void onTaskComplete(CustomResponseObject response) {
		if (response.isSuccessful()) {
			exchangeRate = new BigDecimal(response.getMessage());
			CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.mainActivity_balanceCHF), exchangeRate, balance);
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		}
		showPopupWindow();
	}
    
	private void showPopupWindow(){	

		if(isFirstTime == null){
			SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
			boolean defaultValue = true;
			isFirstTime = sharedPref.getBoolean(getString(R.string.sharedPreferences_isFirstTime), defaultValue);
		}
		
		if (isFirstTime){
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup group = (ViewGroup) findViewById(R.id.nfc_instruction_popup);
			View layout = inflater.inflate(R.layout.activity_popup_nfc_instructions, group);
			popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
			popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
		
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
