package ch.uzh.csg.coinblesk.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;

import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.LoggingConfig;

/**
 * Entry point for the CoinBlesk app. Everything that needs to be set up before the app starts should be done here.
 */
public class CoinBleskApplication extends Application {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(CoinBleskApplication.class);
    
    @Override
    public void onCreate() {
        LoggingConfig.configure();
        
        LOGGER.info("CoinBlesk ist starting....");
        
        super.onCreate();

    }
}
