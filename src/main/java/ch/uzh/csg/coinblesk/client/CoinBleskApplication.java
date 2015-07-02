package ch.uzh.csg.coinblesk.client;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.persistence.InternalStorage;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.util.LoggingConfig;

/**
 * Entry point for the CoinBlesk app. Everything that needs to be set up before the app starts should be done here.
 */
public class CoinBleskApplication extends Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoinBleskApplication.class);

    private InternalStorage mStorageHandler;
    private RequestFactory requestFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingConfig.configure();

        mStorageHandler = new InternalStorage(this);
        requestFactory = new DefaultRequestFactory();

        LOGGER.info("CoinBlesk is starting...");
    }

    public InternalStorage getStorageHandler() {
        return mStorageHandler;
    }

    /**
     * Set the request factory. This method is only used for testing to set a custom request factory that mocks server responses.
     * @param requestFactory
     */
    public void setRequestFactory(RequestFactory requestFactory){
        this.requestFactory = requestFactory;
    }


    public RequestFactory getRequestFactory() {
        return this.requestFactory;
    }



}
