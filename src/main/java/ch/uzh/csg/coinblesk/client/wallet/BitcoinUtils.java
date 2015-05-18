package ch.uzh.csg.coinblesk.client.wallet;

import org.bitcoinj.core.Coin;

import java.math.BigDecimal;

/**
 * Created by rvoellmy on 5/16/15.
 */
public class BitcoinUtils {

    public static BigDecimal coinToBigDecimal(Coin coin) {
        return BigDecimal.valueOf(coin.value, Coin.SMALLEST_UNIT_EXPONENT);
    }

    public static Coin bigDecimalToCoin(BigDecimal amount) {
        return Coin.parseCoin(amount.toString());
    }
}
