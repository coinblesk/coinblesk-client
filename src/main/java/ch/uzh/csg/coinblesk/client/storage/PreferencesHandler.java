package ch.uzh.csg.coinblesk.client.storage;

import java.security.KeyPair;

/**
 * Interface for persisting coinblesk specific data.
 */
public interface PreferencesHandler {

    String getServerWatchingKey();
    void setServerWatchingKey(String serverWatchingKey);

    String getBitcoinNet();
    void setBitcoinNet(String bitcoinNet);

    void setRefundTx(String base64EncodedTx);
    String getRefundTx();

    void setRefundTxValidBlock(long validBlockNr);
    long getRefundTxValidBlock();

    void setUsername(String username);
    String getUsername();

    void setKeyPair(KeyPair keyPair);
    KeyPair getKeyPair();

    /**
     * Deletes all data
     */
    void clear();
}