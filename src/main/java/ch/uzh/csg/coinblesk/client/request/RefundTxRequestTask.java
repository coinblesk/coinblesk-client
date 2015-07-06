package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;

/**
 * This class sends a request to payout a defined amount of bitcoins to the
 * inserted bitcoin-address.
 */
public class RefundTxRequestTask extends RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> {

	public RefundTxRequestTask(RequestCompleteListener<RefundTxTransferObject> cro, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/signRefundTx", cro, context);
	}

}
