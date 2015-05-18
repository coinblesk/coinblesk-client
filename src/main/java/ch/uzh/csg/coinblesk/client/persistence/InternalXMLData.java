package ch.uzh.csg.coinblesk.client.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.content.Context;
import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.model.UserAccount;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import ch.uzh.csg.coinblesk.util.Converter;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

/**
 * Generates a xml data string and offers the functionality to manipulate the
 * content of that xml string in order to be stored on the local storage of the
 * device. This class contains all the information which needs to be persisted
 * locally.
 * 
 * While some fields are always fetched from the server and changes are not
 * commited, other information like the KEYPAIR and ADDRESSBOOK exist only on
 * the device.
 * <strong>THIS CLASS IS DEPRECTAED AND UNTESTED</strong>
 * 
 * @author Jeton Memeti
 * 
 */
@SuppressLint({ "DefaultLocale", "Assert" })
@Deprecated
public class InternalXMLData implements PersistentData {

    private final static Logger LOGGER = LoggerFactory.getLogger(InternalXMLData.class);

    private static final String ROOT = "persisted-data";

    private static final String SERVER = "server";
    private static final String SERVER_IP = "ip";
    private static final String SERVER_KEY = "public-key";
    private static final String SERVER_KEY_KEY_NR = "keynumber";
    private static final String SERVER_KEY_PKIALGORITHM = "pkialgorithm";
    private static final String SERVER_KEY_BASE64 = "base64-publickey";

    private static final String USER_ACCOUNT = "user-account";
    private static final String USER_ID = "id";
    private static final String USER_NAME = "name";
    private static final String USER_BALANCE = "balance";

    private static final String KEYPAIR = "keypair";
    private static final String KEYPAIR_PKIALGORITHM = "pkialgorithm";
    private static final String KEYPAIR_KEYNR = "keynumber";
    private static final String KEYPAIR_PUBKEY = "publickey";
    private static final String KEYPAIR_PRIVKEY = "privatekey";

    private static final String PENDING_REQUESTS = "pending-requests";
    private static final String PENDING_REQUEST = "pending-request";
    private static final String PENDING_REQUEST_USERNAME_PAYEE = "username-payee";
    private static final String PENDING_REQUEST_CURRENCY = "currency";
    private static final String PENDING_REQUEST_AMOUNT = "amount";
    private static final String PENDING_REQUEST_TIMESTAMP = "timestamp";

    private static final String ADDRESS_BOOK = "address-book";
    private static final String TRUSTED_CONTACTS = "trusted-contacts";
    private static final String CONTACTS = "contacts";

    private String username;
    private String password;
    private String xml;
    private File directory;
    private String filename;

    private InternalStorageEncrypter encrypter = null;

    public InternalXMLData(String password, String username, File directory) {
        this.username = username;
        this.password = password;
        this.directory = directory;
        this.filename = getFilename(username);
        
        this.encrypter = new InternalStorageEncrypter();

        try {
            init();
        } catch (Exception e) {
            LOGGER.error("Failed to create XML", e);
        }
    }
    
    private String getFilename(String username) {
        return "coinblesk_data_store_" + username;
    }

    /**
     * Creates an empty xml string containing only the structure of the xml file
     * but no content.
     * 
     * @throws Exception
     *             an xml exception
     */
    private void createEmptyXML() throws Exception {
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
        // server key nr element
        Element keyNumber = doc.createElement(SERVER_KEY_KEY_NR);
        serverKey.appendChild(keyNumber);
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
        // contacts
        Element contacts = doc.createElement(CONTACTS);
        addressBook.appendChild(contacts);
        // trusted contacts
        Element trustedContacts = doc.createElement(TRUSTED_CONTACTS);
        addressBook.appendChild(trustedContacts);

        xmlToString(doc); save();
    }

    void xmlToString(Document doc) throws Exception {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc.getDocumentElement());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
        StringBuffer strBuf = writer.getBuffer();
        xml = strBuf.toString();
    }

    private Document stringToXml(String xml) {
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Failed to create XML document builder", e);
            return null;
        }
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        try {
            return db.parse(is);
        } catch (SAXException e) {
            LOGGER.error("Failed to parse XML document", e);
            return null;
        } catch (IOException e) {
            LOGGER.error("Failed to write XML document", e);
            return null;
        }
    }

    public void setServerIp(String ip) throws Exception {
        Document doc = stringToXml(xml);

        Node serverElement = doc.getElementsByTagName(SERVER).item(0);
        NodeList serverNodes = serverElement.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Node node = serverNodes.item(i);
            if (node.getNodeName().equals(SERVER_IP)) {
                Node ipNode = node.getChildNodes().item(0);
                ipNode.setNodeValue(ip);
                break;
            }
        }
        xmlToString(doc); save();
        save();
    }

    @Override
    public String getServerIp() throws Exception {
        Document doc = stringToXml(xml);

        Node serverElement = doc.getElementsByTagName(SERVER).item(0);
        NodeList serverNodes = serverElement.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Node node = serverNodes.item(i);
            if (node.getNodeName().equals(SERVER_IP)) {
                Node ipNode = node.getChildNodes().item(0);
                String textContent = ipNode.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                return textContent;
            }
        }
        return null;
    }

    @Override
    public void setServerPublicKey(CustomPublicKey publicKey) throws Exception {
        Document doc = stringToXml(xml);

        Node serverElement = doc.getElementsByTagName(SERVER).item(0);
        NodeList serverNodes = serverElement.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Node node = serverNodes.item(i);
            if (node.getNodeName().equals(SERVER_KEY)) {
                NodeList serverKeyNodes = node.getChildNodes();
                for (int j = 0; j < serverKeyNodes.getLength(); j++) {
                    Node n = serverKeyNodes.item(j);

                    if (n.getNodeName().equals(SERVER_KEY_KEY_NR)) {
                        n.setNodeValue(Byte.toString(publicKey.getKeyNumber()));
                        continue;
                    }

                    if (n.getNodeName().equals(SERVER_KEY_PKIALGORITHM)) {
                        n.setNodeValue(Byte.toString(publicKey.getPkiAlgorithm()));
                        continue;
                    }

                    if (n.getNodeName().equals(SERVER_KEY_BASE64)) {
                        n.setNodeValue(publicKey.getPublicKey());
                        continue;
                    }
                }
                break;
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Returns the server's {@link CustomPublicKey} with the key number provided
     * from the given xml string.
     * 
     * @param xml
     *            the xml string to read out the server's
     *            {@link CustomPublicKey} from
     * @return the server's {@link CustomPublicKey} or null, if this field is
     *         not set or no {@link CustomPublicKey} with the provided key
     *         number could be found
     * @throws Exception
     *             an xml exception
     */
    @Override
    public CustomPublicKey getServerPublicKey() throws Exception {
        Document doc = stringToXml(xml);

        String textContent;

        Node serverElement = doc.getElementsByTagName(SERVER).item(0);
        NodeList serverNodes = serverElement.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Node node = serverNodes.item(i);
            if (node.getNodeName().equals(SERVER_KEY)) {
                NodeList serverKeyNodes = node.getChildNodes();
                if (serverKeyNodes.getLength() != 3)
                    return null;

                Node n = serverKeyNodes.item(0);
                assert n.getNodeName().equals(SERVER_KEY_KEY_NR);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                byte keyNr = Byte.parseByte(textContent);

                n = serverKeyNodes.item(1);
                assert n.getNodeName().equals(SERVER_KEY_PKIALGORITHM);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                byte pkiAlgorithm = Byte.parseByte(textContent);

                n = serverKeyNodes.item(2);
                assert n.getNodeName().equals(SERVER_KEY_BASE64);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                String publicKey = textContent;

                return new CustomPublicKey(keyNr, pkiAlgorithm, publicKey);
            }
        }
        return null;
    }

    /**
     * Sets the {@link UserAccount} to the xml and returns the updated xml
     * string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param userAccount
     *            the {@link UserAccount} to be added
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void setUserAccount(UserAccountObject userAccount) throws Exception {
        Document doc = stringToXml(xml);

        Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
        NodeList userAccountNodes = userAccountElement.getChildNodes();
        for (int i = 0; i < userAccountNodes.getLength(); i++) {
            Node node = userAccountNodes.item(i);

            if (node.getNodeName().equals(USER_ID)) {
                node.setNodeValue(Long.toString(userAccount.getId()));
                continue;
            }

            if (node.getNodeName().equals(USER_NAME)) {
                node.setNodeValue(userAccount.getUsername());
                continue;
            }

            if (node.getNodeName().equals(USER_BALANCE)) {
                long balance = Converter.getLongFromBigDecimal(userAccount.getBalanceBTC());
                node.setNodeValue(Long.toString(balance));
                continue;
            }

        }
        xmlToString(doc); save();
    }

    /**
     * Returns the {@link UserAccount} from the given xml string.
     * 
     * @param xml
     *            the xml string to read out the {@link UserAccount} from
     * @return the {@link UserAccount} or null, if any mandatory property is not
     *         set correctly or the {@link UserAccount} is not set
     * @throws Exception
     *             an xml exception
     */
    @Override
    public UserAccountObject getUserAccount() {
        Document doc = stringToXml(xml);

        long userId = 0;
        String username = null;
        BigDecimal balance = null;
        String paymentAddress = null;

        Node userAccountElement = doc.getElementsByTagName(USER_ACCOUNT).item(0);
        NodeList userAccountNodes = userAccountElement.getChildNodes();
        for (int i = 0; i < userAccountNodes.getLength(); i++) {
            Node node = userAccountNodes.item(i);
            String textContent;

            if (node.getNodeName().equals(USER_ID)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                userId = Long.parseLong(textContent);
                continue;
            }

            if (node.getNodeName().equals(USER_NAME)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                username = textContent;
                continue;
            }

            if (node.getNodeName().equals(USER_BALANCE)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                balance = Converter.getBigDecimalFromLong(Long.parseLong(textContent));
                continue;
            }

        }

        if (userId == 0 || username == null || balance == null || paymentAddress == null) {
            return null;
        } else {
            UserAccountObject ua = new UserAccountObject();
            ua.setId(userId);
            ua.setUsername(username);
            ua.setBalanceBTC(balance);
            return ua;

        }
    }

    /**
     * Sets the user's {@link CustomKeyPair} to the xml and returns the updated
     * xml string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param customKeyPair
     *            the {@link CustomKeyPair} to be added
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void setUserKeyPair(CustomKeyPair customKeyPair) throws Exception {
        Document doc = stringToXml(xml);

        Node userAccountElement = doc.getElementsByTagName(KEYPAIR).item(0);
        NodeList userAccountNodes = userAccountElement.getChildNodes();
        for (int i = 0; i < userAccountNodes.getLength(); i++) {
            Node node = userAccountNodes.item(i);

            if (node.getNodeName().equals(KEYPAIR_PKIALGORITHM)) {
                node.setNodeValue(Byte.toString(customKeyPair.getPkiAlgorithm()));
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_KEYNR)) {
                node.setNodeValue(Byte.toString(customKeyPair.getKeyNumber()));
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_PUBKEY)) {
                node.setNodeValue(customKeyPair.getPublicKey());
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_PRIVKEY)) {
                node.setNodeValue(customKeyPair.getPrivateKey());
                continue;
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Returns the user's {@link CustomKeyPair} from the given xml string.
     * 
     * @param xml
     *            the xml string to read out the {@link CustomKeyPair} from
     * @return the user's {@link CustomKeyPair} or null, if has not been set
     * @throws Exception
     *             an xml exception
     */
    @Override
    public CustomKeyPair getUserKeyPair() throws Exception {
        Document doc = stringToXml(xml);

        byte pkiAlgorithm = 0;
        byte keyNumber = 0;
        String publicKey = null;
        String privateKey = null;

        Node userAccountElement = doc.getElementsByTagName(KEYPAIR).item(0);
        NodeList userAccountNodes = userAccountElement.getChildNodes();
        for (int i = 0; i < userAccountNodes.getLength(); i++) {
            Node node = userAccountNodes.item(i);
            String textContent;

            if (node.getNodeName().equals(KEYPAIR_PKIALGORITHM)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                pkiAlgorithm = Byte.parseByte(textContent);
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_KEYNR)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                keyNumber = Byte.parseByte(textContent);
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_PUBKEY)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                publicKey = textContent;
                continue;
            }

            if (node.getNodeName().equals(KEYPAIR_PRIVKEY)) {
                textContent = node.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    return null;

                privateKey = textContent;
                continue;
            }
        }

        if (pkiAlgorithm == 0 || keyNumber == 0 || publicKey == null || privateKey == null)
            return null;
        else
            return new CustomKeyPair(pkiAlgorithm, keyNumber, publicKey, privateKey);
    }

    /**
     * Returns the address book contacts from the given xml string.
     * 
     * @param xml
     *            the xml string to read out the address book entries from
     * @return a set of string containing all entries or an empty set, if there
     *         are no address book entries
     * @throws Exception
     *             an xml exception
     */
    @Override
    public Set<String> getAddressBookContacts() throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);
        Set<String> contactsSet = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);

            if (addressBookChild.getNodeName().equals(CONTACTS)) {
                NodeList contactNodes = addressBookChild.getChildNodes();
                for (int j = 0; j < contactNodes.getLength(); j++) {
                    Node contact = contactNodes.item(j);
                    contactsSet.add(contact.getTextContent());
                }
            }
        }
        return contactsSet;
    }

    /**
     * Adds an address book entry to the xml and returns the updated xml string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param username
     *            the username to add to the address book
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void addAddressBookContact(String username) throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);

            if (addressBookChild.getNodeName().equals(CONTACTS)) {
                Element contact = doc.createElement("contact");
                contact.setNodeValue(username);
                addressBookChild.appendChild(contact);
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Returns the trusted address book contacts from the given xml string.
     * 
     * @param xml
     *            the xml string to read out the trusted address book entries
     *            from
     * @return a set of string containing all trusted contacts or an empty set,
     *         if there are not trusted contacts
     * @throws Exception
     *             an xml exception
     */
    @Override
    public Set<String> getTrusteAddressBookdContacts() throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);
        Set<String> trustedContactsSet = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);

            if (addressBookChild.getNodeName().equals(TRUSTED_CONTACTS)) {
                NodeList trustedContactNodes = addressBookChild.getChildNodes();
                for (int j = 0; j < trustedContactNodes.getLength(); j++) {
                    Node contact = trustedContactNodes.item(j);
                    trustedContactsSet.add(contact.getTextContent());
                }
            }
        }
        return trustedContactsSet;
    }

    /**
     * Adds a trusted address book entry to the xml and returns the updated xml
     * string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param username
     *            the username to add to the address book
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void addTrustedAddressBookContact(String username) throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);

            if (addressBookChild.getNodeName().equals(TRUSTED_CONTACTS)) {
                Element contact = doc.createElement("contact");
                contact.setNodeValue(username);
                addressBookChild.appendChild(contact);
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Removes the address book entry with the given username from the xml and
     * returns the updated xml string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param username
     *            the username to remove from the address book
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void removeAddressBookContact(String username) throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);
            if (addressBookChild.getNodeName().equals(CONTACTS)) {

                NodeList nodeList = addressBookChild.getChildNodes();
                for (int j = 0; j < nodeList.getLength(); j++) {
                    Node nodeToRemove = nodeList.item(j);

                    if (nodeToRemove.getTextContent().equals(username)) {
                        addressBookChild.removeChild(nodeToRemove);
                    }
                }
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Removes the trusted address book entry with the given username from the
     * xml and returns the updated xml string.
     * 
     * @param xml
     *            the xml string to be updated
     * @param username
     *            the username to remove from the trusted contacts
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void removeTrustedAddressBookEntry(String username) throws Exception {
        Document doc = stringToXml(xml);

        Node addressBookElement = doc.getElementsByTagName(ADDRESS_BOOK).item(0);

        NodeList addressBookNodes = addressBookElement.getChildNodes();
        for (int i = 0; i < addressBookNodes.getLength(); i++) {
            Node addressBookChild = addressBookNodes.item(i);
            if (addressBookChild.getNodeName().equals(TRUSTED_CONTACTS)) {

                NodeList nodeList = addressBookChild.getChildNodes();
                for (int j = 0; j < nodeList.getLength(); j++) {
                    Node nodeToRemove = nodeList.item(j);

                    if (nodeToRemove.getTextContent().equals(username)) {
                        addressBookChild.removeChild(nodeToRemove);
                    }
                }
            }
        }
        xmlToString(doc); save();
    }

    /**
     * Returns all stored {@link PersistedPaymentRequest}s from the given xml
     * string.
     * 
     * @param xml
     *            the xml string to read out the {@link PersistedPaymentRequest}
     *            s from
     * @return a set of {@link PersistedPaymentRequest} or an empty set, if
     *         there are not {@link PersistedPaymentRequest}s
     * @throws Exception
     *             an xml exception or if the stored code cannot be mapped to a
     *             {@link Currency}
     */
    @Override
    public Set<PersistedPaymentRequest> getPersistedPaymentRequests() throws Exception {
        Document doc = stringToXml(xml);

        Set<PersistedPaymentRequest> set = new LinkedHashSet<PersistedPaymentRequest>();

        Node pendingRequestsElement = doc.getElementsByTagName(PENDING_REQUESTS).item(0);
        NodeList pendingRequestsNodes = pendingRequestsElement.getChildNodes();

        String username;
        Currency currency;
        long amount;
        long timestamp;

        String textContent;

        for (int i = 0; i < pendingRequestsNodes.getLength(); i++) {
            Node pendingRequestNode = pendingRequestsNodes.item(i);
            if (pendingRequestNode.getNodeName().equals(PENDING_REQUEST)) {
                NodeList nodeList = pendingRequestNode.getChildNodes();
                if (nodeList.getLength() != 4)
                    continue;

                Node n = nodeList.item(0);
                assert n.getNodeName().equals(PENDING_REQUEST_USERNAME_PAYEE);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                username = textContent;

                n = nodeList.item(1);
                assert n.getNodeName().equals(PENDING_REQUEST_CURRENCY);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                currency = Currency.getCurrency(Byte.parseByte(textContent));

                n = nodeList.item(2);
                assert n.getNodeName().equals(PENDING_REQUEST_AMOUNT);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                amount = Long.parseLong(textContent);

                n = nodeList.item(3);
                assert n.getNodeName().equals(PENDING_REQUEST_TIMESTAMP);
                textContent = n.getTextContent();
                if (textContent == null || textContent.isEmpty())
                    continue;

                timestamp = Long.parseLong(textContent);

                set.add(new PersistedPaymentRequest(username, currency, amount, timestamp));
            }
        }
        return set;
    }

    /**
     * Adds the {@link PersistedPaymentRequest} to the xml and returns the
     * updated xml string. Assure that you are not adding a
     * {@link PersistedPaymentRequest} which is already contained in the set.
     * 
     * @param xml
     *            the xml string to be updated
     * @param persistedRequest
     *            the {@link PersistedPaymentRequest} to be added
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void addPersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception {
        Document doc = stringToXml(xml);

        Node pendingRequestsElement = doc.getElementsByTagName(PENDING_REQUESTS).item(0);

        Element pendingRequestNode = doc.createElement(PENDING_REQUEST);

        Element usernameElement = doc.createElement(PENDING_REQUEST_USERNAME_PAYEE);
        usernameElement.setNodeValue(persistedRequest.getUsername());
        pendingRequestNode.appendChild(usernameElement);

        Element currencyElement = doc.createElement(PENDING_REQUEST_CURRENCY);
        currencyElement.setNodeValue(String.valueOf(persistedRequest.getCurrency().getCode()));
        pendingRequestNode.appendChild(currencyElement);

        Element amountElement = doc.createElement(PENDING_REQUEST_AMOUNT);
        amountElement.setNodeValue(String.valueOf(persistedRequest.getAmount()));
        pendingRequestNode.appendChild(amountElement);

        Element timestampElement = doc.createElement(PENDING_REQUEST_TIMESTAMP);
        timestampElement.setNodeValue(String.valueOf(persistedRequest.getTimestamp()));
        pendingRequestNode.appendChild(timestampElement);

        pendingRequestsElement.appendChild(pendingRequestNode);

        xmlToString(doc); save();
    }

    /**
     * Removes the {@link PersistedPaymentRequest} from the xml and returns the
     * updated xml string. Assure that you are not trying to delete a
     * {@link PersistedPaymentRequest} which is not contained in the set.
     * 
     * @param xml
     *            the xml string to be updated
     * @param persistedRequest
     *            the {@link PersistedPaymentRequest} to be removed
     * @return the updated xml string
     * @throws Exception
     *             an xml exception
     */
    @Override
    public void deletePersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception {
        Document doc = stringToXml(xml);

        Node pendingRequestsElement = doc.getElementsByTagName(PENDING_REQUESTS).item(0);
        NodeList pendingRequestsNodes = pendingRequestsElement.getChildNodes();
        for (int i = 0; i < pendingRequestsNodes.getLength(); i++) {
            Node pendingRequestNode = pendingRequestsNodes.item(i);

            NodeList childNodes = pendingRequestNode.getChildNodes();
            String username = childNodes.item(0).getTextContent();
            if (username == null || username.isEmpty() || !username.equals(persistedRequest.getUsername())) {
                continue;
            }
            String currency = childNodes.item(1).getTextContent();
            if (currency == null || currency.isEmpty() || Byte.parseByte(currency) != persistedRequest.getCurrency().getCode()) {
                continue;
            }
            String amount = childNodes.item(2).getTextContent();
            if (amount == null || amount.isEmpty() || Long.parseLong(amount) != persistedRequest.getAmount()) {
                continue;
            }
            String timestamp = childNodes.item(3).getTextContent();
            if (timestamp == null || timestamp.isEmpty() || Long.parseLong(timestamp) != persistedRequest.getTimestamp()) {
                continue;
            }
            pendingRequestsElement.removeChild(pendingRequestNode);
        }

        xmlToString(doc); save();
    }

    private boolean save() throws Exception {
        String encrypted = encrypter.encrypt(xml, password);
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(getStorageFile()));
            writer.write(encrypted);
        } catch (Exception e) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private File getStorageFile() {
        return new File(directory, filename);
    }

    /**
     * Initializes the {@link InternalStorageHandler}. This must always be
     * called after instantiating a new object. It reads the existing xml file
     * or creates a new one if it does not exist.
     * 
     * @return true if a new file was created, false otherwise
     * @throws WrongPasswordException
     * @throws IOException
     */
    private boolean init() throws Exception {
        if (fileExists()) {
            BufferedReader reader = new BufferedReader(new FileReader(getStorageFile()));
            String line;
            String content = "";
            while ((line = reader.readLine()) != null) {
                content += line;
            }
            reader.close();

            encrypter.decrypt(content, password, ROOT);
            return true;
        } else {
            createEmptyXML();
            return false;
        }
    }

    private boolean fileExists() {
        String[] fileList = directory.list();
        for (int i = 0; i < fileList.length; i++) {
            String file = fileList[i];
            if (file.equals(getFilename(username))) {
                return true;
            }
        }
        return false;
    }

}
