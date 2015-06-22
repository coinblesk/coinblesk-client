package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.base.Joiner;
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
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.BalanceType;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.math.BigDecimal;
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

    private boolean isWalletReady() {
        return clientWalletKit != null && clientWalletKit.isRunning();
    }

    /**
     * Restores a wallet from an Mnemonic seed.
     *
     * @param bitcoinNet
     * @param watchingKey
     * @param mnemonic
     * @param creationTime
     * @return
     * @throws UnreadableWalletException
     */
    public Service restoreWalletFromSeed(final BitcoinNet bitcoinNet, final String watchingKey, String mnemonic, Long creationTime) throws UnreadableWalletException {
        LOGGER.debug("Restoring wallet from mnemonic seed");

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
        Service service = init(bitcoinNet, watchingKey, mnemonic, creationTime);
        service.awaitRunning();
        LOGGER.debug("Wallet service started");

        return replayBlockchain(bitcoinNet, serverWatchingKey);
    }

    /**
     * Replays / Resynchronizes the blockchain.
     *
     * @return
     */
    private Service replayBlockchain(final BitcoinNet bitcoinNet, String serverWatchingKey) {
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
        return init(bitcoinNet, serverWatchingKey);
    }

//    /**
//     * Replays / Resynchronizes the blockchain.
//     *
//     * @return
//     */
//    private Service replayBlockchain(final BitcoinNet bitcoinNet, String serverWatchingKey) {
//        LOGGER.debug("Replaying blockchain");
//
//        clientWalletKit.peerGroup().stop();
//        try {
//            clientWalletKit.store().close();
//        } catch (BlockStoreException e) {
//            // TODO: handle
//            LOGGER.error("Failed closing blockstore");
//        }
//        clientWalletKit.peerGroup().removeWallet(clientWalletKit.wallet());
//
//        File[] files = getFilesDir().listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String filename) {
//                return filename.startsWith(getWalletFilesPrefix(bitcoinNet)) && filename.endsWith("chain");
//            }
//        });
//
//        for (File f : files) {
//            LOGGER.debug("Deleted file {}", f.getName());
//            f.delete();
//        }
//
//        clientWalletKit.wallet().reset();
//
//        return clientWalletKit;
//
//    }

    public Service init() {
        BitcoinNet bitcoinNet = ClientController.getStorageHandler().getBitcoinNet();
        String serverWatchingKey = ClientController.getStorageHandler().getWatchingKey();
        return init(bitcoinNet, serverWatchingKey);
    }

    public Service init(final BitcoinNet bitcoinNet, final String watchingKey) {
        return init(bitcoinNet, watchingKey, null, null);
    }

    public Service init(final BitcoinNet bitcoinNet, final String watchingKey, @Nullable String mnemonic, @Nullable Long creationTime) {

        this.bitcoinNet = bitcoinNet;
        this.serverWatchingKey = watchingKey;

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
            if(bitcoinNet == BitcoinNet.TESTNET || bitcoinNet == BitcoinNet.MAINNET) {
                clientWalletKit.setCheckpoints(getCheckpoints(bitcoinNet));
            }

            // custom dummy dns discovery for unittests
            if(bitcoinNet == BitcoinNet.UNITTEST) {
                clientWalletKit.setDiscovery(new DnsDiscovery(new String[]{"localhost"}, params));
            }

            return clientWalletKit.startAsync();
        } finally {
            lock.unlock();

        }
    }

    private CoinBleskWalletAppKit getAppKit() {

        // init the app kit if it's not running already
        if (clientWalletKit == null || clientWalletKit.state() != Service.State.STARTING || clientWalletKit.state() != Service.State.RUNNING) {
            if(bitcoinNet == null || serverWatchingKey == null) {
                init();
            } else {
                init(bitcoinNet, serverWatchingKey);
            }
        }

        // wait for the wallet kit to start
        if(clientWalletKit.state() == Service.State.STARTING) {
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
     * Adds the {@link ServerTransactionSigner} to the wallet and / or sets the context. Context is
     * needed to make the request to the server.
     */
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

            if (amount.signum() < 0) {
                // negative amount -> pay out
                transaction.setType(ch.uzh.csg.coinblesk.model.Transaction.TransactionType.PAY_OUT);
            } else {
                // positive amount -> pay in
                if (tx.getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATIONS) {
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

    /**
     * Blocking stop of the wallet
     */
    public void stop() {
        clientWalletKit.stopAsync();
        clientWalletKit.awaitTerminated();
        clientWalletKit = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return walletBinder;
    }

    @Override
    public void onDestroy() {
//        if (clientWalletKit != null) {
//            clientWalletKit.stopAsync();
//            clientWalletKit = null;
//        }
    }

}
