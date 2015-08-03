package ch.uzh.csg.coinblesk.client.storage.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.security.PublicKey;

/**
 * Holds an entry of the address book.
 */
@Table(name = "AddressBookEntry")
public class AddressBookEntry extends Model {

    // do not delete! this default constructor is required
    public AddressBookEntry() {
        super();

        // default values
        this.trusted = false;
    }

    /**
     * Creates a new AddressBookEntry for a given public key
     * @param publicKey the public key of the address book entry
     */
    public AddressBookEntry(PublicKey publicKey) {
        this();
        this.publicKey = publicKey;
    }

    @Column(name = "BitcoinAddress")
    private String bitcoinAddress;

    @Column(name = "Name", index = true)
    private String name;

    @Column(name = "PublicKey", index = true)
    private PublicKey publicKey;

    @Column(name = "Trusted")
    private boolean trusted;

    public String getBitcoinAddress() {
        return bitcoinAddress;
    }

    public void setBitcoinAddress(String bitcoinAddress) {
        this.bitcoinAddress = bitcoinAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }
}
