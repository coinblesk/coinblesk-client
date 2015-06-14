package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import com.google.common.util.concurrent.Service;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.Set;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.testutils.TestUtils;

/**
 * Created by rvoellmy on 6/7/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({WalletService.class})
@PowerMockIgnore({"javax.crypto.*"})
public class WalletServiceTest {

    private static final BitcoinNet bitcoinNet = BitcoinNet.UNITTEST;
    private static final NetworkParameters params = UnitTestParams.get();

    private static final File testDirectory = new File("testdirectory");

    private WalletService walletService;

    private static BlockStore blockStore;

    private static String serverWatchingKey;

    @BeforeClass
    public static void setupClass() throws Exception {
        TestUtils.configureLogger(Level.INFO);

        // create server watching key
        String mnemonic = "actor critic filter assist load now age strike right certain column paddle";
        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        kcg.createAndActivateNewHDChain();
        System.out.println(kcg.getActiveKeyChain().getMnemonicCode());
        DeterministicKey watchingKey = kcg.getActiveKeyChain().getWatchingKey();
        serverWatchingKey = watchingKey.serializePubB58(params);

        if(!testDirectory.exists()) {
            testDirectory.mkdirs();
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        for (File f : testDirectory.listFiles()) {
            f.delete();
        }
        testDirectory.delete();
    }

    @Before
    public void setUp() throws Exception {

        // create a fake block store
        blockStore = new MemoryBlockStore(params);

        // make sure there are no wallet files still around...
        cleanTestDirectory();

        walletService = PowerMockito.spy(new WalletService());

        // mock methods
        PowerMockito.doReturn(testDirectory).when(walletService).getFilesDir();
        PowerMockito.doReturn(new FileInputStream("src/main/res/raw/checkpoints_testnet")).when(walletService, "getCheckpoints", BitcoinNet.TESTNET);
        PowerMockito.doReturn(new FileInputStream("src/main/res/raw/checkpoints")).when(walletService, "getCheckpoints", bitcoinNet.MAINNET);

        // mock android context
        Context mockContext = PowerMockito.mock(Context.class);
        PowerMockito.doReturn(mockContext).when(walletService).getApplicationContext();

    }

    @After
    public void tearDown() {
        System.out.println("Cleaning up...");
        walletService.stop();
        cleanTestDirectory();
    }

    @Test
    public void testInit() {
        // test wallet service for racing conditions
        System.out.println("Starting service");
        Service service = walletService.init(bitcoinNet, serverWatchingKey);
        System.out.println("Starting service again...");
        walletService.init(bitcoinNet, serverWatchingKey);

        service.awaitRunning();
        Assert.assertTrue(service.isRunning());
    }

    @Test
    public void testGetBalance() throws Exception {
        walletService.init(bitcoinNet, serverWatchingKey);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(walletService.getBalance()));
        injectTx(walletService.getBitcoinAddress(), BigDecimal.ONE);
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(walletService.getBalance()));

    }

    @Test
    public void testWalletExistsOnDevice() throws Exception {
        Assert.assertFalse(walletService.walletExistsOnDevice(bitcoinNet));
        walletService.init(bitcoinNet, serverWatchingKey, null, null);
        walletService.getService().awaitRunning();
        Assert.assertTrue(walletService.walletExistsOnDevice(bitcoinNet));
    }

    @Test
    public void testRestoreWalletFromSeed() throws Exception {

        walletService.init(bitcoinNet, serverWatchingKey);
        String mnemonic = walletService.getMnemonicSeed();

        // send a coin to the wallet
        FakeTxBuilder.BlockPair bp = injectTx(walletService.getBitcoinAddress(), BigDecimal.ONE);
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(walletService.getBalance()));

        Set<Transaction> txsBeforeRestore = extractAppKit(walletService).wallet().getTransactions(false);

        // delete the wallet and restore it again
        walletService.stop();
        removeWalletFiles();

        Service service = walletService.restoreWalletFromSeed(bitcoinNet, serverWatchingKey, mnemonic, 0L);
        service.awaitRunning();
        Assert.assertEquals(mnemonic, walletService.getMnemonicSeed());
        Assert.assertTrue(extractAppKit(walletService).wallet().getActiveKeychain().isMarried());

        // simulate the receiving of a block
        extractAppKit(walletService).chain().add(bp.block);
        Assert.assertEquals(params.getGenesisBlock().getHash(), bp.block.getPrevBlockHash());

        System.out.println("balance: " + walletService.getUnconfirmedBalance());
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(walletService.getBalance()));

        Set<Transaction> txsAfterRestore = extractAppKit(walletService).wallet().getTransactions(false);
        Assert.assertEquals(txsAfterRestore, txsBeforeRestore);
    }

    @Test
    public void testOnDestroy() throws Exception {
        walletService.onDestroy();
        walletService.init(bitcoinNet, serverWatchingKey);
        walletService.getBalance(); // blocking call
        Assert.assertTrue(walletService.getService().isRunning());
    }

    @Test
    public void testGetBitcoinAddress() throws Exception {
        walletService.init(bitcoinNet, serverWatchingKey);

        String addrString = walletService.getBitcoinAddress();
        Address addr = new Address(params, addrString);
        Assert.assertTrue(addr.isP2SHAddress());
    }

    private FakeTxBuilder.BlockPair injectTx(String address, BigDecimal amount) throws Exception {

        Coin coin = Coin.parseCoin(amount.toString());
        Address addr = new Address(params, address);
        Transaction tx = FakeTxBuilder.createFakeTx(params, coin, addr);

        CoinBleskWalletAppKit appKit = Whitebox.invokeMethod(walletService, "getAppKit");

        FakeTxBuilder.BlockPair bp = FakeTxBuilder.createFakeBlock(blockStore, tx);

        appKit.wallet().receiveFromBlock(tx, bp.storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
        appKit.wallet().notifyNewBestBlock(bp.storedBlock);
        appKit.wallet().getTransaction(tx.getHash());  // Can be null if tx is a double spend that's otherwise irrelevant.

        return bp;

    }

    private CoinBleskWalletAppKit extractAppKit(WalletService walletService) throws Exception {
        return Whitebox.invokeMethod(walletService, "getAppKit");
    }

    private void removeWalletFiles() {

        File[] files = testDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(walletService.getWalletFilesPrefix(bitcoinNet)) && filename.endsWith(".wallet");
            }
        });

        deleteFiles(files);

    }

    private void cleanTestDirectory() {
        deleteFiles(testDirectory.listFiles());
    }

    private void deleteFiles(File[] files) {
        for (File f : files) {
            System.out.println("Deleted file " + f.getName());
            f.delete();
        }
    }
}