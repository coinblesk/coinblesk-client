package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to retrieve the defined payout rules of the
 * authenticated user.
 */
public class PayOutRuleGetRequestTask extends RequestTask <TransferObject, PayOutRulesTransferObject>{

	public PayOutRuleGetRequestTask(IAsyncTaskCompleteListener<PayOutRulesTransferObject> cro, TransferObject input, PayOutRulesTransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/get", cro);
	}

	@Override
	protected PayOutRulesTransferObject responseService(TransferObject to) {
		return execGet();
	}

}
