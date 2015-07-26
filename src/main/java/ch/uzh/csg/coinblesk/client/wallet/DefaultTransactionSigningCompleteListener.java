package ch.uzh.csg.coinblesk.client.wallet;

import android.util.Base64;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

import ch.uzh.csg.coinblesk.client.persistence.StorageHandler;

/**
 * Created by rvoellmy on 7/4/15.
 */
public class DefaultTransactionSigningCompleteListener extends TransactionSigningCompleteListener {

    private Wallet wallet;
    private StorageHandler storage;

    public DefaultTransactionSigningCompleteListener(Wallet wallet, StorageHandler storage) {
        this.wallet = wallet;
        this.storage = storage;
    }

    @Override
    public void onSuccess(Transaction tx) {
        if(!tx.isTimeLocked()) {
            // the signed transaction was a normal transaction. Commit it to the wallet,
            // if it was not already seen on the network.
            wallet.maybeCommitTx(tx);

            if(tx.getMemo() == DefaultTransactionMemos.REDEPOSIT_TX_MEMO) {
                // this was a redeposit: Reset refund transaction valid block in storage
                storage.setRefundTxValidBlock(wallet.getLastBlockSeenHeight());
            }
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
