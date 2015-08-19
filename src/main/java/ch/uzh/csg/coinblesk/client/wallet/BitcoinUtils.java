package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Base64;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
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
import java.util.Date;
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

    public static Coin satoshiToCoin(long satoshi) {
        return Coin.valueOf(satoshi);
    }

    public static BigDecimal satoshiToBigDecimal(long satoshi) {
        return new BigDecimal(Coin.valueOf(satoshi).toPlainString());
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

    /**
     * Converts a String of a bitcoin address in a byte array of length 20 (Hash160).
     *
     * @param bitcoinAddress
     * @param bitcoinNet
     * @return
     */
    public static byte[] addressToBytes(String bitcoinAddress, BitcoinNet bitcoinNet) {
        try {
            Address addr = new Address(getNetworkParameters(bitcoinNet), bitcoinAddress);
            return addr.getHash160();
        } catch (AddressFormatException e) {
            LOGGER.error("Invalid bitcoin address: {}", bitcoinAddress);
            return null;
        }
    }

    /**
     * Checks if the passed address is a valid P2SH address.
     *
     * @param address    the string of the bitcoin address
     * @param bitcoinNet the bitcoinNet
     * @return true if the address is a valid P2SH address.
     */
    public static boolean isP2SHAddress(String address, BitcoinNet bitcoinNet) {
        try {
            return new Address(getNetworkParameters(bitcoinNet), address).isP2SHAddress();
        } catch (AddressFormatException e) {
            return false;
        }
    }

    /**
     * @return a P2SH, created from a given script hash.
     */
    public static String getAddressFromScriptHash(byte[] scriptHash, BitcoinNet bitcoinNet) {
        return Address.fromP2SHHash(getNetworkParameters(bitcoinNet), scriptHash).toString();
    }

    /**
     * Takes a raw transaction and returns the transaction hash
     *
     * @param rawTx the raw transaction to get the hash from
     * @return the hash of the transaction (txId)
     */
    public static String getTxHash(byte[] rawTx, BitcoinNet bitcoinNet) {
        Transaction tx = new Transaction(getNetworkParameters(bitcoinNet), rawTx);
        return tx.getHashAsString();
    }

    /**
     * Converts a Base64 encoded transaction to a hex encoded transaction
     *
     * @param base64Tx
     * @return
     */
    public static String base64TxToHex(String base64Tx) {
        byte[] rawTx = Base64.decode(base64Tx, Base64.NO_WRAP);
        return bytesToHex(rawTx);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String refundTxValidBlockToFriendlyString(long blocksRemaining, Context context) {
        Date validDate = new Date(System.currentTimeMillis() + 1000 * 60 * 10 * blocksRemaining);
        return DateUtils.getRelativeDateTimeString(context, validDate.getTime(), DateUtils.WEEK_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString();
    }

}
