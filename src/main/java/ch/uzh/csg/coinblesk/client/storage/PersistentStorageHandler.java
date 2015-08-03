package ch.uzh.csg.coinblesk.client.storage;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * Handles storing and retrieving user information which needs to be persisted
 * locally.
 */
public class PersistentStorageHandler implements StorageHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(PersistentStorageHandler.class);

    private PreferencesHandler data;
    private DatabaseHandler db;

    /**
     * Instantiates a new {@link PersistentStorageHandler}.
     */
    public PersistentStorageHandler(Context context) {
        this.data = new CoinBleskCloudData(context);
        this.db = new DatabaseHandler();
    }

    @Override
    public String getUsername() {
        return data.getUsername();
    }

    @Override
    public void setUsername(String username) {
        data.setUsername(username);
    }


    @Override
    public String getWatchingKey() {
        return data.getServerWatchingKey();
    }

    @Override
    public void setWatchingKey(String watchingKey) {
        data.setServerWatchingKey(watchingKey);
    }

    @Override
    public BitcoinNet getBitcoinNet() {
        String bitcoinNet = data.getBitcoinNet();
        return bitcoinNet == null ? null : BitcoinNet.of(bitcoinNet);
    }

    @Override
    public void setBitcoinNet(BitcoinNet bitcoinNet) {
        data.setBitcoinNet(bitcoinNet.toString());
    }

    @Override
    public String getRefundTx() {
        return data.getRefundTx();
    }

    @Override
    public void setRefundTx(String base64encodedRefundTx) {
        data.setRefundTx(base64encodedRefundTx);
    }

    @Override
    public long getRefundTxValidBlock() {
        return data.getRefundTxValidBlock();
    }

    @Override
    public void setRefundTxValidBlock(long refundTxValidBlock) {
        data.setRefundTxValidBlock(refundTxValidBlock);
    }

    @Override
    public boolean hasUserData() {
        return getBitcoinNet() != null &&
                getWatchingKey() != null;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void saveAddressBookEntry(AddressBookEntry entry) {
        db.saveAddressBookEntry(entry);
    }

    @Override
    public AddressBookEntry getAddressBookEntry(PublicKey publicKey) {
        return db.getAddressBookEntry(publicKey);
    }

    @Override
    public void saveTransactionMetaData(TransactionMetaData txMetaData) {
        db.saveTransactionMetaData(txMetaData);
    }

    @Override
    public TransactionMetaData getTransactionMetaData(String txId) {
        return db.getTransactionMetaData(txId);
    }

    @Override
    public KeyPair getKeyPair() {
        return data.getKeyPair();
    }

    @Override
    public void setKeyPair(KeyPair keyPair) {
        data.setKeyPair(keyPair);
    }



    @Override
    public void removeAllUntrustedAddressBookEntries() {
        db.removeAllUntrustedAddressBookEntries();
    }



    @Override
    public List<AddressBookEntry> getAddressBook() {
        return db.getAddressBook();
    }

    @Override
    public void deleteAddressBookEntry(PublicKey pubKey) {
        db.deleteAddressBookEntry(pubKey);
    }


}
