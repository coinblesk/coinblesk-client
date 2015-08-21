package ch.uzh.csg.coinblesk.client.wallet;

import com.google.common.primitives.Ints;

import org.bitcoinj.core.Transaction;

import java.util.List;

/**
 * Wrapper class for a raw half signed transaction and the child numbers that specify which keys from the HD wallet have been
 * used to sign the inputs.
 */
public class HalfSignedTransaction {

    private final byte[] halfSignedTx;
    private final int[] childNumbers;

    public HalfSignedTransaction(Transaction halfSignedTx, List<Integer> childNumbers) {
        this.halfSignedTx = halfSignedTx.bitcoinSerialize();
        this.childNumbers = Ints.toArray(childNumbers);
    }

    public int[] getChildNumbers() {
        return childNumbers;
    }

    public byte[] getHalfSignedTx() {
        return halfSignedTx;
    }
}
