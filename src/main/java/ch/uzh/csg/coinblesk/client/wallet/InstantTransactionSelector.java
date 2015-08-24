package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.wallet.DefaultCoinSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.util.Constants;

/**
 * This is a custom {@link org.bitcoinj.wallet.CoinSelector} that collects unspent transactions that
 * are either transactions signed by the server or already have the minimum amount of confirmations.
 */
public class InstantTransactionSelector extends DefaultCoinSelector {

    private final static Logger LOGGER = LoggerFactory.getLogger(InstantTransactionSelector.class);

    private static InstantTransactionSelector instance;

    /** Returns a global static instance of the selector. */
    public static InstantTransactionSelector get() {
        // This doesn't have to be thread safe as the object has no state, so discarded duplicates are harmless.
        if (instance == null)
            instance = new InstantTransactionSelector();
        return instance;
    }

    @Override
    protected boolean shouldSelect(Transaction tx) {
        boolean hasMinConf = tx.getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATIONS;
        boolean isServerSigned = (tx.getMemo() != null && tx.getMemo().equals(DefaultTransactionMemos.SERVER_SIGNED_TX));
        boolean isSelfCreated = tx.getConfidence().getSource() == TransactionConfidence.Source.SELF;
        LOGGER.trace("Selecting transaction {}: HasMinConf={}, isServerSigned={}, isSelfSigned={}", tx.getHashAsString(), hasMinConf, isServerSigned, isSelfCreated);


        return hasMinConf || isServerSigned || isSelfCreated;
    }

}
