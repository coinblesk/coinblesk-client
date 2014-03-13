package ch.uzh.csg.mbps.client.util;

import java.io.StringReader;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * This class encrypts and decrypts the sensible user information's into the 
 * internal storage.
 * copied from http://stackoverflow.com/questions/14022934/android-cipher-encrypt-decrypt
 */
public class InternalStorageSecurityUtils {
	private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";
	private static final String DELIMITER = "]";
	private static final int ITERATION_COUNT = 1000;
	private static final int KEY_LENGTH = 256;
	private static final int SALT_LENGTH = KEY_LENGTH / 8;
	private static SecureRandom random = new SecureRandom();
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * Encrypts the user informations. The key which is used to encrypt the user
	 * information is the password.
	 * 
	 * @param xmltext
	 *            The string from the internal storage
	 * @param password
	 *            The inserted password
	 * @return Returns
	 * @throws Exception
	 */
	public static String encrypt(String xmltext, String password) throws Exception {
		byte[] salt = generateSalt();
		return encrypt(xmltext, getKey(salt, password), salt);
	}
	
	private static String encrypt(String xmltext, SecretKey key, byte[] salt) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");

        byte[] iv = generateIv(cipher.getBlockSize());
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
        byte[] cipherText = cipher.doFinal(xmltext.getBytes("UTF-8"));
        
        byte[] saltByte = org.bouncycastle.util.encoders.Base64.encode(salt);
        byte[] ivByte = org.bouncycastle.util.encoders.Base64.encode(iv);
        byte[] cipherTextBytes = org.bouncycastle.util.encoders.Base64.encode(cipherText);
        
        if (salt != null) {
            return String.format("%s%s%s%s%s", new String(saltByte), DELIMITER, new String(ivByte), DELIMITER, new String(cipherTextBytes));
        }

        return String.format("%s%s%s", new String(ivByte), DELIMITER, new String(cipherTextBytes));
	}

	/**
	 * Decrypts the stored user information from the internal storage. The
	 * password is used as key to decrypt it.
	 * 
	 * @param ciphertext
	 *            The string of the user informations.
	 * @param password
	 *            The inserted password
	 * @return Returns The readable string format of the user's information.
	 * @throws WrongPasswordException
	 */
	public static String decrypt(String ciphertext, String password) throws WrongPasswordException {
	    String[] fields = ciphertext.split(DELIMITER);
	    if (fields.length != 3)
	        return null;
	    
	    try {
	        byte[] salt = org.bouncycastle.util.encoders.Base64.decode(fields[0]);
	        byte[] iv = org.bouncycastle.util.encoders.Base64.decode(fields[1]);
	        byte[] cipherBytes = org.bouncycastle.util.encoders.Base64.decode(fields[2]);
  
	        SecretKey key = getKey(salt, password);
	        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");
	        IvParameterSpec ivParams = new IvParameterSpec(iv);
	        cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
	        byte[] xmltext = cipher.doFinal(cipherBytes);
	        String xmlStr = new String(xmltext, "UTF-8");
	        
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        InputSource is = new InputSource(new StringReader(xmlStr));
	        Document xmlElement = builder.parse(is);
	        
			if (xmlElement.getDocumentElement().getAttribute("identifier").matches("mbps_client")) {
				return xmlStr;
			} else {
				throw new WrongPasswordException(password);
			}
	    } catch (Throwable e) {
	    	if (e instanceof WrongPasswordException)
	    		throw (WrongPasswordException) e;
	    	return null;
	    }
	}
	
	private static SecretKey getKey(byte[] salt, String password) throws Exception {
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
	}
	
	private static byte[] generateIv(int length) {
	    byte[] b = new byte[length];
	    random.nextBytes(b);
	    return b;
	}
	
	private static byte[] generateSalt() {
	    byte[] b = new byte[SALT_LENGTH];
	    random.nextBytes(b);
	    return b;
	}
}
