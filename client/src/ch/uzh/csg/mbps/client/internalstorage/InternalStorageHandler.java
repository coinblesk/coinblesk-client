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
	
	
	//TODO jeton: remove throws exception
	
	
	
	
	

	public void saveServerPublicKey(CustomPublicKey publicKey) throws Exception {
		if (xmlData.getServerPublicKey(currentXML, publicKey.getKeyNumber()) == null) {
			// only save the public key if it is not already saved (i.e., a new one)
			currentXML = xmlData.setServerPublicKey(currentXML, publicKey);
			encryptAndSave();
		}
	}

	public CustomPublicKey getServerPublicKey(byte keyNumber) throws Exception {
		return xmlData.getServerPublicKey(currentXML, keyNumber);
	}

	public void saveServerIP(String ip) throws Exception {
		currentXML = xmlData.setServerIp(currentXML, ip);
		encryptAndSave();
	}

	public String getServerIP() throws Exception {
		return xmlData.getServerIp(currentXML);
	}

	public long getUserId() throws Exception {
		return xmlData.getUserId(currentXML);
	}

	public String getUsername() throws Exception {
		return xmlData.getUsername(currentXML);
	}

	public BigDecimal getUserBalance() throws Exception {
		return xmlData.getUserBalance(currentXML);
	}

	public String getUserPaymentAddress() throws Exception {
		return xmlData.getUserPaymentAddress(currentXML);
	}

	public void saveKeyPair(CustomKeyPair customKeyPair) throws Exception {
		currentXML = xmlData.setUserKeyPair(currentXML, customKeyPair);
		encryptAndSave();
	}

	public CustomKeyPair getKeyPair() throws Exception {
		return xmlData.getUserKeyPair(currentXML);
	}

	/**
	 * Returns an alphabetically ordered Set<String> with all usernames stored
	 * in address book.
	 * 
	 * @param context
	 *            Application Context
	 * @return Set<String> with all usernames stored in address book.
	 * @throws Exception
	 */
	public Set<String> getAddressBook() throws Exception {
		return xmlData.getAddressBookContacts(currentXML);
	}

	/**
	 * Adds an entry (username) to the address book of known users.
	 * 
	 * @param context Application Context
	 * @param username to store
	 * @throws Exception 
	 */
	public void addAddressBookEntry(String username) throws Exception{
		currentXML = xmlData.addAddressBookContact(currentXML, username);
		encryptAndSave();
	}

	/**
	 * Removes an entry (username) from the address book of known users.
	 * 
	 * @param context Application Context
	 * @param username to remove
	 * @throws Exception 
	 */
	public void removeAddressBookEntry(String username) throws Exception{
		currentXML = xmlData.removeAddressBookContact(currentXML, username);

		//delete also trusted address book entry if available
		currentXML = xmlData.removeTrustedAddressBookEntry(currentXML, username);

		encryptAndSave();
	}

	/**
	 * Removes all entries from address book which are not trusted.
	 * 
	 * @param context Application Context
	 * @throws Exception
	 */
	public void removeAllUntrustedAddressBookEntries() throws Exception{
		Set<String> trustedAddressBook = getTrustedAddressbook();
		Set<String> addressesToRemove = getAddressBook();

		addressesToRemove.removeAll(trustedAddressBook);

		for (Iterator<String> it = addressesToRemove.iterator(); it.hasNext();) {
			String username = it.next();
			currentXML = xmlData.removeAddressBookContact(currentXML, username);	
		}
		encryptAndSave();
	}
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames of trusted users stored
	 * in the trusted address book.
	 * 
	 * @param context
	 * @return Set<String> with all trusted usernames (alphabetically ordered)
	 * @throws Exception
	 */
	public Set<String> getTrustedAddressbook() throws Exception {
		return xmlData.getTrusteAddressBookdContacts(currentXML);
	}

	/**
	 * Adds an entry (username) to the address book of trusted users.
	 * 
	 * @param context Application Context
	 * @param username to store
	 * @throws Exception 
	 */
	public void addTrustedAddressBookEntry(String username) throws Exception{
		currentXML = xmlData.addTrustedAddressBookContact(currentXML, username);
		encryptAndSave();
	}
	
	/**
	 * Removes an entry (username) from the address book of trusted users.
	 * 
	 * @param context (Application Context)
	 * @param username to remove
	 * @throws Exception 
	 */
	public void removeTrustedAddressBookEntry(String username) throws Exception{
		currentXML = xmlData.removeTrustedAddressBookEntry(currentXML, username);
		encryptAndSave();
	}

	/**
	 * Returns boolean which indicates if a given username is saved in
	 * trustedAddressBook and therefore trusted (true) or not (false).
	 * 
	 * @param context
	 *            (Application Context)
	 * @param username to check
	 * @return boolean if username is trusted
	 * @throws Exception
	 */
	public boolean isTrustedContact(String username) throws Exception {
		Set<String> trustedContacts = getTrustedAddressbook();
		return trustedContacts.contains(username);
	}

}
