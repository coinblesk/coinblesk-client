package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;

/**
 * This class sends a request to payout a defined amount of bitcoins to the
 * inserted bitcoin-address.
 */
public class PayOutRequestTask extends RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> {
		
	public PayOutRequestTask(RequestCompleteListener<SignedTxTransferObject> cro, ServerSignatureRequestTransferObject input, Context context) {
		super(input, new SignedTxTransferObject(), Constants.BASE_URL + "/wallet/signAndBroadcastTx", cro, context);
	}

}
