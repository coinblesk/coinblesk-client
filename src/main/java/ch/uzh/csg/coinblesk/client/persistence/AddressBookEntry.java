package ch.uzh.csg.coinblesk.client.persistence;

import java.security.PublicKey;

/**
 * Holds an entry of the address book.
 */
public class AddressBookEntry {

    private String bitcoinAddress;
    private String name;
    private PublicKey publicKey;
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
