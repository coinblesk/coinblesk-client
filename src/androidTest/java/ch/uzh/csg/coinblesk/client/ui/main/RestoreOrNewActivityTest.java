package ch.uzh.csg.coinblesk.client.ui.main;

import android.test.suitebuilder.annotation.LargeTest;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.junit.After;
import org.junit.Before;

import basetests.ActivityTestWithLogin;
import ch.uzh.csg.coinblesk.client.R;

/**
 * Created by rvoellmy on 6/23/15.
 */
@LargeTest
public class RestoreOrNewActivityTest extends ActivityTestWithLogin {

    private final static int TIMEOUT = 30*1000;

    // these actually have some test coins on them, so don't change!
    private final static String RESTORE_SEED = "pause quarter bar elder always donkey elevator north scout symbol clever rather";
    private final static String ADDRESS = "2N9f6XZqRiSHHdTBjS2tKTwXd2f6bt1RebV";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateNewWallet() throws Throwable {
        prepareInternalStorage();
        onlineLogin();
        checkifMainActivity();

        Thread.sleep(30000);
    }

    public void testRestoreWallet() throws Throwable {
        prepareInternalStorage();
        onlineLogin();

        solo.clearEditText((EditText) solo.getView(R.id.restoreOrNew_edit_passphrase));
        solo.enterText((EditText) solo.getView(R.id.restoreOrNew_edit_passphrase), RESTORE_SEED);
        solo.clickOnView(solo.getView(R.id.restoreOrNew_button_restoreWallet));

        checkifMainActivity();

        // make sure the wallet seed is the same as the one we entered
        solo.setNavigationDrawer(Solo.OPENED);
        solo.clickOnActionBarHomeButton();
        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Wallet Backup");
        assertTrue(solo.waitForText(RESTORE_SEED, 0, TIMEOUT));

        // make sure the receive address is the same as before
        solo.goBack();
        solo.goBack();
        solo.setNavigationDrawer(Solo.OPENED);
        solo.waitForText("Top Up Account", 0, TIMEOUT);
        solo.clickOnMenuItem("Top Up Account");
        assertTrue(solo.waitForText(ADDRESS, 0, TIMEOUT));



    }

    private void checkifMainActivity() {
        solo.waitForActivity(RestoreOrNewActivity.class, TIMEOUT);
        solo.clickOnView(solo.getView(R.id.restoreOrNew_button_createNewWallet));
        solo.assertCurrentActivity("Should be RestoreOrNewActivity", RestoreOrNewActivity.class);
    }
}