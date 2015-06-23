package ch.uzh.csg.coinblesk.client.ui.authentication;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FilenameFilter;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.persistence.WrongPasswordException;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.AbstractAsyncActivity;
import ch.uzh.csg.coinblesk.client.ui.main.MainActivity;
import ch.uzh.csg.coinblesk.client.ui.main.RestoreOrNewActivity;
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
 * Created by rvoellmy on 6/20/15.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {

    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";
    private final static String SERVER_WATCHING_KEY = TestUtils.getServerWatchingKey(TestNet3Params.get());

    private Solo solo;
    private CoinBleskApplication mApplication;

    private final TransferObject loginResponse = new TransferObject();
    private final ReadRequestObject readResponse = new ReadRequestObject();
    private final ReadRequestObject commitPublicKeyResponse = new ReadRequestObject();

    private RequestFactory requestFactory;

    public LoginActivityTest() {
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
        clearInternalData();
        solo.finishOpenedActivities();
        super.tearDown();
    }

    /**
     * Test for the case that the user logs in succesfully, but there is no wallet stored on the device.
     * @throws Throwable
     */
    @Test
    public void testLoginWithoutWallet() throws Throwable {

        prepareInternalStorage();

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

        solo.clearEditText((EditText) solo.getView(R.id.loginUsernameEditText));
        solo.enterText((EditText) solo.getView(R.id.loginUsernameEditText), USERNAME);
        solo.enterText((EditText) solo.getView(R.id.loginPasswordEditText), PASSWORD);

        solo.clickOnView(solo.getView(R.id.loginSignInBtn));

        // assert that the restore actitvity started
        solo.waitForActivity(RestoreOrNewActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should be RestoreOrNewActivity", RestoreOrNewActivity.class);

        solo.clickOnView(solo.getView(R.id.restoreOrNew_button_createNewWallet));
        solo.waitForActivity(MainActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should be RestoreOrNewActivity", MainActivity.class);

    }

    /**
     * Test for the case someone is offline and his account is already stored on the device (e.g. from
     * restored from the cloud) but the wallet hasn't been created yet.
     * @throws Throwable
     */
    @Test
    public void testOfflineWithoutWallet() throws Throwable {

        prepareInternalStorage();

        loginResponse.setSuccessful(false);

        solo.clearEditText((EditText) solo.getView(R.id.loginUsernameEditText));
        solo.enterText((EditText) solo.getView(R.id.loginUsernameEditText), USERNAME);
        solo.enterText((EditText) solo.getView(R.id.loginPasswordEditText), PASSWORD);

        solo.clickOnView(solo.getView(R.id.loginSignInBtn));

        // assert that the main actitvity started
        solo.waitForActivity(RestoreOrNewActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should be main activity", RestoreOrNewActivity.class);

    }

    /**
     * Test for the case that someone is offline and tries to log in for the first time
     * @throws Throwable
     */
    @Test
    public void testOfflineWithoutAccount() throws Throwable {

        loginResponse.setSuccessful(false);

        solo.clearEditText((EditText) solo.getView(R.id.loginUsernameEditText));
        solo.enterText((EditText) solo.getView(R.id.loginUsernameEditText), USERNAME);
        solo.enterText((EditText) solo.getView(R.id.loginPasswordEditText), PASSWORD);

        solo.clickOnView(solo.getView(R.id.loginSignInBtn));

        // assert that the main activity started
        assertTrue(solo.waitForText(solo.getString(R.string.establish_internet_connection), 0, 3000L));
        solo.waitForActivity(LoginActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should still be login activity", LoginActivity.class);

    }

    private void prepareInternalStorage() {

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


    private void prepareRequestFactory(){
        requestFactory = new RequestFactory() {
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

        ((AbstractAsyncActivity) solo.getCurrentActivity()).setRequestFactory(requestFactory);

    }

    private void clearInternalData() {

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
}