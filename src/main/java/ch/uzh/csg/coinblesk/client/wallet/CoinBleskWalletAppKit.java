package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.signers.TransactionSigner;

import java.io.File;

/**
 * Created by rvoellmy on 5/24/15.
 */
public class CoinBleskWalletAppKit extends WalletAppKit {

    private Context context;

    public CoinBleskWalletAppKit(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    @Override
    protected void onSetupCompleted() {
        maybeInitServerTransactionSigner(context);
    }

    private void maybeInitServerTransactionSigner(Context context) {
        for (TransactionSigner signer : vWallet.getTransactionSigners()) {
            if (signer instanceof ServerTransactionSigner) {
                Preconditions.checkNotNull(context);
                ((ServerTransactionSigner) signer).setContext(context);
                return;
            }
        }
    }

    public CoinBleskWalletAppKit setAndroidContext(Context context) {
        this.context = context;
        return this;
    }
}
