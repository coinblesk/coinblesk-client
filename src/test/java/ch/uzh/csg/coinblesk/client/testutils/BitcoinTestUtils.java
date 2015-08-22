package ch.uzh.csg.coinblesk.client.testutils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;

import java.util.Random;

/**
 * Created by rvoellmy on 8/20/15.
 */
public class BitcoinTestUtils {

    private static final Random RND = new Random(42L);

    public static Address getRandomAddress(NetworkParameters params) {
        byte[] addressBytes = new byte[20];
        RND.nextBytes(addressBytes);
        return new Address(params, addressBytes);
    }
}
