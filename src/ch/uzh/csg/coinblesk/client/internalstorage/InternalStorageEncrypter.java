package ch.uzh.csg.coinblesk.client.internalstorage;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.spongycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * This class encrypts and decrypts Strings (mainly the sensitive user data
 * stored on the internal device storage). (Copied from
 * http://stackoverflow.com/questions/14022934/android-cipher-encrypt-decrypt)
 */
public class InternalStorageEncrypter {
	private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";
	private static final String DELIMITER = "]]]]";
	private static final int ITERATION_COUNT = 1000;
	private static final int KEY_LENGTH = 256;
	private static final int SALT_LENGTH = KEY_LENGTH / 8;
	private static SecureRandom random = new SecureRandom();
	
	/**
	 * Encrypt the given string plain text and returns the cipher text.
	 * 
	 * @param xml
	 *            the plain text to be encrypted
	 * @param password
	 *            the password to be used for encryption
	 * @return the cipher text
	 * @throws Exception
	 *             if the encryption was not successful
	 */
	protected String encrypt(String xml, String password) throws Exception {
		byte[] salt = generateSalt();
		return encrypt(xml, getKey(salt, password), salt);
	}
	
	private String encrypt(String xml, SecretKey key, byte[] salt) throws Exception {
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");
		
		byte[] iv = generateIv(cipher.getBlockSize());
		IvParameterSpec ivParams = new IvParameterSpec(iv);
		
		cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
		byte[] cipherText = cipher.doFinal(xml.getBytes(Charset.forName("UTF-8")));
		
		byte[] saltBytes = Base64.encode(salt);
		byte[] ivBytes = Base64.encode(iv);
		byte[] cipherTextBytes = Base64.encode(cipherText);
		
        return String.format("%s%s%s%s%s", new String(saltBytes), DELIMITER, new String(ivBytes), DELIMITER, new String(cipherTextBytes));
	}

	/**
	 * Decrypts the cipher text and returns the plain text.
	 * 
	 * @param ciphertext
	 *            the cipher text to be decrypted
	 * @param password
	 *            the password to be used for the decryption
	 * @param rootElementName
	 *            the root element of the xml file (used as validation if the
	 *            decryption was successful and therefore the provided password
	 *            was valid)
	 * @return the plain text
	 * @throws WrongPasswordException
	 *             if the provided password does not match
	 * @throws CorruptFileException
	 *             if the stored file is corrupt and cannot be decrypted
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 */
	protected String decrypt(String ciphertext, String password, String rootElementName) throws WrongPasswordException, Exception {
	    String[] fields = ciphertext.split(DELIMITER);
	    if (fields.length != 3)
	        throw new CorruptFileException();
	    
	        byte[] salt = Base64.decode(fields[0]);
	        byte[] iv = Base64.decode(fields[1]);
	        byte[] cipherBytes = Base64.decode(fields[2]);
  
	        SecretKey key = getKey(salt, password);
	        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");
	        IvParameterSpec ivParams = new IvParameterSpec(iv);
	        cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
	        
	        try {
	        	byte[] xmlBytes = cipher.doFinal(cipherBytes);
	        	String xml = new String(xmlBytes, Charset.forName("UTF-8"));
	        	
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        	DocumentBuilder builder = factory.newDocumentBuilder();
	        	InputSource is = new InputSource(new StringReader(xml));
	        	Document doc = builder.parse(is);
	        	
	        	if (doc.getDocumentElement().getNodeName().equals(rootElementName))
	        		return xml;
	        } catch (Exception e) {
	        }
	        throw new WrongPasswordException("The password is invalid.");
	}
	
	private SecretKey getKey(byte[] salt, String password) throws Exception {
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);

		byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
		return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
	}

	private byte[] generateIv(int length) {
		byte[] b = new byte[length];
		random.nextBytes(b);
		return b;
	}

	private byte[] generateSalt() {
		byte[] b = new byte[SALT_LENGTH];
		random.nextBytes(b);
		return b;
	}
	
}
