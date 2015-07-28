package ch.uzh.csg.coinblesk.client.persistence;

import java.util.Set;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;

/**
 * Created by rvoellmy on 7/6/15.
 */
public interface StorageHandler {

    String getUsername();
    void setUsername(String username);

    /**
     * Returns an alphabetically ordered Set<String> with all usernames stored
     * in address book.
     *
     * @return Set<String> with all usernames stored in address book or null, if
     * there is no such element in the underlying xml
     */
    Set<String> getAddressBook();

    /**
     * Adds an entry (username) to the address book of known users.
     *
     * @param username the username to be saved
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    boolean addAddressBookEntry(String username);

    /**
     * Removes an entry (username) from the address book of known users.
     *
     * @param username the username to be removed
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    boolean removeAddressBookEntry(String username);

    /**
     * Removes all entries from address book which are not trusted.
     *
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    boolean removeAllUntrustedAddressBookEntries();

    /**
     * Returns an alphabetically ordered Set<String> with all usernames of
     * trusted users stored in the trusted address book.
     *
     * @return Set<String> with all trusted usernames (alphabetically ordered)
     * or null, if there is no such element in the underlying xml
     */
    Set<String> getTrustedAddressbook();

    boolean isTrustedContact(String contact);

    /**
     * Adds an entry (username) to the address book of trusted users.
     *
     * @param username the username to be saved
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    boolean addTrustedAddressBookEntry(String username);

    /**
     * Removes an entry (username) from the address book of trusted users.
     *
     * @param username the username to be removed
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    boolean removeTrustedAddressBookEntry(String username);

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
}
