package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

/**
 * Listener responsible for setting transaction memos.
 */
public class UpdateTxMemosListener extends AbstractWalletEventListener {

    private WalletService walletService;

    public UpdateTxMemosListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        walletService.setTxMemos();
    }
}
