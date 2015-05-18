package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;

import org.bitcoinj.core.AbstractPeerEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet.BalanceType;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
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

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.model.HistoryPayInTransaction;
import ch.uzh.csg.coinblesk.model.HistoryPayInTransactionUnverified;
import ch.uzh.csg.coinblesk.model.HistoryPayOutTransaction;

public class WalletService extends android.app.Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
    
    private final static String WALLET_FILES_PREFIX = "_bitcoinj";

    public class BitcoinWalletBinder extends Binder {
        public WalletService getService() {
            return WalletService.this;
        }
    }

    private final IBinder walletBinder = new BitcoinWalletBinder();
    private WalletAppKit clientWalletKit;
    private NetworkParameters params;
    
    private SyncProgress syncProgress;
    
    public WalletService() {
        this.syncProgress = new SyncProgress();
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
        } else if(bitcoinNet == BitcoinNet.TESTNET){
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

    /**
     * Synchronizes the blockchain and sets up the wallet
     * 
     * @return a {@link Service} object
     */
    public Service init() {

        if(clientWalletKit != null && clientWalletKit.isRunning()) {
            // wallet was already initialized
            return clientWalletKit;
        }

        String bitcoinNetStr = getApplicationContext().getSharedPreferences("wallet", 0).getString("bitcoinnet", null);
        BitcoinNet bitcoinNet = BitcoinNet.of(bitcoinNetStr);
        
        final String watchingKey = getApplicationContext().getSharedPreferences("wallet", 0).getString("watchingkey", null);

        params = getNetworkParams(bitcoinNet);

        clientWalletKit = new WalletAppKit(params, getFilesDir(), getWalletFilesPrefix(bitcoinNet))
                .setBlockingStartup(false)
                .setDownloadListener(new AbstractPeerEventListener() {
                    @Override
                    public void onBlocksDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, int blocksLeft) {
                        LOGGER.debug("{} blocks left to download...", blocksLeft);
                        if(!syncProgress.hasStarted()) {
                            syncProgress.setTotalBlocks(blocksLeft);
                        }
                        syncProgress.setBlocksRemaining(blocksLeft);
                    }
                });
        
        // create new wallet if new setup
        if(isNewSetup(bitcoinNet)) {
            clientWalletKit.restoreWalletFromSeed(getWalletSeed());
        }

        // Add checkpoints (this will speed up the blockchain synchronization
        // significantly)
        if (bitcoinNet != BitcoinNet.REGTEST) {
            clientWalletKit.setCheckpoints(getCheckpoints(bitcoinNet));
        }

        Service initService = clientWalletKit.startAsync();

        initService.addListener(new Service.Listener() {
            @Override
            public void running() {
                LOGGER.info("wallet is set up");
                initializeTransactionSigner(watchingKey);
            }
        }, Threading.USER_THREAD);

        return initService;
    }

    private WalletAppKit getAppKit() {
        if (clientWalletKit == null) {
            init();
            clientWalletKit.awaitRunning();
        }
        return clientWalletKit;
    }

    /**
     * 
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

    /**
     * Adds the {@link ServerTransactionSigner} to the wallet signers if it
     * isn't already added.
     */
    private void initializeTransactionSigner(final String serverSeed) {

        // check if wallet is already married
        for (TransactionSigner signer : clientWalletKit.wallet().getTransactionSigners()) {
            if (signer instanceof ServerTransactionSigner) {
                // wallet already set up and restored from disk
                LOGGER.info("Wallet was already set up and married");
                ((ServerTransactionSigner) signer).setContext(getApplicationContext());
                return;
            }
        }

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
     * 
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
    
    /**
     * 
     * @return the total balance of the user as a friendly string. Includes unconfirmed transactions
     */
    public String getFriendlyBalance() {
        return getAppKit().wallet().getBalance(BalanceType.ESTIMATED).toFriendlyString();
    }
    
    public BigDecimal getUnconfirmedBalance() {
        return BitcoinUtils.coinToBigDecimal(getAppKit().wallet().getBalance(BalanceType.ESTIMATED));
    }

    public void createPayment(String address, BigDecimal amount) throws AddressFormatException, InsufficientMoneyException {
        Address btcAddress;
        btcAddress = new Address(params, address);
        Transaction tx = getAppKit().wallet().createSend(btcAddress, BitcoinUtils.bigDecimalToCoin(amount));
    }


    public List<HistoryPayInTransaction> getPayInTransactionHistory(int maxNumberOfTransactions) {

        // transform bitcoinj transactions to our transaction history object...
        List<Transaction> txs = clientWalletKit.wallet().getRecentTransactions(maxNumberOfTransactions, false);
        List<HistoryPayInTransaction> payIns = Lists.newArrayListWithCapacity(maxNumberOfTransactions);

        for(Transaction tx : txs) {
            if(tx.getConfidence().getDepthInBlocks() == Constants.MIN_CONFIRMATIONS) {
                BigDecimal amount = BitcoinUtils.coinToBigDecimal(tx.getValueSentToMe(clientWalletKit.wallet()));
                HistoryPayInTransaction payIn = new HistoryPayInTransaction(tx.getUpdateTime(), amount);
            }
        }

        return payIns;
    }

    public List<HistoryPayInTransactionUnverified> getPayInTransactionUnverifiedHistory() {
        //TODO
        return Lists.newArrayList();
    }

    public List<HistoryPayOutTransaction> getPayOutTransactionHistory() {
        //TODO
        return Lists.newArrayList();
    }

    public TransactionHistory getTransactionHistory() {
        int maxNrOfTxs = 100;
        //List<HistoryPayInTransaction> payInTransactions, List<HistoryPayInTransactionUnverified> payInTransactionUnverifiedHistory, List<HistoryPayOutTransaction> payOutTransactions
        return new TransactionHistory(getPayInTransactionHistory(maxNrOfTxs), getPayInTransactionUnverifiedHistory(), getPayOutTransactionHistory());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return walletBinder;
    }
    
    @Override
    public void onDestroy() {
        if(clientWalletKit != null) {
            clientWalletKit.stopAsync();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
