package ch.uzh.csg.coinblesk.client.ui.authentication;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.TextView;

import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.main.MainActivity;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;
import testutils.TestUtils;

/**
 * Created by rvoellmy on 6/20/15.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {

    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";

    private Activity mActivity;
    private CoinBleskApplication mApplication;

    private Button mLoginButton;
    private TextView mUsernameText;
    private TextView mPasswordText;

    public LoginActivityTest() {
        super(LoginActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        getInstrumentation();
        mActivity = getActivity(); // get a references to the app under test
        mApplication = (CoinBleskApplication) mActivity.getApplication();

        mLoginButton = (Button) mActivity.findViewById(R.id.loginSignInBtn);
        mUsernameText = (TextView) mActivity.findViewById(R.id.loginUsernameEditText);
        mPasswordText = (TextView) mActivity.findViewById(R.id.loginPasswordEditText);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testPreconditions() {
        assertNotNull(mActivity);
        assertNotNull(mLoginButton);
        assertNotNull(mApplication);
    }

    @Test
    public void testOfflineLogin() throws Throwable {

        //init fake account
        UserAccountObject userAccount = new UserAccountObject();
        userAccount.setUsername(USERNAME);
        userAccount.setPassword(PASSWORD);
        mApplication.initStorageHandler(mActivity.getApplicationContext(), USERNAME, PASSWORD);
        mApplication.getStorageHandler().saveUserAccount(userAccount);

        // set other required data
        mApplication.getStorageHandler().setBitcoinNet(BitcoinNet.TESTNET);
        mApplication.getStorageHandler().setWatchingKey(TestUtils.getServerWatchingKey(TestNet3Params.get()));

        //check fake user account
        assertNotNull(mApplication.getStorageHandler().getUserAccount());
        assertEquals(USERNAME, mApplication.getStorageHandler().getUserAccount().getUsername());
        assertEquals(PASSWORD, mApplication.getStorageHandler().getUserAccount().getPassword());

        // register next activity that need to be monitored.
        Instrumentation.ActivityMonitor activityMonitor = getInstrumentation().addMonitor(MainActivity.class.getName(), null, false);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUsernameText.setText(USERNAME);
                mPasswordText.setText(PASSWORD);
                mLoginButton.performClick();
            }
        });

        Activity nextActivity = getInstrumentation().waitForMonitorWithTimeout(activityMonitor, 30000L);
        System.out.println(nextActivity);
        // next activity is opened and captured.
        assertNotNull(nextActivity);
        assertTrue(nextActivity instanceof MainActivity);
        nextActivity.finish();

    }
}