package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.google.common.util.concurrent.Service;

import junit.framework.Assert;

import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.persistence.PersistentStorageHandler;
import testutils.BitcoinTestUtils;

/**
 * Created by rvoellmy on 7/4/15.
 */
public class BlockChainSyncAlarmReceiverTest extends AndroidTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // preapre internal storage
        PersistentStorageHandler storage = new PersistentStorageHandler(getContext());
        storage.setBitcoinNet(BitcoinNet.TESTNET);
        storage.setWatchingKey(BitcoinTestUtils.getServerWatchingKey(TestNet3Params.get()));
        ((CoinBleskApplication) getContext().getApplicationContext()).setStorageHandler(storage);
    }

    public void testBlockChainSyncAlarmReceiver() throws Exception {

        BlockChainSyncAlarmReceiver blockChainSynReceiver = new BlockChainSyncAlarmReceiver();
        Intent i = new Intent();
        Context context = getContext();
        blockChainSynReceiver.onReceive(context, i);

        Thread.sleep(5000);
        Assert.assertNotNull(blockChainSynReceiver.getWalletService());

        // check state
        Service.State state = blockChainSynReceiver.getWalletService().getService().state();
        Assert.assertTrue(state == state.STARTING || state == Service.State.RUNNING);
    }

}