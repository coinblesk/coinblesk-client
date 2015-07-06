package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;

import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.script.Script;
import org.bitcoinj.signers.StatelessTransactionSigner;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.request.RequestFactory;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class sends partially signed transaction to the server. The server will sign the transaction and broadcast it.
 * Each unspent transaction output can only be used once.
 * Even though this class implements the {@link StatelessTransactionSigner} interface, it is actually not stateless,
 * because it needs a {@link Context} to send a request to the server. Serializing the context to permanent storage is
 * probably not a good idea, which is why this class doesn't implement the serializing methods of the
 * {@link org.bitcoinj.signers.TransactionSigner} interface.
 */
public class ServerTransactionSigner extends StatelessTransactionSigner {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerTransactionSigner.class);

    private Context context;
    private List<TransactionSigningCompleteListener> listeners;

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {

        checkNotNull(context, "Context needs to be initialized in order to perform this request");

        ServerSignatureRequestTransferObject txSigRequest = new ServerSignatureRequestTransferObject();

        Transaction tx = propTx.partialTx;
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            TransactionOutput txOut = txIn.getConnectedOutput();
            if (txOut == null) {
                continue;
            }
            Script scriptPubKey = txOut.getScriptPubKey();
            if (!scriptPubKey.isPayToScriptHash()) {
                LOGGER.warn(this.getClass().getSimpleName() + " works only with P2SH transactions");
                return false;
            }

            checkNotNull(txIn.getScriptSig());

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

            txSigRequest.addIndexAndDerivationPath(i, childNumbersToIntArray(propTx.keyPaths.get(scriptPubKey)));

        }

        String serializedTx = android.util.Base64.encodeToString(tx.bitcoinSerialize(), android.util.Base64.NO_WRAP);
        txSigRequest.setPartialTx(serializedTx);

        if (!tx.isTimeLocked()) {
            launchServerRequest(tx, txSigRequest);
        } else {
            launchRefundTxRequest(tx, txSigRequest);
        }

        return true;
    }

    private int[] childNumbersToIntArray(List<ChildNumber> childNumbers) {
        int[] path = new int[childNumbers.size()];
        for (int i = 0; i < childNumbers.size(); i++) {
            path[i] = childNumbers.get(i).getI();
        }
        return path;
    }

    private void launchServerRequest(final Transaction tx, ServerSignatureRequestTransferObject txSigRequest) {
        RequestFactory requestFactory = ((CoinBleskApplication) context.getApplicationContext()).getRequestFactory();
        RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequestTask = requestFactory.payOutRequest(new RequestCompleteListener<TransferObject>() {
            public void onTaskComplete(TransferObject response) {

                // we notify the listeners with the partial transaction here. We don't need the
                // fully signed transaction, the server will broadcast it, and we can commit it to the
                // wallet even if it's not fully signed.
                notifyListeners(tx, response.isSuccessful());

                if (response.isSuccessful()) {
                    LOGGER.info("Transaction signing and broadcast was successful");
                    Toast.makeText(context, R.string.payment_success, Toast.LENGTH_LONG).show();
                } else {
                    LOGGER.error("Transaction failed: " + response.getMessage());
                    Toast.makeText(context, R.string.payment_failure, Toast.LENGTH_LONG).show();
                }

            }
        }, txSigRequest, new TransferObject(), context);
        payOutRequestTask.execute();
    }

    private void launchRefundTxRequest(final Transaction tx, ServerSignatureRequestTransferObject txSigRequest) {
        RequestFactory requestFactory = ((CoinBleskApplication) context.getApplicationContext()).getRequestFactory();
        RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundRequestTask = requestFactory.refundTxRequest(new RequestCompleteListener<RefundTxTransferObject>() {
            @Override
            public void onTaskComplete(RefundTxTransferObject response) {

                Transaction refundTx = null;
                // TODO: change success/fail strings below

                if (response.isSuccessful()) {
                    refundTx = new Transaction(tx.getParams(), Base64.decode(response.getRefundTx(), Base64.NO_WRAP));
                    LOGGER.info("Refund tx request was successful");
                    Toast.makeText(context, R.string.payment_success, Toast.LENGTH_LONG).show();
                } else {
                    LOGGER.error("Refund tx request failed: " + response.getMessage());
                    Toast.makeText(context, R.string.payment_failure, Toast.LENGTH_LONG).show();
                }

                notifyListeners(refundTx, response.isSuccessful());
            }
        }, txSigRequest, new RefundTxTransferObject(), context);

        refundRequestTask.execute();
    }


    /**
     * Sets the context of this transaction signer. IMPORTANT: This must be done *before* getting
     * signatures from the server. Otherwise the request will fail.
     *
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * adds a listener to this transaction signer that will be called when the transaction is complete.
     *
     * @param listener
     */
    public void addTransactionSigningCompleteListener(TransactionSigningCompleteListener listener) {
        if (listeners == null) {
            listeners = new LinkedList<>();
        }
        listeners.add(listener);
    }

    private void notifyListeners(final Transaction tx, boolean successful) {

        if (listeners == null) {
            return;
        }

        for (TransactionSigningCompleteListener listener : listeners) {
            if (successful) {
                listener.onSuccess(tx);
            } else {
                listener.onFailed(tx);
            }
        }
    }

}
