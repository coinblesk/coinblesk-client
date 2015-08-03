package ch.uzh.csg.coinblesk.client.storage.serializer;

import android.util.Base64;

import com.activeandroid.serializer.TypeSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by rvoellmy on 8/1/15.
 */
public class PublicKeySerializer extends TypeSerializer {

    private final static Logger LOGGER = LoggerFactory.getLogger(PublicKeySerializer.class);

    private final static KeyFactory keyFactory;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<?> getDeserializedType() {
        return PublicKey.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return String.class;
    }

    @Override
    public String serialize(Object o) {
        if(o == null) {
            return null;
        }

        PublicKey pubKey = (PublicKey) o;
        return Base64.encodeToString(pubKey.getEncoded(), Base64.NO_WRAP);
    }

    @Override
    public PublicKey deserialize(Object o) {
        if(o == null) {
            return null;
        }
        String pubKeyBase64 = (String) o;
        byte[] pubKeySerialized = Base64.decode(pubKeyBase64, Base64.NO_WRAP);

        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(pubKeySerialized));
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Failed to deserialize public key");
        }
        return null;
    }
}
