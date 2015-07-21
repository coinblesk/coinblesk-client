package ch.uzh.csg.coinblesk.client.exchange;

import android.content.Context;

import com.xeiam.xchange.currency.Currencies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;

/**
 * Created by rvoellmy on 7/14/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
public class ExchangeTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testSell_fail() throws Exception {
        // try selling without credentials
        Exchange exchange = new Exchange(Exchange.BITSTAMP);

        Exchange.TradePlacedListener mockListener = Mockito.mock(Exchange.TradePlacedListener.class);
        exchange.sell(BigDecimal.ONE, mockListener, Mockito.mock(Context.class));

        Robolectric.getBackgroundThreadScheduler().runOneTask();

        Mockito.verify(mockListener, Mockito.times(1)).onFail(Mockito.anyString());
        Mockito.verify(mockListener, Mockito.times(0)).onSuccess();
    }

    @Test
    public void testGetExchangeRate() throws Exception {
        Exchange exchange = new Exchange(Exchange.BITSTAMP);
        BigDecimal exchangeRate = exchange.getExchangeRate(Currencies.CHF);
        Assert.assertEquals(1, exchangeRate.signum());
        System.out.println(exchangeRate);
    }
}