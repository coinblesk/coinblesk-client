package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.BalanceType;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.WalletTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.persistence.StorageHandler;
import ch.uzh.csg.coinblesk.client.util.Constants;

public class WalletService extends android.app.Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    private final static String WALLET_FILES_PREFIX = "_bitcoinj";


    /**
     * The maximum duration until the refund transaction is broadcast.
     */
    public final static int REFUND_LOCKTIME_MONTH = 5;

    /**
     * If a earlier created refund transaction becomes valid in less month than this,
     * the clients' funds are redeposited, meaning the once created refund transaction
     * will be invalidated
     */
    public final static int REFUND_LOCKTIME_THRESHOLD = 2;

    public class LocalBinder extends Binder {
        public WalletService getService() {
            return WalletService.this;
        }
    }

    private final IBinder walletBinder = new LocalBinder();
    private CoinBleskWalletAppKit clientWalletKit;
    private final ReentrantLock lock = Threading.lock("WalletService");

    private BitcoinNet bitcoinNet;
    private String serverWatchingKey;
    private StorageHandler storage;

    private SyncProgress syncProgress;

    public WalletService() {
        this.syncProgress = new SyncProgress();
    }

    public SyncProgress getSyncProgress() {
        return syncProgress;
    }

    /**
     * @param bitcoinNet
     * @return the checkpoints stream
     */
    private InputStream getCheckpoints(BitcoinNet bitcoinNet) {
        if (bitcoinNet == BitcoinNet.MAINNET) {
            return getResources().openRawResource(R.raw.checkpoints);
        } else if (bitcoinNet == BitcoinNet.TESTNET) {
            return getResources().openRawResource(R.raw.checkpoints_testnet);
        } else {
            throw new IllegalArgumentException("Can only get checkpoints of testnet and mainnet");
        }
    }

    /**
     * @param bitcoinNet
     * @return true if no wallet is stored on the device
     */
    public boolean walletExistsOnDevice(final BitcoinNet bitcoinNet) {
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(getWalletFilesPrefix(bitcoinNet));
            }
        });

        return files.length != 0;
    }

    public String getWalletFilesPrefix(BitcoinNet bitcoinNet) {
        return bitcoinNet.toString().toLowerCase(Locale.ENGLISH) + WALLET_FILES_PREFIX;
    }

    /**
     * Restores a wallet from an Mnemonic seed.
     *
     * @param storage      the StorageHandler of the application
     * @param mnemonic     the mnemonic seed to restore the private keys from
     * @param creationTime the creation date of the key, UNIX epoch
     * @return
     * @throws UnreadableWalletException
     */
    public Service restoreWalletFromSeed(StorageHandler storage, String mnemonic, Long creationTime) throws UnreadableWalletException {
        Preconditions.checkNotNull(storage, "Storage handler cannot be null.");

        LOGGER.debug("Restoring wallet from mnemonic seed");
        bitcoinNet = storage.getBitcoinNet();

        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(getWalletFilesPrefix(bitcoinNet));
            }
        });

        for (File f : files) {
            LOGGER.warn("Deleting file {}", f.getName());
            f.delete();
        }

        LOGGER.debug("Starting wallet service normally");
        Service service = init(storage, mnemonic, creationTime);
        service.awaitRunning();
        LOGGER.debug("Wallet service started");

        return replayBlockchain();
    }

    /**
     * Replays / Resynchronizes the blockchain.
     *
     * @return
     */
    private Service replayBlockchain() {
        LOGGER.debug("Replaying blockchain");

        clientWalletKit.awaitRunning();

        LOGGER.debug("Stopping wallet service");
        clientWalletKit.stopAsync();
        clientWalletKit.awaitTerminated();

        LOGGER.debug("Wallet service was stopped, removing the block store file");
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(getWalletFilesPrefix(bitcoinNet)) && filename.endsWith("chain");
            }
        });

        for (File f : files) {
            LOGGER.debug("Deleted file {}", f.getName());
            f.delete();
        }

        LOGGER.debug("Restarting wallet service");
        return init(storage);
    }

    public Service init(StorageHandler storageHandler) {
        return init(storageHandler, null, null);
    }

    /**
     * This method will be called after the bitcoin wallet is started
     */
    private void postInit() {

        checkRefundTxState();

        // add wallet listeners
        getAppKit().wallet().addEventListener(new CreateNewRefundTxListener(this));
    }

    private Service init(StorageHandler storage, @Nullable String mnemonic, @Nullable Long creationTime) {

        Preconditions.checkNotNull(storage, "Storage handler cannot be null.");

        this.storage = storage;

        bitcoinNet = storage.getBitcoinNet();
        serverWatchingKey = storage.getWatchingKey();

        // check state
        Preconditions.checkNotNull(bitcoinNet, "bitcoinnet has to be set in the storage for the wallet service to start.");
        Preconditions.checkNotNull(serverWatchingKey, "server watching key has to be set in the storage for the wallet service to start.");

        // we need a lock here to make sure below is not executed multiple times.
        // Else if two threads at the same time try to bind the service, IllegalStateException
        // will be thrown
        lock.lock();

        try {
            if (clientWalletKit != null && clientWalletKit.state() != Service.State.TERMINATED) {
                return clientWalletKit;
            }

            NetworkParameters params = getNetworkParams(bitcoinNet);

            clientWalletKit = new CoinBleskWalletAppKit(params, getFilesDir(), getWalletFilesPrefix(bitcoinNet));
            clientWalletKit
                    .marryWallet(serverWatchingKey, mnemonic, creationTime)
                    .setAndroidContext(getApplicationContext())
                    .setBlockingStartup(false)
                    .setDownloadListener(new DownloadProgressTracker() {
                        @Override
                        public void onBlocksDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, int blocksLeft) {
                            LOGGER.debug("{} blocks left to download...", blocksLeft);
                            if (!syncProgress.hasStarted()) {
                                syncProgress.setTotalBlocks(blocksLeft);
                            }
                            syncProgress.setBlocksRemaining(blocksLeft);
                        }

                        @Override
                        protected void doneDownload() {
                            LOGGER.debug("Blockchain sync finished");
                            syncProgress.setFinished();
                        }
                    });

            // load checkpoints
            if (bitcoinNet == BitcoinNet.TESTNET || bitcoinNet == BitcoinNet.MAINNET) {
                clientWalletKit.setCheckpoints(getCheckpoints(bitcoinNet));
            }

            // custom dummy dns discovery for unittests
            if (bitcoinNet == BitcoinNet.UNITTEST) {
                clientWalletKit.setDiscovery(new DnsDiscovery(new String[]{"localhost"}, params));
            }

            // run postInit method when the wallet is running
            clientWalletKit.addListener(new Service.Listener() {
                @Override
                public void running() {
                    super.running();
                    LOGGER.debug("Wallet service is running....");
                    try {
                        postInit();
                    } catch (IllegalStateException e) {
                        // this happens if the wallet service was shut somewhere in the process of this. Not a problem
                    }

                }
            }, Executors.newSingleThreadExecutor());

            return clientWalletKit.startAsync();
        } finally {
            lock.unlock();

        }
    }

    /**
     * Redepositing means sending all available bitcoins to the own P2SH address. This is necessary to invalidate a earlier created
     * refund transaction. If we don't do this before a refund transaction becomes valid, the server can no longer guarantee that we don't double-spend
     * our bitcoins, and therefore instant transaction are no longer allowed.
     */
    public void redeposit() {
        NetworkParameters params = getNetworkParams(bitcoinNet);

        try {
            Address btcAddress = new Address(params, getBitcoinAddress());
            Wallet.SendRequest req = Wallet.SendRequest.emptyWallet(btcAddress);
            req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
            req.tx.setMemo(DefaultTransactionMemos.REDEPOSIT_TX_MEMO);
            getAppKit().wallet().completeTx(req);
        } catch (AddressFormatException | InsufficientMoneyException e) {
            LOGGER.error("Failed to create bitcoin address", e);
        }
    }

    /**
     * @return a base58 encoded watching key of this wallet
     */
    public String getWatchingKey() {
        return getAppKit().wallet().getWatchingKey().serializePubB58(getNetworkParams(bitcoinNet));
    }

    /**
     * @return the {@link BitcoinNet} the wallet is running on.
     */
    public BitcoinNet getBitcoinNet() {
        return bitcoinNet;
    }

    /**
     * This method is responsible for
     * <ul>
     * <li>Creating a refund transaction if we don't have one yet</li>
     * <li>Redepositing the bitcoins if an earlier generated refund transaction becomes valid soon</li>
     * <li>Broadcasting the refund transaction if it is valid</li>
     * </ul>
     */
    public void checkRefundTxState() {

        LOGGER.debug("Checking refund transaction state...");

        long currentChainHeight = getCurrentBlockHeight();

        try {
            if (getUnconfirmedBalance().compareTo(BigDecimal.ZERO) == 0) {
                // client has no bitcoins: nothing to do...
                LOGGER.debug("Client has no bitcoins, nothing to do...");
                return;
            } else if (storage.getRefundTxValidBlock() < 0) {
                // no refund transaction yet, create one
                LOGGER.debug("No refund transaction found, creating one now...");
                createRefundTransaction();
            } else if (currentChainHeight > storage.getRefundTxValidBlock()) {
                // an earlier created refund transaction has become valid. This should only happen if we
                // were not able to redeposit the bitcoins, and could be an indication that the server
                // disappeared. If this is the case, we issue the refund transaction, which sends us back all
                // bitcoins.
                LOGGER.info("Refund transaction is now valid. Apparently we were not able to create a redeposit. Let's braodcast that refund transaction...");
                broadcastRefundTx();
            } else if (storage.getRefundTxValidBlock() - BitcoinUtils.monthsToBlocks(REFUND_LOCKTIME_THRESHOLD) < currentChainHeight) {
                // we are in the threshold until a earlier created refund transaction becomes valid. This
                // means that we have to redeposit the bitcoins, or else the earlier issued refund transaction
                // becomes valid, and the server will no longer sign our transactions.
                LOGGER.info("Refund transaction becomes valid soon, redepositing bitcoins now to invalidate the old refund transaction");
                redeposit();
            }
        } catch (Exception e) {
            LOGGER.debug("Unexpected error checking the refund transaction state", e);
        }
    }

    /**
     * Broadcasts the stored refund transaction.
     */
    public void broadcastRefundTx() {

        byte[] serializedRefundTx = Base64.decode(storage.getRefundTx(), Base64.NO_WRAP);
        Transaction refundTx = new Transaction(getNetworkParams(bitcoinNet), serializedRefundTx);
        Wallet.SendRequest req = Wallet.SendRequest.forTx(refundTx);

        try {
            getAppKit().wallet().completeTx(req);
        } catch (InsufficientMoneyException e) {
            LOGGER.error("Refund transaction is no longer valid, funds have already been spent. Uh-oh.");
            return;
        }

    }

    private CoinBleskWalletAppKit getAppKit() {

        if (storage == null) {
            storage = getCoinbleskApplication().getStorageHandler();
        }

        if (clientWalletKit == null) {
            // start the wallet service if it isn't running
            init(storage);
        }

        // wait for the wallet kit to start
        if (clientWalletKit.state() == Service.State.STARTING) {
            clientWalletKit.awaitRunning();
        }

        return clientWalletKit;
    }

    /**
     * @return the wallet {@link Service}
     */
    public Service getService() {
        return clientWalletKit;
    }

    /**
     * Returns the mnemonic code of the wallet's keys. This method will not
     * return until the wallet has been initialized
     *
     * @return
     */
    public String getMnemonicSeed() {
        getAppKit().awaitRunning();
        List<String> mnemonicCode = clientWalletKit.wallet().getActiveKeychain().getSeed().getMnemonicCode();
        return Joiner.on(" ").join(mnemonicCode);
    }

    /**
     * @param bitcoinNet
     * @return the {@link NetworkParameters} for a specific network
     */
    private NetworkParameters getNetworkParams(BitcoinNet bitcoinNet) {
        switch (bitcoinNet) {
            case UNITTEST:
                return UnitTestParams.get();
            case REGTEST:
                return RegTestParams.get();
            case TESTNET:
                return TestNet3Params.get();
            case MAINNET:
                return MainNetParams.get();
            default:
                throw new RuntimeException("Please set the server property bitcoin.net to (unittest|regtest|testnet|mainnet)");
        }
    }

    /**
     * Checks if the stored refund transaction is already valid.
     *
     * @return true if a valid refund transaction is stored on the device
     */
    public boolean isRefundTxValid() {
        return getCurrentBlockHeight() >= storage.getRefundTxValidBlock();
    }

    public String getBitcoinAddress() {
        return getAppKit().wallet().currentReceiveAddress().toString();
    }

    public BigDecimal getUnconfirmedBalance() {
        return BitcoinUtils.coinToBigDecimal(getAppKit().wallet().getBalance(BalanceType.ESTIMATED));
    }

    public BigDecimal getBalance() {
        return BitcoinUtils.coinToBigDecimal(getAppKit().wallet().getBalance(BalanceType.AVAILABLE));
    }

    public void createPayment(String address, BigDecimal amount) throws AddressFormatException, InsufficientMoneyException {
        NetworkParameters params = getNetworkParams(bitcoinNet);
        Address btcAddress = new Address(params, address);
        Coin sendAmount = BitcoinUtils.bigDecimalToCoin(amount);

        // create the send request
        Wallet.SendRequest req = Wallet.SendRequest.to(btcAddress, sendAmount);
        req.fee = BitcoinUtils.bigDecimalToCoin(Constants.DEFAULT_FEE);
        req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;

        getAppKit().wallet().completeTx(req);

        for (TransactionInput txIn : req.tx.getInputs()) {
            if (txIn.getConnectedOutput().getScriptPubKey().isPayToScriptHash()) {
                // at least one input needs to be signed by the server,
                // which means that we are not responsible for broadcasting the transaction
                return;
            }
        }

        getAppKit().peerGroup().broadcastTransaction(req.tx);
        getAppKit().wallet().maybeCommitTx(req.tx);
    }


    /**
     * Creates a time-locked transaction signed by the coinblesk server that sends all available coins to a personal address, and
     * stores it on the device.
     */
    public void createRefundTransaction() {

        try {
            Address privateAddr = new Address(getNetworkParams(bitcoinNet), getPrivateAddress());
            Wallet.SendRequest req = Wallet.SendRequest.emptyWallet(privateAddr);
            req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;

            // calculate the blocktime.
            long lockTime = getLockTime(REFUND_LOCKTIME_MONTH);
            req.tx.setLockTime(lockTime);

            // at least one input needs a so-called sequence number set to 0.
            // For details, see: https://bitcoin.org/en/developer-guide#locktime-and-sequence-number
            List<Transaction> unspents = new ArrayList<>(getAppKit().wallet().getTransactionPool(WalletTransaction.Pool.UNSPENT).values());
            if (unspents.isEmpty()) {
                // no unspent transaction found -> use pending instead
                unspents.addAll(new ArrayList<>(getAppKit().wallet().getTransactionPool(WalletTransaction.Pool.PENDING).values()));
            }

            Preconditions.checkState(!unspents.isEmpty(), "Cannot create refund transaction without any unspent transactions.");

            TransactionOutput outputToAdd = null;
            for (TransactionOutput output : unspents.get(0).getOutputs()) {
                if (output.isMine(getAppKit().wallet())) {
                    outputToAdd = output;
                }
            }
            Preconditions.checkNotNull(outputToAdd, "Couldn't find an unspent output that belongs to us");
            req.tx.addInput(outputToAdd);

            req.tx.getInput(0).setSequenceNumber(0);
            req.tx.setMemo(DefaultTransactionMemos.REFUND_TX_MEMO);

            getAppKit().wallet().completeTx(req);

        } catch (Exception e) {
            LOGGER.error("Failed to create refund transaction", e);
        }
    }

    private long getLockTime(int month) {
        long currentBlockHeight = getAppKit().peerGroup().getMostCommonChainHeight();
        long lockTime = BitcoinUtils.monthsToBlocks(month); //assuming 1 block every 10 minutes
        return currentBlockHeight + lockTime;
    }

    public TransactionHistory getTransactionHistory() {
        int maxNumberOfTransactions = 500;

        List<Transaction> txs = getAppKit().wallet().getRecentTransactions(maxNumberOfTransactions, false);

        List<TransactionObject> allTransactions = Lists.newArrayListWithCapacity(maxNumberOfTransactions);


        for (Transaction tx : txs) {

            // get the amount sent to/from our wallet
            BigDecimal amount = BitcoinUtils.coinToBigDecimal(tx.getValue(getAppKit().wallet()));

            // now let's translate the bitcoinJ Transaction to our simplified Transaction object
            TransactionObject transaction = new TransactionObject();
            transaction.setAmount(amount.abs());
            transaction.setTimestamp(tx.getUpdateTime());

            if (amount.signum() < 0) {
                // negative amount -> pay out
                transaction.setType(TransactionObject.TransactionType.PAY_OUT);
            } else {
                // positive amount -> pay in
                if (tx.getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATIONS) {
                    // confirmed tx
                    transaction.setType(TransactionObject.TransactionType.PAY_IN);
                } else {
                    // TODO: work with confidence instead of confirmations
                    // unconfirmed tx
                    transaction.setType(TransactionObject.TransactionType.PAY_IN_UNVERIFIED);
                }
            }

            allTransactions.add(transaction);
        }

        return new TransactionHistory(allTransactions);
    }

    /**
     * @return A private, non-multi sig address. Private address are used to create
     * refund-transactions.
     */
    public String getPrivateAddress() {
        DeterministicSeed seed = getAppKit().wallet().getActiveKeychain().getSeed();
        KeyChainGroup kcg = new KeyChainGroup(getNetworkParams(bitcoinNet), seed);
        return kcg.getActiveKeyChain().getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(getNetworkParams(bitcoinNet)).toString();
    }

    /**
     * Returns the current chain height, as reported by a majority of our peers.
     *
     * @return the most common chain height
     */
    public long getCurrentBlockHeight() {
        return getAppKit().peerGroup().getMostCommonChainHeight();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return walletBinder;
    }

    /**
     * Blocking stop of the wallet
     */
    public void stop() {
        if (clientWalletKit != null) {
            clientWalletKit.stopAsync().awaitTerminated();
            clientWalletKit = null;
        }
    }

    private CoinBleskApplication getCoinbleskApplication() {
        return ((CoinBleskApplication) getApplication());
    }

    /**
     * Checks whether the passed bitcoin address is a valid address.
     *
     * @param addr the bitcoin address
     * @return true if the address is valid
     */
    public boolean isValidAddress(String addr) {
        try {
            new Address(getNetworkParams(bitcoinNet), addr);
            return true;
        } catch (AddressFormatException e) {
            return false;
        }
    }


    /**
     * This method should not be called and is only used for testing.
     *
     * @return the {@link CoinBleskWalletAppKit}
     */
    public CoinBleskWalletAppKit getCoinBleskWalletAppKit() {
        return getAppKit();
    }


}
