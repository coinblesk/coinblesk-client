package ch.uzh.csg.coinblesk.client.ui.main;

import android.content.Intent;
import android.os.Bundle;

import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;

/**
 * This is the launcher activity of the app. It is responsible for starting:
 * <ul>
 *     <li>The {@link MainActivity} if an account is already stored on the device, or</li>
 *     <li>The {@link RestoreOrNewActivity} the app is launched for the first time</li>
 * </ul>
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(!getCoinBleskApplication().getStorageHandler().hasUserData()) {
            // new setup, start restore or new activity
            Intent intent = new Intent(this, RestoreOrNewActivity.class);
            startActivity(intent);
        } else {
            // existing account, start main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

    }

}
