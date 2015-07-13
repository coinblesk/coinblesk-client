package testutils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;

import java.math.BigDecimal;

/**
 * Created by rvoellmy on 6/21/15.
 */
public class BitcoinTestUtils {

    public static String getServerWatchingKey(NetworkParameters params) {
        // create server watching key
        KeyChainGroup kcg = new KeyChainGroup(params, getSeed());
        DeterministicKey watchingKey = kcg.getActiveKeyChain().getWatchingKey();
        return watchingKey.serializePubB58(params);
    }

    /**
     * Deterministically creates a bitcoin address.
     *
     * @param params the {@link NetworkParameters} of the address
     * @return a bitcoin address
     */
    public static String getBitcoinAddress(NetworkParameters params) {
        KeyChainGroup kcg = new KeyChainGroup(params, getSeed());
        return kcg.getActiveKeyChain().getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(params).toString();
    }

    private static DeterministicSeed getSeed() {
        // create the seed
        String mnemonic = "actor critic filter assist load now age strike right certain column paddle"; // don't change! Ever!
        try {
            return new DeterministicSeed(mnemonic, null, "", 0);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     * Sends fake coins to the wallet. The fake coins are "broadcasted" to the wallet in a block, so they will already
     * have a confirmation.
     *
     * @param amount  the amount to send
     * @param addr    the receive address
     * @param appKit the app kits to broadcast the block to
     */
    public static void sendFakeCoins(NetworkParameters params, BigDecimal amount, String addr, WalletAppKit appKit) {

        try {
            Address receiveAddr = new Address(params, addr);
            Coin coinAmount = Coin.parseCoin(amount.toString());
            Transaction tx = FakeTxBuilder.createFakeTx(params, coinAmount, receiveAddr);
            Block block = FakeTxBuilder.makeSolvedTestBlock(appKit.store().getChainHead().getHeader(), tx);
            // "send" BTC to the client
            appKit.chain().add(block);
            Thread.sleep(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}