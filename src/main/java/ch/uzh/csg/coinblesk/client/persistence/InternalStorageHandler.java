package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.model.UserAccount;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import ch.uzh.csg.paymentlib.persistency.IPersistencyHandler;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

/**
 * Handles storing and retrieving user information which needs to be persisted
 * locally. The user information is encrypted using
 * {@link InternalStorageEncrypter}.
 * 
 * @author Jeton Memeti
 * 
 */
public class InternalStorageHandler implements IPersistencyHandler {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(InternalStorageHandler.class);

	private PersistentData data;

	private UserAccountObject userAccount;
	private CustomKeyPair userKeyPair;
	private CustomPublicKey serverPublicKey;
	private String serverIp;
	private Set<String> addressbook;
	private Set<String> trustedAddressbook;
	private Set<PersistedPaymentRequest> persistedPaymentRequests = null;

	/**
	 * Instantiates a new {@link InternalStorageHandler}.
	 * 
	 * @param context
	 *            the application's context used to be able to write on the
	 *            private storage
	 * @param username
	 *            used as filename
	 * @param password
	 *            used to encrypt/decrypt the file
	 */
	public InternalStorageHandler(Context context, String username, String password) {
		this.data = PersistenceFactory.getCloudStorage(username, password, context);
		this.userAccount = null;
	}
	
	/**
	 * Saves the {@link UserAccount} to the internal storage.
	 * 
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean saveUserAccount(UserAccountObject userAccount) {
	    try {
            data.setUserAccount(userAccount);
            return true;
        } catch (Exception e) {
            LOGGER.error("failed to save user account", e);
            return false;
        }
	}

	/**
	 * Returns the {@link UserAccount} from internal storage.
	 */
	public UserAccountObject getUserAccount() {
		if (userAccount == null) {
		    userAccount = data.getUserAccount();
		}
		return userAccount;
	}
	
	/**
	 * Sets the user's password and saves it to the internal storage. Uses this
	 * password from now on to encrypt/decrypt the file.
	 * 
	 * @param password
	 *            the new password
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean setUserPassword(String password) {
		getUserAccount().setPassword(password);
		return saveUserAccount(userAccount);
	}

	/**
	 * Sets the user's email and saves it to the internal storage.
	 * 
	 * @param email
	 *            the new email address
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean setUserEmail(String email) {
		getUserAccount().setEmail(email);
		return saveUserAccount(userAccount);
	}

	/**
	 * Saves the user's {@link CustomKeyPair} to the internal storage.
	 * 
	 * @param customKeyPair
	 *            the {@link CustomKeyPair} to be saved
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean saveKeyPair(CustomKeyPair customKeyPair) {
		this.userKeyPair = customKeyPair;
		
		try {
            data.setUserKeyPair(customKeyPair);
            return true;
        } catch (Exception e) {
            LOGGER.error("failed to save user account", e);
            return false;
        }
	}

	/**
	 * Returns the user's {@link CustomKeyPair} or null, if it was not set
	 * before.
	 */
	public CustomKeyPair getKeyPair() {
		if (userKeyPair == null) {
			try {
				userKeyPair = data.getUserKeyPair();
			} catch (Exception e) {
			}
		}
		return userKeyPair;
	}
	
	/**
	 * Saves the server's {@link CustomPublicKey} to the internal storage.
	 * 
	 * @param publicKey
	 *            the {@link CustomPublicKey} to be saved
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean saveServerPublicKey(CustomPublicKey publicKey) {
		this.serverPublicKey = publicKey;
		try {
			data.setServerPublicKey(publicKey);
			return true;
		} catch (Exception e) {
			return false;
		}
		
		
		
	}
	
	/**
	 * Returns the server's {@link CustomPublicKey} or null, if there is no
	 * {@link CustomPublicKey} stored.
	 */
	public CustomPublicKey getServerPublicKey() {
		if (serverPublicKey == null) {
			try {
				serverPublicKey = data.getServerPublicKey();
			} catch (Exception e) {
			}
		}
		return serverPublicKey;
	}
	
	/**
	 * Saves the server's IP to the internal storage.
	 * 
	 * @param ip
	 *            the server's IP to be saved
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean saveServerIP(String ip) {
		this.serverIp = ip;
		try {
			data.setServerIp(ip);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the server's IP or null, if it was not set before.
	 */
	public String getServerIP() {
		if (serverIp == null) {
			try {
				serverIp = data.getServerIp();
			} catch (Exception e) {
			}
		}
		return serverIp;
	}
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames stored
	 * in address book.
	 * 
	 * @return Set<String> with all usernames stored in address book or null, if
	 *         there is no such element in the underlying xml
	 */
	public Set<String> getAddressBook() {
		if (addressbook == null) {
			try {
				addressbook = data.getAddressBookContacts();
			} catch (Exception e) {
			}
		}
		return addressbook;
	}
	
	/**
	 * Adds an entry (username) to the address book of known users.
	 * 
	 * @param username
	 *            the username to be saved
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
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
	 * @param username
	 *            the username to be removed
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
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

				if(isTrusted){
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
	 *         but could not be persisted
	 */
	public boolean removeAllUntrustedAddressBookEntries() {
		Set<String> trustedAddressBook = new TreeSet<String>(getTrustedAddressbook());
		Set<String> addressesToRemove = new TreeSet<String>(getAddressBook());

		boolean removed = false;
		if (!trustedAddressBook.isEmpty()) {
			removed = addressesToRemove.removeAll(trustedAddressBook);
		} 
		
		if(removed || trustedAddressBook.isEmpty()){
			try {
				for (Iterator<String> it = addressesToRemove.iterator(); it.hasNext();) {
					String username = it.next();
					data.removeAddressBookContact(username);
					addressbook.remove(username);
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		} else{
			return false;
		}
	}
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames of
	 * trusted users stored in the trusted address book.
	 * 
	 * @return Set<String> with all trusted usernames (alphabetically ordered)
	 *         or null, if there is no such element in the underlying xml
	 */
	public Set<String> getTrustedAddressbook() {
		if (trustedAddressbook == null) {
			try {
				trustedAddressbook = data.getTrusteAddressBookdContacts();
			} catch (Exception e) {
			}
		}
		return trustedAddressbook;
	}
	
	/**
	 * Adds an entry (username) to the address book of trusted users.
	 * 
	 * @param username
	 *            the username to be saved
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
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
	 * @param username
	 *            the username to be removed
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
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
	
	/**
	 * Returns if a given username is saved in trustedAddressBook and therefore
	 * trusted (true) or not (false).
	 * 
	 * @param username
	 *            to check
	 * @return if username is trusted
	 */
	public boolean isTrustedContact(String username) {
		return getTrustedAddressbook().contains(username);
	}
	
	private void initPersistedPaymentRequests() {
		if (persistedPaymentRequests == null) {
			try {
				persistedPaymentRequests = data.getPersistedPaymentRequests();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * See javadoc of {@link IPersistencyHandler}
	 */
	public boolean addPersistedPaymentRequest(PersistedPaymentRequest paymentRequest) {
		initPersistedPaymentRequests();
		if (persistedPaymentRequests == null)
			return false;
		
		if (persistedPaymentRequests.contains(paymentRequest))
			return true;
		
		persistedPaymentRequests.add(paymentRequest);
		try {
			data.addPersistedPaymentRequest(paymentRequest);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * See javadoc of {@link IPersistencyHandler}
	 */
	public boolean deletePersistedPaymentRequest(PersistedPaymentRequest paymentRequest) {
		initPersistedPaymentRequests();
		if (persistedPaymentRequests == null)
			return false;
		
		if (!persistedPaymentRequests.contains(paymentRequest))
			return true;
		
		persistedPaymentRequests.remove(paymentRequest);
		try {
			data.deletePersistedPaymentRequest(paymentRequest);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * See javadoc of {@link IPersistencyHandler}
	 */
	public PersistedPaymentRequest getPersistedPaymentRequest(String username, Currency currency, long amount) {
		initPersistedPaymentRequests();
		if (persistedPaymentRequests == null)
			return null;
		
		// timestamp -1 will be ignored in the equals method
		PersistedPaymentRequest toSearchFor = new PersistedPaymentRequest(username, currency, amount, -1);
		for (PersistedPaymentRequest ppr : persistedPaymentRequests) {
			if (ppr.equals(toSearchFor))
				return ppr;
		}
		return null;
	}
	
	public Set<PersistedPaymentRequest> getPersistedPaymentRequests() {
		initPersistedPaymentRequests();
		return persistedPaymentRequests;
	}

	public String getWatchingKey() {
		return data.getServerWatchingKey();
	}

	public void setWatchingKey(String watchingKey) {
		data.setServerWatchingKey(watchingKey);
	}

	public BitcoinNet getBitcoinNet() {
		String bitcoinNet = data.getBitcoinNet();
		return BitcoinNet.of(bitcoinNet);
	}

	public void setBitcoinNet(BitcoinNet bitcoinNet) {
		data.setBitcoinNet(bitcoinNet.toString());
	}

	/**
	 * deletes all internal data
	 */
	public void clear(){
		data.clear();
	}


}
