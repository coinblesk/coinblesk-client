package ch.uzh.csg.coinblesk.client.persistence;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import ch.uzh.csg.coinblesk.client.wallet.RefundTx;

public class CoinBleskCloudData  extends BackupAgentHelper implements PersistentStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoinBleskCloudData.class);
    
    // The names of the SharedPreferences groups that the application maintains.  These
    // are the same strings that are passed to getSharedPreferences(String, int).
    public final String COINBLSEK_DATA = "CoinBleskSharedData";

    // An arbitrary string used within the BackupAgentHelper implementation to
    // identify the SharedPreferenceBackupHelper's data.
    public static final String COINBLESK_PREFS_BACKUP_KEY = "CoinBleskPrefs";

    public static final String BITCOIN_NET = "bitcoin-net";
    public static final String SERVER_WATCHING_KEY = "server-watching-key";
    public static final String CONTACTS = "contacts";
    public static final String TRUSTED_CONTACTS = "trusted-contacts";
    public static final String REFUND_TX = "refund-tx";
    public static final String REFUND_TX_VALID_BLOCK = "refund-tx-valid-block";

    private final SharedPreferences prefs;
    private Gson gson;

    public CoinBleskCloudData(Context context) {
        this.prefs = new CloudBackedSharedPreferences(COINBLESK_PREFS_BACKUP_KEY, context);
        this.gson = new GsonBuilder().create();
    }


    public void onCreate() {
        // allocate a helper and install it
        SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, COINBLSEK_DATA);
        addHelper(COINBLESK_PREFS_BACKUP_KEY, helper);
    }
    
    @Override
    public void removeTrustedAddressBookEntry(String username) {
        Set<String> addresses = getAddresses(true);
        addresses.remove(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public void removeAddressBookContact(String username) {
        Set<String> addresses = getAddresses(false);
        addresses.remove(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public void addTrustedAddressBookContact(String username) {
        Set<String> addresses = getAddresses(true);
        addresses.add(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public Set<String> getTrusteAddressBookdContacts() {
        return getAddresses(true);
    }

    @Override
    public void addAddressBookContact(String username) {
        Set<String> contactSet = getAddresses(false);
        contactSet.add(username);
        saveAddressBook(contactSet, false);
    }

    private void saveAddressBook(Collection<String> contacts, boolean trustet) {
        String[] contactArray = contacts.toArray(new String[contacts.size()]);
        String json = gson.toJson(contactArray);
        prefs.edit().putString(trustet ? TRUSTED_CONTACTS : CONTACTS, json).commit();
    }

    private Set<String> getAddresses(boolean trustet) {
        String json = prefs.getString(trustet ? TRUSTED_CONTACTS : CONTACTS, "[]");
        String[] contacts = gson.fromJson(json, String[].class);
        return Sets.newHashSet(contacts);
    }

    @Override
    public Set<String> getAddressBookContacts() {
        return getAddresses(false);
    }

    @Override
    public String getServerWatchingKey() {
        return prefs.getString(SERVER_WATCHING_KEY, null);
    }

    @Override
    public void setServerWatchingKey(String serverWatchingKey) {
        LOGGER.debug("Saved server watching key: {}", serverWatchingKey);
        prefs.edit().putString(SERVER_WATCHING_KEY, serverWatchingKey).commit();
    }

    @Override
    public String getBitcoinNet() {
        return prefs.getString(BITCOIN_NET, null);
    }

    @Override
    public void setBitcoinNet(String bitcoinNet) {
        LOGGER.debug("Saved server bitcoin network: {}", bitcoinNet);
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
    public void clear() {
        prefs.edit().clear().commit();
    }


}
