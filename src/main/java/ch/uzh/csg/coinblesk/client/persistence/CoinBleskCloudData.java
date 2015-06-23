package ch.uzh.csg.coinblesk.client.persistence;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

public class CoinBleskCloudData  extends BackupAgentHelper implements PersistentData {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoinBleskCloudData.class);
    
    // The names of the SharedPreferences groups that the application maintains.  These
    // are the same strings that are passed to getSharedPreferences(String, int).
    static final String COINBLSEK_DATA = "CoinBleskSharedData";

    // An arbitrary string used within the BackupAgentHelper implementation to
    // identify the SharedPreferenceBackupHelper's data.
    static final String COINBLESK_PREFS_BACKUP_KEY = "CoinBleskPrefs";

    private static final String SERVER_IP = "ip";
    private static final String BITCOIN_NET = "bitcoin-net";
    private static final String SERVER_WATCHING_KEY = "server-watching-key";
    private static final String PAYMENT_REQUESTS = "payment-requests";
    private static final String USER_ACCOUNT = "user-account";
    private static final String SERVER_PUB_KEY = "server-public-key";
    private static final String USER_KEYPAIR = "user-keypair";
    private static final String CONTACTS = "contacts";
    private static final String TRUSTED_CONTACTS = "trusted-contacts";

    private String password;
    private String username;
    private InternalStorageEncrypter encrypter;
    
    private final SharedPreferences prefs;
    private Gson gson;

    public CoinBleskCloudData(String username, String password, Context context) {
        this.encrypter = new InternalStorageEncrypter();
        this.password = password;
        this.username = username;
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
    public void setServerIp(String ip) throws Exception {
        prefs.edit().putString(SERVER_IP, ip).apply();
    }

    private Type getPersistedPaymentRequestType() {
        return new TypeToken<Set<PersistedPaymentRequest>>(){}.getType();
    }

    @Override
    public void deletePersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception {
        Set<PersistedPaymentRequest> paymentRequestsList = getPersistedPaymentRequests();
        paymentRequestsList.remove(persistedRequest);
        savePersistedPaymentRequests(paymentRequestsList);
    }

    private void savePersistedPaymentRequests(Set<PersistedPaymentRequest> paymentRequests) {
        String json = gson.toJson(paymentRequests, getPersistedPaymentRequestType());
        prefs.edit().putString(PAYMENT_REQUESTS, json).apply();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<PersistedPaymentRequest> getPersistedPaymentRequests() throws Exception {
        String json = prefs.getString(PAYMENT_REQUESTS, "[]");
        return gson.fromJson(json, getPersistedPaymentRequestType());
    }

    @Override
    public void addPersistedPaymentRequest(PersistedPaymentRequest persistedRequest) throws Exception {
        Set<PersistedPaymentRequest> paymentRequests = getPersistedPaymentRequests();
        paymentRequests.add(persistedRequest);
        savePersistedPaymentRequests(paymentRequests);
    }

    @Override
    public void removeTrustedAddressBookEntry(String username) throws Exception {
        Set<String> addresses = getAddresses(true);
        addresses.remove(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public void removeAddressBookContact(String username) throws Exception {
        Set<String> addresses = getAddresses(false);
        addresses.remove(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public void addTrustedAddressBookContact(String username) throws Exception {
        Set<String> addresses = getAddresses(true);
        addresses.add(username);
        saveAddressBook(addresses, true);
    }

    @Override
    public Set<String> getTrusteAddressBookdContacts() throws Exception {
        return getAddresses(true);
    }

    @Override
    public void addAddressBookContact(String username) throws Exception {
        Set<String> contactSet = getAddresses(false);
        contactSet.add(username);
        saveAddressBook(contactSet, false);
    }

    private void saveAddressBook(Collection<String> contacts, boolean trustet) {
        String[] contactArray = contacts.toArray(new String[contacts.size()]);
        String json = gson.toJson(contactArray);
        prefs.edit().putString(trustet ? TRUSTED_CONTACTS : CONTACTS, json).apply();
    }

    private Set<String> getAddresses(boolean trustet) {
        String json = prefs.getString(trustet ? TRUSTED_CONTACTS : CONTACTS, "[]");
        String[] contacts = gson.fromJson(json, String[].class);
        return Sets.newHashSet(contacts);
    }

    @Override
    public Set<String> getAddressBookContacts() throws Exception {
        return getAddresses(false);
    }

    @Override
    public CustomKeyPair getUserKeyPair() throws Exception {
        String json = prefs.getString(USER_KEYPAIR, null);
        Preconditions.checkNotNull(json);
        return gson.fromJson(json, CustomKeyPair.class);
    }

    @Override
    public void setUserKeyPair(CustomKeyPair customKeyPair) throws Exception {
        String json = gson.toJson(customKeyPair);
        prefs.edit().putString(USER_KEYPAIR, json).apply();
    }

    @Override
    public UserAccountObject getUserAccount() {
        String json = prefs.getString(USER_ACCOUNT, null);

        if(json == null) {
            return null;
        }

        // decrypt encrypted json...
        String decryptedJson;
        try{
            decryptedJson = encrypter.decrypt(json, password);
        } catch (WrongPasswordException e) {
            LOGGER.error("Wrong password!", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Loading of user data failed!", e);
            return null;
        }
        return gson.fromJson(decryptedJson, UserAccountObject.class);
    }

    @Override
    public void setUserAccount(UserAccountObject userAccount) throws Exception {
        String json = gson.toJson(userAccount);

        // encrypt user account data and save it...
        String encryptedJson = encrypter.encrypt(json, password);
        prefs.edit().putString(USER_ACCOUNT, encryptedJson).apply();
    }

    @Override
    public CustomPublicKey getServerPublicKey() throws Exception {
        String json = prefs.getString(SERVER_PUB_KEY, null);
        Preconditions.checkNotNull(json);
        return gson.fromJson(json, CustomPublicKey.class);
    }

    @Override
    public void setServerPublicKey(CustomPublicKey publicKey) throws Exception {
        String json = gson.toJson(publicKey);
        prefs.edit().putString(SERVER_PUB_KEY, json).apply();
    }

    @Override
    public String getServerIp() throws Exception {
        String serverIp = prefs.getString(SERVER_IP, null);
        Preconditions.checkNotNull(serverIp);
        return serverIp;
    }

    @Override
    public String getServerWatchingKey() {
        return prefs.getString(SERVER_WATCHING_KEY, null);
    }

    @Override
    public void setServerWatchingKey(String serverWatchingKey) {
        prefs.edit().putString(SERVER_WATCHING_KEY, serverWatchingKey).apply();
    }

    @Override
    public String getBitcoinNet() {
        String bitcoinNet = prefs.getString(BITCOIN_NET, null);

        return prefs.getString(BITCOIN_NET, null);
    }

    @Override
    public void setBitcoinNet(String bitcoinNet) {
        prefs.edit().putString(BITCOIN_NET, bitcoinNet).commit();
        String bla = prefs.getString(BITCOIN_NET, null);
        System.out.println(bla);

    }

    @Override
    public void clear() {
        prefs.edit().clear().commit();
    }


}
