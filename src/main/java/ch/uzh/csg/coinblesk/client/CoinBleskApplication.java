package ch.uzh.csg.coinblesk.client;

import android.app.Application;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.persistence.InternalStorageHandler;
import ch.uzh.csg.coinblesk.client.persistence.WrongPasswordException;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.util.LoggingConfig;

/**
 * Entry point for the CoinBlesk app. Everything that needs to be set up before the app starts should be done here.
 */
public class CoinBleskApplication extends Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoinBleskApplication.class);

    private InternalStorageHandler mStorageHandler;
    private RequestFactory requestFactory = new RequestFactory();

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingConfig.configure();
        LOGGER.info("CoinBlesk is starting...");
    }

    public boolean initStorageHandler(Context context, String username, String password) throws WrongPasswordException {
        try {
            mStorageHandler = new InternalStorageHandler(context, username, password);
            return true;
        } catch (Exception e) {
            if (e instanceof WrongPasswordException) {
                throw new WrongPasswordException(e.getMessage());
            } else {
                throw new RuntimeException("Failed to create internal storage handler", e);
            }
        }
    }

    public InternalStorageHandler getStorageHandler() {
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
