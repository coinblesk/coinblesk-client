package ch.uzh.csg.mbps.client.internalstorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;

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
	
	public InternalStorageHandler(Context context, String username, String password) throws Exception {
		this.context = context;
		this.fileName = username+".xml";
		this.password = password;
		
		this.xmlData = new InternalXMLData();
		this.encrypter = new InternalStorageEncrypter();
		
		this.currentXML = null;
		init();
	}
	
	private void init() throws Exception {
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
	
	public void saveServerPublicKey(CustomPublicKey publicKey) throws Exception {
		currentXML = xmlData.setServerPublicKey(currentXML, publicKey);
		encryptAndSave();
	}
	
	public CustomPublicKey getServerPublicKey() throws Exception {
		return xmlData.getServerPublicKey(currentXML);
	}
	
	public void saveServerIP(String ip) throws Exception {
		currentXML = xmlData.setServerIp(currentXML, ip);
		encryptAndSave();
	}
	
	public String getServerIP() throws Exception {
		return xmlData.getServerIp(currentXML);
	}
	
	public void saveUserAccount(UserAccount userAccount) throws Exception {
		currentXML = xmlData.setUserAccount(currentXML, userAccount);
		encryptAndSave();
	}
	
	public UserAccount getUserAccount() throws Exception {
		return xmlData.getUserAccount(currentXML);
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
	
	//TODO simon: implement addressbook stuff

}
