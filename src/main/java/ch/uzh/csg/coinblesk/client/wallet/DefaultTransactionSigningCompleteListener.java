package ch.uzh.csg.coinblesk.client.wallet;

import android.util.Base64;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * Created by rvoellmy on 7/4/15.
 */
public class DefaultTransactionSigningCompleteListener extends TransactionSigningCompleteListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTransactionSigningCompleteListener.class);

    private Wallet wallet;
    private StorageHandler storage;

    public DefaultTransactionSigningCompleteListener(Wallet wallet, StorageHandler storage) {
        this.wallet = wallet;
        this.storage = storage;
    }

    @Override
    public void onSuccess(Transaction tx) {

        LOGGER.debug("Received signed transaction from server:\n{}", tx);

        if(!tx.isTimeLocked()) {

            if(tx.getMemo() == DefaultTransactionMemos.REDEPOSIT_TX_MEMO) {
                // this was a redeposit: Reset refund transaction valid block in storage
                storage.setRefundTxValidBlock(wallet.getLastBlockSeenHeight());
            }

            // update meta data
            TransactionMetaData metaData = storage.getTransactionMetaData(tx.getHashAsString());
            if(metaData == null) {
                metaData = metaData != null ? metaData : new TransactionMetaData();
                metaData.setType(TransactionMetaData.TransactionType.COINBLESK_PAY_OUT);
                metaData.setSender("You");
                metaData.setReceiver("Bitcoin Network");
                storage.saveTransactionMetaData(metaData);
                LOGGER.debug("Saved transaction meta data in database: {}", metaData.toString());
            }

            LOGGER.debug("Adding server signed transaction to the wallet", tx.getHashAsString());
            tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
            wallet.receivePending(tx, null);

        } else {
            LOGGER.debug("Saving time-locked refund transaction {} in local storage", tx.getHashAsString());
            // the signed transaction was a refund transaction
            storage.setRefundTx(Base64.encodeToString(tx.unsafeBitcoinSerialize(), Base64.NO_WRAP));
            if(storage.getRefundTxValidBlock() > tx.getLockTime() || storage.getRefundTxValidBlock() < 0) {
                // save the block when this refund transaction becomes valid
                storage.setRefundTxValidBlock(tx.getLockTime());
            }
        }
    }
}
