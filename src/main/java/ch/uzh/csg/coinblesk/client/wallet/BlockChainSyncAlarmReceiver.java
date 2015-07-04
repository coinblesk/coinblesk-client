package ch.uzh.csg.coinblesk.client.wallet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.persistence.PersistentStorageHandler;

public class BlockChainSyncAlarmReceiver extends BroadcastReceiver implements ServiceConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockChainSyncAlarmReceiver.class);

    private Context context;
    private WalletService walletService;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        // bind to wallet service
        Intent walletServiceIntent = new Intent(context, WalletService.class);
        context.bindService(walletServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        final BlockChainSyncAlarmReceiver self = this;


        WalletService.LocalBinder binder = (WalletService.LocalBinder) service;
        walletService = binder.getService();
        PersistentStorageHandler storage = ((CoinBleskApplication) context.getApplicationContext()).getStorageHandler();
        walletService.init(storage);

        walletService.getSyncProgress().addSyncCompleteListener(new SyncProgress.SyncProgressFinishedListener() {
            @Override
            public void onFinished() {
                LOGGER.debug("Finished synchronizing the blockchain. Shutting down wallet service");
                context.unbindService(self);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        LOGGER.debug("{} disconnected from the wallet service.", name.toShortString());
    }

    public boolean isSyncing() {
        if(walletService == null) {
            return false;
        } else {
            return !walletService.getSyncProgress().isFinished();
        }
    }

    public WalletService getWalletService() {
        return walletService;
    }
}
