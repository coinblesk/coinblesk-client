package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitstamp.BitstampExchange;
import com.xeiam.xchange.currency.Currencies;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.kraken.KrakenExchange;
import com.xeiam.xchange.service.polling.trade.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import ch.uzh.csg.coinblesk.Currency;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 7/14/15.
 */
public class Exchange {

    private final static Logger LOGGER = LoggerFactory.getLogger(Exchange.class);

    public interface TradePlacedListener {
        void onSuccess();

        void onFail(String msg);
    }

    public interface ExchangeRateListener {
        void onSuccess(BigDecimal exchangeRate);

        void onError(String msg);
    }

    private class SymbolAndExchangeRate {
        public SymbolAndExchangeRate(Currency currency, BigDecimal exchangeRate) {
            this.currency = currency;
            this.exchangeRate = exchangeRate;
        }

        public Currency currency;
        public BigDecimal exchangeRate;
    }

    public static final String BITSTAMP = BitstampExchange.class.getName();
    public static final String KRAKEN = KrakenExchange.class.getName();

    private com.xeiam.xchange.Exchange exchange;
    private CurrencyPair pair;

    private Cache<String, SymbolAndExchangeRate> forexCache;
    private Cache<String, BigDecimal> exchangeRateCache;

    public Exchange(String exchange) {

        // set up exchange and determine the currency pair
        this.exchange = ExchangeFactory.INSTANCE.createExchange(exchange);
        setCurrencyPair(this.exchange);

        // build the cache for the exchange rates
        this.forexCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
        this.exchangeRateCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
    }

    public Exchange(ExchangeSpecification exSpec) {
        this(exSpec.getExchangeClassName());
        ExchangeSpecification exSpecWithCredentials = this.exchange.getDefaultExchangeSpecification();
        exSpecWithCredentials.setApiKey(exSpec.getApiKey());
        exSpecWithCredentials.setSecretKey(exSpec.getSecretKey());
        exSpecWithCredentials.setUserName(exSpec.getUserName());
        this.exchange.applySpecification(exSpec);
    }

    private void setCurrencyPair(com.xeiam.xchange.Exchange exchange) {
        // set the currency in this priority: USD, EUR
        for (CurrencyPair pair : exchange.getMetaData().getCurrencyPairs()) {
            if (pair.equals(CurrencyPair.BTC_USD) || pair.equals(new CurrencyPair(Currencies.XBT, Currencies.USD))) {
                this.pair = CurrencyPair.BTC_USD;
                break;
            }
            if (pair.equals(CurrencyPair.BTC_EUR) || pair.equals(new CurrencyPair(Currencies.XBT, Currencies.EUR))) {
                this.pair = CurrencyPair.BTC_EUR;
                break;
            }
        }
        Preconditions.checkNotNull(this.pair, "Only exchanges that trade BTC against USD or EUR are currently supported");
    }

    /**
     * Sell bitcoins on the exchange
     *
     * @param amount   the amount of bitcoins to sell, in BTC
     * @param listener
     * @throws IOException
     */
    public void sell(final BigDecimal amount, final TradePlacedListener listener) throws IOException {

        if (!hasCredentials()) {
            listener.onFail("Selling bitcoins is not possible without authentication.");
            return;
        }

        getExchangeRate(new ExchangeRateListener() {
            @Override
            public void onSuccess(final BigDecimal exchangeRate) {

                AsyncTask<BigDecimal, Void, Void> placeOrderTask = new AsyncTask<BigDecimal, Void, Void>() {

                    @Override
                    protected Void doInBackground(final BigDecimal... amount) {

                        Preconditions.checkState(amount.length == 1, "Amount not speciefied for sell order.");

                        // we are placing a sell order at the price (currrentAsk - 5%). This will make sure that the trade is executed,
                        // while making sure we will never lose sell for less than 95% of our expected value. This means that in
                        // theory it is possible that a trade never gets executed, if the market suddenly crashes.
                        BigDecimal limitPrice = exchangeRate.multiply(new BigDecimal("0.95"));

                        try {
                            LimitOrder order = new LimitOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_USD).tradableAmount(amount[0]).limitPrice(limitPrice).build();
                            PollingTradeService tradeService = exchange.getPollingTradeService();
                            tradeService.placeLimitOrder(order);
                            listener.onSuccess();
                        } catch (Exception e) {
                            listener.onFail(e.getMessage());
                        }

                        return null;
                    }
                };
                placeOrderTask.execute(amount);

            }

            @Override
            public void onError(String msg) {
                // failed to get exchange rate from exchange
                listener.onFail(msg);
            }
        });


    }

    /**
     * Get the exchange rate of this exchange in the default currency. The default currency (e.g. CHF) is specified by the server.
     *
     * @param cro     the request task listener. Will hold the exchange rate if successful
     * @param context
     */
    public void getBTCExchangeRateInDefaultCurrency(final RequestCompleteListener<ExchangeRateTransferObject> cro, final Context context) {

        final ExchangeRateTransferObject exchangeRateObj = new ExchangeRateTransferObject();

        getExchangeRate(new ExchangeRateListener() {
            @Override
            public void onSuccess(final BigDecimal exchangeRate) {

                // load forex exchange rate from cache
                SymbolAndExchangeRate exchangeRateAndCurrency = forexCache.getIfPresent(pair.counterSymbol);
                if (exchangeRateAndCurrency != null) {
                    // found in cache: execute listener
                    exchangeRateObj.setExchangeRate(exchangeRateAndCurrency.currency, exchangeRateAndCurrency.exchangeRate.multiply(exchangeRate).toString());
                    exchangeRateObj.setSuccessful(true);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            cro.onTaskComplete(exchangeRateObj);
                        }
                    });

                    return;
                }

                // exchange rate no in cache -> load from server

                final CoinBleskApplication application = (CoinBleskApplication) context.getApplicationContext();
                RequestFactory requestFactory = application.getRequestFactory();

                RequestTask<TransferObject, ExchangeRateTransferObject> exchangeRateRequest = requestFactory.exchangeRateRequest(pair.counterSymbol, new RequestCompleteListener<ExchangeRateTransferObject>() {
                    @Override
                    public void onTaskComplete(ExchangeRateTransferObject response) {
                        if (response.isSuccessful()) {
                            Currency symbol = response.getExchangeRates().keySet().iterator().next();
                            BigDecimal forexExchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                            BigDecimal btcExchangeRateInBaseCurrency = forexExchangeRate.multiply(exchangeRate);
                            exchangeRateObj.setExchangeRate(symbol, btcExchangeRateInBaseCurrency.toString());

                            // save forex exchange rate in cache
                            forexCache.put(pair.counterSymbol, new SymbolAndExchangeRate(symbol, forexExchangeRate));

                            // execute listener
                            exchangeRateObj.setSuccessful(true);
                            cro.onTaskComplete(exchangeRateObj);
                        } else {
                            exchangeRateObj.setSuccessful(false);
                            exchangeRateObj.setMessage(response.getMessage());
                            cro.onTaskComplete(exchangeRateObj);
                        }
                    }
                }, context);

                exchangeRateRequest.execute();

            }

            @Override
            public void onError(String msg) {
                exchangeRateObj.setSuccessful(false);
                exchangeRateObj.setMessage(msg);
                cro.onTaskComplete(exchangeRateObj);
            }
        });

    }

    /**
     * Obtains the exchange rate from the primary exchange. If the request failed, the secondary exchange will be asked for the exchange rate.
     *
     * @param listener the exchange rate listener
     */
    private void getExchangeRate(final ExchangeRateListener listener) {

        BigDecimal exchangeRate = exchangeRateCache.getIfPresent(getExchangeId());
        if(exchangeRate != null) {
            LOGGER.debug("Loaded exchange rate from cache: {}", exchangeRate);
            listener.onSuccess(exchangeRate);
            return;
        }

        LOGGER.debug("Exchange rate not in cache, get it from exchange {}", getExchangeId());
        AsyncTask<Void, Void, BigDecimal> exchangeRateTask = new AsyncTask<Void, Void, BigDecimal>() {
            @Override
            protected BigDecimal doInBackground(Void... params) {
                try {
                    return exchange.getPollingMarketDataService().getTicker(pair).getBid();
                } catch (IOException e) {
                    LOGGER.error("failed to get exchange rate from {}: {}", getExchangeId(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(BigDecimal exchangeRate) {
                if (exchangeRate != null) {
                    listener.onSuccess(exchangeRate);
                    exchangeRateCache.put(getExchangeId(), exchangeRate);
                    LOGGER.debug("Saved exchange rate of exchange {} in cache", getExchangeId());
                } else {
                    listener.onError("Failed to obtain exchange rate from bitcoin exchange");
                }
            }
        };
        exchangeRateTask.execute();
    }

    /**
     * @return true if the exchange was set up with credentials, meaning trading is possible.
     */
    public boolean hasCredentials() {
        return exchange.getExchangeSpecification() != null;
    }

    /**
     * @return a unique id of the exchange
     */
    public String getExchangeId() {
        return exchange.getExchangeSpecification().getExchangeClassName();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Exchange) {
            return ((Exchange) o).getExchangeId().equals(getExchangeId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getExchangeId().hashCode();
    }
}
