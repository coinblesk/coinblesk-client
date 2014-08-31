package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to delete the defined payout rules.
 */
public class PayOutRuleResetRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public PayOutRuleResetRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/reset", cro, context);
	}

	@Override
	protected TransferObject responseService(TransferObject to) {
		return execGet();
	}

}
