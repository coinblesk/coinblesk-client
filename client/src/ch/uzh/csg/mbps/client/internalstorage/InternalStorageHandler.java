package ch.uzh.csg.mbps.client.internalstorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.keys.CustomPublicKey;
import ch.uzh.csg.mbps.model.UserAccount;

//TODO jeton: javadoc
public class InternalStorageHandler {

	//TODO jeton: implement IPersistencyHandler
	//TODO jeton: when saving each time adding a persisted crap, it takes way too long because of the encryption!!!
	//TODO jeton: extract save and document!!!

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

	public InternalStorageHandler(Context context, String username, String password) {
		this.context = context;
		this.fileName = username+".xml";
		this.password = password;

		this.xmlData = new InternalXMLData();
		this.encrypter = new InternalStorageEncrypter();

		this.currentXML = null;
		this.userAccount = null;
	}

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

	public UserAccount getUserAccount() {
		if (userAccount == null) {
			try {
				userAccount = xmlData.getUserAccount(currentXML);
			} catch (Exception e) {
			}
		}
		return userAccount;
	}
	
	public boolean setUserPassword(String password) {
		userAccount.setPassword(password);
		return saveUserAccount(userAccount);
	}

	public boolean setUserEmail(String saveEmail) {
		userAccount.setEmail(saveEmail);
		return saveUserAccount(userAccount);
	}

	public boolean setUserBalance(BigDecimal balance) {
		userAccount.setBalance(balance);
		return saveUserAccount(userAccount);
	}
	
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

	public CustomKeyPair getKeyPair() {
		if (userKeyPair == null) {
			try {
				userKeyPair = xmlData.getUserKeyPair(currentXML);
			} catch (Exception e) {
			}
		}
		return userKeyPair;
	}
	
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

	public CustomPublicKey getServerPublicKey(byte keyNumber) throws Exception {
		if (serverPublicKey == null) {
			try {
				serverPublicKey = xmlData.getServerPublicKey(currentXML, keyNumber);
			} catch (Exception e) {
			}
		}
		return serverPublicKey;
	}
	
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
	 * @param context
	 *            Application Context
	 * @return Set<String> with all usernames stored in address book.
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
	 * @param context Application Context
	 * @param username to store
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
	 * @param context Application Context
	 * @param username to remove
	 */
	public boolean removeAddressBookEntry(String username) {
		if (!getAddressBook().contains(username)) {
			return true;
		} else {
			addressbook.remove(username);
			try {
				currentXML = xmlData.removeAddressBookContact(currentXML, username);
				//delete also trusted address book entry if available
				currentXML = xmlData.removeTrustedAddressBookEntry(currentXML, username);
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
	 * @param context Application Context
	 */
	public boolean removeAllUntrustedAddressBookEntries() {
		Set<String> trustedAddressBook = getTrustedAddressbook();
		Set<String> addressesToRemove = getAddressBook();
		
		addressesToRemove.removeAll(trustedAddressBook);
		
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
	}
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames of trusted users stored
	 * in the trusted address book.
	 * 
	 * @param context
	 * @return Set<String> with all trusted usernames (alphabetically ordered)
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
	 * @param context Application Context
	 * @param username to store
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
	 * @param context (Application Context)
	 * @param username to remove
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
	 * Returns boolean which indicates if a given username is saved in
	 * trustedAddressBook and therefore trusted (true) or not (false).
	 * 
	 * @param context
	 *            (Application Context)
	 * @param username to check
	 * @return boolean if username is trusted
	 */
	public boolean isTrustedContact(String username) {
		return getTrustedAddressbook().contains(username);
	}

}
