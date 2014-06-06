package ch.uzh.csg.mbps.client.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Xml;
import ch.uzh.csg.mbps.model.UserAccount;

/**
 * This class is used to store / read a xml format of the user information's
 * from the internal storage.
 */
public class InternalStorageXML{

	public static String createUsingXMLSerializer() throws Exception {
		XmlSerializer xmlSerializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
		
	    xmlSerializer.setOutput(writer);
	    xmlSerializer.startDocument("UTF-8", true);
	    xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	    
	    //open tag:<accounts>
	    xmlSerializer.startTag("", "account");
	    xmlSerializer.attribute("", "identifier", "mbps_client");
	 
	    //close tag:</accounts>
	    xmlSerializer.endTag("", "account");
	    
	    xmlSerializer.endDocument();
	    return writer.toString(); 
	}
	
	private static String writeUsingXMLSerializer(UserAccount user) throws Exception {
	    XmlSerializer xmlSerializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    
		xmlSerializer.setOutput(writer);
		xmlSerializer.startDocument("UTF-8", true);
		xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		
		// open tag: <user_account>
		xmlSerializer.startTag("", "user_account");
		xmlSerializer.attribute("", "identifier", "mbps_client");		
 
			//open tag: <id>
		xmlSerializer.startTag("", "id");
		xmlSerializer.text(String.valueOf(user.getId()));
		// close tag: </id>
		xmlSerializer.endTag("", "id");
		
		// open tag: <balance>
		xmlSerializer.startTag("", "balance");
		xmlSerializer.text(String.valueOf(user.getBalance()));
		// close tag: </balance>
		xmlSerializer.endTag("", "balance");
 
		//TODO jeton: remove!
//			// open tag: <transaction_nr>
//		xmlSerializer.startTag("", "transaction_nr");
//		xmlSerializer.text(String.valueOf(user.getTransactionNumber()));
//		// close tag: </transaction_nr>
//		xmlSerializer.endTag("", "transaction_nr");
//		
//		// open tag: <private_key>
//		xmlSerializer.startTag("", "private_key");
//		xmlSerializer.text(user.getPrivateKey());
//		// close tag: </private_key>
//		xmlSerializer.endTag("", "private_key");
// 
//			// open tag: <public_key>
//		xmlSerializer.startTag("", "public_key");
//		xmlSerializer.text(user.getPublicKey());
//		// close tag: </public_key>
//		xmlSerializer.endTag("", "public_key");

		// open tag: <payment_address>
		xmlSerializer.startTag("", "payment_address");
		xmlSerializer.text(user.getPaymentAddress());
		// close tag: </payment_address>
		xmlSerializer.endTag("", "payment_address");
		
		// close tag: </user_account>
		xmlSerializer.endTag("", "user_account");
 
			xmlSerializer.endDocument();
	    return writer.toString();
	}
	
	/**
	 * Stores the user informations from {@link ClientController} as xml format
	 * into the internal storage.
	 * 
	 * @param context
	 *            The conatext of the current view (activity).
	 * @return Returns true if the process was successes.
	 */
	public static boolean writeUserAccountIntoFile(Context context){
		BufferedWriter writer = null;
		boolean success = false;
		
		try {
			String xmlFile = writeUsingXMLSerializer(ClientController.getUser());
			writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(ClientController.getFilename(), Context.MODE_PRIVATE)));
			String encrypted = InternalStorageSecurityUtils.encrypt(xmlFile, ClientController.getUser().getPassword());
			writer.write(encrypted);
			success = true;
		} catch (Exception e) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
		    }
		}
		
		return success;
	}
	
	/**
	 * Stores the public key of the server from {@link ClientController} as
	 * plain text into the internal storage.
	 * 
	 * @param context
	 *            The context of the current view (activity).
	 * @param encodedServerPublicKey
	 *            The public key of the server as string
	 * @return Returns true if the process successes.
	 */
	public static boolean writePublicKeyIntoFile(Context context, String encodedServerPublicKey){
		BufferedWriter writer = null;
		boolean success = false;
		
		try {
			writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(ClientController.getServerPublicKeyFilename(), Context.MODE_PRIVATE)));
			writer.write(encodedServerPublicKey);
			success = true;
		} catch (Exception e) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
		    }
		}
		return success;
	}
	
	/**
	 * Retrieve the user information from the internal storage.
	 * 
	 * @param context
	 *            The context of the current view (activity).
	 * @param password
	 *            The password of the user. This is the key to decrypt the file.
	 * @return Returns the decrypt string of the user information.
	 * @throws FileNotFoundException
	 *             The file does not exists.
	 * @throws IOException
	 * @throws WrongPasswordException
	 *             The Password was wrong.
	 */
	public static String readUserAccountFromFile(Context context, String password) throws FileNotFoundException, IOException, WrongPasswordException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(ClientController.getFilename())));
		String readText;
		String data = "";
		while((readText = reader.readLine()) != null){
			data += readText;
		}
		reader.close();
		return InternalStorageSecurityUtils.decrypt(data, password);
	}
	
	/**
	 * Retrieve the server's public key from the internal storage.
	 * 
	 * @param context
	 *            The context of the current view (activity).
	 * @return Returns the information as string.
	 * @throws FileNotFoundException
	 *             The file does not exists.
	 * @throws IOException
	 * @throws Exception
	 */
	public static String readPublicKeyFromFile(Context context) throws FileNotFoundException, IOException, Exception{
		BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(ClientController.getServerPublicKeyFilename())));
		String readText;
		String data = "";
		while((readText = reader.readLine()) != null){
			data += readText;
		}
		reader.close();
		return data;
	}
	
	private static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

	/**
	 * Retrieves the user information from the xml format.
	 * 
	 * @param text
	 *            The xml-format string.
	 * @param password
	 *            The password of the user.
	 * @return Returns the user with the user information.
	 */
	public static UserAccount readUserAccount(String text, String password) {
		Document xmlInput = null;
		UserAccount user = null;
		try {
			//TODO jeton: edit!!
			xmlInput = loadXMLFromString(text);
			if (Double.valueOf(xmlInput.getElementsByTagName("balance").item(0).getTextContent()) > -1
					&& Integer.valueOf(xmlInput.getElementsByTagName("transaction_nr").item(0).getTextContent()) > -1) {
				user = new UserAccount();
				user.setId(Long.parseLong(xmlInput.getElementsByTagName("id").item(0).getTextContent()));
				user.setBalance(new BigDecimal(xmlInput.getElementsByTagName("balance").item(0).getTextContent()));
//				user.setTransactionNumber(Long.parseLong(xmlInput.getElementsByTagName("transaction_nr").item(0).getTextContent()));
				user.setPassword(password);
//				user.setPrivateKey(xmlInput.getElementsByTagName("private_key").item(0).getTextContent());
//				user.setPublicKey(xmlInput.getElementsByTagName("public_key").item(0).getTextContent());
				user.setPaymentAddress(xmlInput.getElementsByTagName("payment_address").item(0).getTextContent());
				user.setEmail("");
				user.setUsername("");

				return user;
			}
		} catch (Exception e) {
			return null;
		}
		return user;
	}
}