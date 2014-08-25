package ch.uzh.csg.mbps.client.request;

import net.minidev.json.JSONObject;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to store the defined payout rules.
 */
public class PayOutRuleRequestTask extends RequestTask<PayOutRulesTransferObject, TransferObject> {

	public PayOutRuleRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, PayOutRulesTransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/create", cro);
	}

	@Override
	protected TransferObject responseService(PayOutRulesTransferObject obj) throws Exception {
		JSONObject jsonObject = new JSONObject();
		obj.encode(jsonObject);
		return execPost(jsonObject);
	}
}
