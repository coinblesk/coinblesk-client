package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.MarriedKeyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.SecureRandom;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;

/**
 * Created by rvoellmy on 5/24/15.
 */
public class CoinBleskWalletAppKit extends WalletAppKit {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoinBleskWalletAppKit.class);

    /**
     * This class holds the data needed for restoring a wallet
     */
    private class RestoreData {
        public final String mnemonic;
        public final Long creationDate;

        public RestoreData(String mnemonic, Long creationDate) {
            this.creationDate = creationDate;
            this.mnemonic = mnemonic;
        }
    }

    private Context context;

    private String serverSeed;
    private RestoreData restoreData;

    public CoinBleskWalletAppKit(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    @Override
    protected void onSetupCompleted() {
        maybeAllowSpendingUnconfirmed();
        maybeMarryWallet();
        maybeInitServerTransactionSigner(context);
    }

    private void maybeInitServerTransactionSigner(final Context context) {

        Preconditions.checkNotNull(context);

        // create listener for when transactions have been signed
        StorageHandler storage = ((CoinBleskApplication) context.getApplicationContext()).getStorageHandler();
        TransactionSigningCompleteListener listener = new DefaultTransactionSigningCompleteListener(vWallet, storage);

        // add the server transaction signer to the wallet
        for (TransactionSigner signer : vWallet.getTransactionSigners()) {
            if (signer instanceof ServerTransactionSigner) {
                LOGGER.debug("Server transaction signer already installed, set android context");
                ((ServerTransactionSigner) signer).setContext(context);
                ((ServerTransactionSigner) signer).addTransactionSigningCompleteListener(listener);
                return;
            }
        }

        // add the transaction signer to the wallet that is responsible of
        // obtaining the server's signature
        LOGGER.debug("No server transaction signer found, initialize it now.");
        ServerTransactionSigner serverTransactionSigner = new ServerTransactionSigner();
        serverTransactionSigner.setContext(context);
        serverTransactionSigner.addTransactionSigningCompleteListener(listener);
        vWallet.addTransactionSigner(serverTransactionSigner);

    }

    public CoinBleskWalletAppKit setAndroidContext(Context context) {
        this.context = context;
        return this;
    }

    public CoinBleskWalletAppKit marryWallet(String serverSeed, @Nullable String mnemonic, @Nullable Long creationTime) {
        this.serverSeed = serverSeed;
        this.restoreData = (mnemonic == null || creationTime == null) ? null : new RestoreData(mnemonic, creationTime);
        return this;
    }

    private void maybeAllowSpendingUnconfirmed() {
        // allow spending unconfirmed txs on regtest and testnet
        if (params == TestNet3Params.get() || params == RegTestParams.get()) {
            LOGGER.debug("Allowing spending unconfirmed transactions");
            vWallet.allowSpendingUnconfirmedTransactions();
        }
    }

    /**
     * This method is called after the wallet has been set up.
     */
    private void maybeMarryWallet() {

        if (vWallet.getActiveKeychain().isMarried()) {
            LOGGER.debug("Wallet is already married to the server.");
            return;
        }

        DeterministicSeed seed;
        if (null != restoreData) {
            // restore wallet
            try {
                LOGGER.debug("Restoring wallet from seed");
                seed = new DeterministicSeed(restoreData.mnemonic, null, "", restoreData.creationDate);
            } catch (UnreadableWalletException e) {
                // TODO: handle this
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.debug("Creating new wallet seed");
            seed = getNewWalletSeed();
        }

        // new wallet -> setup married wallet
        LOGGER.info("Marrying wallet to the server key");

        // create watching HD key from server seed
        DeterministicKey serverWatchingKey = DeterministicKey.deserializeB58(serverSeed, params);

        // marry this clients wallet to the server wallet
        MarriedKeyChain marriedKeyChain = MarriedKeyChain.builder()
                .seed(seed)
                .followingKeys(serverWatchingKey)
                .threshold(2)
                .build();

        LOGGER.debug("Activating married key chain");
        vWallet.addAndActivateHDChain(marriedKeyChain);

    }

    /**
     * @return e new wallet seed
     */
    private DeterministicSeed getNewWalletSeed() {

        // bitcoinJ will synchronize 1 week ahead of the specified time because
        // of time skews. So using the current time is absolutely safe
        DeterministicSeed seed = new DeterministicSeed(new SecureRandom(), 128, "", System.currentTimeMillis() / 1000);

        return seed;
    }


}
