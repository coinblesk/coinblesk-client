package ch.uzh.csg.coinblesk.client.wallet;

/**
 * Simple listener that can be added to the {@link WalletService} to be notified of wallet changes.
 */
public abstract class WalletListener {

    public abstract void onWalletChange();

}
