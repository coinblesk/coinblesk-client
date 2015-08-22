package ch.uzh.csg.coinblesk.client.storage;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

/**
 * This storage handler does not persist data. Should not be used in production.
 */
public class MemoryStorageHandler implements StorageHandler {

    private long refundTxValidBlock = RefundTx.NO_REFUND_TX_VALID_BLOCK;
    private String refundTx;
    private BitcoinNet bitcoinNet;
    private String serverWatchingKey;
    private boolean hasSentClientWatchingKey;
    private StorageHandlerListener storageHandlerListener = null;
    private boolean storageFailed = false;

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setUsername(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getServerWatchingKey() {
        return serverWatchingKey;
    }

    @Override
    public boolean hasSentClientWatchingKey() {
        return hasSentClientWatchingKey;
    }
    @Override
    public void sentClientWatchingKey(boolean hasSentClientWatchingKey) {
        this.hasSentClientWatchingKey = hasSentClientWatchingKey;
    }

    @Override
    public BitcoinNet getBitcoinNet() {
        return bitcoinNet;
    }

    @Override
    public void setBitcoinNetAndServerWatchingKey(BitcoinNet bitcoinNet, String serverWatchingKey) {
        this.bitcoinNet = bitcoinNet;
        this.serverWatchingKey = serverWatchingKey;
        if(storageHandlerListener != null) {
            storageHandlerListener.storageHandlerSet(this);
        }
    }

    @Override
    public String getRefundTx() {
        return refundTx;
    }

    @Override
    public void setRefundTx(String base64encodedRefundTx) {
        this.refundTx = base64encodedRefundTx;
    }

    @Override
    public long getRefundTxValidBlock() {
        return refundTxValidBlock;
    }

    @Override
    public void setRefundTxValidBlock(long refundTxValidBlock) {
        this.refundTxValidBlock = refundTxValidBlock;
    }

    @Override
    public boolean hasUserData() {
        return getBitcoinNet() != null && getServerWatchingKey()!=null;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void saveAddressBookEntry(AddressBookEntry entry) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public AddressBookEntry getAddressBookEntry(PublicKey publicKey) {
        throw new UnsupportedOperationException("Not implemented");    }

    @Override
    public void saveTransactionMetaData(TransactionMetaData txMetaData) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public TransactionMetaData getTransactionMetaData(String txId) {
        throw new UnsupportedOperationException("Not implemented");    }

    @Override
    public KeyPair getKeyPair() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setKeyPair(KeyPair keyPair) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeAllUntrustedAddressBookEntries() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AddressBookEntry> getAddressBook() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deleteAddressBookEntry(PublicKey pubKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addStorageHandlerListener(StorageHandlerListener storageHandlerListener) {

    }

    @Override
    public void setStorageFailed(boolean failed) {
        this.storageFailed = failed;
        if(storageHandlerListener != null && failed) {
            storageHandlerListener.failed();
        } else if(storageHandlerListener != null && !failed) {
            storageHandlerListener.storageHandlerSet(this);
        }
    }
}
