package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.Transaction;

/**
 * Created by rvoellmy on 6/30/15.
 */
public interface TransactionSigningCompleteListener {
    void onSuccess(Transaction tx);
}
