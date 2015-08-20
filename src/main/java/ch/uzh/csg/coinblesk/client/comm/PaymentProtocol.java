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

    public enum Type{
        PAYMENT_REQUEST, PAYMENT_REQUEST_RESPONSE,
        PAYMENT_OK, PAYMENT_NOK, FULL_TRANSACTION,
        SERVER_NOK, PAYMENT_SEND, PAYMENT_SEND_RESPONSE
    }

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

    //variable -4 bytes child path,
    private int[] childPaths;
    final private static int CHILD_PATH_LENGTH = 4;

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

    public int[] getChildPath() { return childPaths; }


    public byte[] toBytes(PrivateKey privateKey) throws UnsupportedEncodingException, NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        final byte header = (byte) ((type.ordinal() & 0x7) | (version << 3));
        final byte[] data;
        
        int offset = 0;
        final byte[] signature;
        final int signatureLen;
        final byte[] retVal;
        switch (type) {
            case PAYMENT_REQUEST:
            	final byte[] publicKeyEncoded1 = publicKey.getEncoded();
                final int publicKeyLen1 = publicKeyEncoded1.length;
                if(publicKeyLen1 > 255) {
                	throw new RuntimeException("public key is too large");
                }
                final byte[] userEncoded1 = user.getBytes("UTF-8");
                final int userLen1 = userEncoded1.length;
                if(userLen1 > 255) {
                	throw new RuntimeException("user is too large");
                }
            	
                data = new byte[HEADER_LENGTH + publicKeyLen1 + 1 +SENDTO_LENGTH + AMOUNT_LENGTH + BT_LENGTH + userLen1 + 1];
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
                System.arraycopy(sendTo, 0, data, offset, SENDTO_LENGTH);
                offset += SENDTO_LENGTH;

                break;

            case PAYMENT_SEND:
                final byte[] publicKeyEncoded2 = publicKey.getEncoded();
                final int publicKeyLen2 = publicKeyEncoded2.length;
                if(publicKeyLen2 > 255) {
                    throw new RuntimeException("public key is too large");
                }
                final byte[] userEncoded2 = user.getBytes("UTF-8");
                final int userLen2 = userEncoded2.length;
                if(userLen2 > 255) {
                    throw new RuntimeException("user is too large");
                }

                data = new byte[HEADER_LENGTH + publicKeyLen2 + 1 + BT_LENGTH + userLen2 + 1];
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

                break;

            case PAYMENT_REQUEST_RESPONSE:
                final int halfSignedTransactionLen = halfSignedTransaction.length;

                final byte[] publicKeyEncoded3 = publicKey.getEncoded();
                final int publicKeyLen3 = publicKeyEncoded3.length;
                if(publicKeyLen3 > 255) {
                    throw new RuntimeException("public is too large");
                }

                final byte[] userEncoded3 = user.getBytes("UTF-8");
                final int userLen3 = userEncoded3.length;
                if(userLen3 > 255) {
                    throw new RuntimeException("user is too large");
                }

                final int childPathLen1 = childPaths.length;
                data = new byte[HEADER_LENGTH + halfSignedTransactionLen + CHILD_PATH_LENGTH + (4 * childPathLen1) + 2 + userLen3 + 1 + publicKeyLen3 + 1 + BT_LENGTH];
                data[offset++] = header;

                //public key
                data[offset++] = (byte) publicKeyLen3;
                System.arraycopy(publicKeyEncoded3,0,data, offset, publicKeyLen3);
                offset += publicKeyLen3;

                //user
                data[offset++] = (byte) userLen3;
                System.arraycopy(userEncoded3, 0, data, offset, userLen3);
                offset += userLen3;
                //bluetooth
                System.arraycopy(btAddress,0,data, offset, BT_LENGTH);
                offset += BT_LENGTH;

                //transaction
                offset = encodeShort(halfSignedTransactionLen, data, offset);
                System.arraycopy(halfSignedTransaction,0,data, offset, halfSignedTransactionLen);
                offset += halfSignedTransactionLen;
                //child path
                offset = encodeInt(childPathLen1, data, offset);
                offset = arrayCopy(childPaths, data, offset);

                break;

            case PAYMENT_SEND_RESPONSE:

                final byte[] publicKeyEncoded4 = publicKey.getEncoded();
                final int publicKeyLen4 = publicKeyEncoded4.length;
                if(publicKeyLen4 > 255) {
                    throw new RuntimeException("user is too large");
                }

                final byte[] userEncoded4 = user.getBytes("UTF-8");
                final int userLen4 = userEncoded4.length;
                if(userLen4 > 255) {
                    throw new RuntimeException("user is too large");
                }

                data = new byte[HEADER_LENGTH + userLen4 + 1 + publicKeyLen4 + 1 + SENDTO_LENGTH+BT_LENGTH];
                data[offset++] = header;

                //public key
                data[offset++] = (byte) publicKeyLen4;
                System.arraycopy(publicKeyEncoded4,0,data, offset, publicKeyLen4);
                offset += publicKeyLen4;

                //user
                data[offset++] = (byte) userLen4;
                System.arraycopy(userEncoded4, 0, data, offset, userLen4);
                offset += userLen4;

                //bluetooth
                System.arraycopy(btAddress,0,data, offset, BT_LENGTH);
                offset += BT_LENGTH;

                //BTC address
                System.arraycopy(sendTo, 0, data, offset, SENDTO_LENGTH);
                offset += SENDTO_LENGTH;

                break;

            case FULL_TRANSACTION:
                final int fullSignedTransactionLen = fullySignedTransaction.length;
                final int childPathLen2 = childPaths.length;
                data = new byte[HEADER_LENGTH+fullSignedTransactionLen + 2 + BT_LENGTH + CHILD_PATH_LENGTH + (4 * childPathLen2)];
                data[offset++] = header;
                //transaction
                offset = encodeShort((short) fullSignedTransactionLen, data, offset);
                System.arraycopy(fullySignedTransaction,0,data, offset, fullSignedTransactionLen);
                offset += fullSignedTransactionLen;
                //child path
                offset = encodeInt(childPathLen2, data, offset);
                offset = arrayCopy(childPaths, data, offset);

                break;
            case PAYMENT_NOK:
            case SERVER_NOK:
            case PAYMENT_OK:
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



    public static PaymentProtocol fromBytesUnverified(final byte[] data) throws InvalidKeySpecException, UnsupportedEncodingException, NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        return fromBytes(data, null);
    }

    public static Type type(final byte[] data) {
        return Type.values()[data[0] & 0x7];
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
            case PAYMENT_REQUEST:
                //public key
                final int publicKeyLen1 = data[offset++] & 0xff;
                final byte[] publicKeyEncoded1 = new byte[publicKeyLen1];
                System.arraycopy(data,offset,publicKeyEncoded1,0,publicKeyLen1);
                offset += publicKeyLen1;
                PublicKey publicKeyMsg1 = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded1));
                if(publicKey != null && !publicKey.equals(publicKeyMsg1)) {
                    throw new RuntimeException("two different public keys?");
                }
                publicKey = publicKeyMsg1;
                paymentMessage.publicKey=publicKey;
                //user
                final int userLen1 = data[offset++] & 0xff;
                final byte[] userEncoded1 = new byte[userLen1];
                System.arraycopy(data,offset,userEncoded1,0,userLen1);
                offset += userLen1;
                final String user1 = new String(userEncoded1, "UTF-8");
                paymentMessage.user = user1;
                //bluetooth
                final byte[] btAddress1 = new byte[BT_LENGTH];
                System.arraycopy(data,offset,btAddress1, 0, BT_LENGTH);
                offset += BT_LENGTH;
                paymentMessage.btAddress = btAddress1;

                //amount
                final long satoshi = decodeLong(data, offset);
                offset += AMOUNT_LENGTH;
                paymentMessage.satoshis = satoshi;
                //address
                final byte[] sendTo1 = new byte[SENDTO_LENGTH];
                System.arraycopy(data, offset, sendTo1, 0, SENDTO_LENGTH);
                offset += SENDTO_LENGTH;
                paymentMessage.sendTo = sendTo1;

                break;

            case PAYMENT_SEND:
                //public key
                final int publicKeyLen2 = data[offset++] & 0xff;
                final byte[] publicKeyEncoded2 = new byte[publicKeyLen2];
                System.arraycopy(data,offset,publicKeyEncoded2,0,publicKeyLen2);
                offset += publicKeyLen2;
                PublicKey publicKeyMsg2 = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded2));
                if(publicKey != null && !publicKey.equals(publicKeyMsg2)) {
                    throw new RuntimeException("two different public keys?");
                }
                publicKey = publicKeyMsg2;
                paymentMessage.publicKey=publicKey;
                //user
                final int userLen2 = data[offset++] & 0xff;
                final byte[] userEncoded2 = new byte[userLen2];
                System.arraycopy(data,offset,userEncoded2,0,userLen2);
                offset += userLen2;
                final String user2 = new String(userEncoded2, "UTF-8");
                paymentMessage.user = user2;
                //bluetooth
                final byte[] btAddress2 = new byte[BT_LENGTH];
                System.arraycopy(data,offset,btAddress2, 0, BT_LENGTH);
                offset += BT_LENGTH;
                paymentMessage.btAddress = btAddress2;
                break;
            case PAYMENT_REQUEST_RESPONSE:
                //public key
                final int publicKeyLen3 = data[offset++] & 0xff;
                final byte[] publicKeyEncoded3 = new byte[publicKeyLen3];
                System.arraycopy(data,offset,publicKeyEncoded3,0,publicKeyLen3);
                offset += publicKeyLen3;
                PublicKey publicKeyMsg3 = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded3));
                if(publicKey != null && !publicKey.equals(publicKeyMsg3)) {
                    throw new RuntimeException("two different public keys?");
                }
                publicKey = publicKeyMsg3;
                paymentMessage.publicKey=publicKey;
                //user
                final int userLen3 = data[offset++] & 0xff;
                final byte[] userEncoded3 = new byte[userLen3];
                System.arraycopy(data,offset,userEncoded3,0,userLen3);
                offset += userLen3;
                final String user3 = new String(userEncoded3, "UTF-8");
                paymentMessage.user = user3;
                //bluetooth
                final byte[] btAddress3 = new byte[BT_LENGTH];
                System.arraycopy(data,offset,btAddress3, 0, BT_LENGTH);
                offset += BT_LENGTH;
                paymentMessage.btAddress = btAddress3;
                //transaction
                final short transactionLen1 = decodeShort(data,offset);
                offset += 2;
                final byte[] transactionEncoded1 = new byte[transactionLen1];
                System.arraycopy(data,offset,transactionEncoded1,0, transactionLen1);
                offset += transactionLen1;
                paymentMessage.halfSignedTransaction = transactionEncoded1;
                //childpath
                final int childPathLen1 = decodeInt(data, offset);
                offset += 4;
                final int[] childPaths1 = new int[childPathLen1];
                offset = arrayCopy(data, childPaths1, offset);
                paymentMessage.childPaths = childPaths1;

                break;

            case PAYMENT_SEND_RESPONSE:
                //public key
                final int publicKeyLen4 = data[offset++] & 0xff;
                final byte[] publicKeyEncoded4 = new byte[publicKeyLen4];
                System.arraycopy(data,offset,publicKeyEncoded4,0,publicKeyLen4);
                offset += publicKeyLen4;
                PublicKey publicKeyMsg4 = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded4));
                if(publicKey != null && !publicKey.equals(publicKeyMsg4)) {
                    throw new RuntimeException("two different public keys?");
                }
                publicKey = publicKeyMsg4;
                paymentMessage.publicKey=publicKey;
                //user
                final int userLen4 = data[offset++] & 0xff;
                final byte[] userEncoded4 = new byte[userLen4];
                System.arraycopy(data,offset,userEncoded4,0,userLen4);
                offset += userLen4;
                final String user4 = new String(userEncoded4, "UTF-8");
                paymentMessage.user = user4;
                //bluetooth
                final byte[] btAddress4 = new byte[BT_LENGTH];
                System.arraycopy(data,offset,btAddress4, 0, BT_LENGTH);
                offset += BT_LENGTH;
                paymentMessage.btAddress = btAddress4;
                //address
                final byte[] sendTo2 = new byte[SENDTO_LENGTH];
                System.arraycopy(data, offset, sendTo2, 0, SENDTO_LENGTH);
                offset += SENDTO_LENGTH;
                paymentMessage.sendTo = sendTo2;
                break;

            case FULL_TRANSACTION:
                final short transactionLen2 = decodeShort(data,offset);
                offset += 2;
                final byte[] transactionEncoded2 = new byte[transactionLen2];
                System.arraycopy(data,offset,transactionEncoded2,0, transactionLen2);
                offset += transactionLen2;
                paymentMessage.fullySignedTransaction = transactionEncoded2;
                //childpath
                final int childPathLen2 = decodeInt(data, offset);
                offset += 4;
                final int[] childPaths2 = new int[childPathLen2];
                offset = arrayCopy(data, childPaths2, offset);
                paymentMessage.childPaths = childPaths2;
                break;

            case SERVER_NOK:
            case PAYMENT_NOK:
            case PAYMENT_OK:
                break;
        }

        //check signature
        if(publicKey != null) {
            paymentMessage.signatureVerified = verify(publicKey, data, offset);
        }
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
                && Arrays.equals(sendTo, p.sendTo) && Arrays.equals(btAddress, p.btAddress) && satoshis == p.satoshis && Arrays.equals(childPaths, p.childPaths);
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

    public static PaymentProtocol paymentRequest(final PublicKey publicKey, final String user, final byte[] btAddress, final long satoshis, final byte[] sendTo) {
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
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_REQUEST);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress =btAddress;
        paymentMessage.satoshis = satoshis;
        paymentMessage.sendTo = sendTo;
        return paymentMessage;
    }

    public static PaymentProtocol paymentRequestResponse(final PublicKey publicKey, final String user, final byte[] btAddress, final byte[] halfSignedTransaction, int[] childPaths) {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_REQUEST_RESPONSE);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress = btAddress;
        paymentMessage.halfSignedTransaction = halfSignedTransaction;
        paymentMessage.childPaths = childPaths;
        return paymentMessage;
    }

    public static PaymentProtocol paymentSend(final PublicKey publicKey, final String user, final byte[] btAddress) {
        if(publicKey == null) {
            throw new RuntimeException("public key cannot be null");
        }
        if(user == null) {
            throw new RuntimeException("user cannot be null");
        }
        if(btAddress.length != BT_LENGTH) {
            throw new RuntimeException("wrong length of the bluetooth address");
        }
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_SEND);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress = btAddress;
        return paymentMessage;
    }

    public static PaymentProtocol paymentSendResponse(final PublicKey publicKey, final String user, final byte[] btAddress, final byte[] sendTo) {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_SEND_RESPONSE);
        paymentMessage.publicKey = publicKey;
        paymentMessage.user = user;
        paymentMessage.btAddress = btAddress;
        paymentMessage.sendTo = sendTo;
        return paymentMessage;
    }

    public static PaymentProtocol fullTransaction(final byte[] fullySignedTransaction, int[] childPaths) {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.FULL_TRANSACTION);
        paymentMessage.fullySignedTransaction = fullySignedTransaction;
        paymentMessage.childPaths = childPaths;
        return paymentMessage;
    }

    public static PaymentProtocol paymentNok() {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_NOK);
        return paymentMessage;
    }

    public static PaymentProtocol serverNok() {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.SERVER_NOK);
        return paymentMessage;
    }

    public static PaymentProtocol paymentOk() {
        final PaymentProtocol paymentMessage = new PaymentProtocol(Type.PAYMENT_OK);
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
                | (((long) data[offset + 6] & 0xff) << 8)  | (((long) data[offset + 7] & 0xff)));
    }

    private static int encodeInt(final int value, final byte[] data, final int offset) {
        data[offset  ] = (byte) (value >> 24);
        data[offset+1] = (byte) (value >> 16);
        data[offset+2] = (byte) (value >> 8);
        data[offset+3] = (byte) value;
        return offset + 4;
    }

    private static int decodeInt(final byte[] data, final int offset) {
        return   ( ((data[offset    ] & 0xff) << 24) | (( data[offset + 1] & 0xff) << 16)
                 | ((data[offset + 2] & 0xff) << 8)  | (( data[offset + 3] & 0xff)));
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

    //encode
    private int arrayCopy(final int[] childPaths, final byte[] data, int offset) {
        final int len = childPaths.length;
        for (int i=0;i<len;i++) {
            offset = encodeInt(childPaths[i], data,offset);
        }
        return offset;
    }

    //decode
    private static int arrayCopy(final byte[] data, final int[] childPaths, int offset) {
        final int len = childPaths.length;
        for (int i=0;i<len;i++) {
            childPaths[i] = decodeInt(data, offset);
            offset += 4;
        }
        return offset;
    }

    public static KeyPair generateKeys()
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        final ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("brainpoolp256r1");
        final KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "SC");
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
    }

    public Type type() {
        return type;
    }
}
