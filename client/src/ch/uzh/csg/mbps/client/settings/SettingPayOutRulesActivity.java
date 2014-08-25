package ch.uzh.csg.mbps.client.settings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.PayOutRuleGetRequestTask;
import ch.uzh.csg.mbps.client.request.PayOutRuleRequestTask;
import ch.uzh.csg.mbps.client.request.PayOutRuleResetRequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;
import ch.uzh.csg.mbps.model.PayOutRule;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class is the view the pay out rules are defined.
 */
public class SettingPayOutRulesActivity extends AbstractAsyncActivity {
	private static String payOutAddress;
	private Button saveRuleBtn;
	private Button resetRuleBtn;
	private EditText payOutAddressInput;
	private TextView createRuleView;
	private Spinner spinner;
	
	private static final String CREATION_SUCCESS = "Your new rule has successfully been saved.";
	private static final String RESET_SUCCESS = "Your payout rules have successfully been reseted.";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting_pay_out_rules);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		saveRuleBtn = (Button)findViewById(R.id.settingPayOut_store_Btn);
		resetRuleBtn = (Button)findViewById(R.id.settingPayOut_reset_Btn);
		payOutAddressInput = (EditText)findViewById(R.id.settingPayOut_addressInput);
		createRuleView = (TextView)findViewById(R.id.settingPayOut_created_rules_view);
		
		initClickListener();
		prepareSpinner();
		checkOnlineModeAndProceed();
	}

	@Override
    public void onResume(){
    	super.onResume();
    	invalidateOptionsMenu();
    }
	
	private void initClickListener() {
		
		payOutAddressInput.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s == null || s.length() == 0)
					payOutAddress = "";
				else
					payOutAddress = s.toString();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});
		
		saveRuleBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				//Check if the bitcoin-address is inserted
				if(payOutAddress == null || payOutAddress.isEmpty()){
					displayResponse(getResources().getString(R.string.insert_payout_address));
				} else {					
					switch (spinner.getSelectedItemPosition()) {
					case 0:
						if (!isInRange(CustomOnItemSelectedListener.getBalanceAmount())) {
							displayResponse(getResources().getString(R.string.insert_min_amount));				
						} else {
							initBalanceRuleRequest();
						}
						break;
					case 1:
						if(CustomOnItemSelectedListener.getDaySelections().isEmpty()) {
							displayResponse(getResources().getString(R.string.at_least_one_day));
						} else if (CustomOnItemSelectedListener.getTimeSelections().isEmpty()) {
							displayResponse(getResources().getString(R.string.at_least_one_time));
						} else {
							initDayTimeRequest();
						}
						break;
					default:
					}
				}
				
			}
		});
		
		resetRuleBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				launchResetRequest();	
			}

		});
		
		createRuleView.setText("");
		
	}
    
	/**
	 * Checks if the inserted amount is less than the minimum accepted amount of
	 * bitcoins.
	 * 
	 * @param input
	 *            The inserted amount of bitcoin.
	 * @return Returns true if the amount is higher than the minimum accepted
	 *         amount of bitcoins.
	 */
	private boolean isInRange(BigDecimal input) {
		BigDecimal min = CurrencyFormatter.getBigDecimalBtc(Constants.MIN_VALUE_PAYOUT);
		return min.compareTo(input) == 1 ? false : true;
	}
	
	private void initBalanceRuleRequest() {
		PayOutRulesTransferObject transferObject = new PayOutRulesTransferObject();
		List<PayOutRule> list = new ArrayList<PayOutRule>(1);
		PayOutRule balance = new PayOutRule(ClientController.getStorageHandler().getUserAccount().getId(), CustomOnItemSelectedListener.getBalanceAmount(), payOutAddress);
		list.add(balance);
		transferObject.setPayOutRulesList(list);
		launchPayOutRuleRequest(transferObject);
	}
    
	private void launchPayOutRuleRequest(PayOutRulesTransferObject transferObject) {
		showLoadingProgressDialog();
		PayOutRuleRequestTask request = new PayOutRuleRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
			public void onTaskComplete(TransferObject response) {
				if (response.isSuccessful()) {
					createRuleView.setText("");
					launchPayOutRuleGetRequest();
					showDialog(getResources().getString(R.string.defined_rules_title), getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()), getResources().getString(R.string.defined_successfully));
				} else {
					displayResponse(response.getMessage());
				}
				dismissProgressDialog();
	            
            }
		}, transferObject, new TransferObject());
		request.execute();
	}
    
	private void initDayTimeRequest() {
		PayOutRulesTransferObject transferObject = new PayOutRulesTransferObject();
		List<PayOutRule> list = new ArrayList<PayOutRule>();
		//Get for every day all time slots
		for (int day : CustomOnItemSelectedListener.getDaySelections()) {
			for (TextView hour : CustomOnItemSelectedListener.getTimeSelections()) {
				PayOutRule dayTime = new PayOutRule(ClientController.getStorageHandler().getUserAccount().getId(), Integer.parseInt(hour.getText().toString()), day, payOutAddress);
				list.add(dayTime);
			}
		}
		transferObject.setPayOutRulesList(list);
		launchPayOutRuleRequest(transferObject);
	}
	
	private void launchResetRequest() {
		showLoadingProgressDialog();
		PayOutRuleResetRequestTask resetRequest = new PayOutRuleResetRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {

			public void onTaskComplete(TransferObject response) {
				if (response.isSuccessful()) {
					showDialog(getResources().getString(R.string.defined_rules_title), getResources().getIdentifier("ic_payment_succeeded", "drawable", getPackageName()), getResources().getString(R.string.reset_pay_out_rules));
					createRuleView.setText("");
				} else {
					displayResponse(response.getMessage());
				}
				dismissProgressDialog();
	            
            }
		}, new TransferObject(), new TransferObject());
		resetRequest.execute();
	}
	
	private void responseComplete(List<PayOutRule> rules) {
		List<String> payOutRuleList = new ArrayList<String>();
		
		for (PayOutRule rule : rules) {
			if (rule.getBalanceLimitBTC() == null) {
				payOutRuleList.add(createDayTimeView(rule));
			} else {
				payOutRuleList.add(createBalanceView(rule));
			}
		}
		
		insertRuleIntoView(payOutRuleList);
	}
	
	private void prepareSpinner() {
    	spinner = (Spinner) findViewById(R.id.settingPayOut_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.settingPayOut_array , android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new CustomOnItemSelectedListener(this));
	}
	
	private void checkOnlineModeAndProceed() {
		if (ClientController.isOnline()) {
			launchPayOutRuleGetRequest();
		} else {
			saveRuleBtn.setEnabled(false); 
			resetRuleBtn.setEnabled(false);
		}
	}

	private void launchPayOutRuleGetRequest() {
		PayOutRuleGetRequestTask getRequest = new PayOutRuleGetRequestTask(new IAsyncTaskCompleteListener<PayOutRulesTransferObject>() {

			public void onTaskComplete(PayOutRulesTransferObject response) {
				if (response.isSuccessful()) {
					responseComplete(response.getPayOutRulesList());
				} else {
					displayResponse(response.getMessage());
				}
				dismissProgressDialog();
	            
            }
		}, new TransferObject(), new PayOutRulesTransferObject());
		getRequest.execute();
	}

	private String createDayTimeView(PayOutRule rule) {
    	String text = getResources().getString(R.string.display_day_time_rule) +"\n";
    	text += getResources().getString(R.string.display_address) + rule.getPayoutAddress() + "\n";
    	text += getDayString(rule.getDay());
    	
		if (DateFormat.is24HourFormat(this)) {
			text += getResources().getString(R.string.display_time) + rule.getHour() + ":00\n";
		} else {
			text += getAMPMString(rule.getHour());
		}
    	
    	return text;
	}

	private String getDayString(int day) {
		switch(day){
    	case 1:
    		return getResources().getString(R.string.display_sunday);
    	case 2:
    		return getResources().getString(R.string.display_monday);
    	case 3:
    		return getResources().getString(R.string.display_tuesday);
    	case 4:
    		return getResources().getString(R.string.display_wednesday);
    	case 5:
    		return getResources().getString(R.string.display_thursday);
    	case 6:
    		return getResources().getString(R.string.display_friday);
    	case 7:
    		return getResources().getString(R.string.display_saturday);
    	default:
    		return getResources().getString(R.string.display_default_day);
    	}
	}

	private String getAMPMString(int hour) {
		switch(hour){
		case 0:
			return getResources().getString(R.string.display_time) +"12:00 PM\n";
		case 1:
			return getResources().getString(R.string.display_time) +"1:00 AM\n";
		case 2:
			return getResources().getString(R.string.display_time) +"2:00 AM\n";
		case 3:
			return getResources().getString(R.string.display_time) +"3:00 AM\n";
		case 4:
			return getResources().getString(R.string.display_time) +"4:00 AM\n";
		case 5:
			return getResources().getString(R.string.display_time) +"5:00 AM\n";
		case 6:
			return getResources().getString(R.string.display_time) +"6:00 AM\n";
		case 7:
			return getResources().getString(R.string.display_time) +"7:00 AM\n";
		case 8:
			return getResources().getString(R.string.display_time) +"8:00 AM\n";
		case 9:
			return getResources().getString(R.string.display_time) +"9:00 AM\n";
		case 10:
			return getResources().getString(R.string.display_time) +"10:00 AM\n";
		case 11:
			return getResources().getString(R.string.display_time) +"11:00 AM\n";
		case 12:
			return getResources().getString(R.string.display_time) +"12:00 AM\n";
		case 13:
			return getResources().getString(R.string.display_time) +"1:00 PM\n";
		case 14:
			return getResources().getString(R.string.display_time) +"2:00 PM\n";
		case 15:
			return getResources().getString(R.string.display_time) +"3:00 PM\n";
		case 16:
			return getResources().getString(R.string.display_time) +"4:00 PM\n";
		case 17:
			return getResources().getString(R.string.display_time) +"5:00 PM\n";
		case 18:
			return getResources().getString(R.string.display_time) +"6:00 PM\n";
		case 19:
			return getResources().getString(R.string.display_time) +"7:00 PM\n";
		case 20:
			return getResources().getString(R.string.display_time) +"8:00 PM\n";
		case 21:
			return getResources().getString(R.string.display_time) +"9:00 PM\n";
		case 22:
			return getResources().getString(R.string.display_time) +"10:00 PM\n";
		case 23:
			return getResources().getString(R.string.display_time) +"11:00 PM\n";
		default:
			return getResources().getString(R.string.display_time) +"\n";
		}
	}
	
	private String createBalanceView(PayOutRule rule) {
		String text = getResources().getString(R.string.display_balance_rule)+"\n";
		text += getResources().getString(R.string.display_address) + rule.getPayoutAddress() + "\n";
		text += getResources().getString(R.string.display_balance) + rule.getBalanceLimitBTC() + " BTC\n";
		return text;
	}
	
    private void insertRuleIntoView(List<String> payOutRuleList) {
		for (String rule : payOutRuleList) {
			createRuleView.append(rule + "\n");
		}
	}

	public static String getPayOutAddress(){
    	return payOutAddress;
    }

}
