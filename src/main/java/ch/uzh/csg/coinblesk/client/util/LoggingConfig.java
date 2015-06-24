package ch.uzh.csg.coinblesk.client.util;

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class LoggingConfig {

    public static void configure() {
        final LogConfigurator logConfigurator = new LogConfigurator();

        logConfigurator.setUseLogCatAppender(true);
        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "coinblesk.log");
        logConfigurator.setRootLevel(Level.DEBUG);

        // Set log level of a specific logger
        logConfigurator.setLevel("org.bitcoinj", Level.INFO);
        
        logConfigurator.configure();
    }

}