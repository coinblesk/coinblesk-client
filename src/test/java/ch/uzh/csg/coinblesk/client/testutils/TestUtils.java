package ch.uzh.csg.coinblesk.client.testutils;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;

/**
 * Created by rvoellmy on 6/7/15.
 */
public class TestUtils {

    public static void configureLogger(Level level) {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        //configure the appender
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(level);
        console.activateOptions();
        //add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);
    }

    public static void configureLogger() {
        configureLogger(Level.DEBUG);
    }

    public static String getServerWatchingKey(NetworkParameters params) {
        // create server watching key
        String mnemonic = "actor critic filter assist load now age strike right certain column paddle"; // don't change! Ever!
        DeterministicSeed seed = null;
        try {
            seed = new DeterministicSeed(mnemonic, null, "", 0);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        DeterministicKey watchingKey = kcg.getActiveKeyChain().getWatchingKey();
        return watchingKey.serializePubB58(params);
    }

}