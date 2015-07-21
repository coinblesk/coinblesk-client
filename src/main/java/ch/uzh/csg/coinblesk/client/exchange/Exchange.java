package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;
import android.os.AsyncTask;

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
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;
import com.xeiam.xchange.service.polling.trade.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.customserialization.Currency;
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

    public Exchange(String exchange) {

        // set up exchange and determine the currency pair
        this.exchange = ExchangeFactory.INSTANCE.createExchange(exchange);
        setCurrencyPair(this.exchange);

        // build the cache for the exchange rates
        this.forexCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
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
                this.pair = pair;
                break;
            }
            if (pair.equals(CurrencyPair.BTC_EUR) || pair.equals(new CurrencyPair(Currencies.XBT, Currencies.EUR))) {
                this.pair = pair;
                break;
            }
        }
        Preconditions.checkNotNull(this.pair, "Only exchanges that trade BTC against USD or EUR are currently supported");
    }

    public void sell(BigDecimal amount, final TradePlacedListener listener) throws IOException {

        if (!hasCredentials()) {
            listener.onFail("Selling bitcoins is not possible without authentication.");
            return;
        }

        AsyncTask<BigDecimal, Void, Void> placeOrderTask = new AsyncTask<BigDecimal, Void, Void>() {
            @Override
            protected Void doInBackground(BigDecimal... amount) {

                Preconditions.checkState(amount.length == 1, "Amount not speciefied for sell order.");

                try {
                    PollingTradeService tradeService = exchange.getPollingTradeService();

                    // we are placing a sell order at the price (currrentAsk - 5%). This will make sure that the trade is executed,
                    // while making sure we will never lose sell for less than 95% of our expected value. This means that in
                    // theory it is possible that a trade never gets executed, if the market suddenly crashes.
                    BigDecimal limitPrice = getExchangeRate().multiply(new BigDecimal("0.95"));

                    LimitOrder order = new LimitOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_USD).tradableAmount(amount[0]).limitPrice(limitPrice).build();
                    tradeService.placeLimitOrder(order);
                    listener.onSuccess();
                } catch (Exception e) {
                    listener.onFail(e.getMessage());
                    return null;
                }
                return null;
            }
        };
        placeOrderTask.execute(amount);

    }

    /**
     * Gets the current balance of the specified exchange, in the default currency specified by the server (e.g. CHF).
     * @param cro
     * @param context
     */
    public void getBalanceInDefaultCurrency(final RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) throws IOException {
        BigDecimal balance = getBalance();
        getExchangeRateInDefaultCurrency(balance, cro, context);
    }

    /**
     *
     * @param amount the amount to convert to the default currency (e.g. CHF)
     * @param cro
     * @param context
     * @throws IOException
     */
    private void getExchangeRateInDefaultCurrency(final BigDecimal amount, final RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) throws IOException {

        // forex exchange rate
        SymbolAndExchangeRate exchangeRateAndCurrency = forexCache.getIfPresent(pair.counterSymbol);
        if(exchangeRateAndCurrency != null) {
            // found in cache: execute listener
            ExchangeRateTransferObject exchangeRateObj = new ExchangeRateTransferObject();
            exchangeRateObj.setExchangeRate(exchangeRateAndCurrency.currency, exchangeRateAndCurrency.exchangeRate.multiply(amount).toString());
            exchangeRateObj.setSuccessful(true);
            cro.onTaskComplete(exchangeRateObj);
            return;
        }

        // exchange rate not saved in cache: get it from server
        final CoinBleskApplication application = (CoinBleskApplication) context.getApplicationContext();
        RequestFactory requestFactory = application.getRequestFactory();
        RequestTask<TransferObject, ExchangeRateTransferObject> exchangeRateRequest = requestFactory.exchangeRateRequest(pair.counterSymbol, new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                Currency symbol = response.getExchangeRates().keySet().iterator().next();
                BigDecimal forexExchangeRate = new BigDecimal(response.getExchangeRates().values().iterator().next());
                BigDecimal btcExchangeRateInBaseCurrency = forexExchangeRate.multiply(amount);

                ExchangeRateTransferObject exchangeRateObj = new ExchangeRateTransferObject();
                exchangeRateObj.setExchangeRate(symbol, btcExchangeRateInBaseCurrency.toString());

                // save forex exchange rate in cache
                forexCache.put(pair.counterSymbol, new SymbolAndExchangeRate(symbol, forexExchangeRate));

                // execute listener
                exchangeRateObj.setSuccessful(true);
                cro.onTaskComplete(exchangeRateObj);
            }
        }, context);
        exchangeRateRequest.execute();
    }

    /**
     * @return the current exchange rate of 1 BTC on the specified exchange, in the default currency specified by the server (e.g. CHF).
     * @throws IOException if the connection failed
     */
    public void getBTCExchangeRateInDefaultCurrency(final RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) throws IOException {

        // bitcoin exchange rate
        final BigDecimal currentBtcBid = getExchangeRate();

        getExchangeRateInDefaultCurrency(currentBtcBid, cro, context);

    }

    /**
     * @return the current bid price of this market
     * @throws IOException if the connection to the exchange failed
     */
    private BigDecimal getExchangeRate() throws IOException {
        PollingMarketDataService marketDataService = exchange.getPollingMarketDataService();
        return marketDataService.getTicker(pair).getBid();
    }

    /**
     * @return the current balance on this exchange
     * @throws IOException
     */
    private BigDecimal getBalance() throws IOException {
        if(!hasCredentials()) {
            LOGGER.error("Tried to get balance of exchange without credentials");
            return BigDecimal.ZERO;
        }
        return exchange.getPollingAccountService().getAccountInfo().getBalance(pair.counterSymbol);
    }

    /**
     * @return true if the exchange was set up with credentials, meaning trading is possible.
     */
    public boolean hasCredentials() {
        return exchange.getExchangeSpecification() != null;
    }
}
