package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.PayOutRulesTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to store the defined payout rules.
 */
public class PayOutRuleRequestTask extends RequestTask<PayOutRulesTransferObject, TransferObject> {

	public PayOutRuleRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, PayOutRulesTransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/rules/create", cro, context);
	}

	@Override
	protected TransferObject responseService(PayOutRulesTransferObject obj) throws Exception {
		JSONObject jsonObject = new JSONObject();
		obj.encode(jsonObject);
		return execPost(jsonObject);
	}
}
