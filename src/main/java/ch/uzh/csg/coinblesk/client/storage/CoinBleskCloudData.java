package ch.uzh.csg.coinblesk.client.storage;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

public class CoinBleskCloudData  extends BackupAgentHelper implements PreferencesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoinBleskCloudData.class);

    // The names of the SharedPreferences groups that the application maintains.  These
    // are the same strings that are passed to getSharedPreferences(String, int).
    public static final String COINBLSEK_DATA = "CoinBleskSharedData";

    // An arbitrary string used within the BackupAgentHelper implementation to
    // identify the SharedPreferenceBackupHelper's data.
    public static final String COINBLESK_PREFS_BACKUP_KEY = "CoinBleskPrefs";

    public static final String BITCOIN_NET = "bitcoin-net";
    public static final String SERVER_WATCHING_KEY = "server-watching-key";
    public static final String REFUND_TX = "refund-tx";
    public static final String REFUND_TX_VALID_BLOCK = "refund-tx-valid-block";
    public static final String USERNAME = "username";
    public static final String PRIV_KEY = "private-key";
    public static final String PUB_KEY = "public-key";

    private SharedPreferences prefs;

    private final static KeyFactory keyFactory;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        try {
            keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public CoinBleskCloudData(Context context) {
        this.prefs = new CloudBackedSharedPreferences(COINBLESK_PREFS_BACKUP_KEY, context);

    }

    public CoinBleskCloudData() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.prefs = new CloudBackedSharedPreferences(COINBLESK_PREFS_BACKUP_KEY, getApplicationContext());

        // allocate a helper and install it
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, COINBLSEK_DATA);
        addHelper(COINBLESK_PREFS_BACKUP_KEY, helper);

    }

    @Override
    public String getServerWatchingKey() {
        return prefs.getString(SERVER_WATCHING_KEY, null);
    }

    @Override
    public void setServerWatchingKey(String serverWatchingKey) {
        prefs.edit().putString(SERVER_WATCHING_KEY, serverWatchingKey).commit();
    }

    @Override
    public String getBitcoinNet() {
        return prefs.getString(BITCOIN_NET, null);
    }

    @Override
    public void setBitcoinNet(String bitcoinNet) {
        prefs.edit().putString(BITCOIN_NET, bitcoinNet).commit();
    }

    @Override
    public void setRefundTx(String base64EncodedTx) {
        prefs.edit().putString(REFUND_TX, base64EncodedTx).commit();
    }

    @Override
    public String getRefundTx() {
        return prefs.getString(REFUND_TX, null);
    }

    @Override
    public void setRefundTxValidBlock(long validBlockNr) {
        prefs.edit().putLong(REFUND_TX_VALID_BLOCK, validBlockNr).commit();
    }

    @Override
    public long getRefundTxValidBlock() {
        return prefs.getLong(REFUND_TX_VALID_BLOCK, RefundTx.NO_REFUND_TX_VALID_BLOCK);
    }

    @Override
    public void setUsername(String username) {
        prefs.edit().putString(USERNAME, username).commit();
    }

    @Override
    public String getUsername() {
        return prefs.getString(USERNAME, null);
    }

    @Override
    public void setKeyPair(KeyPair keyPair) {
        byte[] privKey = keyPair.getPrivate().getEncoded();
        byte[] pubKey = keyPair.getPublic().getEncoded();

        prefs.edit().putString(PUB_KEY, Base64.encodeToString(pubKey, Base64.NO_WRAP)).commit();
        prefs.edit().putString(PRIV_KEY, Base64.encodeToString(privKey, Base64.NO_WRAP)).commit();
    }

    @Override
    public KeyPair getKeyPair() {

        String privKeyBase64 = prefs.getString(PRIV_KEY, null);
        String pubKeyBase64 = prefs.getString(PUB_KEY, null);

        if(privKeyBase64 == null || pubKeyBase64 == null) { return null; }

        try {
            PrivateKey privKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privKeyBase64, Base64.NO_WRAP)));
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(pubKeyBase64, Base64.NO_WRAP)));
            return new KeyPair(pubKey, privKey);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Failed to deserialize key pair...");
            return null;
        }
    }

    @Override
    public void clear() {
        prefs.edit().clear().commit();
    }


}
