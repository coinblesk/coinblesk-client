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
import ch.uzh.csg.coinblesk.client.wallet.WalletService.BitcoinWalletBinder;

/**
 * Base class for {@link Activity} that utilizes Wallet functionality.
 * 
 * @author rvoellmy
 *
 */
public class WalletActivity extends AbstractAsyncActivity implements ServiceConnection {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(WalletActivity.class);
    
    private WalletService walletService;
    private boolean walletConnected = false;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BitcoinWalletBinder binder = (BitcoinWalletBinder) service;
        walletService = binder.getService();
        walletConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        LOGGER.debug("{} disconnected from the wallet service.", name.toShortString());
        walletConnected = false;
    }
    
    
    @Override
    protected void onStart() {

        super.onStart();
        
        // bind to wallet service
        Intent intent = new Intent(this, WalletService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
    
    
    protected WalletService getWalletService() {
        return walletService;
    }
    
    
    @Override
    protected void onStop() {
        super.onStop();
        walletConnected = false;
        unbindService(this);

    }

    protected boolean walletConnected() {
        return walletConnected;
    }

}
