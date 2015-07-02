package basetests;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FilenameFilter;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.persistence.WrongPasswordException;
import ch.uzh.csg.coinblesk.client.request.DefaultRequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.authentication.LoginActivity;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.responseobject.CustomPublicKeyObject;
import ch.uzh.csg.coinblesk.responseobject.ReadRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import testutils.MockRequestTask;
import testutils.TestUtils;

/**
 * Created by rvoellmy on 6/23/15.
 */
public class ActivityTestWithLogin extends ActivityInstrumentationTestCase2<LoginActivity> {

    protected final static String USERNAME = "username";
    protected final static String PASSWORD = "password";
    protected final static String SERVER_WATCHING_KEY = TestUtils.getServerWatchingKey(TestNet3Params.get());

    protected Solo solo;
    protected CoinBleskApplication mApplication;

    protected final TransferObject loginResponse = new TransferObject();
    protected final ReadRequestObject readResponse = new ReadRequestObject();
    protected final ReadRequestObject commitPublicKeyResponse = new ReadRequestObject();

    public ActivityTestWithLogin() {
        super(LoginActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        mApplication = (CoinBleskApplication) getActivity().getApplication();

        clearInternalData();
        prepareRequestFactory();

    }


    @After
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    /**
     * Sets up a fake account, sets bitcoinnet and server watching key. All this is required to successfully log in.
     */
    protected void prepareInternalStorage() {

        //init fake account
        UserAccountObject userAccount = new UserAccountObject();
        userAccount.setUsername(USERNAME);
        userAccount.setPassword(PASSWORD);
        userAccount.setCreationDate(1435050268L);
        try {
            mApplication.initStorageHandler(getActivity().getApplicationContext(), USERNAME, PASSWORD);
        } catch (WrongPasswordException e) {
            e.printStackTrace();
        }

        mApplication.getStorageHandler().saveUserAccount(userAccount);

        // set other required data
        mApplication.getStorageHandler().setBitcoinNet(BitcoinNet.TESTNET);
        mApplication.getStorageHandler().setWatchingKey(SERVER_WATCHING_KEY);

        //check fake user account
        assertNotNull(mApplication.getStorageHandler().getUserAccount());
        assertEquals(USERNAME, mApplication.getStorageHandler().getUserAccount().getUsername());
        assertEquals(PASSWORD, mApplication.getStorageHandler().getUserAccount().getPassword());
    }


    protected void prepareRequestFactory(){
        RequestFactory requestFactory = new DefaultRequestFactory() {
            @Override
            public RequestTask<TransferObject, TransferObject> loginRequest(IAsyncTaskCompleteListener<TransferObject> completeListener, String username, String password, Context context) {
                return new MockRequestTask(completeListener, loginResponse);
            }

            @Override
            public RequestTask<TransferObject, ReadRequestObject> readRequest(IAsyncTaskCompleteListener<ReadRequestObject> completeListener, TransferObject input, ReadRequestObject output, Context context) {
                return new MockRequestTask(completeListener, readResponse);
            }

            @Override
            public RequestTask<CustomPublicKeyObject, TransferObject> commitPublicKeyRequest(IAsyncTaskCompleteListener<TransferObject> completeListener, CustomPublicKeyObject input, CustomPublicKeyObject output, Context context) {
                return new MockRequestTask(completeListener, commitPublicKeyResponse);
            }
        };

        mApplication.setRequestFactory(requestFactory);

    }

    protected void clearInternalData() {

        // delete internal data
        try {
            mApplication.initStorageHandler(getActivity().getApplicationContext(), USERNAME, PASSWORD);
        } catch (WrongPasswordException e) {
            e.printStackTrace();
        }
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

    protected void offlineLogin() {
        loginResponse.setSuccessful(false);
        login();
    }

    protected void onlineLogin() {
        //login response
        loginResponse.setSuccessful(true);

        //read response
        readResponse.setSuccessful(true);
        readResponse.setVersion(Constants.CLIENT_VERSION);
        CustomPublicKeyObject pubKeyObj = new CustomPublicKeyObject();
        pubKeyObj.setCustomPublicKey(new CustomPublicKey((byte) 0, (byte) 0, "fakepubkey"));
        readResponse.setCustomPublicKey(pubKeyObj);
        readResponse.setServerWatchingKey(SERVER_WATCHING_KEY);
        readResponse.setBitcoinNet(BitcoinNet.TESTNET);

        commitPublicKeyResponse.setSuccessful(true);
        commitPublicKeyResponse.setMessage("0");

        login();
    }

    private void login(){

        // enter username and password
        solo.clearEditText((EditText) solo.getView(R.id.loginUsernameEditText));
        solo.enterText((EditText) solo.getView(R.id.loginUsernameEditText), USERNAME);
        solo.enterText((EditText) solo.getView(R.id.loginPasswordEditText), PASSWORD);

        // click the sign in button
        solo.clickOnView(solo.getView(R.id.loginSignInBtn));
    }


}
