package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;
import android.util.Base64;

import com.google.common.util.concurrent.Service;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.Set;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.persistence.MemoryStorageHandler;
import ch.uzh.csg.coinblesk.client.persistence.StorageHandler;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.testutils.MockRequestTask;
import ch.uzh.csg.coinblesk.client.testutils.TestUtils;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/7/15.
 */
@PrepareForTest({WalletService.class})
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "javax.crypto.*"})
@Config(manifest = "src/main/AndroidManifest.xml")
public class WalletServiceTest {

    private static final BitcoinNet bitcoinNet = BitcoinNet.UNITTEST;
    private static final NetworkParameters params = UnitTestParams.get();

    private static final File testDirectory = new File("testdirectory");

    private WalletService walletService;
    private static BlockStore blockStore;
    private StorageHandler storage;

    @BeforeClass
    public static void setupClass() throws Exception {
        TestUtils.configureLogger(Level.DEBUG);

        if (!testDirectory.exists()) {
            testDirectory.mkdirs();
        }

    }

    @AfterClass
    public static void tearDownClass() {
        cleanTestDirectory();
    }

    @Before
    public void setUp() throws Exception {

        // create a fake block store
        blockStore = new MemoryBlockStore(params);

        // make sure there are no wallet files still around...
        cleanTestDirectory();

        // build the service
        walletService = Robolectric.buildService(WalletService.class).bind().get();
        walletService = PowerMockito.spy(walletService);

        // mocks
        PowerMockito.doReturn(testDirectory).when(walletService).getFilesDir();

        // mock internal storage
        storage = PowerMockito.spy(new MemoryStorageHandler());
        PowerMockito.doReturn(TestUtils.getServerWatchingKey(params)).when(storage).getWatchingKey();
        PowerMockito.doReturn(bitcoinNet).when(storage).getBitcoinNet();

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
        Service service = walletService.init(storage);
        System.out.println("Starting service again...");
        walletService.init(storage);

        service.awaitRunning();
        Assert.assertTrue(service.isRunning());
    }

    @Test
    public void testGetBalance() throws Exception {
        walletService.init(storage);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(walletService.getBalance()));
        injectTx(walletService.getBitcoinAddress(), BigDecimal.ONE);
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(walletService.getBalance()));

    }

    @Test
    public void testWalletExistsOnDevice() throws Exception {
        Assert.assertFalse(walletService.walletExistsOnDevice(bitcoinNet));
        walletService.init(storage);
        walletService.getService().awaitRunning();
        Assert.assertTrue(walletService.walletExistsOnDevice(bitcoinNet));
    }

    @Test
    public void testCreatePayment() throws Exception {

        walletService.init(storage);
        String receiveAddress = walletService.getPrivateAddress();

        final BigDecimal fundingAmount = new BigDecimal("1.01");
        final BigDecimal sendAmount = BigDecimal.ONE;
        final Address receiveAddr = new Address(params, receiveAddress);

        RequestFactory requestFactory = new DefaultRequestFactory() {
            @Override
            public RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> payOutRequest(RequestCompleteListener<SignedTxTransferObject> completeListener, ServerSignatureRequestTransferObject input, Context context) {
                // check if the request is correct
                Assert.assertNotNull(input);
                Assert.assertNotNull(input.getPartialTx());
                Assert.assertNotNull(input.getIndexAndDerivationPaths());
                Assert.assertFalse(input.getIndexAndDerivationPaths().isEmpty());

                // assert that the partial tx is correct
                byte[] partialTxBytes = Base64.decode(input.getPartialTx(), android.util.Base64.NO_WRAP);
                Transaction tx = new Transaction(params, partialTxBytes);
                Assert.assertFalse(tx.isTimeLocked());
                Assert.assertFalse(tx.isAnyOutputSpent());

                // check if the correct amount was sent
                boolean outputFound = false;
                for (TransactionOutput txOut : tx.getOutputs()) {
                    Address outputAddr = txOut.getAddressFromP2PKHScript(params);

                    if (outputAddr == null) {
                        // output to P2SH
                        continue;
                    }

                    if (txOut.getAddressFromP2PKHScript(params).equals(receiveAddr)) {
                        outputFound = true;
                        Assert.assertEquals(0, txOut.getValue().compareTo(Coin.parseCoin(sendAmount.toString())));
                    }
                }
                Assert.assertTrue("Output was not found in the generated transaction", outputFound);

                TransferObject response = new TransferObject();
                response.setSuccessful(true);

                return new MockRequestTask(completeListener, response);
            }
        };

        ((CoinBleskApplication) RuntimeEnvironment.application).setRequestFactory(requestFactory);

        // send a coin to the wallet
        injectTx(walletService.getBitcoinAddress(), fundingAmount);
        Assert.assertEquals(0, fundingAmount.compareTo(walletService.getBalance()));

        // create a transaction
        BigDecimal balanceBeforePayment = walletService.getBalance();
        walletService.createPayment(receiveAddr.toString(), sendAmount);

        Assert.assertTrue(walletService.getBalance().compareTo(balanceBeforePayment) < 0);

        // try sending with insufficient funds
        boolean insufficientMoney = false;
        try {
            walletService.createPayment(receiveAddr.toString(), sendAmount);
        } catch (InsufficientMoneyException e) {
            insufficientMoney = true;
        }
        Assert.assertTrue(insufficientMoney);

    }

    @Test
    public void testCreateRefundTx() throws Exception {

        BigDecimal totalAmount = BigDecimal.TEN;
        Mockito.doNothing().when(walletService).checkRefundTxState();

        ((CoinBleskApplication) RuntimeEnvironment.application).setStorageHandler(storage);
        walletService.init(storage);

        // mock server response
        RequestFactory requestFactory = new DefaultRequestFactory() {
            @Override
            public RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundTxRequest(RequestCompleteListener<RefundTxTransferObject> completeListener, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context) {

                Transaction tx = new Transaction(params, Base64.decode(input.getPartialTx(), Base64.NO_WRAP));
                Assert.assertTrue(tx.isTimeLocked());

                RefundTxTransferObject response = new RefundTxTransferObject();
                response.setSuccessful(true);
                response.setRefundTx(input.getPartialTx()); // only partially signed but doesn't matter for the test

                return new MockRequestTask<>(completeListener, response);
            }
        };
        ((CoinBleskApplication) RuntimeEnvironment.application).setRequestFactory(requestFactory);

        // check preconditions
        Assert.assertEquals(storage.getRefundTxValidBlock(), RefundTx.NO_REFUND_TX_VALID_BLOCK);
        Assert.assertNull(storage.getRefundTx());

        // send some coins to the wallet
        String addr = walletService.getBitcoinAddress();
        injectTx(addr, totalAmount);

        Thread.sleep(1000);
        ShadowApplication.runBackgroundTasks();
        Thread.sleep(1000);

        Assert.assertNotSame(storage.getRefundTxValidBlock(), RefundTx.NO_REFUND_TX_VALID_BLOCK);

        Assert.assertNotNull(storage.getRefundTx());
        Transaction refundTx = new Transaction(params, Base64.decode(storage.getRefundTx(), Base64.NO_WRAP));
        Assert.assertTrue(refundTx.isTimeLocked());



    }

    @Test
    public void testGetPrivateAddress() throws Exception {
        walletService.init(storage);
        String mnemonic = walletService.getMnemonicSeed();
        Address addr = new Address(params, walletService.getPrivateAddress());

        // check that the address is not a P2SH address
        Assert.assertFalse(addr.isP2SHAddress());
        // Check that the wallet is still in married mode
        Assert.assertTrue(new Address(params, walletService.getBitcoinAddress()).isP2SHAddress());

        // test that address generated from seed is the same as the generated private address
        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
        DeterministicKeyChain privateChain = DeterministicKeyChain.builder().seed(seed).build();
        Address addr2 = privateChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(params);

        Assert.assertEquals(addr, addr2);

        // Check if we can send coins without server
        FakeTxBuilder.BlockPair bp = injectTx(addr.toString(), BigDecimal.TEN);
        Assert.assertEquals(0, BigDecimal.TEN.compareTo(walletService.getBalance()));
        walletService.createPayment("n4eY3qiP9pi32MWC6FcJFHciSsfNiYFYgR", BigDecimal.ONE);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(walletService.getBalance()));

    }

    @Test
    public void testRestoreWalletFromSeed() throws Exception {

        walletService.init(storage);
        String mnemonic = walletService.getMnemonicSeed();
        String address = walletService.getBitcoinAddress();

        // send a coin to the wallet
        FakeTxBuilder.BlockPair bp = injectTx(walletService.getBitcoinAddress(), BigDecimal.ONE);
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(walletService.getBalance()));

        Set<Transaction> txsBeforeRestore = extractAppKit(walletService).wallet().getTransactions(false);

        // delete the wallet and restore it again
        walletService.stop();
        removeWalletFiles();

        Service service = walletService.restoreWalletFromSeed(storage, mnemonic, 0L);
        service.awaitRunning();
        Assert.assertEquals(mnemonic, walletService.getMnemonicSeed());
        Assert.assertEquals(address, walletService.getBitcoinAddress());
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
        walletService.init(storage);
        walletService.getBalance(); // blocking call
        Assert.assertTrue(walletService.getService().isRunning());
    }

    @Test
    public void testGetBitcoinAddress() throws Exception {
        walletService.init(storage);

        String addrString = walletService.getBitcoinAddress();
        Address addr = new Address(params, addrString);
        Assert.assertTrue(addr.isP2SHAddress());
    }

    @Test
    public void testCheckRefundTxState_noBalance() throws Exception {

        Mockito.doNothing().when(walletService).broadcastRefundTx();
        Mockito.doNothing().when(walletService).createRefundTransaction();
        Mockito.doNothing().when(walletService).redeposit();
        ((CoinBleskApplication) RuntimeEnvironment.application).setStorageHandler(storage);

        walletService.init(storage);
        walletService.getService().awaitRunning();

        Mockito.verify(walletService, Mockito.times(0)).broadcastRefundTx();
        Mockito.verify(walletService, Mockito.times(0)).createRefundTransaction();
        Mockito.verify(walletService, Mockito.times(0)).redeposit();
    }

    @Test
    public void testCheckRefundTxState_noRefundTx() throws Exception {

        // mock the balance and internal storage
        PowerMockito.doReturn(null).when(storage).getRefundTx();
        PowerMockito.doReturn(-1L).when(storage).getRefundTxValidBlock();
        PowerMockito.doReturn(BigDecimal.ONE).when(walletService).getUnconfirmedBalance();
        ((CoinBleskApplication) RuntimeEnvironment.application).setStorageHandler(storage);


        Mockito.doNothing().when(walletService).broadcastRefundTx();
        Mockito.doNothing().when(walletService).createRefundTransaction();
        Mockito.doNothing().when(walletService).redeposit();

        walletService.init(storage);
        walletService.getService().awaitRunning();

        Thread.sleep(3000);

        Mockito.verify(walletService, Mockito.times(0)).broadcastRefundTx();
        Mockito.verify(walletService, Mockito.times(1)).createRefundTransaction();
        Mockito.verify(walletService, Mockito.times(0)).redeposit();

    }

    @Test
    public void testCheckRefundTxState_validRefundTransaction() throws Exception {

        // pretend we are in the middle of the "threshold phase"
        long mockCurrentBlockHeight = 100;
        long mockRefundTxValidBlock = 50;

        // mock the balance, block height and internal storage
        Mockito.doReturn(mockRefundTxValidBlock).when(storage).getRefundTxValidBlock();
        Mockito.doReturn(mockCurrentBlockHeight).when(walletService).getCurrentBlockHeight();
        Mockito.doReturn(BigDecimal.ONE).when(walletService).getUnconfirmedBalance();

        Mockito.doNothing().when(walletService).broadcastRefundTx();
        Mockito.doNothing().when(walletService).createRefundTransaction();
        Mockito.doNothing().when(walletService).redeposit();

        walletService.init(storage);
        walletService.getService().awaitRunning();

        Thread.sleep(3000);

        Mockito.verify(walletService, Mockito.times(1)).broadcastRefundTx();
        Mockito.verify(walletService, Mockito.times(0)).createRefundTransaction();
        Mockito.verify(walletService, Mockito.times(0)).redeposit();

    }


    @Test
    public void testCheckRefundTxState_refundTxInThreshold() throws Exception {

        // pretend we are in the middle of the "threshold phase"
        long mockRefundTxValidBlock = BitcoinUtils.monthsToBlocks(WalletService.REFUND_LOCKTIME_MONTH);
        long mockCurrentBlockHeight = mockRefundTxValidBlock - BitcoinUtils.monthsToBlocks(WalletService.REFUND_LOCKTIME_THRESHOLD) / 2;

        // mock the balance, block height and internal storage
        Mockito.doReturn(mockRefundTxValidBlock).when(storage).getRefundTxValidBlock();
        Mockito.doReturn(mockCurrentBlockHeight).when(walletService).getCurrentBlockHeight();
        Mockito.doReturn(BigDecimal.ONE).when(walletService).getUnconfirmedBalance();

        Mockito.doNothing().when(walletService).broadcastRefundTx();
        Mockito.doNothing().when(walletService).createRefundTransaction();
        Mockito.doNothing().when(walletService).redeposit();

        walletService.init(storage);
        walletService.getService().awaitRunning();

        Thread.sleep(3000);

        Mockito.verify(walletService, Mockito.times(0)).broadcastRefundTx();
        Mockito.verify(walletService, Mockito.times(0)).createRefundTransaction();
        Mockito.verify(walletService, Mockito.times(1)).redeposit();

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

    private static void cleanTestDirectory() {
        deleteFiles(testDirectory.listFiles());
    }

    private static void deleteFiles(File[] files) {
        for (File f : files) {
            System.out.println("Deleted file " + f.getName());
            f.delete();
        }
    }
}