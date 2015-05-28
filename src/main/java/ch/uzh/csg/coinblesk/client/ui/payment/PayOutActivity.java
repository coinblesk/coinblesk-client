package ch.uzh.csg.coinblesk.client.ui.payment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.request.ExchangeRateRequestTask;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class is a view to send bitcoins from the system to the inserted
 * bitcoin-address.
 */
public class PayOutActivity extends WalletActivity {
    public static BigDecimal exchangeRate;
    private BigDecimal payOutAmount;
    private Button acceptBtn;
    private Button allBtn;
    private Button scanQRButton;
    private EditText payoutAmountEditText;
    private EditText payoutAddress;

    private TextView btcBalance;
    private TextView chfBalance;
    private TextView chfAmount;

    private boolean walletConnected = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        walletConnected = true;

        CurrencyViewHandler.setBTC(btcBalance, getWalletService().getBalance(), getApplicationContext());
        CurrencyViewHandler.clearTextView(chfBalance);

        checkOnlineModeAndProceed();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);
        walletConnected = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_out);
        setScreenOrientation();

        getActionBar().setDisplayHomeAsUpEnabled(true);

        chfBalance = (TextView) findViewById(R.id.payOut_BalanceCHF);
        btcBalance = (TextView) findViewById(R.id.payOut_Balance);
        chfAmount = (TextView) findViewById(R.id.payOut_AmountCHF);

        acceptBtn = (Button) findViewById(R.id.payOut_payOutButton);
        allBtn = (Button) findViewById(R.id.payout_ButtonAll);
        payoutAmountEditText = (EditText) findViewById(R.id.payOut_Amount);
        payoutAddress = (EditText) findViewById(R.id.payOut_Address);
        scanQRButton = (Button) findViewById(R.id.payout_ButtonScanQR);

        initClickListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkOnlineModeAndProceed();
        invalidateOptionsMenu();
    }

    private void initClickListener() {

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ClientController.isConnectedToServer()) {
                    // hide virtual keyboard
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    launchPayOutRequest();
                }
            }
        });

        scanQRButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanQRAction();
            }

        });

        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(walletConnected) {
                    BigDecimal balance = getWalletService().getBalance();
                    BigDecimal amount = CurrencyViewHandler.getBTCAmountInDefinedUnit(balance, getApplicationContext());

                    // TODO: Subtract fee
                    payoutAmountEditText.setText(amount.toPlainString());
                }
            }
        });

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
    }


    private void scanQRAction() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    private void launchPayOutRequest() {
        if (!payoutAmountEditText.getText().toString().isEmpty() && !payoutAddress.getText().toString().isEmpty()) {
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

            dismissProgressDialog();

        } else {
            displayResponse(getResources().getString(R.string.fill_necessary_fields));
        }
    }

    private void checkOnlineModeAndProceed() {
        if (ClientController.isConnectedToServer()) {
            launchExchangeRateRequest();
        } else {
            acceptBtn.setEnabled(false);
            allBtn.setEnabled(false);
            scanQRButton.setEnabled(false);
        }
    }

    /**
     * Launches request for updating Exchange Rate
     */
    public void launchExchangeRateRequest() {
        if (ClientController.isConnectedToServer()) {
            showLoadingProgressDialog();
            RequestTask<TransferObject, TransferObject> request = new ExchangeRateRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
                public void onTaskComplete(TransferObject response) {
                    dismissProgressDialog();
                    if (response.isSuccessful()) {
                        exchangeRate = new BigDecimal(response.getMessage());
                        BigDecimal balance = getWalletService().getBalance();
                        CurrencyViewHandler.setExchangeRateView(exchangeRate, (TextView) findViewById(R.id.payout_exchangeRate));
                        CurrencyViewHandler.setToCHF(chfBalance, exchangeRate, balance);
                    } else {
                        exchangeRate = BigDecimal.ZERO;
                        displayResponse(response.getMessage());
                        chfBalance.setText("");
                    }
                }
            }, new TransferObject(), new TransferObject(), getApplicationContext());
            request.execute();
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
