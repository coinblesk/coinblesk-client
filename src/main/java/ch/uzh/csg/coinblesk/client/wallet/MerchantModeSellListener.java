package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.exchange.MerchantModeManager;

/**
 * Created by rvoellmy on 7/22/15.
 */
public class MerchantModeSellListener extends AbstractWalletEventListener {

    private Context context;

    public MerchantModeSellListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        MerchantModeManager merchantModeManager = ((CoinBleskApplication) context.getApplicationContext()).getMerchantModeManager();

        if(!merchantModeManager.merchantModeActive()) return;

        BigDecimal sellAmount = new BigDecimal(tx.getValueSentToMe(wallet).toString());
        merchantModeManager.sell(sellAmount);
    }

}
