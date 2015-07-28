package ch.uzh.csg.coinblesk.client.util;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.net.NioClientManager;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;

public class LoggingConfig {

    public static void configure() {
        BasicLogcatConfigurator.configureDefaultContext();

        // root level logger
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);

        ((Logger) LoggerFactory.getLogger(BitcoinSerializer.class)).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger(Peer.class)).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger(PeerGroup.class)).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger(NioClientManager.class)).setLevel(Level.WARN);
    }

}
