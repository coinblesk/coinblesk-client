package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;
import android.os.AsyncTask;

import com.google.common.base.Preconditions;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitstamp.BitstampExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.kraken.KrakenExchange;
import com.xeiam.xchange.oer.OERExchange;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;
import com.xeiam.xchange.service.polling.trade.PollingTradeService;

import java.io.IOException;
import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;

/**
 * Created by rvoellmy on 7/14/15.
 */
public class Exchange {

    public interface TradePlacedListener {
        void onSuccess();

        void onFail(String msg);
    }

    public static final String BITSTAMP = BitstampExchange.class.getName();
    public static final String KRAKEN = KrakenExchange.class.getName();

    private com.xeiam.xchange.Exchange exchange;
    private com.xeiam.xchange.Exchange forex;
    private CurrencyPair pair;

    public Exchange(String exchange) {
        this.exchange = ExchangeFactory.INSTANCE.createExchange(exchange);

        // set the currency in this priority: USD, EUR
        for (CurrencyPair pair : this.exchange.getMetaData().getCurrencyPairs()) {
            if (pair.equals(CurrencyPair.BTC_USD)) {
                this.pair = pair;
                break;
            }
            if (pair.equals(CurrencyPair.BTC_EUR)) {
                this.pair = pair;
                break;
            }
        }
        Preconditions.checkNotNull(this.pair, "Only exchanges that trade BTC against USD or EUR are currently supported");

        // create the forex exchange
        this.forex = ExchangeFactory.INSTANCE.createExchange(OERExchange.class.getName());
    }

    public Exchange(String exchange, ExchangeSpecification exSpec) {
        this.exchange = ExchangeFactory.INSTANCE.createExchange(exchange);
        this.exchange.applySpecification(exSpec);
    }

    public void sell(BigDecimal amount, final TradePlacedListener listener, Context context) throws IOException {

        if (!hasCredentials()) {
            listener.onFail("Selling bitcoins is not possible without authentication.");
            return;
        }

        final CoinBleskApplication application = (CoinBleskApplication) context.getApplicationContext();

        AsyncTask<BigDecimal, Void, Void> placeOrderTask = new AsyncTask<BigDecimal, Void, Void>() {
            @Override
            protected Void doInBackground(BigDecimal... amount) {

                Preconditions.checkState(amount.length == 1, "Amount no speciefied for sell order.");

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
     * @param currencySymbol the currency to get the exchange rate to
     * @return the current exchange rate
     * @throws IOException if the connection failed
     */
    public BigDecimal getExchangeRate(String currencySymbol) throws IOException {
        PollingMarketDataService forexDataService = forex.getPollingMarketDataService();
        BigDecimal currentBid = getExchangeRate();
        BigDecimal currentExchangeRate = forexDataService.getTicker(new CurrencyPair(currencySymbol, pair.counterSymbol)).getLast();
        return currentBid.multiply(currentExchangeRate);
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
    public BigDecimal getBalance() throws IOException {
        return exchange.getPollingAccountService().getAccountInfo().getBalance(pair.counterSymbol);
    }

    /**
     * @return true if the exchange was set up with credentials, meaning trading is possible.
     */
    public boolean hasCredentials() {
        return exchange.getExchangeSpecification() != null;
    }
}
