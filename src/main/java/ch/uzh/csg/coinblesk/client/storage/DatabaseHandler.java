package ch.uzh.csg.coinblesk.client.storage;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

import java.security.PublicKey;
import java.util.List;

import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.storage.serializer.PublicKeySerializer;

/**
 * Database abstraction for storing and retrieving transaction meta data
 */
public class DatabaseHandler {

    private PublicKeySerializer publicKeySerializer = new PublicKeySerializer();

    public void saveTransactionMetaData(TransactionMetaData txMetaData) {
        txMetaData.save();
    }

    public TransactionMetaData getTransactionMetaData(String txId) {
        return new Select().from(TransactionMetaData.class).where("TxId = ?", txId).executeSingle();
    }

    public void saveAddressBookEntry(AddressBookEntry entry) {
        entry.save();
    }

    public AddressBookEntry getAddressBookEntry(PublicKey pubKey) {
        return new Select().from(AddressBookEntry.class).where("PublicKey = ?", publicKeySerializer.serialize(pubKey)).executeSingle();
    }

    public void removeAllUntrustedAddressBookEntries() {
        new Delete().from(AddressBookEntry.class).where("Trusted = ?", false).execute();
    }

    public List<AddressBookEntry> getAddressBook() {
        return new Select().from(AddressBookEntry.class).execute();
    }

    public void deleteAddressBookEntry(PublicKey pubKey) {
        new Delete().from(AddressBookEntry.class).where("PublicKey = ?", pubKey).execute();
    }
}
