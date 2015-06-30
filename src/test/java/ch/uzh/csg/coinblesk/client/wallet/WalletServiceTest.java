package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import com.google.common.util.concurrent.Service;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Level;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
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
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.persistence.InternalStorageHandler;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.testutils.MockRequestTask;
import ch.uzh.csg.coinblesk.client.testutils.TestUtils;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

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

    private CoinBleskApplication mApplication;
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


        // mock internal storage
        mApplication = PowerMockito.spy(new CoinBleskApplication());
        PowerMockito.doReturn(mApplication).when(walletService).getApplication();
        PowerMockito.doReturn(mApplication).when(mockContext).getApplicationContext();

        InternalStorageHandler mockStorage = PowerMockito.mock(InternalStorageHandler.class);
        PowerMockito.doReturn(mockStorage).when(mApplication).getStorageHandler();

        PowerMockito.doReturn(BitcoinNet.UNITTEST).when(mockStorage).getBitcoinNet();
        PowerMockito.doReturn(serverWatchingKey).when(mockStorage).getWatchingKey();

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
    public void testCreatePayment() throws Exception {

        walletService.init(bitcoinNet, serverWatchingKey);
        String receiveAddress = walletService.getPrivateAddress();

        final BigDecimal fundingAmount = new BigDecimal("1.01");
        final BigDecimal sendAmount = BigDecimal.ONE;
        final Address receiveAddr = new Address(params, receiveAddress);

        RequestFactory requestFactory = new RequestFactory() {
            @Override
            public RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(IAsyncTaskCompleteListener<TransferObject> completeListener, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
                // check if the request is correct
                Assert.assertNotNull(input);
                Assert.assertNotNull(input.getPartialTx());
                Assert.assertNotNull(input.getIndexAndDerivationPaths());
                Assert.assertFalse(input.getIndexAndDerivationPaths().isEmpty());

                // assert that the partial tx is correct
                byte[] partialTxBytes = Base64.decodeBase64(input.getPartialTx());
                Transaction tx = new Transaction(params, partialTxBytes);
                Assert.assertFalse(tx.isTimeLocked());
                Assert.assertFalse(tx.isAnyOutputSpent());

                // check if the correct amount was sent
                boolean outputFound = false;
                for(TransactionOutput txOut : tx.getOutputs()) {
                    Address outputAddr = txOut.getAddressFromP2PKHScript(params);

                    if(outputAddr == null) {
                        // output to P2SH
                        continue;
                    }

                    if(txOut.getAddressFromP2PKHScript(params).equals(receiveAddr)) {
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

        PowerMockito.doReturn(requestFactory).when(mApplication).getRequestFactory();

        // send a coin to the wallet
        FakeTxBuilder.BlockPair bp = injectTx(walletService.getBitcoinAddress(), fundingAmount);
        Assert.assertEquals(0, fundingAmount.compareTo(walletService.getBalance()));

        // create a transaction
        walletService.createPayment(receiveAddr.toString(), sendAmount);

    }

    @Test
    public void testGetRefundTx() throws Exception {

    }

    @Test
    public void testGetPrivateAddress() throws Exception {
        String mnemonic = walletService.getMnemonicSeed();
        WalletAppKit walletAppKit = extractAppKit(walletService);
        Address addr = walletAppKit.wallet().getActiveKeychain().getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(params);
        Assert.assertFalse(addr.isP2SHAddress());

        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        kcg.createAndActivateNewHDChain();

        DeterministicKeyChain privateChain = DeterministicKeyChain.builder().seed(seed).build();
        walletAppKit.wallet().addAndActivateHDChain(privateChain);

        Address addr2 = walletAppKit.wallet().currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);

        Assert.assertEquals(addr, addr2);
    }

    @Test
    public void testRestoreWalletFromSeed() throws Exception {

        walletService.init(bitcoinNet, serverWatchingKey);
        String mnemonic = walletService.getMnemonicSeed();
        String address = walletService.getBitcoinAddress();

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