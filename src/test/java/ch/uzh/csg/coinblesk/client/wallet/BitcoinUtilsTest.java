package ch.uzh.csg.coinblesk.client.wallet;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.bitcoinj.core.Address;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;

/**
 * Created by rvoellmy on 7/26/15.
 */
public class BitcoinUtilsTest extends TestCase {

    private final static BitcoinNet BITCOIN_NET = BitcoinNet.TESTNET;

    public void testAddressToBytes() {
        String address = "2N27fai1gsgxZ3FvcSp9yiKCMoxRDBSBGfQ";
        byte[] addressBytes = BitcoinUtils.addressToBytes(address, BITCOIN_NET);
        Address restoredAddress = Address.fromP2SHHash(BitcoinUtils.getNetworkParameters(BITCOIN_NET), addressBytes);
        Assert.assertEquals(address, restoredAddress.toString());
    }

    public void testIsP2SHAddress() {
        String p2shAddress = "2N27fai1gsgxZ3FvcSp9yiKCMoxRDBSBGfQ";
        String normalAddress = "mhS98F5ZkRPVafrGSUWnRMjcJwAq5aexEy";

        Assert.assertTrue(BitcoinUtils.isP2SHAddress(p2shAddress, BITCOIN_NET));
        Assert.assertFalse(BitcoinUtils.isP2SHAddress(normalAddress, BITCOIN_NET));
        Assert.assertFalse(BitcoinUtils.isP2SHAddress("some invalid address", BITCOIN_NET));
    }

}