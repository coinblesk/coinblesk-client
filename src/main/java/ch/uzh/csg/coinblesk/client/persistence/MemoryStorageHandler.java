package ch.uzh.csg.coinblesk.client.persistence;

import java.util.Set;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

/**
 * This storage handler does not persist data. Should not be used in production.
 */
public class MemoryStorageHandler implements StorageHandler {

    private long refundTxValidBlock = RefundTx.NO_REFUND_TX_VALID_BLOCK;
    private String refundTx;
    private BitcoinNet bitcoinNet;
    private String serverWatchingKey;

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setUsername(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> getAddressBook() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean addAddressBookEntry(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean removeAddressBookEntry(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean removeAllUntrustedAddressBookEntries() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> getTrustedAddressbook() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isTrustedContact(String contact) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean addTrustedAddressBookEntry(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean removeTrustedAddressBookEntry(String username) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getWatchingKey() {
        return serverWatchingKey;
    }

    @Override
    public void setWatchingKey(String watchingKey) {
        this.serverWatchingKey = watchingKey;
    }

    @Override
    public BitcoinNet getBitcoinNet() {
        return bitcoinNet;
    }

    @Override
    public void setBitcoinNet(BitcoinNet bitcoinNet) {
        this.bitcoinNet = bitcoinNet;
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
        return getBitcoinNet() != null &&
                getWatchingKey() != null;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
