package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.xeiam.xchange.ExchangeSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

/**
 * Created by rvoellmy on 7/21/15.
 */
public class ExchangeManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeManager.class);

    private Set<Exchange> exchanges;
    private Context context;
    private boolean merchantModeActivated;

    private Set<String> primaryExchangeKeys;

    public ExchangeManager(Context context) {
        this.exchanges = new HashSet<>();
        this.context = context;

        this.primaryExchangeKeys = new HashSet<>();
        primaryExchangeKeys.add("merchant_mode");
        primaryExchangeKeys.add("primary_exchange");
        primaryExchangeKeys.add("primary_exchange_username");
        primaryExchangeKeys.add("primary_exchange_api_key");
        primaryExchangeKeys.add("primary_exchange_api_secret");

        // add default exchange without credentials for exchange rate
        addExchange(new Exchange(Exchange.KRAKEN));
    }

    /**
     * Adds an exchange to the list of set up exchanges. The exchange first added is the primary exchange.
     * All later added exchanges are fallback exchanges, meaning they will only be used if the primary exchange failed.
     *
     * @param exchange the exchange to add to the list of exchanges
     */
    public void addExchange(Exchange exchange) {
        // remove the existing exchange in case we already have it configured
        exchanges.remove(exchange);

        // add the exchange
        exchanges.add(exchange);
    }


    private void sell(final Iterator<Exchange> exchangeIterator, final BigDecimal amount) throws IOException {

        if (!exchangeIterator.hasNext()) {
            LOGGER.warn("All exchanges failed!");
            Toast.makeText(context, context.getString(R.string.merchantMode_toast_sellingFailed), Toast.LENGTH_LONG);
            return;
        }

        exchangeIterator.next().sell(amount, new Exchange.TradePlacedListener() {
            @Override
            public void onSuccess() {
                // great, nothing to do...
                LOGGER.info("Selling bitcoins succeeded");
            }

            @Override
            public void onFail(String msg) {
                try {
                    LOGGER.error("Failed selling bitcoins: {}", msg);
                    sell(exchangeIterator, amount);
                } catch (IOException e) {
                    LOGGER.error("failed selling BTC", e);
                }
            }
        });
    }

    public void sell(BigDecimal amount) {

        if (merchantModeActivated && exchanges.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.merchantMode_toast_merchantModeActiveButNoExchanges), Toast.LENGTH_LONG);
        }

        try {
            sell(exchanges.iterator(), amount);
        } catch (IOException e) {
            LOGGER.error("failed to sell BTC on exchange", e);
        }
    }

    public boolean merchantModeActive() {
        return merchantModeActivated;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("merchant_mode")) {
            merchantModeActivated = sharedPreferences.getBoolean("merchant_mode", false);
        }

        if (primaryExchangeKeys.contains(key)) {
            addExchange(sharedPreferences);
        }
    }


    private void addExchange(SharedPreferences sharedPreferences) {
        // primary exchange settings changes, check if settings complete
        for (String pref : primaryExchangeKeys) {
            if (!sharedPreferences.contains(pref)) return;
        }

        // all necessary values are there -> set up exchange
        CoinBleskApplication application = (CoinBleskApplication) context.getApplicationContext();

        // determine exchange id
        String exchange;
        switch (sharedPreferences.getString("primary_exchange", "").toLowerCase()) {
            case "kraken":
                exchange = Exchange.KRAKEN;
                break;
            case "bitstamp":
                exchange = Exchange.BITSTAMP;
                break;
            default:
                return;
        }

        // set the credentials
        ExchangeSpecification exSpec = new ExchangeSpecification(exchange);
        exSpec.setUserName(sharedPreferences.getString("primary_exchange_username", ""));
        exSpec.setApiKey(sharedPreferences.getString("primary_exchange_api_key", ""));
        exSpec.setSecretKey(sharedPreferences.getString("primary_exchange_api_secret", ""));

        // create exchange
        application.getMerchantModeManager().addExchange(new Exchange(exSpec));
    }

    private void getExchangeRate(final Iterator<Exchange> exchangeIterator, final RequestCompleteListener<ExchangeRateTransferObject> rcl) {

        if (!exchangeIterator.hasNext()) {
            // all exchanges failed
            ExchangeRateTransferObject transferObject = new ExchangeRateTransferObject();
            transferObject.setSuccessful(false);
            transferObject.setMessage("Failed to obtain exchange rate");
            rcl.onTaskComplete(transferObject);
            return;
        }

        exchangeIterator.next().getBTCExchangeRateInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                if (response.isSuccessful()) {
                    rcl.onTaskComplete(response);
                } else {
                    getExchangeRate(exchangeIterator, rcl);
                }
            }
        }, context);

    }

    public void getExchangeRate(final RequestCompleteListener<ExchangeRateTransferObject> rcl) {
        getExchangeRate(exchanges.iterator(), rcl);
    }
}
