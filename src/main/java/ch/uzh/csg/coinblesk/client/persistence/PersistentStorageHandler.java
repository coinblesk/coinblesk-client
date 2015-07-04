package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;

/**
 * Handles storing and retrieving user information which needs to be persisted
 * locally.
 */
public class PersistentStorageHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(PersistentStorageHandler.class);

    private PersistentStorage data;

    private Set<String> addressbook;
    private Set<String> trustedAddressbook;


    /**
     * Instantiates a new {@link PersistentStorageHandler}.
     *
     * @param context the application's context used to be able to write on the
     *                private storage
     */
    public PersistentStorageHandler(Context context) {
        this.data = PersistenceFactory.getCloudStorage(context);
    }

    /**
     * Returns an alphabetically ordered Set<String> with all usernames stored
     * in address book.
     *
     * @return Set<String> with all usernames stored in address book or null, if
     * there is no such element in the underlying xml
     */
    public Set<String> getAddressBook() {
        if (addressbook == null) {
            addressbook = data.getAddressBookContacts();
        }
        return addressbook;
    }

    /**
     * Adds an entry (username) to the address book of known users.
     *
     * @param username the username to be saved
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    public boolean addAddressBookEntry(String username) {
        if (getAddressBook().contains(username)) {
            return true;
        } else {
            addressbook.add(username);
            try {
                data.addAddressBookContact(username);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Removes an entry (username) from the address book of known users.
     *
     * @param username the username to be removed
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    public boolean removeAddressBookEntry(String username) {
        boolean isTrusted = trustedAddressbook.contains(username);

        if (!getAddressBook().contains(username)) {
            return true;
        } else {
            addressbook.remove(username);
            if (isTrusted) {
                trustedAddressbook.remove(username);
            }
            try {
                data.removeAddressBookContact(username);

                if (isTrusted) {
                    //delete also trusted address book entry if available
                    data.removeTrustedAddressBookEntry(username);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Removes all entries from address book which are not trusted.
     *
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    public boolean removeAllUntrustedAddressBookEntries() {
        Set<String> trustedAddressBook = new TreeSet<>(getTrustedAddressbook());
        Set<String> addressesToRemove = new TreeSet<>(getAddressBook());

        boolean removed = false;
        if (!trustedAddressBook.isEmpty()) {
            removed = addressesToRemove.removeAll(trustedAddressBook);
        }

        if (removed || trustedAddressBook.isEmpty()) {
            try {
                for (Iterator<String> it = addressesToRemove.iterator(); it.hasNext(); ) {
                    String username = it.next();
                    data.removeAddressBookContact(username);
                    addressbook.remove(username);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns an alphabetically ordered Set<String> with all usernames of
     * trusted users stored in the trusted address book.
     *
     * @return Set<String> with all trusted usernames (alphabetically ordered)
     * or null, if there is no such element in the underlying xml
     */
    public Set<String> getTrustedAddressbook() {
        if (trustedAddressbook == null) {
            trustedAddressbook = data.getTrusteAddressBookdContacts();
        }
        return trustedAddressbook;
    }

    public boolean isTrustedContact(String contact) {
        return getTrustedAddressbook().contains(contact);
    }

    /**
     * Adds an entry (username) to the address book of trusted users.
     *
     * @param username the username to be saved
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    public boolean addTrustedAddressBookEntry(String username) {
        if (getTrustedAddressbook().contains(username)) {
            return true;
        } else {
            trustedAddressbook.add(username);
            try {
                data.addTrustedAddressBookContact(username);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Removes an entry (username) from the address book of trusted users.
     *
     * @param username the username to be removed
     * @return true if the file was saved, false if it is just set temporarily
     * but could not be persisted
     */
    public boolean removeTrustedAddressBookEntry(String username) {
        if (!getTrustedAddressbook().contains(username)) {
            return true;
        } else {
            trustedAddressbook.remove(username);
            try {
                data.removeTrustedAddressBookEntry(username);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public String getWatchingKey() {
        return data.getServerWatchingKey();
    }

    public void setWatchingKey(String watchingKey) {
        data.setServerWatchingKey(watchingKey);
    }

    public BitcoinNet getBitcoinNet() {
        String bitcoinNet = data.getBitcoinNet();
        return bitcoinNet == null ? null : BitcoinNet.of(bitcoinNet);
    }

    public void setBitcoinNet(BitcoinNet bitcoinNet) {
        LOGGER.debug("Saving bitcoin net {}", bitcoinNet);
        data.setBitcoinNet(bitcoinNet.toString());
    }

    /**
     * @return the last, base64 encoded refund transaction
     */
    public String getRefundTx() {
        return data.getRefundTx();
    }

    /**
     * Set the newest refund transaction. Old refund transactions are deleted.
     *
     * @param base64encodedRefundTx the base64 encoded refund transaction
     */
    public void setRefundTx(String base64encodedRefundTx) {
        data.setRefundTx(base64encodedRefundTx);
    }


    /**
     * This method returns the block number from which the a earlier generated refund transaction
     * becomes valid and ready to spend. This means that the server can no longer guarantee that
     * the client doesn't double-spend.
     * Usually if the last seen block's number is larger than this, we need to re-deposit the bitcoins.
     *
     * @return block number when at least one earlier generated refund-transaction becomes valid
     * <strong>or</strong> -1 if no block number has been set.
     */
    public long getRefundTxValidBlock() {
        return data.getRefundTxValidBlock();
    }

    /**
     * This method returns sets block number from which the a earlier generated refund transaction
     * becomes valid and ready to spend. This means that the server can no longer guarantee that
     * the client doesn't double-spend.
     * <p>
     * Usually this is set after a redeposit or new wallet setup, after the refund transaction
     * has been signed by the server.
     */
    public void setRefundTxValidBlock(long refundTxValidBlock) {
        data.setRefundTxValidBlock(refundTxValidBlock);
    }

    /**
     * Checks if all data that is required to launch the wallet is stored on the device.
     *
     * @return true if all required data is stored on the device.
     */
    public boolean hasUserData() {
        return getBitcoinNet() != null &&
                getWatchingKey() != null;
    }

    /**
     * deletes all internal data
     */
    public void clear() {
        data.clear();
    }


}
