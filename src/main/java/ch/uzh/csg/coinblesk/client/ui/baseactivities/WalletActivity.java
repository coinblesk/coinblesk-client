package ch.uzh.csg.coinblesk.client.ui.baseactivities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import ch.uzh.csg.coinblesk.client.wallet.WalletService.LocalBinder;

/**
 * Base class for {@link Activity} that utilizes Wallet functionality.
 * 
 * @author rvoellmy
 * @author Thomas Bocek
 *
 */
public abstract class WalletActivity extends BaseActivity implements ServiceConnection {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(WalletActivity.class);
    
    private WalletService walletService = null;
    private boolean walletConnected = false;
    
    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        final LocalBinder binder = (LocalBinder) service;
        walletService = binder.getService();
        walletConnected = true;
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        LOGGER.debug("{} disconnected from the wallet service.", name.toShortString());
        walletService = null;
        walletConnected = false;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        startWalletService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        walletConnected = false;
        unbindService(this);

    }

    protected void startWalletService() {
        if(!walletConnected || walletService == null) {
            final Intent serviceIntent = new Intent(this, WalletService.class);
            bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
        }
    }

    protected boolean walletConnected() {
        return walletConnected;
    }
    
    public WalletService getWalletService() {
        return walletService;
    }
}
