package ch.uzh.csg.coinblesk.client.ui.main;

import com.robotium.solo.Solo;

import org.junit.After;
import org.junit.Before;

import basetests.BaseInstrumentationTest;
import ch.uzh.csg.coinblesk.client.CoinBleskApplication;

/**
 * Created by rvoellmy on 7/3/15.
 */
public class SplashActivityTest extends BaseInstrumentationTest<SplashActivity> {


    public SplashActivityTest() {
        super(SplashActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        solo = new Solo(getInstrumentation(), getActivity());
        mApplication = (CoinBleskApplication) getActivity().getApplication();

        clearInternalData();
        prepareResponses();
        prepareRequestFactory();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clearInternalData();
    }

    public void testFirstLaunch() {
        solo.waitForActivity(RestoreOrNewActivity.class, TIMEOUT);
    }

    public void testExistingWallet() {
        prepareInternalStorage();
        solo.waitForActivity(MainActivity.class, TIMEOUT);
    }
}
