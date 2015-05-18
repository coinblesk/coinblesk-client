package ch.uzh.csg.coinblesk.client.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.CurrencyFormatter;
import ch.uzh.csg.coinblesk.client.R;

/**
 * Copy by: http://stackoverflow.com/questions/6893147/android-custom-dialog-calculator-laying-out-different-on-android-2-2-1-and-2-3
 */
public class CalculatorDialog extends Dialog implements DialogInterface {
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

	private ArrayList<Float> mathVariables = new ArrayList<Float>();
	private float mathVariable1;
	private float mathVariable2;
    
	private int currentOperation = 0;
	private int nextOperation;
    
	private final static int ADD = 1;
	private final static int SUBTRACT = 2;
	private final static int MULTIPLY = 3;
	private final static int EQUALS = 5;
    
	private final static int CLEAR = 1;
	private final static int DONT_CLEAR = 0;
	private int clearCalcDisplay = 0;

	public CalculatorDialog(Context context) {
		super(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_calculator);

		this.setTitle(this.getContext().getResources().getString(R.string.calcDialog_title));
		
		calcDialogDisplay = (EditText) findViewById(R.id.calc_dialog_display);
		enterTotal = (TextView) findViewById(R.id.enter_total);
		allClear = (TextView) findViewById(R.id.all_clear);
		seven = (TextView) findViewById(R.id.seven);
		eight = (TextView) findViewById(R.id.eight);
		nine = (TextView) findViewById(R.id.nine);
		four = (TextView) findViewById(R.id.four);
		five = (TextView) findViewById(R.id.five);
		six =(TextView) findViewById(R.id.six);
		multiply = (TextView) findViewById(R.id.multiply);
		one = (TextView) findViewById(R.id.one);
		two = (TextView) findViewById(R.id.two);
		three = (TextView) findViewById(R.id.three);
		subtract = (TextView) findViewById(R.id.subtract);
		decimal = (TextView) findViewById(R.id.decimal);
		zero = (TextView) findViewById(R.id.zero);
		equals = (TextView) findViewById(R.id.equals);
		addition = (TextView) findViewById(R.id.addition);

		calcDialogDisplay.setKeyListener(DigitsKeyListener.getInstance(true,true));

		registerListeners();
	}

	private void registerListeners () {

		enterTotal.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				calcLogic(EQUALS);
				String calculatedValues = calcDialogDisplay.getText().toString();
				if (calculatedValues.length()==0 || calculatedValues.contentEquals("")){
					calculatedValues = "0.0";
				}

				try {
					if(Constants.inputUnit.equals(ReceivePaymentActivity.INPUT_UNIT_CHF)){
						Constants.inputValueCalculator = CurrencyFormatter.getBigDecimalChf(calculatedValues);						
					}
					else{
						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
						String bitcoinUnit = settings.getString("bitcoin_list", "");
						if(bitcoinUnit.equals(Constants.MILI_BTC)){
							Constants.inputValueCalculator = new BigDecimal(calculatedValues).setScale(5, RoundingMode.HALF_UP);
						}
						else if (bitcoinUnit.equals(Constants.MICRO_BTC)){
							Constants.inputValueCalculator = new BigDecimal(calculatedValues).setScale(2, RoundingMode.HALF_UP);
						}else{							
							Constants.inputValueCalculator = CurrencyFormatter.getBigDecimalBtc(calculatedValues);
						}
					}
				} catch (Exception e) {
					Constants.inputValueCalculator = BigDecimal.ZERO;
				}
				CalculatorDialog.this.dismiss();
			}
		});

		allClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				calcDialogDisplay.setText("");
				mathVariable1 = 0;
				mathVariable2 = 0;
				mathVariables.removeAll(mathVariables);
				currentOperation = 0;
				nextOperation = 0;                  
			}
		});

		seven.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");
					
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("7");
			}
		});

		eight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");
				
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("8");
			}
		});

		nine.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR)
					calcDialogDisplay.setText("");
					
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("9");
			}
		});

		four.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("4");
			}
		});

		five.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("5");
			}
		});

		six.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("6");
			}
		});

		multiply.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				calcLogic(MULTIPLY);
			}
		});

		one.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("1");
			}
		});

		two.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("2");
			}
		});

		three.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("3");
			}
		});

		subtract.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				calcLogic(SUBTRACT);
			}               
		});

		decimal.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append(".");
			}
		});

		zero.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (clearCalcDisplay == CLEAR) {
					calcDialogDisplay.setText("");
				}
				clearCalcDisplay = DONT_CLEAR;
				calcDialogDisplay.append("0");
			}
		});

		equals.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				calcLogic(EQUALS);

			}
		});

		addition.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				calcLogic(ADD);
			}
		});
	}

	private void calcLogic(int operator) {
		try {
			mathVariables.add(Float.parseFloat(calcDialogDisplay.getText().toString()));
		} catch (NumberFormatException e) {
			//do nothing
		}

		if (operator != EQUALS) {
			nextOperation = operator;
		} else if (operator == EQUALS) {
			nextOperation = 0;
		}

		switch (currentOperation) {
		case ADD:    
			if(mathVariables.size()<2){
				break;
			}
			mathVariable1 = mathVariables.get(0);
			mathVariable2 = mathVariables.get(1);

			mathVariables.removeAll(mathVariables);

			mathVariables.add(mathVariable1 + mathVariable2);

			calcDialogDisplay.setText(String.valueOf(mathVariables.get(0)));
			break;
		case SUBTRACT:
			if(mathVariables.size()<2){
				break;
			}
			mathVariable1 = mathVariables.get(0);
			mathVariable2 = mathVariables.get(1);

			mathVariables.removeAll(mathVariables);

			mathVariables.add(mathVariable1 - mathVariable2);

			calcDialogDisplay.setText(String.valueOf(mathVariables.get(0)));
			break;
		case MULTIPLY:
			if(mathVariables.size()<2){
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
	}

}