package basetests;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import junit.framework.Assert;

import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FilenameFilter;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.customserialization.Currency;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;
import testutils.MockRequestTask;
import testutils.BitcoinTestUtils;

/**
 * Created by rvoellmy on 6/23/15.
 */
public class BaseInstrumentationTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {


    protected final static int TIMEOUT = 30*1000;
    protected final static String SERVER_WATCHING_KEY = BitcoinTestUtils.getServerWatchingKey(TestNet3Params.get());

    protected Solo solo;
    protected CoinBleskApplication mApplication;

    protected SetupRequestObject setupResponse;
    protected ExchangeRateTransferObject exchangeRateResponse;
    protected SignedTxTransferObject payOutResponse;
    protected RefundTxTransferObject refundTxResponse;
    protected TransferObject saveWatchingKeyResponse;

    public BaseInstrumentationTest(Class<T> activityClass) {
        super(activityClass);
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    protected void prepareResponses() {

        setupResponse = new SetupRequestObject();
        setupResponse.setSuccessful(true);
        setupResponse.setBitcoinNet(BitcoinNet.TESTNET);
        setupResponse.setServerWatchingKey(SERVER_WATCHING_KEY);

        exchangeRateResponse = new ExchangeRateTransferObject();
        exchangeRateResponse.setSuccessful(true);
        exchangeRateResponse.setExchangeRate(Currency.CHF, "300.00");

        payOutResponse = new SignedTxTransferObject();
        payOutResponse.setSuccessful(true);

        refundTxResponse = new RefundTxTransferObject();
        refundTxResponse.setSuccessful(true);

        saveWatchingKeyResponse = new TransferObject();
        saveWatchingKeyResponse.setSuccessful(true);

    }


    @After
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    protected void prepareInternalStorage() {
        mApplication.getStorageHandler().setBitcoinNet(BitcoinNet.TESTNET);
        mApplication.getStorageHandler().setWatchingKey(SERVER_WATCHING_KEY);
    }

    protected void prepareRequestFactory(){

        // set up the mock responses here. This also does some basic checks on the requests.
        RequestFactory requestFactory = new RequestFactory() {
            @Override
            public RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundTxRequest(RequestCompleteListener<RefundTxTransferObject> cro, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context) {
                // send the same tx back. It won't be signed but it doesn't matter
                refundTxResponse.setRefundTx(input.getPartialTx());
                return new MockRequestTask<>(cro, refundTxResponse);
            }

            @Override
            public RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> payOutRequest(RequestCompleteListener<SignedTxTransferObject> cro, ServerSignatureRequestTransferObject input, Context context) {
                Assert.assertNotNull(input.getPartialTx());
                Assert.assertNotNull(input.getIndexAndDerivationPaths());
                Assert.assertFalse(input.getIndexAndDerivationPaths().isEmpty());
                return new MockRequestTask<>(cro, payOutResponse);
            }

            @Override
            public RequestTask<WatchingKeyTransferObject, TransferObject> saveWatchingKeyRequest(RequestCompleteListener<TransferObject> cro, WatchingKeyTransferObject input, Context context) {
                Assert.assertNotNull(input.getWatchingKey());
                Assert.assertNotNull(input.getBitcoinNet());
                return new MockRequestTask<>(cro, saveWatchingKeyResponse);
            }

            @Override
            public RequestTask<TransferObject, SetupRequestObject> setupRequest(RequestCompleteListener<SetupRequestObject> cro, Context context) {
                return new MockRequestTask<>(cro, setupResponse);
            }

            @Override
            public RequestTask<TransferObject, ExchangeRateTransferObject> exchangeRateRequest(String symbol, RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) {
                return new MockRequestTask<>(cro, exchangeRateResponse);
            }

        };

        mApplication.setRequestFactory(requestFactory);

    }

    protected void clearInternalData() {

        // delete internal data
        mApplication.getStorageHandler().clear();

        //delete bitcoin wallet
        File[] bitcoinFiles = getActivity().getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith("wallet") || filename.endsWith("chain");
            }
        });

        for(File f : bitcoinFiles) {
            f.delete();
        }

    }

    protected CoinBleskApplication getCoinBleskApplication() {
        CoinBleskApplication app = (CoinBleskApplication) getInstrumentation().getTargetContext().getApplicationContext();

        try {
            // we need to wait for the application to initialize, else a null pointer
            // exception will be thrown.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return app;
    }

    /**
     * Waits until the {@link ch.uzh.csg.coinblesk.client.wallet.WalletService} is running. Only works for activities that
     * extend the {@link ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity}.
     */
    protected void waitForWalletService() {
        final WalletActivity activity = (WalletActivity) solo.getCurrentActivity();
        boolean started = solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return activity.getWalletService() != null;
            }
        }, TIMEOUT);

        assertTrue("Failed to start wallet service before timeout", started);
    }

}
