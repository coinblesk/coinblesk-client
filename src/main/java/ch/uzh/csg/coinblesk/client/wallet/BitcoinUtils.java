package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;

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
     *
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
     *
     * @param month
     * @return
     */
    public static long monthsToBlocks(int month) {
        return 6 * 24 * 30 * month;
    }

    public static NetworkParameters getNetworkParameters(BitcoinNet bitcoinNet) {
        switch (bitcoinNet) {
            case UNITTEST:
                return UnitTestParams.get();
            case REGTEST:
                return RegTestParams.get();
            case TESTNET:
                return TestNet3Params.get();
            case MAINNET:
                return MainNetParams.get();
            default:
                throw new RuntimeException("Please set the server property bitcoin.net to (unittest|regtest|testnet|mainnet)");
        }
    }
}
