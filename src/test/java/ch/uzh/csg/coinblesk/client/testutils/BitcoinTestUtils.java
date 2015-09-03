package ch.uzh.csg.coinblesk.client.testutils;

import com.google.common.base.Joiner;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;

import java.security.SecureRandom;
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

    public static String getRandomMnemonic() {
        DeterministicSeed seed = new DeterministicSeed(new SecureRandom(), 128, "", 0);
        return Joiner.on(" ").join(seed.getMnemonicCode());
    }

    public static String getRandomWatchingKey(NetworkParameters params) {
        DeterministicKeyChain kc = new DeterministicKeyChain(new SecureRandom(), 128);
        return kc.getWatchingKey().serializePubB58(params);
    }
}
