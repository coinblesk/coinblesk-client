package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.exchange.ExchangeManager;

/**
 * Created by rvoellmy on 7/22/15.
 */
public class MerchantModeSellListener extends AbstractWalletEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(MerchantModeSellListener.class);

    private Context context;

    public MerchantModeSellListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        ExchangeManager merchantModeManager = ((CoinBleskApplication) context.getApplicationContext()).getMerchantModeManager();

        if(!merchantModeManager.merchantModeActive()) return;

        // this is weird but apparently it happens sometimes
        if(prevBalance.isGreaterThan(newBalance)) {
            LOGGER.warn("onCoinsReceived was called even though our balance is now smaller");
            return;
        }

        BigDecimal sellAmount = BitcoinUtils.coinToBigDecimal(newBalance.subtract(prevBalance));
        LOGGER.debug("Received cois and merchant mode is active... Trying to sell {} bitcoins", sellAmount);

        merchantModeManager.sell(sellAmount);
    }

}
