package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;

import com.xeiam.xchange.ExchangeSpecification;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.testutils.MockRequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.customserialization.Currency;
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

    }

    @Test
    public void testGetBalance_BitStamp() throws Exception {

        Context mockContext = mockExchangeRateRequest();

        ExchangeSpecification exSpec = new ExchangeSpecification(Exchange.BITSTAMP);
        exSpec.setApiKey("0YUEhzlBvRxq8jm3JpagY9V1yzD3nUtm");
        exSpec.setSecretKey("70IUUSfqQMDqgu4cDZWm5NrmcF9glF5M");
        exSpec.setUserName("75802");

        Exchange exchange = new Exchange(exSpec);
        exchange.getBalanceInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, mockContext);
    }

    @Test
    public void testGetBalance_Kraken() throws Exception {

        Context mockContext = mockExchangeRateRequest();

        ExchangeSpecification exSpec = new ExchangeSpecification(Exchange.KRAKEN);
        exSpec.setApiKey("+l/9WrTKSKRaEVGQuhr/th+F+f+Mm3Zs/Zp8C2LhbsdAMsdsL1zeafnE");
        exSpec.setSecretKey("YX1aHqx233mtMP/zrE4Fntrybi72Op/BPn9ZQyxfjohLY8CTBV55+k9sTQbIiOiSeKgPQc9dABJ+Tyz/Ww6r4w==");
        exSpec.setUserName("omnibrain");

        Exchange exchange = new Exchange(exSpec);
        exchange.getBalanceInDefaultCurrency(new RequestCompleteListener<ExchangeRateTransferObject>() {
            @Override
            public void onTaskComplete(ExchangeRateTransferObject response) {
                System.out.println(response.toJson());
                Assert.assertNotNull(response);
                Assert.assertTrue(response.isSuccessful());
                Assert.assertFalse(response.getExchangeRates().isEmpty());
                Assert.assertTrue(new BigDecimal(response.getExchangeRates().values().iterator().next()).signum() > 0);
            }
        }, mockContext);
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