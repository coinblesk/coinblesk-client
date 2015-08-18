package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.Currency;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.testutils.MockRequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 7/14/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class ExchangeTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testSell_fail() throws Exception {
        // try selling without credentials
        Exchange exchange = new Exchange(Exchange.BITSTAMP);

        Exchange.TradePlacedListener mockListener = Mockito.mock(Exchange.TradePlacedListener.class);
        exchange.sell(BigDecimal.ONE, mockListener);

        Robolectric.getBackgroundThreadScheduler().runOneTask();

        Mockito.verify(mockListener, Mockito.times(1)).onFail(Mockito.anyString());
        Mockito.verify(mockListener, Mockito.times(0)).onSuccess();
    }

    @Test
    public void testGetExchangeRate() throws Exception {

        Context mockContext = mockExchangeRateRequest();

        Exchange exchange = new Exchange(Exchange.BITSTAMP);
        exchange.getBTCExchangeRateInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, mockContext);

        Robolectric.getBackgroundThreadScheduler().runOneTask();

        // try again, this should load the exchange rate from the cache.
        exchange.getBTCExchangeRateInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, Mockito.mock(Context.class));

        Robolectric.getBackgroundThreadScheduler().runOneTask();


        // do the same with another exchange that has other currencies
        exchange = new Exchange(Exchange.KRAKEN);
        exchange.getBTCExchangeRateInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, mockContext);

        Robolectric.getBackgroundThreadScheduler().runOneTask();

        // try again, this should load the exchange rate from the cache.
        exchange.getBTCExchangeRateInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, Mockito.mock(Context.class));

    }

    private Context mockExchangeRateRequest() {
        // mock the request factory to obtain a mock exchange rate
        DefaultRequestFactory requestFactory = new DefaultRequestFactory() {
            @Override
            public RequestTask<TransferObject, ExchangeRateTransferObject> exchangeRateRequest(String symbol, RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) {
                ExchangeRateTransferObject exchangeRate = new ExchangeRateTransferObject();
                exchangeRate.setExchangeRate(Currency.CHF, "0.93");
                exchangeRate.setSuccessful(true);
                return new MockRequestTask<>(cro, exchangeRate);
            }
        };

        Context mockContext = Mockito.mock(Context.class);
        CoinBleskApplication mockApplication = Mockito.mock(CoinBleskApplication.class);
        Mockito.doReturn(requestFactory).when(mockApplication).getRequestFactory();
        Mockito.doReturn(mockApplication).when(mockContext).getApplicationContext();

        return mockContext;
    }


}