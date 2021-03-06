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

import org.bitcoinj.core.AbstractBlockChainListener;
import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.BalanceType;
import org.bitcoinj.core.Wallet.SendRequest;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicKeyChain;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
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

    /**
     * If the wallet service is in NFC mode, transactions are not sent to the server for
     * signing, but to the other client via NFC.
     * TODO: Make this less ugly, eg. by broadcasting an intent with the server signature request.
     */
    public static boolean nfcMode = false;
    public static HalfSignedTransaction sigReq;

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
    private DeterministicKeyChain serverKeyChain;
    private StorageHandler storage;
    private SyncProgress syncProgress;
    private Map<String, WalletListener> listeners;

    public WalletService() {
        this.syncProgress = new SyncProgress();
        this.listeners = new HashMap<>();
    }

    /**
     * Indicates whether the wallet service is in NFC mode. If so, transactions are not sent to the server for signing,
     * but sent to the other client requesting bitcoins via NFC.
     *
     * @return true if in NFC mode
     */
    public static boolean isNfcMode() {
        return nfcMode;
    }

    public SyncProgress getSyncProgress() {
        return syncProgress;
    }


    public void addBitcoinListener(String id, WalletListener listener) {
        listeners.put(id, listener);
    }

    /**
     * Adds a listener to the wallet that is notfied when:
     * <ul>
     * <li>Bitcoins were sent to the wallet</li>
     * <li>Bitcoins were sent from the wallet</li>
     * <li>The number of confirmations changed (-> new block was broadcast)</li>
     * </ul>
     *
     * @param clazz    the class that registered the listener
     * @param listener
     */
    public void addBitcoinListener(Class clazz, WalletListener listener) {
        addBitcoinListener(clazz.getName(), listener);
    }

    public void removeBitcoinListener(String listenerId) {
        listeners.remove(listenerId);
    }

    /**
     * Removes a bitcoin listener that was previously registered.
     *
     * @param clazz the class that registered the listener
     */
    public void removeBitcoinListener(Class clazz) {
        removeBitcoinListener(clazz.getName());
    }

    private void notifyNewTransaction() {
        for (WalletListener listener : listeners.values()) {
            listener.onWalletChange();
        }
    }

    private void initTxListener() {

        // add listener for receiving and sending coins
        getAppKit().wallet().addEventListener(new AbstractWalletEventListener() {

            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                notifyNewTransaction();
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                notifyNewTransaction();
            }
        });

        // add a listener for new blocks -> this means the number of confirmations have changed
        getAppKit().chain().addListener(new AbstractBlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                notifyNewTransaction();
            }
        });
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

        LOGGER.debug("Shutting down wallet app kit");
        clientWalletKit.stopAsync().awaitTerminated();

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

        Context.propagate(new Context(getNetworkParams(bitcoinNet)));

        checkRefundTxState();

        // add wallet listeners
        getAppKit().wallet().addEventListener(new CreateNewRefundTxListener(this)); // responsible for creating refund txs
        getAppKit().wallet().addEventListener(new MerchantModeSellListener(this)); // responsible for selling BTC if merchant mode is active
        getAppKit().wallet().addEventListener(new UpdateTxMemosListener(this)); // responsible for flagging the transactions

        // notify listeners about wallet changes
        initTxListener();

        // For some reason the transactions created by Bitcoin Core are time-locked with
        // a time lock smaller than the best chain height.
        // Since the server is anyway only signing transactions that have N confirmations, it's
        // save to accept these time-locked transactions (if they're in the
        // chain, they're valid...). By default, bitcoinj considers these
        // time locked transactions to be "risky".
        getAppKit().wallet().setAcceptRiskyTransactions(true);

        // Set the instant transaction memo for transactions from the coinblesk
        // network
        setTxMemos();

        // for debugging
        LOGGER.debug("Transactions stored in wallet:");
        for (Transaction tx : getAppKit().wallet().getTransactions(true)) {
            LOGGER.debug(tx.toString());
            LOGGER.debug("Transaction meta data: {}", storage.getTransactionMetaData(tx.getHashAsString()));
            LOGGER.debug("Transaction Memo: {}, Transaction Source: {}", tx.getMemo(), tx.getConfidence().getSource());
            LOGGER.debug("HEX encoded: {}:", BitcoinUtils.bytesToHex(tx.unsafeBitcoinSerialize()));
        }
        LOGGER.debug("Current block height: {}", getAppKit().chain().getBestChainHeight());
    }

    private Service init(StorageHandler storage, @Nullable String mnemonic, @Nullable Long creationTime) {

        Preconditions.checkNotNull(storage, "Storage handler cannot be null.");

        this.storage = storage;

        bitcoinNet = storage.getBitcoinNet();
        serverWatchingKey = storage.getServerWatchingKey();

        // check state
        Preconditions.checkNotNull(bitcoinNet, "bitcoinnet has to be set in the storage for the wallet service to start.");
        Preconditions.checkNotNull(serverWatchingKey, "server watching key has to be set in the storage for the wallet service to start.");

        Context.propagate(new Context(getNetworkParams(bitcoinNet)));

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
                    .marryWallet(serverWatchingKey, mnemonic, creationTime);

            clientWalletKit
                    .setAndroidContext(getCoinbleskApplication())
                    .setBlockingStartup(false)
                    .setDownloadListener(new DownloadProgressTracker() {

                        @Override
                        public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                            super.onChainDownloadStarted(peer, blocksLeft);
                            syncProgress.setProgress(0);
                        }

                        @Override
                        protected void progress(double pct, int blocksSoFar, Date date) {
                            syncProgress.setProgress(pct / 100);
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
                        LOGGER.debug("Post init failed", e);
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
            SendRequest req = SendRequest.emptyWallet(btcAddress);
            req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
            req.memo = DefaultTransactionMemos.REDEPOSIT_TX_MEMO;
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
            if (getBalance().compareTo(BigDecimal.ZERO) == 0) {
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
            } else {
                LOGGER.debug("No re-deposit or refund transaction needed....");
            }
        } catch (Exception e) {
            LOGGER.debug("Unexpected error checking the refund transaction state", e);
        }
    }

    private void createPayment(Address address, Coin amount) throws AddressFormatException, InsufficientMoneyException {

        // create the send request
        SendRequest req = SendRequest.to(address, amount);
        req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
        req.coinSelector = InstantTransactionSelector.get();
        req.tx.getConfidence().setSource(TransactionConfidence.Source.SELF);

        getAppKit().wallet().completeTx(req);

        for (TransactionInput txIn : req.tx.getInputs()) {
            try {
                txIn.verify();
            } catch (VerificationException e) {
                // the inputs are not fully signed yet -> we are not responsible for broadcasting
                // this is expected.
                return;
            }
        }

        // inputs are fully signed. This case should only occur if the refund transaction was broadcast,
        // and the user's funds sent to a personal address.
        LOGGER.debug("Broadcasting fully signed transaction {}", req.tx.getHashAsString());
        getAppKit().peerGroup().broadcastTransaction(req.tx);
        getAppKit().wallet().receivePending(req.tx, null);

        return;
    }

    /**
     * Creates an NFC payment. This means that the half signed transaction is NOT sent to the server, but is returned synchronously.
     * <strong>IMPORTANT: </strong> This method is only intended for sending to P2SH addresses. You pass the hash160 of a normal
     * P2PKH address, the bitcoins will be lost forever!
     *
     * @param scriptHash the hash of the redeem script (P2SH address)
     * @param satoshis   the amount to send, in satoshis
     * @return The half signed, raw bitcoin transaction
     * @throws AddressFormatException
     * @throws InsufficientMoneyException
     */
    public HalfSignedTransaction createNfcPayment(byte[] scriptHash, long satoshis) throws AddressFormatException, InsufficientMoneyException {
        try {
            Address address = Address.fromP2SHHash(getNetworkParams(bitcoinNet), scriptHash);
            nfcMode = true;
            createPayment(address, BitcoinUtils.satoshiToCoin(satoshis));
            return sigReq;
        } finally {
            nfcMode = false;
            sigReq = null;
        }
    }

    public void createPayment(String address, BigDecimal amount) throws AddressFormatException, InsufficientMoneyException {
        NetworkParameters params = getNetworkParams(bitcoinNet);
        Address btcAddress = new Address(params, address);
        createPayment(btcAddress, BitcoinUtils.bigDecimalToCoin(amount));
    }

    /**
     * @return The maximum amount of bitcoins to send from this wallet,
     * taking the fee into consideration.
     */
    public BigDecimal getMaxSendAmount() {
        // create a send request emptying the wallet, but without signing the transactions. From
        // the value that is sent to us equals out total amount available for spending.
        SendRequest req = SendRequest.forTx(new Transaction(getNetworkParams(bitcoinNet)));
        req.tx.addOutput(Coin.ZERO, getAppKit().wallet().currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS));
        req.emptyWallet = true;
        req.signInputs = false;
        try {
            getAppKit().wallet().completeTx(req);
        } catch (InsufficientMoneyException e) {
            // cannot happen
        }
        return BitcoinUtils.coinToBigDecimal(req.tx.getValueSentToMe(getAppKit().wallet()));
    }

    /**
     * Broadcasts the stored refund transaction.
     */
    public void broadcastRefundTx() {

        byte[] serializedRefundTx = Base64.decode(storage.getRefundTx(), Base64.NO_WRAP);
        Transaction refundTx = new Transaction(getNetworkParams(bitcoinNet), serializedRefundTx);
        SendRequest req = SendRequest.forTx(refundTx);

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

    public String getBitcoinAddress() {
        return getAppKit().wallet().currentReceiveAddress().toString();
    }

    public BigDecimal getUnconfirmedBalance() {
        return BitcoinUtils.coinToBigDecimal(getAppKit().wallet().getBalance(BalanceType.ESTIMATED));
    }

    public BigDecimal getBalance() {
        return BitcoinUtils.coinToBigDecimal(getAppKit().wallet().getBalance(InstantTransactionSelector.get()));
    }

    /**
     * Checks if a transaction was signed by the server. It does so by extracting the signatures from the
     * raw transaction and checking them against the server public key.
     *
     * @param rawTx        the raw transaction that was signed by the server
     * @param childNumbers the child numbers of the keys used to sign the transaction. They must be in the same order as the inputs
     *                     of the raw transaction.
     * @return true if the transaction was signed by the server
     */
    public boolean isTxSignedByServer(byte[] rawTx, byte[] accountNumbers, int[] childNumbers) {

        Transaction tx = new Transaction(getNetworkParams(bitcoinNet), rawTx);

        if (tx.getInputs().size() != childNumbers.length) {
            throw new IllegalArgumentException("Number of child numbers should be the same as number of inputs of the server signed transaction. ");
        }

        for (int i = 0; i < tx.getInputs().size(); i++) {
            TransactionInput txIn = tx.getInputs().get(i);

            // Master and account keys are always 0: M/0H/{0,1}/childNumbers[i]
            List<ChildNumber> path = Lists.newArrayList(new ChildNumber(0, true), new ChildNumber(accountNumbers[i]), new ChildNumber(childNumbers[i]));

            DeterministicKey pubKeyServer = getServerKeyChain().getKeyByPath(path, true);

            Script redeemScript = new Script(txIn.getScriptSig().getChunks().get(txIn.getScriptSig().getChunks().size() - 1).data);
            Sha256Hash sighash = tx.hashForSignature(i, redeemScript, Transaction.SigHash.ALL, false);
            int insertionIndex = txIn.getScriptSig().getSigInsertionIndex(sighash, pubKeyServer);
            byte[] bitcoinServerSig = txIn.getScriptSig().getChunks().get(insertionIndex).data;
            byte[] serverSigDer = TransactionSignature.decodeFromBitcoin(bitcoinServerSig, false).encodeToDER();

            if (!pubKeyServer.verify(sighash.getBytes(), serverSigDer)) {
                return false;
            }
        }

        return true;
    }

    private DeterministicKeyChain getServerKeyChain() {
        if (serverKeyChain == null) {
            DeterministicKey serverKey = DeterministicKey.deserializeB58(serverWatchingKey, getNetworkParams(bitcoinNet));
            serverKeyChain = new DeterministicKeyChain(serverKey);
        }
        return serverKeyChain;
    }

    /**
     * Creates a time-locked transaction signed by the coinblesk server that sends all available coins to a personal address, and
     * stores it on the device.
     */
    public void createRefundTransaction() {

        try {
            Address privateAddr = new Address(getNetworkParams(bitcoinNet), getPrivateAddress());
            SendRequest req = SendRequest.emptyWallet(privateAddr);
            req.coinSelector = InstantTransactionSelector.get();
            req.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;

            // collect all unspents
            List<Transaction> unspents = new ArrayList<>();
            unspents.addAll(getAppKit().wallet().getTransactionPool(WalletTransaction.Pool.UNSPENT).values());
            unspents.addAll(new ArrayList<>(getAppKit().wallet().getTransactionPool(WalletTransaction.Pool.PENDING).values()));

            Preconditions.checkState(!unspents.isEmpty(), "Cannot create refund transaction without any unspent transactions.");

            for (Transaction tx : getAppKit().wallet().getTransactions(true)) {
                if (InstantTransactionSelector.get().shouldSelect(tx)) {
                    for (TransactionOutput txOut : tx.getOutputs()) {
                        if (txOut.isMine(getAppKit().wallet())) {
                            req.tx.addInput(txOut);
                        }
                    }
                }
            }

            if (req.tx.getInputs().isEmpty()) {
                LOGGER.debug("No unspent output found with enough confirmations. Abort creating refund txransaction");
                return;
            }

            // at least one input needs a so-called sequence number set to 0.
            // For details, see: https://bitcoin.org/en/developer-guide#locktime-and-sequence-number
            req.tx.getInput(0).setSequenceNumber(0);

            // set the locktime
            long lockTime = getLockTime(REFUND_LOCKTIME_MONTH);
            req.tx.setLockTime(lockTime);

            req.memo = DefaultTransactionMemos.REFUND_TX_MEMO;

            getAppKit().wallet().completeTx(req);

        } catch (Exception e) {
            LOGGER.error("Failed to create refund transaction", e);
        }
    }

    private long getLockTime(int month) {
        long lockTime = BitcoinUtils.monthsToBlocks(month); //assuming 1 block every 10 minutes
        LOGGER.debug("Lock time of {} month ({} blocks) plus current block height of {}", month, lockTime, getCurrentBlockHeight());
        return getCurrentBlockHeight() + lockTime;
    }

    public TransactionHistory getTransactionHistory() {
        return getTransactionHistory(200);
    }

    public TransactionHistory getTransactionHistory(int maxNumberOfTransactions) {

        List<Transaction> txs = getAppKit().wallet().getRecentTransactions(maxNumberOfTransactions, false);

        List<TransactionMetaData> allTransactions = Lists.newArrayListWithCapacity(maxNumberOfTransactions);


        for (Transaction tx : txs) {

            // get the amount sent to/from our wallet
            BigDecimal amount = BitcoinUtils.coinToBigDecimal(tx.getValue(getAppKit().wallet()));

            // now let's translate the bitcoinJ Transaction to our simplified Transaction object
            TransactionMetaData transaction = storage.getTransactionMetaData(tx.getHashAsString());
            transaction = transaction != null ? transaction : new TransactionMetaData();

            transaction.setAmount(amount.abs());
            transaction.setTimestamp(tx.getUpdateTime());
            transaction.setConfirmations(tx.getConfidence().getDepthInBlocks());

            if (amount.signum() < 0) {
                // negative amount -> pay out
                transaction.maybeSetType(TransactionMetaData.TransactionType.PAY_OUT);
            } else {
                // positive amount -> pay in
                if (tx.getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATIONS) {
                    // confirmed tx
                    transaction.maybeSetType(TransactionMetaData.TransactionType.PAY_IN);
                } else {
                    // unconfirmed tx
                    transaction.maybeSetType(TransactionMetaData.TransactionType.PAY_IN_UNVERIFIED);
                }
            }

            allTransactions.add(transaction);
        }

        return new TransactionHistory(allTransactions);
    }

    /**
     * Adds a raw transaction to the wallet, if not already present, and broadcasts it to the network.
     *
     * @param txBytes the raw transaction
     * @return the transaction hash
     */
    public String commitAndBroadcastTx(byte[] txBytes) {

        Context.propagate(new Context(getNetworkParams(bitcoinNet)));

        Transaction tx = new Transaction(getNetworkParams(bitcoinNet), txBytes);
        tx.setMemo(DefaultTransactionMemos.SERVER_SIGNED_TX);

        try {
            // check if we have this transaction already in our wallet. This is well possible, because the
            // server also broadcasts the transaction, and maybe we already received it.
            if (getAppKit().wallet().getTransaction(tx.getHash()) == null) {
                getAppKit().wallet().receivePending(tx, null);
                getAppKit().peerGroup().broadcastTransaction(tx);
            }
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to commit and broadcast transaction. ", e);
        }

        return tx.getHashAsString();
    }

    /**
     * Checks in the transaction meta data if the transactions in the wallets were
     * instant transactions and if so the transaction memo is set to {@link DefaultTransactionMemos#SERVER_SIGNED_TX}
     */
    public void setTxMemos() {
        //TODO: make this more efficient, eg by loading all transaction meta data at once...
        for (Transaction tx : getAppKit().wallet().getTransactions(true)) {
            TransactionMetaData metaData = storage.getTransactionMetaData(tx.getHashAsString());

            if (metaData == null) break;

            if (metaData.getType() == TransactionMetaData.TransactionType.COINBLESK_PAY_IN || metaData.getType() == TransactionMetaData.TransactionType.COINBLESK_PAY_OUT) {
                tx.setMemo(DefaultTransactionMemos.SERVER_SIGNED_TX);
            }
        }
    }

    /**
     * Returns the amount of bitcoins sent to the user's wallet
     *
     * @param rawTx the raw transaction that sends bitcoins to the user
     * @return the amount of bitcoins sent to the user
     */
    public BigDecimal getAmountSentToMe(byte[] rawTx) {
        Transaction tx = new Transaction(getNetworkParams(bitcoinNet), rawTx);
        return BitcoinUtils.coinToBigDecimal(tx.getValueSentToMe(getAppKit().wallet()));
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
     * Returns the current chain height. If not connected to any peers yet, the stored block head will be used as
     * a reference.
     *
     * @return the current block height
     */
    public long getCurrentBlockHeight() {
        return Math.max(getAppKit().peerGroup().getMostCommonChainHeight(), getAppKit().chain().getChainHead().getHeight());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return walletBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOGGER.debug("Wallet service was stopped");
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
