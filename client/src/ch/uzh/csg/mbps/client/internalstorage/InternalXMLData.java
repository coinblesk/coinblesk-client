package ch.uzh.csg.mbps.client.internalstorage;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.model.CustomPublicKey;
import ch.uzh.csg.mbps.util.Converter;

//TODO jeton: javadoc
/**
 * This class is used to store / read a xml format of the user information
 * from the internal storage.
 */
public class InternalXMLData {
	private static final String ROOT = "persisted-data";
	
	private static final String SERVER = "server";
	private static final String SERVER_IP = "ip";
	private static final String SERVER_KEY = "public-key";
	private static final String SERVER_KEY_PKIALGORITHM = "pkialgorithm";
	private static final String SERVER_KEY_BASE64 = "base64-publickey";
	
	private static final String USER_ACCOUNT = "user-account";
	private static final String USER_ID = "id";
	private static final String USER_NAME = "name";
	private static final String USER_BALANCE = "balance";
	private static final String USER_PAYMENT_ADRESS = "payment-adress";
	
	private static final String KEYPAIR = "keypair";
	private static final String KEYPAIR_PKIALGORITHM = "pkialgorithm";
	private static final String KEYPAIR_KEYNR = "keynumber";
	private static final String KEYPAIR_PUBKEY = "publickey";
	private static final String KEYPAIR_PRIVKEY = "privatekey";
	
	private static final String PENDING_REQUESTS = "pending-requests";
	
	private static final String ADDRESS_BOOK = "address-book";
	
	public String getRootElementName() {
		return ROOT;
	}
	
	public String createEmptyXML() throws Exception {
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
 
		// root element
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement(ROOT);
		doc.appendChild(rootElement);
 
		// server element
		Element server = doc.createElement(SERVER);
		rootElement.appendChild(server);
		// server ip element
		Element serverIp = doc.createElement(SERVER_IP);
		server.appendChild(serverIp);
		// server key element
		Element serverKey = doc.createElement(SERVER_KEY);
		server.appendChild(serverKey);
		// server key pki algorithm element
		Element pkiAlgorithm = doc.createElement(SERVER_KEY_PKIALGORITHM);
		serverKey.appendChild(pkiAlgorithm);
		// server key public key
		Element serverPublicKey = doc.createElement(SERVER_KEY_BASE64);
		serverKey.appendChild(serverPublicKey);
		
		// user account element
		Element userAccount = doc.createElement(USER_ACCOUNT);
		rootElement.appendChild(userAccount);
		// user id element
		Element userId = doc.createElement(USER_ID);
		userAccount.appendChild(userId);
		// user name element
		Element userName = doc.createElement(USER_NAME);
		userAccount.appendChild(userName);
		// balance element
		Element balance = doc.createElement(USER_BALANCE);
		userAccount.appendChild(balance);
		// payment address element
		Element paymentAddress = doc.createElement(USER_PAYMENT_ADRESS);
		userAccount.appendChild(paymentAddress);
		
		// key pair element
		Element userKeyPair = doc.createElement(KEYPAIR);
		rootElement.appendChild(userKeyPair);
		// pki algorithm element
		Element userPKIAlgorithm = doc.createElement(KEYPAIR_PKIALGORITHM);
		userKeyPair.appendChild(userPKIAlgorithm);
		// key number element
		Element userKeyNumber = doc.createElement(KEYPAIR_KEYNR);
		userKeyPair.appendChild(userKeyNumber);
		// public key element
		Element userPublicKey = doc.createElement(KEYPAIR_PUBKEY);
		userKeyPair.appendChild(userPublicKey);
		// private key element
		Element userPrivateKey = doc.createElement(KEYPAIR_PRIVKEY);
		userKeyPair.appendChild(userPrivateKey);
		
		// pending requests elements
		Element pendingRequests = doc.createElement(PENDING_REQUESTS);
		rootElement.appendChild(pendingRequests);
		
		// address book elements
		Element addressBook = doc.createElement(ADDRESS_BOOK);
		rootElement.appendChild(addressBook);
		
		return xmlToString(doc);
	}
	
	private String xmlToString(Document doc) throws Exception {
		StringWriter writer = new StringWriter();
	    StreamResult result = new StreamResult(writer);
	    DOMSource source = new DOMSource(doc.getDocumentElement());
	    Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    transformer.transform(source, result);
	    StringBuffer strBuf = writer.getBuffer();
	    return strBuf.toString();
	}
	
	private Document stringToXml(String xml) throws Exception {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    return db.parse(is);
	}
	

	public String setServerIp(String xml, String ip) throws Exception {
		Document doc = stringToXml(xml);
		
		Node serverElement = doc.getElementsByTagName(SERVER).item(0);
		NodeList serverNodes = serverElement.getChildNodes();
		for (int i=0; i<serverNodes.getLength(); i++) {
			Node node = serverNodes.item(i);
			if (node.getNodeName().equals(SERVER_IP)) {
				Node ipNode = node.getChildNodes().item(0);
				ipNode.setTextContent(ip);
				break;
			}
		}
		
		return xmlToString(doc);
	}
	
	public String getServerIp(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		Node serverElement = doc.getElementsByTagName(SERVER).item(0);
		NodeList serverNodes = serverElement.getChildNodes();
		for (int i=0; i<serverNodes.getLength(); i++) {
			Node node = serverNodes.item(i);
			if (node.getNodeName().equals(SERVER_IP)) {
				Node ipNode = node.getChildNodes().item(0);
				return ipNode.getTextContent();
			}
		}
		
		return null;
	}

	public String setServerPublicKey(String xml, CustomPublicKey publicKey) throws Exception {
		Document doc = stringToXml(xml);
		
		Node serverElement = doc.getElementsByTagName(SERVER).item(0);
		NodeList serverNodes = serverElement.getChildNodes();
		for (int i=0; i<serverNodes.getLength(); i++) {
			Node node = serverNodes.item(i);
			if (node.getNodeName().equals(SERVER_KEY)) {
				NodeList serverKeyNodes = node.getChildNodes();
				for (int j=0; j<serverKeyNodes.getLength(); j++) {
					Node n = serverKeyNodes.item(j);
					if (n.getNodeName().equals(SERVER_KEY_PKIALGORITHM)) {
						n.setTextContent(Byte.toString(publicKey.getPKIAlgorithm()));
					}
					
					if (n.getNodeName().equals(SERVER_KEY_BASE64)) {
						n.setTextContent(publicKey.getPublicKey());
					}
				}
				break;
			}
		}
		
		return xmlToString(doc);
	}
	
	public CustomPublicKey getServerPublicKey(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		byte pkiAlgorithm = 0;
		String key = null;
		
		Node serverElement = doc.getElementsByTagName(SERVER).item(0);
		NodeList serverNodes = serverElement.getChildNodes();
		for (int i=0; i<serverNodes.getLength(); i++) {
			Node node = serverNodes.item(i);
			if (node.getNodeName().equals(SERVER_KEY)) {
				NodeList serverKeyNodes = node.getChildNodes();
				for (int j=0; j<serverKeyNodes.getLength(); j++) {
					Node n = serverKeyNodes.item(j);
					if (n.getNodeName().equals(SERVER_KEY_PKIALGORITHM)) {
						pkiAlgorithm = Byte.parseByte(n.getTextContent());
					}
					
					if (n.getNodeName().equals(SERVER_KEY_BASE64)) {
						key = n.getTextContent();
					}
				}
				break;
			}
		}
		
		if (pkiAlgorithm != 0 && key != null)
			return new CustomPublicKey(pkiAlgorithm, key);
		else
			return null;
	}

	public String setUserAccount(String xml, UserAccount userAccount) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_ID)) {
				node.setTextContent(Long.toString(userAccount.getId()));
			}
			
			if (node.getNodeName().equals(USER_NAME)) {
				node.setTextContent(userAccount.getUsername());
			}
			
			if (node.getNodeName().equals(USER_BALANCE)) {
				long balance = Converter.getLongFromBigDecimal(userAccount.getBalance());
				node.setTextContent(Long.toString(balance));
			}
			
			if (node.getNodeName().equals(USER_PAYMENT_ADRESS)) {
				node.setTextContent(userAccount.getPaymentAddress());
			}
		}
		
		return xmlToString(doc);
	}
	
	public UserAccount getUserAccount(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		long userId = 0;
		String username = null;
		BigDecimal balance = null;
		String paymentAddress = null;
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_ID)) {
				userId = Long.parseLong(node.getTextContent());
			}
			
			if (node.getNodeName().equals(USER_NAME)) {
				username = node.getTextContent();
			}
			
			if (node.getNodeName().equals(USER_BALANCE)) {
				long balanceLong = Long.parseLong(node.getTextContent());
				balance = Converter.getBigDecimalFromLong(balanceLong);
			}
			
			if (node.getNodeName().equals(USER_PAYMENT_ADRESS)) {
				paymentAddress = node.getTextContent();
			}
		}
		
		if (userId != 0 && username != null && balance != null && paymentAddress != null) {
			UserAccount ua = new UserAccount();
			ua.setId(userId);
			ua.setUsername(username);
			ua.setBalance(balance);
			ua.setPaymentAddress(paymentAddress);
			return ua;
		} else {
			return null;
		}
	}
	
	public long getUserId(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_ID)) {
				return Long.parseLong(node.getTextContent());
			}
		}
		
		return 0;
	}
	
	public String getUsername(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_NAME)) {
				return node.getTextContent();
			}
		}
		
		return null;
	}
	
	public BigDecimal getUserBalance(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_BALANCE)) {
				long balanceLong = Long.parseLong(node.getTextContent());
				return Converter.getBigDecimalFromLong(balanceLong);
			}
		}
		
		return null;
	}
	
	public String getUserPaymentAddress(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(USER_PAYMENT_ADRESS)) {
				return node.getTextContent();
			}
		}
		
		return null;
	}

	public String setUserKeyPair(String xml, CustomKeyPair customKeyPair) throws Exception {
		Document doc = stringToXml(xml);
		
		Node userAccountElement = doc.getElementsByTagName(KEYPAIR).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(KEYPAIR_PKIALGORITHM)) {
				node.setTextContent(Byte.toString(customKeyPair.getPkiAlgorithm()));
			}
			
			if (node.getNodeName().equals(KEYPAIR_KEYNR)) {
				node.setTextContent(Byte.toString(customKeyPair.getKeyNumber()));
			}
			
			if (node.getNodeName().equals(KEYPAIR_PUBKEY)) {
				node.setTextContent(customKeyPair.getPublicKey());
			}
			
			if (node.getNodeName().equals(KEYPAIR_PRIVKEY)) {
				node.setTextContent(customKeyPair.getPrivateKey());
			}
		}
		
		return xmlToString(doc);
	}

	public CustomKeyPair getUserKeyPair(String xml) throws Exception {
		Document doc = stringToXml(xml);
		
		byte pkiAlgorithm = 0;
		byte keyNumber = 0;
		String publicKey = null;
		String privateKey = null;
		
		Node userAccountElement = doc.getElementsByTagName(KEYPAIR).item(0);
		NodeList userAccountNodes = userAccountElement.getChildNodes();
		for (int i=0; i<userAccountNodes.getLength(); i++) {
			Node node = userAccountNodes.item(i);
			
			if (node.getNodeName().equals(KEYPAIR_PKIALGORITHM)) {
				pkiAlgorithm = Byte.parseByte(node.getTextContent());
			}
			
			if (node.getNodeName().equals(KEYPAIR_KEYNR)) {
				keyNumber = Byte.parseByte(node.getTextContent());
			}
			
			if (node.getNodeName().equals(KEYPAIR_PUBKEY)) {
				publicKey = node.getTextContent();
			}
			
			if (node.getNodeName().equals(KEYPAIR_PRIVKEY)) {
				privateKey = node.getTextContent();
			}
		}
		
		if (pkiAlgorithm != 0 && keyNumber != 0 && publicKey != null & privateKey != null)
			return new CustomKeyPair(pkiAlgorithm, keyNumber, publicKey, privateKey);
		else
			return null;
	}
	
}