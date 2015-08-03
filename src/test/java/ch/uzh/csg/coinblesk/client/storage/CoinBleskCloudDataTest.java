package ch.uzh.csg.coinblesk.client.storage;

import android.content.Context;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboSharedPreferences;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;

/**
 * Created by rvoellmy on 8/1/15.
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "javax.crypto.*"})
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class CoinBleskCloudDataTest {

    private CoinBleskCloudData data;

    @Before
    public void setUp() throws Exception {

        Context mockContext = Mockito.mock(Context.class);
        Mockito.doReturn(new RoboSharedPreferences(new HashMap<String, Map<String, Object>>(), CoinBleskCloudData.COINBLESK_PREFS_BACKUP_KEY, Context.MODE_PRIVATE)).when(mockContext).getSharedPreferences(Mockito.anyString(), Mockito.anyInt());

        data = new CoinBleskCloudData(mockContext);
    }

    @Test
    public void testSaveAndLoadKeyPair() throws Exception {
        KeyPair keyPair = PaymentProtocol.generateKeys();

        long start = System.currentTimeMillis();
        data.setKeyPair(keyPair);
        KeyPair restoredKeyPair = data.getKeyPair();
        System.out.println("saving and restoring took " + (System.currentTimeMillis() - start) + "ms");

        Assert.assertEquals(keyPair.getPrivate(), restoredKeyPair.getPrivate());
        Assert.assertEquals(keyPair.getPublic(), restoredKeyPair.getPublic());
    }
}