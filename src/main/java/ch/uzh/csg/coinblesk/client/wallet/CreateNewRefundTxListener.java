package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

/**
 * This wallet event listener is responsible for creating new refund transactions every time we receive bitcoins. This
 * includes receiving change.
 */
public class CreateNewRefundTxListener extends AbstractWalletEventListener {

    private final WalletService walletService;

    public CreateNewRefundTxListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        createRefundTx(wallet);
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        // we also need to create a refund transaction if we sent coins because of
        // the change
        createRefundTx(wallet);
    }

    private void createRefundTx(Wallet wallet) {
        if(wallet.getBalance(Wallet.BalanceType.ESTIMATED).signum() == 0) {
            // no money in the wallet -> no need to create a refund transaction
            return;
        }
        walletService.createRefundTransaction();
    }
}
