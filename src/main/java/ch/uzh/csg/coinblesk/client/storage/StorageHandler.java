package ch.uzh.csg.coinblesk.client.storage;

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

    String getWatchingKey();

    void setWatchingKey(String watchingKey);

    BitcoinNet getBitcoinNet();

    void setBitcoinNet(BitcoinNet bitcoinNet);

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
     * <p>
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

    public void saveAddressBookEntry(AddressBookEntry entry);
    public AddressBookEntry getAddressBookEntry(PublicKey publicKey);

    public void saveTransactionMetaData(TransactionMetaData txMetaData);
    public TransactionMetaData getTransactionMetaData(String txId);

    public KeyPair getKeyPair();
    public void setKeyPair(KeyPair keyPair);

    void removeAllUntrustedAddressBookEntries();

    List<AddressBookEntry> getAddressBook();

    void deleteAddressBookEntry(PublicKey pubKey);
}
