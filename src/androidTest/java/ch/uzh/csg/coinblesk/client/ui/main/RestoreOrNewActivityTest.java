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

    private final static String RESTORE_SEED = "pause quarter bar elder always donkey elevator north scout symbol clever rather";
    private final static String ADDRESS = "2MvoFRRTGFNgdqgHAurpDZyjNzpt1pK69ZH";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateNewWallet() {
        prepareInternalStorage();

        onlineLogin();

        checkifMainActivity();
    }

    public void testRestoreWallet() throws Throwable {
        prepareInternalStorage();
        onlineLogin();

        solo.clearEditText((EditText) solo.getView(R.id.restoreOrNew_edit_passphrase));
        solo.enterText((EditText) solo.getView(R.id.restoreOrNew_edit_passphrase), RESTORE_SEED);
        solo.clickOnView(solo.getView(R.id.restoreOrNew_button_restoreWallet));

        checkifMainActivity();

        solo.setNavigationDrawer(Solo.OPENED);
        solo.clickOnActionBarHomeButton();
        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Wallet Backup");

        // make sure the wallet seed is the same as the one we entered
        assertTrue(solo.waitForText(RESTORE_SEED, 0, 30*1000L));

        //Thread.sleep(3600*1000L);

    }

    private void checkifMainActivity() {
        solo.waitForActivity(RestoreOrNewActivity.class, 30 * 1000);
        solo.clickOnView(solo.getView(R.id.restoreOrNew_button_createNewWallet));
        solo.assertCurrentActivity("Should be RestoreOrNewActivity", RestoreOrNewActivity.class);
    }
}