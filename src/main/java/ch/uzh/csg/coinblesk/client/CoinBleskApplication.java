package ch.uzh.csg.coinblesk.client;

import android.provider.Settings;

import com.activeandroid.ActiveAndroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.exchange.ExchangeManager;
import ch.uzh.csg.coinblesk.client.payment.NfcPaymentListener;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.storage.PersistentStorageHandler;
import ch.uzh.csg.coinblesk.client.storage.StorageHandler;
import ch.uzh.csg.coinblesk.client.util.LoggingConfig;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.nfclib.NfcSetup;

/**
 * Entry point for the CoinBlesk app. Everything that needs to be set up before the app starts should be done here.
 */
public class CoinBleskApplication extends com.activeandroid.app.Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoinBleskApplication.class);

    private StorageHandler mStorageHandler;
    private RequestFactory requestFactory;
    private ExchangeManager merchantModeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        LoggingConfig.configure();

        ActiveAndroid.initialize(this);

        mStorageHandler = new PersistentStorageHandler(this);
        requestFactory = new DefaultRequestFactory();
        merchantModeManager = new ExchangeManager(this);

        LOGGER.info("CoinBlesk is starting...");

        // check if this is a new installation
        if (!getStorageHandler().hasUserData()) {
            //its new, so create a wallet and set a default username
            final String androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            //TODO: make this setting available in the menu
            getStorageHandler().setUsername("Coinblesk User " + androidId);
            requestSetup(new RequestCompleteListener<SetupRequestObject>() {
                @Override
                public void onTaskComplete(SetupRequestObject response) {
                    if (response.isSuccessful()) {
                        LOGGER.debug("set bitcoinnet and serverwatching key");
                        getStorageHandler().setBitcoinNetAndServerWatchingKey(response.getBitcoinNet(), response.getServerWatchingKey());
                        getStorageHandler().setStorageFailed(false);
                    } else {
                        getStorageHandler().setStorageFailed(true);
                    }
                    //now we continue in onServiceConnected, where we dismiss the loading dialog
                }
            });
        } else {
            getStorageHandler().setStorageFailed(false);
        }

    }

    private void requestSetup(final RequestCompleteListener<SetupRequestObject> cro) {

        RequestTask<TransferObject, SetupRequestObject> task = getRequestFactory().setupRequest(new RequestCompleteListener<SetupRequestObject>() {
            @Override
            public void onTaskComplete(SetupRequestObject response) {
                cro.onTaskComplete(response);
            }
        }, this);
        task.execute();
    }

    public StorageHandler getStorageHandler() {
        return mStorageHandler;
    }

    public void setStorageHandler(StorageHandler storageHandler) {
        this.mStorageHandler = storageHandler;
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

    public ExchangeManager getMerchantModeManager() {
        return merchantModeManager;
    }

}
