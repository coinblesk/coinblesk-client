package ch.uzh.csg.coinblesk.client.storage;

import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ch.uzh.csg.coinblesk.client.comm.PaymentProtocol;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * Created by rvoellmy on 8/1/15.
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "javax.crypto.*"})
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class DatabaseHandlerTest {

    private DatabaseHandler db = new DatabaseHandler();

    @Test
    public void testSaveAndLoadTransactionMetaData() throws Exception {

        String txId = "txId";

        TransactionMetaData metaData = new TransactionMetaData();
        metaData.setTxId(txId);
        metaData.setSender("sender");
        metaData.setReceiver("receiver");
        metaData.setType(TransactionMetaData.TransactionType.COINBLESK_PAY_IN);
        metaData.setTimestamp(new Date());

        db.saveTransactionMetaData(metaData);

        TransactionMetaData restoredMetaData = db.getTransactionMetaData(txId);

        Assert.assertNotNull(restoredMetaData);
        Assert.assertEquals(metaData.getType(), restoredMetaData.getType());
        Assert.assertEquals(metaData.getTimestamp(), restoredMetaData.getTimestamp());
        Assert.assertEquals(metaData.getSender(), restoredMetaData.getSender());

    }


    @Test
    public void testSaveAndLoadAddressBookEntry() throws Exception {

        KeyPair keyPair = PaymentProtocol.generateKeys();

        AddressBookEntry entry = createEntry(true, keyPair.getPublic());
        db.saveAddressBookEntry(entry);

        AddressBookEntry restoredEntry = db.getAddressBookEntry(keyPair.getPublic());

        Assert.assertNotNull(restoredEntry);
        Assert.assertEquals(entry.getPublicKey(), restoredEntry.getPublicKey());
        Assert.assertEquals(entry.getBitcoinAddress(), restoredEntry.getBitcoinAddress());
        Assert.assertEquals(entry.getName(), restoredEntry.getName());
        Assert.assertEquals(entry.isTrusted(), restoredEntry.isTrusted());

    }

    @Test
    public void testGetAddressBook() throws Exception {

        int nrOfEntries = 5;

        for (int i = 0; i < nrOfEntries; i++) {
            db.saveAddressBookEntry(createEntry(true));
        }

        Set<PublicKey> pubKeys = new HashSet<>();
        for (AddressBookEntry entry : db.getAddressBook()) {
            pubKeys.add(entry.getPublicKey());
        }

        Assert.assertEquals(nrOfEntries, pubKeys.size());

    }

    @Test
    public void testDeleteUnfavoredEntries() throws Exception {

        int nrOfUnfavoredEntries = 5;
        int nrOfFavoredEntries = 3;

        for (int i = 0; i < nrOfUnfavoredEntries; i++) {
            db.saveAddressBookEntry(createEntry(false));
        }

        // add a favored entries
        for (int i = 0; i < nrOfFavoredEntries; i++) {
            db.saveAddressBookEntry(createEntry(true));
        }

        Assert.assertEquals(nrOfFavoredEntries + nrOfUnfavoredEntries, db.getAddressBook().size());
        db.removeAllUntrustedAddressBookEntries();
        Assert.assertEquals(nrOfFavoredEntries, db.getAddressBook().size());


    }

    private AddressBookEntry createEntry(boolean trusted) throws Exception {
        return createEntry(trusted, null);
    }

    private AddressBookEntry createEntry(boolean trusted, @Nullable PublicKey pubKey) throws Exception {
        AddressBookEntry entry = new AddressBookEntry();
        entry.setPublicKey(pubKey != null ? pubKey : PaymentProtocol.generateKeys().getPublic());
        entry.setBitcoinAddress("bitcoin address");
        entry.setName("name");
        entry.setTrusted(trusted);
        return entry;
    }
}