package ch.uzh.csg.coinblesk.client.wallet;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Protos;

import java.util.List;

import ch.uzh.csg.coinblesk.responseobject.SignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 5/17/15.
 *
 * This class wraps the data that is required for a married wallet to create a fully signed transaction from a partially signed transaction
 */
public class SignatureRequestParameters {

    private static final String DELIMITER = ":";
    private static final String PATH_DELIMITER = "/";
    private static final String FIELD_DELIMITER = "___";

    private final List<Script> redeemScripts;
    private final List<List<ChildNumber>> paths;
    private final Transaction partialTx;


    public SignatureRequestParameters(List<Script> redeemScripts, List<List<ChildNumber>> paths, Transaction partialTx) {
        this.redeemScripts = redeemScripts;
        this.paths = paths;
        this.partialTx = partialTx;
    }

    /**
     *
     * @return a {@link TransferObject} that contains the necessary data for a married wallet to sign an incomplete transaction.
     * Namely it contains the sighash and derivation path of the incomplete transaction
     */
    public SignatureRequestTransferObject toTransferObject() {
        return null; //new SignatureRequestTransferObject(derivationPathToString(derivationPath), sighashToString(sighash));
    }

    private String scriptsToString(List<Script> scripts, List<List<ChildNumber>> paths) {
        Preconditions.checkArgument(scripts.size() == paths.size(), "number of scripts and number of derivation paths must be equal");

//        StringBuilder sb = new StringBuilder();
//        for(int i = 0; i < scripts.size(); i++) {
//            sb.append(scripts.get(i).)
//        }

        return null;
    }


}
