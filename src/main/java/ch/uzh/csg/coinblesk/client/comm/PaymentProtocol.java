package ch.uzh.csg.coinblesk.client.comm;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by tbocek on 14.07.15.
 */
final public class PaymentProtocol {

    public enum Type{CONTACT_AND_PAYMENT_REQUEST, CONTACT_AND_PAYMENT_RESPONSE_OK, CONTACT_AND_PAYMENT_RESPONSE_NOK, FROM_SERVER_REQUEST_OK, FROM_SERVER_REQUEST_NOK/*, UNUSED1, UNUSED2, UNUSED3*/}

    //5 bits max 64, for now its version 0
    final private int version = 0;
    //3 bits
    final private Type type;
    final private static int HEADER_LENGTH = 1;

    //variable, max 256
    private PublicKey publicKey;
    //max. 256bytes
    private String user;
    //variable length

    //max. 64KB
    private byte[] halfSignedTransaction;
    private byte[] fullySignedTransaction;
    //variable length

    //fix 20bytes
    private byte[] sendTo;
    final private static int SENDTO_LENGTH = 20;

    //fix - 64bits - 8bytes
    private long satoshis;
    final private static int AMOUNT_LENGTH = 8;

    //fix - 48bit -6bytes
    private byte[] btAddress;
    final private static int BT_LENGTH = 6;

    private boolean signatureVerified = false;

    private final static KeyFactory keyFactory;
    static {
    	Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    private PaymentProtocol(final Type type) {
        this.type = type;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public String user() {
        return user;
    }

    public byte[] halfSignedTransaction() {
        return halfSignedTransaction;
    }

    public byte[] fullySignedTransaction() {
        return fullySignedTransaction;
    }

    public byte[] sendTo() {
        return sendTo;
    }

    public long satoshis() {
        return satoshis;
    }

    public byte[] btAddress() {
        return btAddress;
    }
    
    public boolean isVerified() {
    	return signatureVerified;
    }


    public byte[] toBytes(PrivateKey privateKey) throws UnsupportedEncodingException, NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        final byte header = (byte) ((type.ordinal() & 0x7) | (version << 3));
        final byte[] data;
        
        int offset = 0;
        final byte[] signature;
        final int signatureLen;
        final byte[] retVal;
        switch (type) {
            case CONTACT_AND_PAYMENT_REQUEST:
            	
            	final byte[] publicKeyEncoded1 = publicKey.getEncoded();
                final int publicKeyLen1 = publicKeyEncoded1.length;
                if(publicKeyLen1 > 255) {
                	throw new RuntimeException("user is too large");
                }
                final byte[] userEncoded1 = user.getBytes("UTF-8");
                final int userLen1 = userEncoded1.length;
                if(userLen1 > 255) {
                	throw new RuntimeException("user is too large");
                }
            	
                data = new byte[HEADER_LENGTH+publicKeyLen1 + 1 +SENDTO_LENGTH+AMOUNT_LENGTH+BT_LENGTH+userLen1 + 1];
                data[offset++] = header;

                //public key
                data[offset++] = (byte) publicKeyLen1;
                System.arraycopy(publicKeyEncoded1,0,data, offset, publicKeyLen1);
                offset += publicKeyLen1;
                //user
                data[offset++] = (byte) userLen1;
                System.arraycopy(userEncoded1,0,data, offset, userLen1);
                offset += userLen1;
                //bluetooth
                System.arraycopy(btAddress,0,data, offset, BT_LENGTH);
                offset += BT_LENGTH;

                //amount
                offset = encodeLong(satoshis, data, offset);

                //BTC address
                System.arraycopy(sendTo,0,data, offset, SENDTO_LENGTH);
                offset += SENDTO_LENGTH;


                break;
            case CONTACT_AND_PAYMENT_RESPONSE_OK:
            	
            	final byte[] publicKeyEncoded2 = publicKey.getEncoded();
                final int publicKeyLen2 = publicKeyEncoded2.length;
                if(publicKeyLen2 > 255) {
                	throw new RuntimeException("user is too large");
                }
                final byte[] userEncoded2 = user.getBytes("UTF-8");
                final int userLen2 = userEncoded2.length;
                if(userLen2 > 255) {
                	throw new RuntimeException("user is too large");
                }
            	
                final int halfSignedTransactionLen = halfSignedTransaction.length;
                data = new byte[HEADER_LENGTH+publicKeyLen2 + 1 +halfSignedTransactionLen + 2 + BT_LENGTH + userLen2 + 1];
                data[offset++] = header;

                //public key
                data[offset++] = (byte) publicKeyLen2;
                System.arraycopy(publicKeyEncoded2,0,data, offset, publicKeyLen2);
                offset += publicKeyLen2;
                //user
                data[offset++] = (byte) userLen2;
                System.arraycopy(userEncoded2,0,data, offset, userLen2);
                offset += userLen2;
                //bluetooth
                System.arraycopy(btAddress,0,data, offset, BT_LENGTH);
                offset += BT_LENGTH;

                //transaction
                offset = encodeShort(halfSignedTransactionLen, data, offset);
                System.arraycopy(halfSignedTransaction,0,data, offset, halfSignedTransactionLen);
                offset += halfSignedTransactionLen;

                break;
            case FROM_SERVER_REQUEST_OK:
                final int fullSignedTransactionLen = fullySignedTransaction.length;
                data = new byte[HEADER_LENGTH+fullSignedTransactionLen + 2 + BT_LENGTH];
                data[offset++] = header;
                //transaction
                offset = encodeShort((short) fullSignedTransactionLen, data, offset);
                System.arraycopy(fullySignedTransaction,0,data, offset, fullSignedTransactionLen);
                offset += fullSignedTransactionLen;

                break;
            case CONTACT_AND_PAYMENT_RESPONSE_NOK:
            case FROM_SERVER_REQUEST_NOK:
                data = new byte[HEADER_LENGTH];
                data[offset++] = header;
                break;
            default:
                throw new RuntimeException("unknown type: "+type);
        }
        //signature
        signature = sign(privateKey, data, offset);
        signatureLen = signature.length;
        retVal = Arrays.copyOf(data, data.length + 1 + signatureLen);
        retVal[offset++] = (byte) signatureLen;
        System.arraycopy(signature,0,retVal, offset, signatureLen);
        return retVal;
    }

    public static PaymentProtocol fromBytes(final byte[] data, PublicKey publicKey) throws InvalidKeySpecException, UnsupportedEncodingException, NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        if(data.length<1) {
            throw new RuntimeException("could not parse bytes");
        }
        int offset = 0;
        final int version = (data[offset] >>> 3) & 0x1F;
        final Type type = Type.values()[data[offset] & 0x7];
        offset++;
        if(version != 0) {
            throw new RuntimeException("wrong version");
        }

        final PaymentProtocol paymentMessage = new PaymentProtocol(type);
        switch (type) {
            case CONTACT_AND_PAYMENT_REQUEST:
            case CONTACT_AND_PAYMENT_RESPONSE_OK:
                //public key and user
                final int publicKeyLen = data[offset++] & 0xff;
                final byte[] publicKeyEncoded = new byte[publicKeyLen];
                System.arraycopy(data,offset,publicKeyEncoded,0,publicKeyLen);
                offset += publicKeyLen;
                PublicKey publicKeyMsg = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded));
                if(publicKey != null && !publicKey.equals(publicKeyMsg)) {
                    throw new RuntimeException("two different public keys?");
                }
                publicKey = publicKeyMsg;
                paymentMessage.publicKey=publicKey;
                final int userLen = data[offset++] & 0xff;
                final byte[] userEncoded = new byte[userLen];
                System.arraycopy(data,offset,userEncoded,0,userLen);
                offset += userLen;
                final String user = new String(userEncoded, "UTF-8");
                paymentMessage.user = user;
                //bluetooth
                final byte[] btAddress = new byte[BT_LENGTH];
                System.arraycopy(data,offset,btAddress, 0, BT_LENGTH);
                offset += BT_LENGTH;
                paymentMessage.btAddress = btAddress;

                if(type ==Type.CONTACT_AND_PAYMENT_REQUEST ) {
                    //amount
                    final long satoshi = decodeLong(data, offset);
                    offset += AMOUNT_LENGTH;
                    paymentMessage.satoshis = satoshi;
                    //address
                    final byte[] sendTo = new byte[SENDTO_LENGTH];
                    System.arraycopy(data, offset, sendTo, 0, SENDTO_LENGTH);
                    offset += SENDTO_LENGTH;
                    paymentMessage.sendTo = sendTo;
                    break;
                } else {
                    final short transactionLen = decodeShort(data,offset);
                    offset += 2;
                    final byte[] transactionEncoded = new byte[transactionLen];
                    System.arraycopy(data,offset,transactionEncoded,0, transactionLen);
                    offset += transactionLen;
                    paymentMessage.halfSignedTransaction = transactionEncoded;
                    break;
                }
            case FROM_SERVER_REQUEST_OK:
                final short transactionLen = decodeShort(data,offset);
                offset += 2;
                final byte[] transactionEncoded = new byte[transactionLen];
                System.arraycopy(data,offset,transactionEncoded,0, transactionLen);
                offset += transactionLen;
                paymentMessage.fullySignedTransaction = transactionEncoded;
                break;

            case FROM_SERVER_REQUEST_NOK:
            case CONTACT_AND_PAYMENT_RESPONSE_NOK:

                break;
        }

        //check signature
        paymentMessage.signatureVerified = verify(publicKey, data, offset);
        return paymentMessage;
    }

    @Override
    public boolean equals(final Object o) {
        if(o == this) {
            return true;
        }
        if(! (o instanceof  PaymentProtocol)) {
            return false;
        }

        final PaymentProtocol p = (PaymentProtocol) o;
        return Objects.equals(type, p.type) && Objects.equals(publicKey, p.publicKey) && Objects.equals(user, p.user)
                && Arrays.equals(halfSignedTransaction, p.halfSignedTransaction) && Arrays.equals(fullySignedTransaction, p.fullySignedTransaction)
                && Arrays.equals(sendTo, p.sendTo) && Arrays.equals(btAddress, p.btAddress) && satoshis == p.satoshis;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type) ^ Objects.hashCode(publicKey) ^ Objects.hashCode(user)
                ^ Arrays.hashCode(halfSignedTransaction) ^ Arrays.hashCode(fullySignedTransaction)
                ^ Arrays.hashCode(sendTo) ^ Arrays.hashCode(btAddress) ^ (int) (satoshis ^ (satoshis >>> 32));
    }

    private static boolean verify(final PublicKey publicKey, final byte[] data, int offset) throws InvalidKeyException, SignatureException, NoSuchProviderException, NoSuchAlgorithmException {
    	if(publicKey == null) {
    		throw new RuntimeException("public key cannot be null");
    	}
        final Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "SC");
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(data, 0, offset);
        final int signatureLen = data[offset++] & 0xff;
        return ecdsaVerify.verify(data, offset, signatureLen);
    }

    private static byte[] sign(final PrivateKey privateKey, final byte[] data, final int offset) throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
    	if(privateKey == null) {
    		throw new RuntimeException("private key cannot be null");
    	}
        final Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", "SC");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(data, 0, offset);
        return ecdsaSign.sign();
    }


    public static PaymentProtocol contactAndPaymentRequest(final PublicKey publicKey, final String user, final byte[] btAddress, final long satoshis, final byte[] sendTo) {
    	if(publicKey == null) {
    		throw new RuntimeException("public key cannot be null");
    	}
    	if(user == null) {
    		throw new RuntimeException("user cannot be null");
    	}
    	if(btAddress.length != BT_LENGTH) {
    		throw new RuntimeException("wrong length of the bluetooth address");
    	}
    	if(sendTo.length != SENDTO_LENGTH) {
    		throw new RuntimeException("wrong length of the receiving address");
    	}
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.CONTACT_AND_PAYMENT_REQUEST);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress =btAddress;
        paymentMessage.satoshis = satoshis;
        paymentMessage.sendTo = sendTo;
        return paymentMessage;
    }

    public static PaymentProtocol contactAndPaymentResponseOk(final PublicKey publicKey, final String user, final byte[] btAddress, final byte[] halfSignedTransaction) {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.CONTACT_AND_PAYMENT_RESPONSE_OK);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress =btAddress;
        paymentMessage.halfSignedTransaction = halfSignedTransaction;
        return paymentMessage;
    }

    public static PaymentProtocol contactAndPaymentResponseNok() {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.CONTACT_AND_PAYMENT_RESPONSE_NOK);
        return paymentMessage;
    }

    public static PaymentProtocol fromServerRequestOk(final byte[] fullySignedTransaction) {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.FROM_SERVER_REQUEST_OK);
        paymentMessage.fullySignedTransaction = fullySignedTransaction;
        return paymentMessage;
    }

    public static PaymentProtocol fromServerRequestNok() {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.FROM_SERVER_REQUEST_NOK);
        return paymentMessage;
    }

    private static int encodeLong(final long value, final byte[] data, final int offset) {
        data[offset  ] = (byte) (value >> 56);
        data[offset+1] = (byte) (value >> 48);
        data[offset+2] = (byte) (value >> 40);
        data[offset+3] = (byte) (value >> 32);
        data[offset+4] = (byte) (value >> 24);
        data[offset+5] = (byte) (value >> 16);
        data[offset+6] = (byte) (value >> 8);
        data[offset+7] = (byte) value;
        return offset + 8;
    }

    private static long decodeLong(final byte[] data, final int offset) {
       return   ( (((long) data[offset    ] & 0xff) << 56) | (((long) data[offset + 1] & 0xff) << 48) | (((long) data[offset + 2] & 0xff) << 40)
                | (((long) data[offset + 3] & 0xff) << 32) | (((long) data[offset + 4] & 0xff) << 24) | (((long) data[offset + 5] & 0xff) << 16)
                | (((long) data[offset + 6] & 0xff) << 8) | (((long) data[offset + 7] & 0xff)));
    }

    private static int encodeShort(final int value, final byte[] data, final int offset) {
    	if(value > 65535) {
    		throw new RuntimeException("short is too large");
    	}
        data[offset   ] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;
        return offset + 2;
    }

    private static short decodeShort(final byte[] data, final int offset) {
        return   (short) (((data[offset] & 0xff) << 8) | ( data[offset + 1] & 0xff));
    }

    public static KeyPair generateKeys()
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        final ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("brainpoolp256r1");
        final KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "SC");
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
    }
}
