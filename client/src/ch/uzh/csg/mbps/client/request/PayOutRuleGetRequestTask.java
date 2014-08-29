package ch.uzh.csg.mbps.client.request;

import android.content.Context;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to retrieve the defined payout rules of the
 * authenticated user.
 */
public class PayOutRuleGetRequestTask extends RequestTask <TransferObject, PayOutRulesTransferObject>{

	public PayOutRuleGetRequestTask(IAsyncTaskCompleteListener<PayOutRulesTransferObject> cro, TransferObject input, PayOutRulesTransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/get", cro, context);
	}

	@Override
	protected PayOutRulesTransferObject responseService(TransferObject to) {
		return execGet();
	}

}
