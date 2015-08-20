package ch.uzh.csg.coinblesk.client.testutils;

import android.util.Base64;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.uzh.csg.coinblesk.responseobject.IndexAndDerivationPath;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;

/**
 * Class that mocks a server key chain. Allows to sign client transactions.
 */
public class MockServerKeyChain {


    private static final Logger LOGGER = LoggerFactory.getLogger(MockServerKeyChain.class);

    private static final String SERVER_MNEMONIC = "actor critic filter assist load now age strike right certain column paddle"; // don't change! Ever!

    private final DeterministicKeyChain serverKeyChain;
    private final NetworkParameters params;

    public MockServerKeyChain(NetworkParameters params) {

        this.params = params;

        // create server watching key
        DeterministicSeed seed = null;
        try {
            seed = new DeterministicSeed(SERVER_MNEMONIC, null, "", 0);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        serverKeyChain = kcg.getActiveKeyChain();
    }

    public String getServerWatchingKey() {
        // create server watching key
        DeterministicSeed seed = null;
        try {
            seed = new DeterministicSeed(SERVER_MNEMONIC, null, "", 0);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
        KeyChainGroup kcg = new KeyChainGroup(params, seed);
        return serverKeyChain.getWatchingKey().serializePubB58(params);
    }

    /**
     * Adds the missing signature to a half signed transaction of a client.
     * @param serverSigReq the signature request
     * @return the fully signed transaction
     */
    public Transaction getServerSignature(ServerSignatureRequestTransferObject serverSigReq) {

        Transaction tx = new Transaction(params, Base64.decode(serverSigReq.getPartialTx(), Base64.NO_WRAP));

        for (IndexAndDerivationPath indexAndPath : serverSigReq.getIndexAndDerivationPaths()) {

            TransactionInput txIn = tx.getInputs().get(indexAndPath.getIndex());

            Script inputScript = txIn.getScriptSig();
            Script redeemScript = new Script(inputScript.getChunks().get(inputScript.getChunks().size() - 1).data);

            ImmutableList<ChildNumber> keyPath = ImmutableList.copyOf(getChildNumbers(indexAndPath.getDerivationPath()));
            DeterministicKey key = serverKeyChain.getKeyByPath(keyPath, true);
            Sha256Hash sighash = tx.hashForSignature(indexAndPath.getIndex(), redeemScript, Transaction.SigHash.ALL, false);
            ECKey.ECDSASignature sig = key.sign(sighash);

            TransactionSignature txSig = new TransactionSignature(sig, Transaction.SigHash.ALL, false);

            int sigIndex = inputScript.getSigInsertionIndex(sighash, key.dropPrivateBytes().dropParent());
            Script dummyP2SHScript = P2SHScript.dummy();

            inputScript = dummyP2SHScript.getScriptSigWithSignature(inputScript, txSig.encodeToBitcoin(), sigIndex);

            txIn.setScriptSig(inputScript);
        }

        LOGGER.info("Signed transaction " + tx.getHashAsString());

        return tx;
    }

    private List<ChildNumber> getChildNumbers(int[] path) {

        List<ChildNumber> childNumbers = Lists.newArrayListWithCapacity(path.length);
        for (int i : path) {
            childNumbers.add(new ChildNumber(i));
        }

        return childNumbers;
    }

    public static class P2SHScript extends Script {

        public P2SHScript(byte[] programBytes) {
            super(programBytes);
        }

        @Override
        public boolean isPayToScriptHash() {
            return true;
        }

        public static P2SHScript dummy() {
            Script dummyScript = new ScriptBuilder().build();
            return new P2SHScript(dummyScript.getProgram());
        }

    }


}
