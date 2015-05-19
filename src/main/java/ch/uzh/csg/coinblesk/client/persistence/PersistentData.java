package ch.uzh.csg.coinblesk.client.persistence;

import java.util.Set;

import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

public interface PersistentData {
    
    void setServerIp(String ip) throws Exception;

    void deletePersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception;

    void addPersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception;

    Set<PersistedPaymentRequest> getPersistedPaymentRequests() throws Exception;

    void removeTrustedAddressBookEntry(String username) throws Exception;

    void removeAddressBookContact(String username) throws Exception;

    void addTrustedAddressBookContact(String username) throws Exception;

    Set<String> getTrusteAddressBookdContacts() throws Exception;

    void addAddressBookContact(String username) throws Exception;

    Set<String> getAddressBookContacts() throws Exception;

    CustomKeyPair getUserKeyPair() throws Exception;

    void setUserKeyPair(CustomKeyPair customKeyPair) throws Exception;

    UserAccountObject getUserAccount();

    void setUserAccount(UserAccountObject userAccount) throws Exception;

    /**
     * Sets the server's {@link CustomPublicKey} to the xml and returns the
     * updated xml string.
     * 
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    CustomPublicKey getServerPublicKey() throws Exception;

    /**
     * Sets the given server IP to the xml and returns the updated xml string.
     * 
     * @throws Exception
     *             an xml exception
     */
    void setServerPublicKey(CustomPublicKey publicKey) throws Exception;

    /**
     * Returns the server IP from the given xml string.
     * 
     *            the xml string to read out the server IP from
     * @return the server IP or null, if this field is not set
     * @throws Exception
     *             an xml exception
     */
    String getServerIp() throws Exception;


    String getServerWatchingKey();

    void setServerWatchingKey(String serverWatchingKey);

    String getBitcoinNet();

    void setBitcoinNet(String bitcoinNet);
}