package ch.uzh.csg.coinblesk.client.wallet;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;

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

    public void testSatoshiToBigDecimal() {
        long satoshis = 100000000;
        BigDecimal btc = BitcoinUtils.satoshiToBigDecimal(satoshis);
        System.out.println(Coin.valueOf(satoshis).toPlainString());
        System.out.println("Bitcoin amount: " + btc);
        Assert.assertEquals(0, BigDecimal.ONE.compareTo(btc));
    }

    public void testCoinToBigDecimal() {
        BigDecimal fiftyBtc = BitcoinUtils.coinToBigDecimal(Coin.FIFTY_COINS);
        Assert.assertEquals(0, fiftyBtc.compareTo(new BigDecimal("50")));
    }

}