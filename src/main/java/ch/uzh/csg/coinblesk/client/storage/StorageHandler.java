package ch.uzh.csg.coinblesk.client.storage;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * This class is an abstraction of the local storage. Everything that needs to be persisted on the device is stored and retrieved through this class.
 */
public interface StorageHandler {

    String getUsername();

    void setUsername(String username);

    BitcoinNet getBitcoinNet();

    /**
     * @return the last, base64 encoded refund transaction
     */
    String getRefundTx();

    /**
     * Set the newest refund transaction. Old refund transactions are deleted.
     *
     * @param base64encodedRefundTx the base64 encoded refund transaction
     */
    void setRefundTx(String base64encodedRefundTx);

    /**
     * This method returns the block number from which the a earlier generated refund transaction
     * becomes valid and ready to spend. This means that the server can no longer guarantee that
     * the client doesn't double-spend.
     * Usually if the last seen block's number is larger than this, we need to re-deposit the bitcoins.
     *
     * @return block number when at least one earlier generated refund-transaction becomes valid
     * <strong>or</strong> -1 if no block number has been set.
     */
    long getRefundTxValidBlock();

    /**
     * This method returns sets block number from which the a earlier generated refund transaction
     * becomes valid and ready to spend. This means that the server can no longer guarantee that
     * the client doesn't double-spend.
     * <p/>
     * Usually this is set after a redeposit or new wallet setup, after the refund transaction
     * has been signed by the server.
     */
    void setRefundTxValidBlock(long refundTxValidBlock);

    /**
     * Checks if all data that is required to launch the wallet is stored on the device.
     *
     * @return true if all required data is stored on the device.
     */
    boolean hasUserData();

    /**
     * deletes all internal data
     */
    void clear();

    /**
     * Only relevant for merchant mode. Exchanges don't allow selling too small amounts. Therefore
     * we need to keep track of the amount of collected bitcoins, such that as soon as we
     * exceed the required amount allowed to sell on the exchange, we create a sell order.
     *
     * @return the current bitcoin buffer. 0 if buffer has never been set.
     */
    BigDecimal getBitcoinBuffer();

    /**
     * Only relevant for merchant mode. Exchanges don't allow selling too small amounts. Therefore
     * we need to keep track of the amount of collected bitcoins, such that as soon as we
     * exceed the required amount allowed to sell on the exchange, we create a sell order.
     */
    void setBitcoinBuffer(BigDecimal buffer);

    void saveAddressBookEntry(AddressBookEntry entry);

    AddressBookEntry getAddressBookEntry(PublicKey publicKey);

    void saveTransactionMetaData(TransactionMetaData txMetaData);

    TransactionMetaData getTransactionMetaData(String txId);

    KeyPair getKeyPair();

    void setKeyPair(KeyPair keyPair);

    void removeAllUntrustedAddressBookEntries();

    List<AddressBookEntry> getAddressBook();

    void deleteAddressBookEntry(PublicKey pubKey);

    String getServerWatchingKey();

    void setBitcoinNetAndServerWatchingKey(BitcoinNet bitcoinNet, String serverWatchingKey);

    boolean hasSentClientWatchingKey();

    void sentClientWatchingKey(boolean b);

    void addStorageHandlerListener(StorageHandlerListener storageHandlerListener);

    void setStorageFailed(boolean failed);
}
