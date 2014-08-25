package ch.uzh.csg.mbps.client.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;



import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.util.encoders.Base64;

import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.exceptions.UnknownPKIAlgorithmException;

/**
 * The KeyHandler allows generating KeyPairs and encoding a key to base64 string
 * or decoding from base64 string.
 * 
 * @author Jeton Memeti
 * 
 */
public class KeyHandler {
	
	private static final String SECURITY_PROVIDER = "SC";
	
	/**
	 * Adds the spongy castle security provider in order to be able to generate
	 * ECC KeyPairs on Android. (See http://rtyley.github.io/spongycastle/ for
	 * more information.)
	 */
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * Generates a KeyPair with the default {@link PKIAlgorithm}.
	 * 
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static KeyPair generateKeyPair() throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return generateKeyPair(PKIAlgorithm.DEFAULT);
	}

	/**
	 * Generates a KeyPair with the provided {@link PKIAlgorithm}.
	 * 
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the KeyPair
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static KeyPair generateKeyPair(PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(algorithm.getKeyPairSpecification());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		keyGen.initialize(ecSpec, new SecureRandom());
		return keyGen.generateKeyPair();
	}
	
	/**
	 * Encodes the given PrivateKey into a String using Base64 encoding.
	 */
	public static String encodePrivateKey(PrivateKey privateKey) {
		byte[] privateEncoded = Base64.encode(privateKey.getEncoded());
		return new String(privateEncoded);
	}
	
	/**
	 * Encodes the given PublicKey into a String using Base64 encoding.
	 */
	public static String encodePublicKey(PublicKey publicKey) {
		byte[] publicEncoded = Base64.encode(publicKey.getEncoded());
		return new String(publicEncoded);
	}
	
	/**
	 * Decodes the given Base64 encoded String into a PublicKey, using the
	 * default {@link PKIAlgorithm}.
	 * 
	 * @param publicKeyEncoded
	 *            the string to be decoded
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PublicKey decodePublicKey(String publicKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePublicKey(publicKeyEncoded, PKIAlgorithm.DEFAULT);
	}

	/**
	 * Decodes the given Base64 encoded String into a PublicKey, using the
	 * provided {@link PKIAlgorithm}.
	 * 
	 * @param publicKeyEncoded
	 *            the string to be decoded
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the PublicKey
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PublicKey decodePublicKey(String publicKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decode(publicKeyEncoded.getBytes());
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		return keyFactory.generatePublic(publicKeySpec);
	}
	
	/**
	 * Decodes the given Base64 encoded String into a PrivateKey, using the
	 * default {@link PKIAlgorithm}.
	 * 
	 * @param privateKeyEncoded
	 *            the string to be decoded
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PrivateKey decodePrivateKey(String privateKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePrivateKey(privateKeyEncoded, PKIAlgorithm.DEFAULT);
	}
	
	/**
	 * Decodes the given Base64 encoded String into a PrivateKey, using the
	 * provided {@link PKIAlgorithm}.
	 * 
	 * @param privateKeyEncoded
	 *            the string to be decoded
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the PublicKey
	 * @return
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PrivateKey decodePrivateKey(String privateKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decode(privateKeyEncoded.getBytes());
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		return keyFactory.generatePrivate(privateKeySpec);
	}

}
