package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rvoellmy on 5/16/15.
 */
public class BitcoinUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitcoinUtils.class);

    public static BigDecimal coinToBigDecimal(Coin coin) {
        return BigDecimal.valueOf(coin.value, Coin.SMALLEST_UNIT_EXPONENT);
    }

    public static Coin bigDecimalToCoin(BigDecimal amount) {
        return Coin.parseCoin(amount.toString());
    }

    /**
     * Checvks if the passed mnemonic is a valid BIP32 mnemonic seed
     * @param mnemonic
     * @return
     */
    public static boolean validMnemonic(String mnemonic) {
        List<String> words = Arrays.asList(mnemonic.split(" "));
        try {
            MnemonicCode.INSTANCE.check(words);
            return true;
        } catch (MnemonicException e) {
            LOGGER.info("Mnemonic verification failed: {}", e);
            return false;
        }
    }

    /**
     * Converts month to number of blocks. E.g. 1 month == 6 * 24 * 30 blocks
     * @param month
     * @return
     */
    public static long monthsToBlocks(int month) {
        return 6 * 24 * 30 * month;
    }
}
