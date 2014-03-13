package ch.uzh.csg.mbps.client.payment;

import java.math.BigDecimal;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.model.PayOutTransaction;
import ch.uzh.csg.mbps.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.mbps.client.request.PayOutRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class is a view to send bitcoins from the system to the inserted
 * bitcoin-address.
 */
public class PayOutActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	public static BigDecimal exchangeRate;
	private BigDecimal payOutAmount;
	private MenuItem menuWarning;
	private PopupWindow popupWindow;
	private Button acceptBtn;
	private EditText payoutAmount;
	private EditText payoutAddress;
	
	private TextView feeAGB;
	private TextView btcBalance;
	private TextView chfBalance;
	private TextView chfAmount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pay_out);
		setScreenOrientation();
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		btcBalance = (TextView) findViewById(R.id.payOut_Balance);
		CurrencyViewHandler.setBTC(btcBalance, ClientController.getUser().getBalance(), getApplicationContext());
		chfBalance = (TextView) findViewById(R.id.payOut_BalanceCHF);
		CurrencyViewHandler.clearTextView(chfBalance);
		
		chfAmount = (TextView) findViewById(R.id.payOut_AmountCHF);
		
		acceptBtn = (Button) findViewById(R.id.payOut_payOutButton);
		payoutAmount = (EditText) findViewById(R.id.payOut_Amount);
		payoutAddress = (EditText) findViewById(R.id.payOut_Address);
		feeAGB = (TextView) findViewById(R.id.payOut_info);
		
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
		getMenuInflater().inflate(R.menu.pay_out, menu);
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
		if (menuWarning != null) {
			if (ClientController.isOnline()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
	}
    
    private void initClickListener() {

		acceptBtn.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
				if (ClientController.isOnline()) {
					launchPayOutRequest();
				}
	  		}
	  	});  

	  	payoutAmount.addTextChangedListener(new TextWatcher() {
	  		/*
	  		 * Listener to update the inserted amount of bitcoin
	  		 * and the exchange rate to swiss currency. 
	  		 */
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				try {
					BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(payoutAmount.getText().toString());
					payOutAmount = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
					
					CurrencyViewHandler.setToCHF(chfAmount, exchangeRate, payOutAmount);
				} catch (NumberFormatException e) {
					CurrencyViewHandler.setToCHF(chfAmount, BigDecimal.ZERO, payOutAmount);
				}
			}

			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {	
			}
		});
	  	
	  	feeAGB.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showPopupWindow();
			}
		});
	}
    
	private void launchPayOutRequest() {
		if (!payoutAmount.getText().toString().isEmpty() && !payoutAddress.getText().toString().isEmpty()) {
			showLoadingProgressDialog();
			BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(payoutAmount.getText().toString());
			payOutAmount = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
			
			PayOutTransaction pot = new PayOutTransaction(ClientController.getUser().getId(), payOutAmount, payoutAddress.getText().toString());
			RequestTask payOut = new PayOutRequestTask(this, pot);
			payOut.execute();
		} else {
			displayResponse(getResources().getString(R.string.fill_necessary_fields));
		}
	}
    
	private void checkOnlineModeAndProceed() {
		if (ClientController.isOnline()) {
			launchExchangeRateRequest();
		} else {
			acceptBtn.setEnabled(false);
		}
	}
	
	private void launchExchangeRateRequest(){
		showLoadingProgressDialog();
		RequestTask getExchangeRate = new ExchangeRateRequestTask(this);
		getExchangeRate.execute();
	}
	
	public void onTaskComplete(CustomResponseObject response) {
		if (response.isSuccessful()) {
			if (response.getType() == Type.EXCHANGE_RATE) {
				exchangeRate = new BigDecimal(response.getMessage());
				CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.payout_exchangeRate));
				CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, ClientController.getUser().getBalance());
			} else {
				showDialog("Pay out", getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()),response.getMessage());
				ClientController.setUserBalance(ClientController.getUser().getBalance().subtract(payOutAmount), getApplicationContext());
				CurrencyViewHandler.setBTC(btcBalance, ClientController.getUser().getBalance(), getApplicationContext());
				CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, ClientController.getUser().getBalance());
				chfAmount.setText("");
				payoutAmount.setText("");
			}
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			exchangeRate = BigDecimal.ZERO;
			reload(getIntent());
			invalidateOptionsMenu();
		} else {
			exchangeRate = BigDecimal.ZERO;
			displayResponse(response.getMessage());
			chfBalance.setText("");
		}
		dismissProgressDialog();
	}
	
	/**
	 * Popup window to inform the user about the fee. 
	 */
	private void showPopupWindow(){	
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup group= (ViewGroup) findViewById(R.id.feeAGBPayOut);
		View layout = inflater.inflate(R.layout.activity_fee_popup_window_layout, group);
		popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
		popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
		
		final ImageButton closeBtn = (ImageButton) layout.findViewById(R.id.feeAGBImageCloseBtn);
		closeBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				popupWindow.dismiss();
			}
		});
	}
	
}
