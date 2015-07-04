package ch.uzh.csg.coinblesk.client.wallet;

import android.util.Base64;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

import ch.uzh.csg.coinblesk.client.persistence.PersistentStorageHandler;

/**
 * Created by rvoellmy on 7/4/15.
 */
public class DefaultTransactionSigningCompleteListener extends TransactionSigningCompleteListener {

    private Wallet wallet;
    private PersistentStorageHandler storage;

    public DefaultTransactionSigningCompleteListener(Wallet wallet, PersistentStorageHandler storage) {
        this.wallet = wallet;
        this.storage = storage;
    }

    @Override
    public void onSuccess(Transaction tx) {
        if(!tx.isTimeLocked()) {
            // the signerd transaction was a normal transaction. Commit it to the wallet,
            // if it was not already seen on the network.
            wallet.maybeCommitTx(tx);
        } else {
            // the signed transaction was a refund transaction
            storage.setRefundTx(Base64.encodeToString(tx.unsafeBitcoinSerialize(), Base64.NO_WRAP));
            if(storage.getRefundTxValidBlock() > tx.getLockTime() || storage.getRefundTxValidBlock() < 0) {
                // save the block when this refund transaction becomes valid
                storage.setRefundTxValidBlock(tx.getLockTime());
            }
        }
    }
}
