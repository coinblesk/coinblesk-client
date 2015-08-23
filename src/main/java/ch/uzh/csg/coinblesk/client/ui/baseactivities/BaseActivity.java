package ch.uzh.csg.coinblesk.client.ui.baseactivities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.ui.fragments.CustomDialogFragment;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;

/**
 * The class BaseActivity is the abstract base class for all
 * activity-contexts. This class inherits from fragmentActivity and extends the
 * activity with some common functions, which are used by almost all activities.
 * This class determines if the application is used by a smart-phone or by a
 * tablet and sets the orientation appropriately.
 */
public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(BaseActivity.class);
    
    private MenuItem menuWarning;
    private MenuItem offlineMode;
    private ProgressDialog progressDialog;
    private boolean destroyed = false;

    private WalletService walletService = null;
    private boolean isBound = false;

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        LOGGER.debug("{} connected to the wallet service.", name.toShortString());
        final WalletService.LocalBinder binder = (WalletService.LocalBinder) service;
        walletService = binder.getService();
        walletStatus(true);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        LOGGER.debug("{} disconnected from the wallet service.", name.toShortString());
        walletService = null;
        walletStatus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWalletService();
    }

    private void startWalletService() {
        final Intent serviceIntent = new Intent(this, WalletService.class);
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    //get notified if the wallet is ready
    protected void walletStatus(boolean connected) {}


    public WalletService getWalletService() {
        return walletService;
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissProgressDialog();
        if(isBound) {
            unbindService(this);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
    }


    /**
     * Starts the progress dialog. As long as the dialog is running other touch
     * actions are ignored.
     */
    public void showLoadingProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
        }

        progressDialog.setMessage(getResources().getString(R.string.loading_progress_dialog));
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.show();
            }
        });
    }

    /**
     * Closes the progress dialog.
     */
    public void dismissProgressDialog() {
        if (progressDialog != null && !destroyed) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Starts a new activity. The method is called only for guiding to another
     * activity.
     * 
     * @param <T>
     *            generic placeholder for the passed parameter
     * @param activity
     *            this parameter represents the activity in which the client is
     *            or was before going to the next activity
     * @param classActvity
     *            The class of the activity which will be started.
     */
    public <T> void launchActivity(Activity activity, Class<T> classActvity) {
        Intent intent = new Intent(activity, classActvity);
        startActivity(intent);
    }

    /**
     * This method is a placeholder for the toast but much simpler to use.
     * 
     * @param response
     *            The message which is shown in a toast call.
     */
    public void displayResponse(String response) {
        Toast.makeText(this, response, Toast.LENGTH_LONG).show();
    }

    /**
     * Checks the resolution of the device and determines its orientation.
     * Smart-phones are in portrait and tablets in landscape view. This method
     * is called by every activity. Disables the rotation function of the
     * screen.
     */
    public void setScreenOrientation() {
        if (isTablet()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    protected void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Shows a customized dialog fragment {@link CustomDialogFragment} with a
     * title, message and an icon for success or fail. Notifies about finished
     * action. The dialog disables other touch actions.
     * 
     * @param title
     *            The title of the notification
     * @param icon
     *            The success or fail symbol are shown. The icons integer value
     *            is passed.
     * @param message
     *            The content of the actions message.
     * @return Returns dialog fragment that contains title, icon, and message
     */
    public CustomDialogFragment showDialog(String title, int icon, String message) {
        FragmentManager fragmentManager = getFragmentManager();
        CustomDialogFragment dialogFR = CustomDialogFragment.create(title, icon, message);
        dialogFR.setCancelable(false);

        try {
            dialogFR.show(fragmentManager, "CustomDialog");
        } catch (Exception e) {
        }

        return dialogFR;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        offlineMode = menu.findItem(R.id.menu_offlineMode);

        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void invalidateOptionsMenu() {
        if (menuWarning != null) {
            if (ConnectionCheck.isNetworkAvailable(this)) {
                menuWarning.setVisible(false);
                offlineMode.setVisible(false);
            } else {
                menuWarning.setVisible(true);
                offlineMode.setVisible(true);
            }
        }
    }

    public CoinBleskApplication getCoinBleskApplication() {
        return (CoinBleskApplication) getApplication();
    }

    public boolean isTablet() {
        return !getResources().getBoolean(R.bool.small_device);
    }

    public void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
