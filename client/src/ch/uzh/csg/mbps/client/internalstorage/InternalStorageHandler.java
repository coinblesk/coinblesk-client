package ch.uzh.csg.mbps.client.internalstorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.keys.CustomPublicKey;
import ch.uzh.csg.mbps.model.UserAccount;

/**
 * Handles storing and retrieving user information which needs to be persisted
 * locally. The user information is encrypted using
 * {@link InternalStorageEncrypter}.
 * 
 * @author Jeton Memeti
 * 
 */
public class InternalStorageHandler {

	/*
	 * TODO jeton: implement IPersistencyHandler
	 * 
	 * regarding PersistedPaymentRequests: when saving each time adding a
	 * persisted crap, it takes way too long because of the encryption!!!
	 * 
	 * --> extract save and document!!!
	 */

	private Context context;
	private String fileName;
	private String password;

	private InternalXMLData xmlData = null;
	private InternalStorageEncrypter encrypter = null;

	private String currentXML;
	
	private UserAccount userAccount;
	private CustomKeyPair userKeyPair;
	private CustomPublicKey serverPublicKey;
	private String serverIp;
	private Set<String> addressbook;
	private Set<String> trustedAddressbook;

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
		this.context = context;
		this.fileName = username+".xml";
		this.password = password;

		this.xmlData = new InternalXMLData();
		this.encrypter = new InternalStorageEncrypter();

		this.currentXML = null;
		this.userAccount = null;
	}

	/**
	 * Initializes the {@link InternalStorageHandler}. This must always be
	 * called after instantiating a new object. It reads the existing xml file
	 * or creates a new one if it does not exist.
	 * 
	 * @return
	 * @throws WrongPasswordException
	 */
	public boolean init() throws WrongPasswordException {
		try {
			if (fileExists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
				String line;
				String content = "";
				while ((line = reader.readLine()) != null) {
					content += line;
				}
				reader.close();
				
				currentXML = encrypter.decrypt(content, password, xmlData.getRootElementName());
			} else {
				currentXML = xmlData.createEmptyXML();
			}
			return true;
		} catch (WrongPasswordException e) {
			throw new WrongPasswordException(e.getMessage());
		} catch (Exception e) {
			return false;
		}
	}

	private boolean fileExists() {
		String[] fileList = context.fileList();
		for (int i=0; i<fileList.length; i++) {
			String file = fileList[i];
			if (file.equals(fileName)) {
				return true;
			}
		}
		return false;
	}

	private void writeToFileSystem(String encrypted) {
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE)));
			writer.write(encrypted);
		} catch (Exception e) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void encryptAndSave() throws Exception {
		String encrypted = encrypter.encrypt(currentXML, password);
		writeToFileSystem(encrypted);
	}
	
	/**
	 * Saves the {@link UserAccount} to the internal storage.
	 * 
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean saveUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
		try {
			currentXML = xmlData.setUserAccount(currentXML, userAccount);
			encryptAndSave();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the {@link UserAccount} from internal storage.
	 */
	public UserAccount getUserAccount() {
		if (userAccount == null) {
			try {
				userAccount = xmlData.getUserAccount(currentXML);
			} catch (Exception e) {
			}
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
		this.password = password;
		userAccount.setPassword(password);
		return saveUserAccount(userAccount);
	}

	/**
	 * Sets the user's email and saves it to the internal storage.
	 * 
	 * @param saveEmail
	 *            the new email address
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean setUserEmail(String email) {
		userAccount.setEmail(email);
		return saveUserAccount(userAccount);
	}

	/**
	 * Sets the user's balance and saves it to the internal storage.
	 * 
	 * @param balance
	 *            the new balance
	 * @return true if the file was saved, false if it is just set temporarily
	 *         but could not be persisted
	 */
	public boolean setUserBalance(BigDecimal balance) {
		userAccount.setBalance(balance);
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
			currentXML = xmlData.setUserKeyPair(currentXML, customKeyPair);
			encryptAndSave();
			return true;
		} catch (Exception e) {
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
				userKeyPair = xmlData.getUserKeyPair(currentXML);
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
			if (xmlData.getServerPublicKey(currentXML, publicKey.getKeyNumber()) == null) {
				// only save the public key if it is not already saved (i.e., a new one)
				currentXML = xmlData.setServerPublicKey(currentXML, publicKey);
				encryptAndSave();
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	//TODO jeton: create method without key number
	/**
	 * Returns the server's {@link CustomPublicKey} with the given key number or
	 * null, if there is no {@link CustomPublicKey} with the given key number.
	 * 
	 * @param keyNumber
	 *            the key number of the {@link CustomPublicKey} to return
	 */
	public CustomPublicKey getServerPublicKey(byte keyNumber) {
		if (serverPublicKey == null) {
			try {
				serverPublicKey = xmlData.getServerPublicKey(currentXML, keyNumber);
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
			currentXML = xmlData.setServerIp(currentXML, ip);
			encryptAndSave();
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
				serverIp = xmlData.getServerIp(currentXML);
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
				addressbook = xmlData.getAddressBookContacts(currentXML);
			} catch (Exception e) {
			}
		}
		return  addressbook;
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
				currentXML = xmlData.addAddressBookContact(currentXML, username);
				encryptAndSave();
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
				currentXML = xmlData.removeAddressBookContact(currentXML, username);

				if(isTrusted){
					//delete also trusted address book entry if available
					currentXML = xmlData.removeTrustedAddressBookEntry(currentXML, username);
				}
				encryptAndSave();
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

		boolean removed = addressesToRemove.removeAll(trustedAddressBook);
		if(removed){
			try {
				for (Iterator<String> it = addressesToRemove.iterator(); it.hasNext();) {
					String username = it.next();
					currentXML = xmlData.removeAddressBookContact(currentXML, username);
					addressbook.remove(username);
				}
				encryptAndSave();
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
				trustedAddressbook = xmlData.getTrusteAddressBookdContacts(currentXML);
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
				currentXML = xmlData.addTrustedAddressBookContact(currentXML, username);
				encryptAndSave();
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
				currentXML = xmlData.removeTrustedAddressBookEntry(currentXML, username);
				encryptAndSave();
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

}
