package basetests;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FilenameFilter;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import testutils.MockRequestTask;
import testutils.TestUtils;

/**
 * Created by rvoellmy on 6/23/15.
 */
public class BaseInstrumentationTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    protected final static String SERVER_WATCHING_KEY = TestUtils.getServerWatchingKey(TestNet3Params.get());

    protected Solo solo;
    protected CoinBleskApplication mApplication;

    protected SetupRequestObject setupResponse;
    protected TransferObject payOutResponse;
    protected TransferObject refundTxResponse;

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

        payOutResponse = new TransferObject();
        payOutResponse.setSuccessful(true);

        refundTxResponse = new TransferObject();
        refundTxResponse.setSuccessful(true);
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

        RequestFactory requestFactory = new RequestFactory() {
            @Override
            public RequestTask<ServerSignatureRequestTransferObject, TransferObject> refundTxRequest(RequestCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
                return new MockRequestTask(cro, setupResponse);
            }

            @Override
            public RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(RequestCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
                return new MockRequestTask(cro, setupResponse);
            }

            @Override
            public RequestTask<TransferObject, SetupRequestObject> setupRequest(RequestCompleteListener<SetupRequestObject> cro, Context context) {
                return new MockRequestTask(cro, setupResponse);
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

}
