package ch.uzh.csg.coinblesk.client.ui.payment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
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

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PublicKey;
import java.util.ArrayList;

import ch.uzh.csg.coinblesk.Currency;
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.payment.PaymentRequest;
import ch.uzh.csg.coinblesk.client.payment.SendRequest;
import ch.uzh.csg.coinblesk.client.ui.main.MainActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.client.wallet.BitcoinUtils;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

/**
 * This is the UI to receive a payment - i.e. to be the seller in a transaction or to actively send bitcoins by NFC.
 */
public class ReceivePaymentActivity extends PaymentActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivePaymentActivity.class);

    private String[] strings;
    private String[] stringsNormal = {"CHF", "BTC"};
    private String[] stringsTablet = {"CHF", "Rp", "BTC"};

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

    private static boolean isPortrait = false;

    protected static final String INPUT_UNIT_CHF = "CHF";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        initNfcListener();

        isSeller = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_payment);
        setScreenOrientation();
        if (getResources().getBoolean(R.bool.small_device)) {
            isPortrait = true;
            strings = stringsNormal;
        } else {
            strings = stringsTablet;
            isPortrait = false;
        }

        Constants.inputUnit = INPUT_UNIT_CHF;
        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        exchangeRate = BigDecimal.ZERO;
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        descriptionOfInputUnit = (TextView) findViewById(R.id.receivePayment_enterAmountIn);

        Spinner spinner = (Spinner) findViewById(R.id.receivePayment_currencySpinner);
        spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, strings));
        spinner.setOnItemSelectedListener(spinnerListener);
        spinner.setSelection(0);
        if (Constants.IS_MENSA_MODE && !isPortrait) {
            isRpInputMode = true;
            spinner.setSelection(1);
        }

        refreshCurrencyTextViews();

        //adapt view for actively sending instead of requesting bitcoins
        Intent myIntent = getIntent(); // gets the previously created intent
        isSendingMode = myIntent.getBooleanExtra("isSend", false);
        if (isSendingMode) {
            TextView title = (TextView) findViewById(R.id.receivePayment_title);
            title.setText(getResources().getString(R.string.sendPayment_title));
            ImageView logo = (ImageView) findViewById(R.id.receivePayment_logo);
            logo.setImageResource(R.drawable.ic_pay_payment_big);
            getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_send_payment));
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

    protected NfcPaymentListener initNfcListener() {
        return new NfcPaymentListener() {

            @Override
            public void onPaymentReceived(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
                paymentRequestReceiver.inactivatePaymentRequest();
                showSuccessDialog(false, amount, senderUserName);
                clearPaymentInfos();
                hideNfcInstructions();
            }

            @Override
            public void onPaymentSent(BigDecimal amount, PublicKey senderPubKey, String senderUserName) {
                sendRequestReceiver.inactivateSendRequest();
                showSuccessDialog(true, amount, senderUserName);
                clearPaymentInfos();
                hideNfcInstructions();
            }

            @Override
            public void onPaymentError(String msg) {
                showErrorDialog();
                hideNfcInstructions();
            }

            @Override
            public void onPaymentRejected(String user) {
                super.onPaymentRejected(user);
                hideNfcInstructions();

            }
        };
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        launchExchangeRateRequest();
    }

    @Override
    public void onPause() {
        clearPaymentInfos();
        super.onPause();
    }

    @Override
    public void onResume() {
        invalidateOptionsMenu();
        super.onResume();
    }

    /**
     * Launches request for updating Exchange Rate
     */
    public void launchExchangeRateRequest() {
        if (ConnectionCheck.isNetworkAvailable(this)) {
            showLoadingProgressDialog();
            getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
                public void onTaskComplete(final ExchangeRateTransferObject response) {
                    dismissProgressDialog();
                    if (response.isSuccessful()) {
                        exchangeRate = new BigDecimal(response.getExchangeRate(Currency.CHF));

                        CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.receivePayment_exchangeRate));
                        CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.receivePayment_exchangeRate));
                        BigDecimal balance = getWalletService().getBalance();
                        CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_balance), balance, getBaseContext());
                        TextView balanceTv = (TextView) findViewById(R.id.receivePayment_balance);
                        balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
                    } else {
                        displayResponse(getResources().getString(R.string.no_connection_server));
                        finish();
                        launchActivity(ReceivePaymentActivity.this, MainActivity.class);
                    }
                }
            });
        }

    }

    /**
     * Updates GUI based on amount entered through {@link CalculatorDialog}
     */
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
            try {
                BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(receiveAmount);
                amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
                inputUnitValue = CurrencyViewHandler.getAmountInCHF(exchangeRate, amountBTC);
                CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.receivePayment_CHFinBTC), exchangeRate, amountBTC);
            } catch (NumberFormatException e) {
                CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.receivePayment_CHFinBTC), exchangeRate, amountBTC);
            }
        }

        //Check if the user defined a fee on the received amount of bitcoin.
        if (settings.getBoolean("include_fee", false)) {
            String percentageStr = settings.getString("fee_amount", "pref_fee_amount");
            try {
                double percentage = 1 + ((double) Integer.valueOf(percentageStr)) / 100;
                amountBTC = amountBTC.multiply(new BigDecimal(percentage)).setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP);
                CurrencyViewHandler.setBTC((TextView) findViewById(R.id.receivePayment_BTCIncFee), amountBTC, getApplicationContext());
                ((TextView) findViewById(R.id.receivePayment_BTCIncFeeText)).setText("(" + getResources().getString(R.string.receivePayment_fee) + " " + percentageStr + "%)");
            } catch (NumberFormatException e) {
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

    /**
     * Creates PaymentInfos necessary for initializeNFC. Only transaction amount > 0 are accepted.
     */
    private void initializePayment() {
        initiator.startInitiating();
        handler.reset();
        receiveAmount = Constants.inputValueCalculator.toPlainString();
        if (isPortrait) {
            receiveAmountEditText.setText(receiveAmount);
        } else {
            receiveAmountTextView.setText(receiveAmount);
        }
        refreshCurrencyTextViews();

        if (amountBTC.compareTo(BigDecimal.ZERO) > 0) {
            if(!isSendingMode) {
                // payment request
                String address = getWalletService().getBitcoinAddress();
                String user = getCoinBleskApplication().getStorageHandler().getUsername();
                Preconditions.checkState(BitcoinUtils.isP2SHAddress(address, getCoinBleskApplication().getStorageHandler().getBitcoinNet()), "NFC payments to non-P2SH addresses is not currently supported.");
                Intent paymentRequestIntent = PaymentRequest.create(amountBTC, user, address).getIntent();
                sendBroadcast(paymentRequestIntent);
                showNfcInstructions();
            } else {
                // send request
                Intent sendRequestIntent = SendRequest.create(amountBTC).getIntent();
                sendBroadcast(sendRequestIntent);
                showNfcInstructions();
            }

        } else {
            hideNfcInstructions();
        }
    }

    private void clearPaymentInfos() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideNfcInstructions();
                Constants.inputValueCalculator = BigDecimal.ZERO;
                receiveAmount = "0";
                if (!isPortrait) {
                    clearCalculator();
                    receiveAmountTextView.setText("");
                }

            }
        });

    }

    /**
     * Shows the animated NFC image to indicate user that a NFC connection can now be established.
     */
    private void showNfcInstructions() {
        findViewById(R.id.receivePayment_establishNfcConnectionInfo).setVisibility(View.VISIBLE);
        ImageView nfcActivity = (ImageView) findViewById(R.id.receivePayment_nfcIcon);
        nfcActivity.setVisibility(View.VISIBLE);
        nfcActivity.setBackgroundResource(R.drawable.animation_nfc);
        nfcActivityAnimation = (AnimationDrawable) nfcActivity.getBackground();
        nfcActivityAnimation.start();
    }

    private void hideNfcInstructions() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.receivePayment_establishNfcConnectionInfo).setVisibility(View.INVISIBLE);
                findViewById(R.id.receivePayment_nfcIcon).setVisibility(View.INVISIBLE);
            }
        });
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
            if (isPortrait) {
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
                } else if (pos == 1) {
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

            if (isPortrait) {
                descriptionOfInputUnit.setText(Constants.inputUnit);
            }
            refreshCurrencyTextViews();
            initializePayment();
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    /**
     * Shows a dialog indicating that the NFC payment was successful
     *
     * @param isSending (isSending = true if initiator sends bitcoins, false if initiator requests bitcoins)
     */
    private void showSuccessDialog(boolean isSending, BigDecimal amountBtc, String user) {
        String answer;

        if (isSending) {
            answer = String.format(getResources().getString(R.string.payment_notification_success_payer),
                    CurrencyViewHandler.formatBTCAsString(amountBtc, this) + " (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")", user);
        } else {
            answer = String.format(getResources().getString(R.string.payment_notification_success_payee),
                    CurrencyViewHandler.formatBTCAsString(amountBtc, this) + " (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBtc) + ")", user);
        }

        showDialog(answer, true);

    }

    /**
     * Shows a dialog indicating that the NFC payment failed
     *
     */
    private void showErrorDialog() {
        showDialog(getResources().getString(R.string.error_transaction_failed), false);
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
    private Button coffee2;

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

        if (isSendingMode || !Constants.IS_MENSA_MODE) {
            removeMensaButtons();
        }

        adaptCalculatorToRpMode();

        registerListeners();
    }

    private void adaptCalculatorToRpMode() {
        if (!isPortrait) {
            if (isRpInputMode)
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
                    if (isRpInputMode)
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

                if (!wasEqualsBeforeTmp) {
                    list.append("= " + calculatedValues + " \n" + "===========" + " \n");
                }

                scrollDown();

                try {
                    if (Constants.inputUnit.equals(ReceivePaymentActivity.INPUT_UNIT_CHF)) {
                        Constants.inputValueCalculator = CurrencyFormatter.getBigDecimalChf(calculatedValues);
                    } else {
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String bitcoinUnit = settings.getString("bitcoin_list", "");
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
                    if (isRpInputMode) {
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
                    if (isRpInputMode) {
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
                if (!isRpInputMode) {
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
                    if (isRpInputMode) {
                        value = value.divide(new BigDecimal(100));
                    }
                    list.append(value.toString() + " \n" + "-----------" + " \n" + " = ");
                    calcLogic(EQUALS);
                    BigDecimal value2 = new BigDecimal(calcDialogDisplay.getText().toString());
                    if (isRpInputMode) {
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
                    if (isRpInputMode) {
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
        if (list != null) list.setText("");
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

    /**
     * Adds the parameter amount to the total Mensa Button amount.
     *
     * @param amount to add to mensaButtonAmount
     */
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

    /**
     * Initializes the buttons specific for Mensa Tablet.
     */
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

        coffee2 = (Button) findViewById(R.id.mensa_coffe2);
        coffee2.setVisibility(View.VISIBLE);
        coffee2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                resetNfc();
                addMensaButtonAmount("1.70");
            }
        });
    }

    /**
     * Hide all Mensa Buttons.
     */
    private void removeMensaButtons() {
        menu_student.setEnabled(false);
        menu_student.setVisibility(View.INVISIBLE);
        menu_employee.setEnabled(false);
        menu_employee.setVisibility(View.INVISIBLE);
        menu_external.setEnabled(false);
        menu_external.setVisibility(View.INVISIBLE);
        coffee.setEnabled(false);
        coffee.setVisibility(View.INVISIBLE);
        coffee2.setEnabled(false);
        coffee2.setVisibility(View.INVISIBLE);
        drink.setEnabled(false);
        drink.setVisibility(View.INVISIBLE);
    }

    /**
     * Disables Mensa Buttons (keep visible)
     */
    private void disableMensaButtons() {
        if (Constants.IS_MENSA_MODE) {
            menu_student.setEnabled(false);
            menu_employee.setEnabled(false);
            menu_external.setEnabled(false);
            coffee.setEnabled(false);
            coffee2.setEnabled(false);
            drink.setEnabled(false);
        }
    }

    /**
     * Enables all Mensa specific Buttons.
     */
    private void enableMensaButtons() {
        if (Constants.IS_MENSA_MODE) {
            menu_student.setEnabled(true);
            menu_employee.setEnabled(true);
            menu_external.setEnabled(true);
            coffee.setEnabled(true);
            coffee2.setEnabled(true);
            drink.setEnabled(true);
        }
    }

    /**
     * Resets the NFC communication and hides the animated NFC image.
     */
    private void resetNfc() {
        hideNfcInstructions();
    }

}
