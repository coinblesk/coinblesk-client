package ch.uzh.csg.coinblesk.client.ui.authentication;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import basetests.ActivityTestWithLogin;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.main.RestoreOrNewActivity;

/**
 * Created by rvoellmy on 6/20/15.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest extends ActivityTestWithLogin {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }


    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for the case that the user logs in succesfully, but there is no wallet stored on the device.
     * @throws Throwable
     */
    @Test
    public void testLoginWithoutWallet() throws Throwable {

        prepareInternalStorage();

        onlineLogin();

        // assert that the restore actitvity started
        solo.waitForActivity(RestoreOrNewActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should be RestoreOrNewActivity", RestoreOrNewActivity.class);

    }

    /**
     * Test for the case someone is offline and his account is already stored on the device (e.g. from
     * restored from the cloud) but the wallet hasn't been created yet.
     * @throws Throwable
     */
    @Test
    public void testOfflineWithoutWallet() throws Throwable {

        prepareInternalStorage();

        offlineLogin();

        // assert that the main actitvity started
        solo.waitForActivity(RestoreOrNewActivity.class, 30 * 1000);
        solo.waitForText(solo.getString(R.id.menu_offlineMode));
        solo.assertCurrentActivity("Should be main activity", RestoreOrNewActivity.class);

    }

    /**
     * Test for the case that someone is offline and tries to log in for the first time
     * @throws Throwable
     */
    @Test
    public void testOfflineWithoutAccount() throws Throwable {

        offlineLogin();

        // assert that the main activity started
        assertTrue(solo.waitForText(solo.getString(R.string.establish_internet_connection), 0, 3000L));
        solo.waitForActivity(LoginActivity.class, 30 * 1000);
        solo.assertCurrentActivity("Should still be login activity", LoginActivity.class);

    }
}