package ch.uzh.csg.coinblesk.client.ui.settings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.client.util.TimePickerRule;
import ch.uzh.csg.coinblesk.client.R;

/**
 * This class is the listener for the payout rules
 * {@link SettingPayOutRulesActivity}. The payout rules consist of
 * a balance limit or a day and time selection. 
 */
public class CustomOnItemSelectedListener implements OnItemSelectedListener, IAsyncTaskCompleteListener<Integer> {
	
	private Activity parentActivity;
	private static List<TextView> timeSelections;
	private static List<Integer> daySelections;
	private LayoutInflater inflater;
	private ViewGroup searchViewHolder;
	private View searchView;
	
	private EditText balanceLimit;
	private static RelativeLayout layoutDayTime;
	private TextView dayTimeTitle;
	
	private DialogFragment newFragment;
	private static BigDecimal balanceAmount;
	private TextView time;
	
	private final static int MAX_NOF_PAYOUT_TIMES = 4;
	
	public CustomOnItemSelectedListener(Activity activity) {
		parentActivity = activity;
		timeSelections = new ArrayList<TextView>();
		daySelections = new ArrayList<Integer>();
		init();
	}

	private void init() {
		inflater = (LayoutInflater)getParentActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		searchViewHolder = (ViewGroup)getParentActivity().findViewById(R.id.settingPayOut_content_area);
	}
	
	private Activity getParentActivity(){
		return parentActivity;
	}

	/**
	 * Creates the view of the selected option in the dropdown menu 
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (searchView != null) {
			searchViewHolder.removeView(searchView);
		}

		switch (position) {
		// Day and Time rule view
		case 0:
			clearDayTimeSelections();
			searchView = inflater.inflate(R.layout.activity_pay_out_balance, null);
			searchViewHolder.addView(searchView);
			createBalanceView();
			break;
		// Balance limit view
		case 1:
			balanceAmount = BigDecimal.ZERO;
			searchView = inflater.inflate(R.layout.activity_pay_out_day_time, null);
			searchViewHolder.addView(searchView);
			createDayTimeView();
			break;
		default:
		}
	}
	
	public void onNothingSelected(AdapterView<?> parent) {	
	}
	
	/**
	 * The view is cleared when the option in the dropdown menu is altered.
	 */
	public void clearDayTimeSelections(){
		if (!timeSelections.isEmpty())
			timeSelections.clear();
		
		if (!daySelections.isEmpty())
			daySelections.clear();
	}
	
	private void createBalanceView() {
		balanceLimit = (EditText)getParentActivity().findViewById(R.id.setting_payOut_balance_Value);
		balanceLimit.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s == null || s.length() == 0)
					balanceAmount = BigDecimal.ZERO;
				else
					balanceAmount = CurrencyFormatter.getBigDecimalBtc(s.toString());
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			public void afterTextChanged(Editable s) {
			}
		});
	}
	
	private void createDayTimeView() {
		layoutDayTime = (RelativeLayout)getParentActivity().findViewById(R.id.pay_out_day_time);
		
		dayTimeTitle = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_title);
		dayTimeTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_content_new, 0);
		dayTimeTitle.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (timeSelections.size() < MAX_NOF_PAYOUT_TIMES) {
					showTimePickerDialog(v);
				} else {
					showToastResponse();
				}
			}
		});
	}
	
	/**
	 * Shows the dialog to choose a time in the day. The time is limited hourly.
	 * 
	 * @param v
	 *            the view context of the view
	 */
	public void showTimePickerDialog(View v) {
		newFragment = new TimePickerRule(this);
		newFragment.show(getParentActivity().getFragmentManager(), "timePicker");
	}
	
	private void showToastResponse() {
		Toast.makeText(getParentActivity(), getParentActivity().getResources().getString(R.string.display_time_slots), Toast.LENGTH_LONG).show();
	}
	
	public void onTaskComplete(Integer hourOfDay) {
		int index = timeSelections.size();
		if (!isHourOfDayExisting(hourOfDay)) {
			time = setTextView(index, hourOfDay);
			timeSelections.add(time);
		} else {
			Toast.makeText(getParentActivity(), getParentActivity().getResources().getString(R.string.display_already_selected), Toast.LENGTH_LONG).show();
		}
	}
	
	private boolean isHourOfDayExisting(Integer hourOfDay) {
		for (TextView view : timeSelections) {
			if (Integer.parseInt(view.getText().toString()) == hourOfDay) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the text view with the value of the selected time slot. 
	 * The user is not allowed to select more than four time slots for a day.
	 * 
	 * @param index
	 *            The position of the time is going to be selected.
	 * @param hourOfDay
	 *            The selected time.
	 * @return Returns the Textview with the defined time slot.
	 */
	private TextView setTextView(int index, Integer hourOfDay) {
		TextView textView = null;
		switch(index){
		case 0:
			textView = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_0);
			break;
		case 1:
			textView = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_1);
			break;
		case 2:
			textView = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_2);
			break;
		case 3:
			textView = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_3);
			break;
		default:
		}
		
		textView.setVisibility(View.VISIBLE);
		textView.setText(String.valueOf(hourOfDay));
		textView.setOnClickListener(new View.OnClickListener() {
		
			public void onClick(View v) {
				removeTextView(v.getId());
			}
		});
		return textView;
	}
	
	private void removeTextView(int id) {
		TextView tempTextView = null;
		for (TextView tv : timeSelections) {
			if (tv.getId() == id) {
				tempTextView = tv;
				break;
			}
		}
		
		clearTimeTextViews();
		if(tempTextView != null)
			timeSelections.remove(tempTextView);
		
		redesignTextView();
	}

	private void clearTimeTextViews(){
		TextView tv0 = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_0);
		TextView tv1 = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_1);
		TextView tv2 = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_2);
		TextView tv3 = (TextView)layoutDayTime.findViewById(R.id.setting_payOut_time_view_3);
		
		tv0.setOnClickListener(null);
		tv1.setOnClickListener(null);
		tv2.setOnClickListener(null);
		tv3.setOnClickListener(null);
		tv0.setVisibility(View.GONE);
		tv1.setVisibility(View.GONE);
		tv2.setVisibility(View.GONE);
		tv3.setVisibility(View.GONE);
	}
	
	private void redesignTextView() {
		List<TextView> tempList = new ArrayList<TextView>();
		
		for (int index = 0; index < timeSelections.size(); index++) {
			int hourOfDay = Integer.parseInt(timeSelections.get(index).getText().toString());
			TextView tempView = setTextView(index, hourOfDay);
			tempList.add(tempView);
		}
		
		timeSelections.clear();
		timeSelections.addAll(tempList);
	}
	
	/**
	 * Gets the balance amount of bitcoins
	 */
	public static BigDecimal getBalanceAmount(){
		if(balanceAmount == null)
			return BigDecimal.ZERO;
		
		return balanceAmount;
	}
	
	/**
	 * Gets the time selections to pay out the bitcoins.
	 * 
	 * @return Returns a list of the selected time slots.
	 */
	public static List<TextView> getTimeSelections(){
		return timeSelections;
	}

	/**
	 * Gets the day selections to pay out the bitcoins.
	 * 
	 * @return Returns a list of selected days.
	 */
	public static List<Integer> getDaySelections(){
		if (!daySelections.isEmpty())
			daySelections.clear();
		
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_monday)).isChecked())
			daySelections.add(2);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_tuesday)).isChecked())
			daySelections.add(3);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_wednesday)).isChecked())
			daySelections.add(4);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_thursday)).isChecked())
			daySelections.add(5);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_friday)).isChecked())
			daySelections.add(6);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_saturday)).isChecked())
			daySelections.add(7);
		if (((CheckBox) layoutDayTime.findViewById(R.id.setting_payOut_day_sunday)).isChecked())
			daySelections.add(1);
        
		return daySelections;
	}
	
}
