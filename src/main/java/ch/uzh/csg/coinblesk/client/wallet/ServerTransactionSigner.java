package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.signers.CustomTransactionSigner;
import org.bitcoinj.signers.StatelessTransactionSigner;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.responseobject.SignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class sends partially signed transaction to the server. The server will sign the transaction and broadcast it.
 */
public class ServerTransactionSigner extends StatelessTransactionSigner {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTransactionSigner.class);

    private Context context;

    @Override
    public boolean isReady() {
        return context != null;
    }

    @Override
    public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {

        checkNotNull(context, "Context needs to be initialized in order to perform this request");

        Transaction tx = propTx.partialTx;
        int numInputs = tx.getInputs().size();

        List<Script> redeemScripts = Lists.newArrayListWithExpectedSize(numInputs);
        List<List<ChildNumber>> paths = Lists.newArrayListWithExpectedSize(numInputs);

        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            TransactionOutput txOut = txIn.getConnectedOutput();
            if (txOut == null) {
                continue;
            }
            Script scriptPubKey = txOut.getScriptPubKey();
            if (!scriptPubKey.isPayToScriptHash()) {
                LOGGER.warn("ServerTransactionSigner works only with P2SH transactions");
                return false;
            }

            Script inputScript = checkNotNull(txIn.getScriptSig());

            try {
                // We assume if its already signed, its hopefully got a SIGHASH type that will not invalidate when
                // we sign missing pieces (to check this would require either assuming any signatures are signing
                // standard output types or a way to get processed signatures out of script execution)
                txIn.getScriptSig().correctlySpends(tx, i, txIn.getConnectedOutput().getScriptPubKey());
                LOGGER.warn("Input {} already correctly spends output, assuming SIGHASH type used will be safe and skipping signing.", i);
                continue;
            } catch (ScriptException e) {
                // Expected.
            }

            RedeemData redeemData = txIn.getConnectedRedeemData(keyBag);
            if (redeemData == null) {
                LOGGER.warn("No redeem data found for input {}", i);
                continue;
            }

            redeemScripts.set(i, redeemData.redeemScript);
            paths.set(i, propTx.keyPaths.get(scriptPubKey));

        }


        SignatureRequestParameters params = new SignatureRequestParameters(propTx.keyPaths.get(scriptPubKey), sighash);

        RequestTask<SignatureRequestTransferObject, TransferObject> request = new SignatureRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
            @Override
            public void onTaskComplete(TransferObject res) {
                if(res.isSuccessful()) {
                    LOGGER.info("Transaction successfully broadcastet");
                } else {
                    LOGGER.error("Transaction failed");
                }
            }
        }, params.toTransferObject(), new TransferObject(), context);

        try {
            request.execute().get();
        } catch (InterruptedException e) {
            LOGGER.warn("Task was interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.warn("Task failed", e);
        }

        return true;
    }


    /**
     * Sets the context of this transaction signer. IMPORTANT: This must be done *before* getting
     * signatures from the server. Otherwise the request will fail.
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

}
