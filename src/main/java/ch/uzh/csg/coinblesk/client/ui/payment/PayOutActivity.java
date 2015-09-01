package ch.uzh.csg.coinblesk.client.ui.payment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.InsufficientMoneyException;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.Currency;
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

/**
 * This class is a view to send bitcoins from the system to the inserted
 * bitcoin-address.
 */
public class PayOutActivity extends BaseActivity {
    public static BigDecimal exchangeRate;
    private BigDecimal payOutAmount;
    private Button payOutBtn;
    private Button allBtn;
    private Button scanQRButton;
    private EditText payoutAmountEditText;
    private EditText payoutAddress;

    private TextView btcBalance;
    private TextView chfBalance;
    private TextView chfAmount;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        checkOnlineModeAndProceed();
        updateBalance();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_out);
        setScreenOrientation();

        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chfBalance = (TextView) findViewById(R.id.payOut_BalanceCHF);
        btcBalance = (TextView) findViewById(R.id.payOut_Balance);
        chfAmount = (TextView) findViewById(R.id.payOut_AmountCHF);

        payOutBtn = (Button) findViewById(R.id.payOut_payOutButton);
        allBtn = (Button) findViewById(R.id.payout_ButtonAll);
        payoutAmountEditText = (EditText) findViewById(R.id.payOut_Amount);
        payoutAddress = (EditText) findViewById(R.id.payOut_Address);
        scanQRButton = (Button) findViewById(R.id.payout_ButtonScanQR);

    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    private void initClickListener() {

        payOutBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ConnectionCheck.isNetworkAvailable(PayOutActivity.this)) {
                    // hide virtual keyboard
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    launchPayOutRequest();
                }
            }
        });
        payOutBtn.setEnabled(true);

        scanQRButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanQRAction();
            }

        });

        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BigDecimal balance = getWalletService().getMaxSendAmount();
                BigDecimal amount = CurrencyViewHandler.getBTCAmountInDefinedUnit(balance, getApplicationContext());
                payoutAmountEditText.setText(amount.toPlainString());
            }
        });
        allBtn.setEnabled(true);

        payoutAmountEditText.addTextChangedListener(new TextWatcher() {
            /*
             * Listener to update the inserted amount of bitcoin
             * and the exchange rate to swiss currency.
             */
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(payoutAmountEditText.getText().toString());
                    payOutAmount = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());
                    CurrencyViewHandler.setToCHF(chfAmount, exchangeRate, payOutAmount);
                } catch (NumberFormatException e) {
                    CurrencyViewHandler.setToCHF(chfAmount, BigDecimal.ZERO, BigDecimal.ZERO);
                }
            }

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        payoutAmountEditText.setEnabled(true);
    }


    private void scanQRAction() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    private void launchPayOutRequest() {
        if (payoutAmountEditText.getText().toString().isEmpty()) {
            displayResponse(getResources().getString(R.string.payOut_error_enterAmount));
            return;
        }
        if (payoutAddress.getText().toString().isEmpty()) {
            displayResponse(getResources().getString(R.string.payOut_error_enterBitcoinAddress));
            return;
        }

        showLoadingProgressDialog();
        BigDecimal tempBTC = CurrencyFormatter.getBigDecimalBtc(payoutAmountEditText.getText().toString());
        payOutAmount = CurrencyViewHandler.getBitcoinsRespectingUnit(tempBTC, getApplicationContext());

        try {
            getWalletService().createPayment(payoutAddress.getText().toString(), payOutAmount);
            chfAmount.setText("");
            payoutAmountEditText.setText("");
            payoutAddress.setText("");
        } catch (AddressFormatException e) {
            displayResponse(getResources().getString(R.string.payOut_error_address));
        } catch (InsufficientMoneyException e) {
            displayResponse(getResources().getString(R.string.payOut_error_balance));
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateBalance();
            }
        }, 1000);

        dismissProgressDialog();
    }

    private void checkOnlineModeAndProceed() {
        if (ConnectionCheck.isNetworkAvailable(this)) {
            launchExchangeRateRequest();
        } else {
            payOutBtn.setEnabled(false);
            allBtn.setEnabled(false);
            scanQRButton.setEnabled(false);
        }
    }

    /**
     * Launches request for updating Exchange Rate
     */
    public void launchExchangeRateRequest() {
        showLoadingProgressDialog();

        getCoinBleskApplication().getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {
            public void onTaskComplete(final ExchangeRateTransferObject response) {
                dismissProgressDialog();
                if (response.isSuccessful()) {
                    exchangeRate = new BigDecimal(response.getExchangeRate(Currency.CHF));
                    final BigDecimal balance = getWalletService().getBalance();
                    CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, balance);
                } else {
                    exchangeRate = BigDecimal.ZERO;
                    chfBalance.setText("");
                }
                initClickListener();
            }
        });
    }

    private void updateBalance() {
        BigDecimal balance = getWalletService().getUnconfirmedBalance();

        CurrencyViewHandler.setBTC(btcBalance, balance, this);
        CurrencyViewHandler.clearTextView(chfBalance);
        if (exchangeRate != null) {
            CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, balance);
        }
    }

    /**
     * Catches Result from Barcode-Scanner and sets PayOut address.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String result = scanResult.getContents();
            try {
                if (result.substring(0, 8).equalsIgnoreCase("bitcoin:")) {
                    String addressAndMore = result.substring(8);
                    String[] resultArray = addressAndMore.split("\\?");
                    String btcAddress = resultArray[0];
                    payoutAddress.setText(btcAddress);
                }
            } catch (Exception e) {
                displayResponse(getResources().getString(R.string.payOut_NoQRCode));
            }
        }
    }

}
