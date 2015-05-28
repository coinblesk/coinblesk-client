package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;

import org.bitcoinj.core.AbstractPeerEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.BalanceType;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.MarriedKeyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.Constants;

public class WalletService extends android.app.Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    private final static String WALLET_FILES_PREFIX = "_bitcoinj";

    public class BitcoinWalletBinder extends Binder {
        public WalletService getService() {
            return WalletService.this;
        }
    }

    private final IBinder walletBinder = new BitcoinWalletBinder();

    private CoinBleskWalletAppKit clientWalletKit;

    private final ReentrantLock lock = Threading.lock("WalletService");

    private NetworkParameters params;

    private SyncProgress syncProgress;

    public WalletService() {
        this.syncProgress = new SyncProgress();
    }

    /**
     * Check if our chain head is the same as the height of the other peers.
     * If it is, it means that we are (most likely) finished with syncing
     * the blockchain.
     *
     * @return true if blockchain synchronization is (most likely) finished
     */
    public boolean isBlockChainSynced() {

        // check if our chain head is the same as the height of the other peers.
        // If it is, it means that we are (most likely) finished with syncing
        // the blockchain, and can return
        int bestHeight = getAppKit().peerGroup().getMostCommonChainHeight();
        int ourHeight = getAppKit().chain().getBestChainHeight();

        return bestHeight == ourHeight;
    }

    public SyncProgress getSyncProgress() {
        return syncProgress;
    }

    /**
     * @return e new wallet seed
     */
    private DeterministicSeed getWalletSeed() {

        // bitcoinJ will synchronize 1 week ahead of the specified time because
        // of time skews. So using the current time is absolutely safe
        DeterministicSeed seed = new DeterministicSeed(new SecureRandom(), 256, "", System.currentTimeMillis() / 1000);

        return seed;
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

    private boolean isNewSetup(final BitcoinNet bitcoinNet) {
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(getWalletFilesPrefix(bitcoinNet));
            }
        });

        return files.length == 0;
    }

    private String getWalletFilesPrefix(BitcoinNet bitcoinNet) {
        return bitcoinNet.toString().toLowerCase(Locale.ENGLISH) + WALLET_FILES_PREFIX;
    }

    private boolean isWalletReady() {
        return clientWalletKit != null && clientWalletKit.isRunning();
    }

    private void initNewWallet(final BitcoinNet bitcoinNet, final String watchingKey) {
        clientWalletKit.restoreWalletFromSeed(getWalletSeed());

        // Add checkpoints (this will speed up the blockchain synchronization
        // significantly)
        if (bitcoinNet != BitcoinNet.REGTEST) {
            clientWalletKit.setCheckpoints(getCheckpoints(bitcoinNet));
        }

        // marry the wallet to the server and set up the custom transaction signer
        clientWalletKit.addListener(new Service.Listener() {
            @Override
            public void running() {
                LOGGER.info("wallet is set up");

                marryWallet(watchingKey);

                // allow spending unconfirmed txs on regtest and testnet
                if (bitcoinNet == BitcoinNet.TESTNET || bitcoinNet == BitcoinNet.REGTEST) {
                    clientWalletKit.wallet().allowSpendingUnconfirmedTransactions();
                }


            }
        }, Threading.USER_THREAD);
    }

    public Service init(final BitcoinNet bitcoinNet, final String watchingKey) {

        // we need a lock here to make sure below is not executed multiple times.
        // Else if two threads at the same time try to bind the service, IllegalStateException
        // will be thrown
        lock.lock();

        try {
            if (isWalletReady()) {
                return clientWalletKit;
            }

            params = getNetworkParams(bitcoinNet);

            clientWalletKit = new CoinBleskWalletAppKit(params, getFilesDir(), getWalletFilesPrefix(bitcoinNet));
            clientWalletKit
                    .setAndroidContext(getApplicationContext())
                    .setBlockingStartup(false)
                    .setDownloadListener(new AbstractPeerEventListener() {
                        @Override
                        public void onBlocksDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, int blocksLeft) {
                            LOGGER.debug("{} blocks left to download...", blocksLeft);
                            if (!syncProgress.hasStarted()) {
                                syncProgress.setTotalBlocks(blocksLeft);
                            }
                            syncProgress.setBlocksRemaining(blocksLeft);
                        }
                    });

            // after the wallet is running
            clientWalletKit.addListener(new Service.Listener() {
                @Override
                public void running() {
                    postInit();
                }
            }, Threading.USER_THREAD);

            // create new wallet if new setup
            if (isNewSetup(bitcoinNet)) {
                initNewWallet(bitcoinNet, watchingKey);
            }

            return clientWalletKit.startAsync();
        } finally {
            lock.unlock();
        }
    }

    /**
     * This method is called after the wallet has been set up.
     */
    private void postInit() {
        // TODO: only allow for testnet
        clientWalletKit.wallet().allowSpendingUnconfirmedTransactions();

        // faster syncing....
        clientWalletKit.peerGroup().setFastCatchupTimeSecs(clientWalletKit.wallet().getEarliestKeyCreationTime());

        // initialize the server transaction signer
        initTransactionSigner();

        // check if we finished syncing the blockchain
        if(isBlockChainSynced()) {
            syncProgress.setFinished();
        }
    }

    /**
     * Synchronizes the blockchain and sets up the wallet. <strong>Please notice:</strong>
     * if this is the first time the wallet is set up, you must use {@link WalletService#init(BitcoinNet, String)}
     * instead. This method assumes that the bitcoin network and the server watching key are already stored on the device
     *
     * @return a {@link Service} object
     */
    public Service init() {

        if (isWalletReady()) {
            return clientWalletKit;
        }

        BitcoinNet bitcoinNet = ClientController.getStorageHandler().getBitcoinNet();
        Preconditions.checkNotNull(bitcoinNet, "bitcoin net was not stored in the internal storage");

        String watchingKey = ClientController.getStorageHandler().getWatchingKey();
        Preconditions.checkNotNull(watchingKey);

        return init(bitcoinNet, watchingKey);

    }

    private CoinBleskWalletAppKit getAppKit() {
        if (clientWalletKit == null) {
            init();
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

    public String getWatchingKey() {
        getAppKit().awaitRunning();
        return clientWalletKit.wallet().getWatchingKey().serializePubB58(clientWalletKit.params());
    }

    private void initTransactionSigner() {

        for (TransactionSigner signer : clientWalletKit.wallet().getTransactionSigners()) {
            if (signer instanceof ServerTransactionSigner) {
                ((ServerTransactionSigner) signer).setContext(getApplicationContext());
                return;
            }
        }

        ServerTransactionSigner serverTransactionSigner = new ServerTransactionSigner();
        serverTransactionSigner.setContext(this);
        clientWalletKit.wallet().addTransactionSigner(serverTransactionSigner);
    }

    /**
     * Adds the {@link ServerTransactionSigner} to the wallet signers if it
     * isn't already added and then marries the wallet to the server key.
     */
    private void marryWallet(final String serverSeed) {

        initTransactionSigner();

        // new wallet -> setup married wallet
        LOGGER.info("New set up, marrying wallet to the server key");

        // create watching HD key from server seed
        DeterministicKey serverWatchingKey = DeterministicKey.deserializeB58(serverSeed, clientWalletKit.params());

        // marry this clients wallet to the server wallet
        MarriedKeyChain marriedKeyChain = MarriedKeyChain.builder().random(new SecureRandom()).followingKeys(serverWatchingKey).threshold(2).build();
        clientWalletKit.wallet().addAndActivateHDChain(marriedKeyChain);

        // add the transaction signer to the wallet that is responsible of
        // obtaining the server's signature
        clientWalletKit.wallet().addTransactionSigner(new ServerTransactionSigner());

    }

    /**
     * @param bitcoinNet
     * @return the {@link NetworkParameters} for a specific network
     */
    private NetworkParameters getNetworkParams(BitcoinNet bitcoinNet) {
        switch (bitcoinNet) {
            case REGTEST:
                return RegTestParams.get();
            case TESTNET:
                return TestNet3Params.get();
            case MAINNET:
                return MainNetParams.get();
            default:
                throw new RuntimeException("Please set the server property bitcoin.net to (regtest|testnet|mainnet)");
        }
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
        Address btcAddress = new Address(params, address);
        Wallet.SendRequest req = Wallet.SendRequest.to(btcAddress, Coin.parseCoin(amount.toString()));
        req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
        getAppKit().wallet().completeTx(req);
    }

    public TransactionHistory getTransactionHistory() {
        int maxNumberOfTransactions = 500;

        List<Transaction> txs = getAppKit().wallet().getRecentTransactions(maxNumberOfTransactions, false);

        List<ch.uzh.csg.coinblesk.model.Transaction> allTransactions = Lists.newArrayListWithCapacity(maxNumberOfTransactions);


        for (Transaction tx : txs) {

            // get the amount sent to/from our wallet
            BigDecimal amount = BitcoinUtils.coinToBigDecimal(tx.getValue(getAppKit().wallet()));

            // now let's translate the bitcoinJ Transaction to our simplified Transaction object
            ch.uzh.csg.coinblesk.model.Transaction transaction = new ch.uzh.csg.coinblesk.model.Transaction();
            transaction.setAmount(amount.abs());
            transaction.setTimestamp(tx.getUpdateTime());

            if(amount.signum() < 0) {
                // negative amount -> pay out
                transaction.setType(ch.uzh.csg.coinblesk.model.Transaction.TransactionType.PAY_OUT);
            } else {
                // positive amount -> pay in
                if(tx.getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATIONS) {
                    // confirmed tx
                    transaction.setType(ch.uzh.csg.coinblesk.model.Transaction.TransactionType.PAY_IN);
                } else {
                    // TODO: work with confidence instead of confirmations
                    // unconfirmed tx
                    transaction.setType(ch.uzh.csg.coinblesk.model.Transaction.TransactionType.PAY_IN_UNVERIFIED);
                }
            }

            allTransactions.add(transaction);
        }

        return new TransactionHistory(allTransactions);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return walletBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        clientWalletKit.stopAsync();

        return false;
    }

    @Override
    public void onDestroy() {
        if (clientWalletKit != null) {
            clientWalletKit.stopAsync();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
