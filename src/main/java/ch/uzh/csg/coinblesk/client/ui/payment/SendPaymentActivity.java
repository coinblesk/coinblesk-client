package ch.uzh.csg.coinblesk.client.ui.payment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.InsufficientMoneyException;

import java.math.BigDecimal;
import java.util.List;

import ch.uzh.csg.coinblesk.Currency;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.ui.main.MainActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

/**
 * This is the UI to send a payment directly to a known receiver without the use of NFC communication.
 */
public class SendPaymentActivity extends WalletActivity {
    private String[] currencies = {"CHF", "BTC"};
    protected CalculatorDialog calculatorDialogFragment;
    protected static BigDecimal amountBTC = BigDecimal.ZERO;
    protected static BigDecimal amountCHF = BigDecimal.ZERO;
    private BigDecimal exchangeRate;
    private EditText sendAmount;
    private TextView descriptionOfInputUnit;
    private Button sendButton;
    private Button addressBookButton;
    private static TextView receiverUsernameTextView;
    private static AddressBookEntry selectedUser;

    private MenuItem menuWarning;
    private MenuItem offlineMode;
    private MenuItem sessionCountdownMenuItem;
    private MenuItem sessionRefreshMenuItem;
    private TextView sessionCountdown;

    protected static final String INPUT_UNIT_CHF = "CHF";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_payment);
        setScreenOrientation();

        Constants.inputUnit = INPUT_UNIT_CHF;
        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        exchangeRate = BigDecimal.ZERO;

        setUpGui();
        refreshCurrencyTextViews();
    }

    @Override
    public void onPause() {
        Constants.inputValueCalculator = BigDecimal.ZERO;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        launchExchangeRateRequest();
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

    protected void initializeMenuItems(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        offlineMode = menu.findItem(R.id.menu_offlineMode);

        //setup timer
        sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
        sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
        sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);

    }

    @Override
    public void invalidateOptionsMenu() {
        if (menuWarning != null) {
            if (ConnectionCheck.isNetworkAvailable(this)) {
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

    /**
     * Launches request for updating Exchange Rate
     */
    public void launchExchangeRateRequest() {
        if (ConnectionCheck.isNetworkAvailable(this)) {
            showLoadingProgressDialog();

            getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
                @Override
                public void onTaskComplete(ExchangeRateTransferObject response) {
                    dismissProgressDialog();
                    if (response.isSuccessful()) {
                        onTaskCompleteExchangeRate(response.getExchangeRate(Currency.CHF));
                    } else {
                        displayResponse(getResources().getString(R.string.no_connection_server));
                        finish();
                        launchActivity(SendPaymentActivity.this, MainActivity.class);
                        return;
                    }
                }
            });
        }
    }

    private void onTaskCompleteExchangeRate(String exchangeRateNew) {
        dismissProgressDialog();
        CurrencyViewHandler.clearTextView((TextView) findViewById(R.id.sendPayment_exchangeRate));
        exchangeRate = new BigDecimal(exchangeRateNew);
        CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.sendPayment_exchangeRate));
        BigDecimal balance = getWalletService().getBalance();
        CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_balance), balance, getBaseContext());
        TextView balanceTv = (TextView) findViewById(R.id.sendPayment_balance);
        balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");
    }

    private void onPaymentComplete(boolean success, String user, BigDecimal amount, @Nullable String errorMsg) {
        dismissProgressDialog();

        //verification of server response not needed as no interaction with selling partner
        if (success) {
            //update textviews
            receiverUsernameTextView.setText("");
            sendAmount.setText("");
            refreshCurrencyTextViews();
            BigDecimal balance = getWalletService().getBalance().subtract(amount);
            CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_balance), balance, getBaseContext());
            TextView balanceTv = (TextView) findViewById(R.id.sendPayment_balance);
            balanceTv.append(" (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, balance) + ")");

            String s = String.format(getResources().getString(R.string.payment_notification_success_payer),
                    CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, amount, SendPaymentActivity.this),
                    user);
            showDialog(getResources().getString(R.string.payment_success), R.drawable.ic_payment_succeeded, s);
        } else {
            showDialog(getResources().getString(R.string.payment_failure), R.drawable.ic_payment_failed, errorMsg);
        }
    }

    private void refreshCurrencyTextViews() {
        amountBTC = BigDecimal.ZERO;
        if (Constants.inputUnit.equals(INPUT_UNIT_CHF)) {
            try {
                amountCHF = CurrencyFormatter.getBigDecimalChf(sendAmount.getText().toString());
                amountBTC = CurrencyViewHandler.getBitcoinExchangeValue(exchangeRate, amountCHF);
                CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), amountBTC, getApplicationContext());
            } catch (NumberFormatException e) {
                CurrencyViewHandler.setBTC((TextView) findViewById(R.id.sendPayment_CHFinBTC), BigDecimal.ZERO, getApplicationContext());
            }
        } else {
            try {
                BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(sendAmount.getText().toString());
                amountBTC = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
                amountCHF = CurrencyViewHandler.getAmountInCHF(exchangeRate, amountBTC);
                CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
            } catch (NumberFormatException e) {
                CurrencyViewHandler.setToCHF((TextView) findViewById(R.id.sendPayment_CHFinBTC), exchangeRate, amountBTC);
            }
        }
    }

    private void openCalculatorDialog() {
        calculatorDialogFragment = new CalculatorDialog(this);
        calculatorDialogFragment.show();
        calculatorDialogFragment.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                sendAmount.setText(Constants.inputValueCalculator.toPlainString());
                refreshCurrencyTextViews();
            }
        });
    }

    private class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context, int textViewResourceId,
                         String[] objects) {
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
            label.setText(currencies[position]);

            return row;
        }
    }

    private OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (pos == 0)
                Constants.inputUnit = INPUT_UNIT_CHF;
            else
                Constants.inputUnit = CurrencyViewHandler.getBitcoinUnit(getApplicationContext());

            descriptionOfInputUnit.setText(Constants.inputUnit);
            refreshCurrencyTextViews();
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private void setUpGui() {
        sendAmount = (EditText) findViewById(R.id.sendPayment_amountText);
        sendAmount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                openCalculatorDialog();
            }
        });
        sendAmount.setFocusable(false);

        descriptionOfInputUnit = (TextView) findViewById(R.id.sendPayment_enterAmountIn);
        receiverUsernameTextView = (EditText) findViewById(R.id.sendPayment_receiver);

        Spinner spinner = (Spinner) findViewById(R.id.sendPayment_currencySpinner);
        spinner.setAdapter(new MyAdapter(this, R.layout.spinner_currency, currencies));
        spinner.setOnItemSelectedListener(spinnerListener);
        spinner.setSelection(0);

        sendButton = (Button) findViewById(R.id.sendPayment_sendButton);
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // hide virtual keyboard
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                showConfirmationDialog();
            }
        });

        addressBookButton = (Button) findViewById(R.id.sendPayment_addressBookButton);
        addressBookButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AddressBookDialog dialog = new AddressBookDialog();
                dialog.show(getFragmentManager(), "sendPaymentActivity");
            }
        });
    }

    private void showConfirmationDialog() {
        String message;

        if (selectedUser != null && amountBTC != null && amountBTC.compareTo(BigDecimal.ZERO) != 0) {
            String amount = CurrencyViewHandler.formatBTCAsString(amountBTC, this) + " (" + CurrencyViewHandler.getAmountInCHFAsString(exchangeRate, amountBTC) + ")";
            message = String.format(getString(R.string.sendPayment_dialog_message), amount, selectedUser.getName());
        } else {
            displayResponse(getString(R.string.fill_necessary_fields));
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.sendPayment_dialog_title));
        alert.setMessage(message);

        alert.setPositiveButton(getString(R.string.sendPayment_dialog_confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                createTransaction();
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Dialog canceled
            }
        });
        alert.show();
    }

    private void createTransaction() {

        showLoadingProgressDialog();

        AsyncTask<Void, Void, Void> payOutTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    getWalletService().createPayment(selectedUser.getBitcoinAddress(), amountBTC);
                    onPaymentComplete(true, selectedUser.getName(), amountBTC, null);
                } catch (AddressFormatException e) {
                    onPaymentComplete(false, selectedUser.getName(), amountBTC, getString(R.string.payOut_error_address));
                } catch (InsufficientMoneyException e) {
                    onPaymentComplete(false, selectedUser.getName(), amountBTC, getString(R.string.payOut_error_balance));
                }
                return null;
            }
        };

        payOutTask.execute();
    }

    /**
     * Creates a dialog which shows all entries from addressbook.
     */
    public static class AddressBookDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            List<AddressBookEntry> receiverEntries = ((CoinBleskApplication) getActivity().getApplicationContext()).getStorageHandler().getAddressBook();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.sendPayment_selectReceiver);

            ScrollView scrollView = new ScrollView(getActivity().getApplicationContext());
            LinearLayout linearLayout = new LinearLayout(getActivity().getApplicationContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            for(final AddressBookEntry addressBookEntry : receiverEntries) {
                final TextView entry = new TextView(getActivity().getApplicationContext());
                final String username = addressBookEntry.getName();

                entry.setGravity(android.view.Gravity.CENTER_VERTICAL);
                entry.setPadding(0, 0, 0, 10);
                entry.setTextColor(Color.BLACK);
                entry.setText(username);
                if (addressBookEntry.isTrusted()) {
                    entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_starred), null, null, null);
                } else {
                    entry.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_not_starred), null, null, null);
                }

                entry.setClickable(true);
                entry.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        receiverUsernameTextView.setText(username);
                        selectedUser = addressBookEntry;
                        dismiss();
                    }
                });

                linearLayout.addView(entry);
            }

            scrollView.addView(linearLayout);
            builder.setView(scrollView);

            return builder.create();
        }
    }

}
