package ch.uzh.csg.coinblesk.client.persistence;

import java.util.Set;

public interface PersistentStorage {
    
    void removeTrustedAddressBookEntry(String username);
    void removeAddressBookContact(String username);

    void addTrustedAddressBookContact(String username);
    Set<String> getTrusteAddressBookdContacts();

    void addAddressBookContact(String username);
    Set<String> getAddressBookContacts();

    String getServerWatchingKey();
    void setServerWatchingKey(String serverWatchingKey);

    String getBitcoinNet();
    void setBitcoinNet(String bitcoinNet);

    void setRefundTx(String base64EncodedTx);
    String getRefundTx();

    void setRefundTxValidBlock(long validBlockNr);
    long getRefundTxValidBlock();

    void setUsername(String username);
    String getUsername();

    /**
     * Deletes all data
     */
    void clear();
}