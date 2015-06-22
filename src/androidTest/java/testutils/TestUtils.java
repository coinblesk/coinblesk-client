package testutils;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;

/**
 * Created by rvoellmy on 6/21/15.
 */
public class TestUtils {

    public static String getServerWatchingKey(NetworkParameters params) throws Exception {
        // create server watching key
        String mnemonic = "actor critic filter assist load now age strike right certain column paddle";
        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        kcg.createAndActivateNewHDChain();
        System.out.println(kcg.getActiveKeyChain().getMnemonicCode());
        DeterministicKey watchingKey = kcg.getActiveKeyChain().getWatchingKey();
        return watchingKey.serializePubB58(params);
    }
}
