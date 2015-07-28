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
public class PersistentStorageHandler implements StorageHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(PersistentStorageHandler.class);

    private PersistentStorage data;

    private Set<String> addressbook;
    private Set<String> trustedAddressbook;


    /**
     * Instantiates a new {@link PersistentStorageHandler}.
     *
     */
    public PersistentStorageHandler(Context context) {
        this.data = new CoinBleskCloudData(context);
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
    public Set<String> getAddressBook() {
        if (addressbook == null) {
            addressbook = data.getAddressBookContacts();
        }
        return addressbook;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Set<String> getTrustedAddressbook() {
        if (trustedAddressbook == null) {
            trustedAddressbook = data.getTrusteAddressBookdContacts();
        }
        return trustedAddressbook;
    }

    @Override
    public boolean isTrustedContact(String contact) {
        return getTrustedAddressbook().contains(contact);
    }

    @Override
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

    @Override
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
        LOGGER.debug("Saving bitcoin net {}", bitcoinNet);
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


}
