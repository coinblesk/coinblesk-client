package ch.uzh.csg.coinblesk.client.storage;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
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

    private CoinBleskCloudData data;
    private DatabaseHandler db;
    private List<StorageHandlerListener> storageHandlerListener = new ArrayList<StorageHandlerListener>();
    private boolean storageFailed = false;


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
    public String getServerWatchingKey() {
        return data.getServerWatchingKey();
    }

    @Override
    public boolean hasSentClientWatchingKey() {
        return data.hasSentClientWatchingKey();
    }
    @Override
    public void sentClientWatchingKey(boolean hasSentClientWatchingKey) {
        data.sentClientWatchingKey(hasSentClientWatchingKey);
    }

    @Override
    public BitcoinNet getBitcoinNet() {
        String bitcoinNet = data.getBitcoinNet();
        return bitcoinNet == null ? null : BitcoinNet.of(bitcoinNet);
    }

    @Override
    public void setBitcoinNetAndServerWatchingKey(BitcoinNet bitcoinNet, String serverWatchingKey) {
        data.setBitcoinNet(bitcoinNet.toString());
        data.setServerWatchingKey(serverWatchingKey);
        synchronized (storageHandlerListener) {
            for (StorageHandlerListener listener : storageHandlerListener) {
                listener.storageHandlerSet(this);
            }
            storageHandlerListener.clear();
        }
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
        return getBitcoinNet() != null && getServerWatchingKey()!=null;
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

    @Override
    public void addStorageHandlerListener(StorageHandlerListener storageHandlerListener) {
        if(data.getServerWatchingKey() != null && data.getBitcoinNet() != null) {
            storageHandlerListener.storageHandlerSet(this);
        } else if(storageFailed) {
            storageHandlerListener.failed();
        } else {
            synchronized (storageHandlerListener) {
                this.storageHandlerListener.add(storageHandlerListener);
            }
        }
    }

    @Override
    public void setStorageFailed(boolean failed) {
        this.storageFailed = failed;

        synchronized (storageHandlerListener) {
             for (StorageHandlerListener listener : storageHandlerListener) {
                 if(failed) {
                      listener.failed();
                 } else {
                     listener.storageHandlerSet(this);
                 }
             }
                storageHandlerListener.clear();;
        }
    }

}
