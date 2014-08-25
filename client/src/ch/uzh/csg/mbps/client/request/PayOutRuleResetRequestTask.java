package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to delete the defined payout rules.
 */
public class PayOutRuleResetRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public PayOutRuleResetRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/reset", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject to) {
		return execGet();
	}

}
