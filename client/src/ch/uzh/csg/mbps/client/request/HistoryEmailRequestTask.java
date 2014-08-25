package ch.uzh.csg.mbps.client.request;

import net.minidev.json.JSONObject;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to get an email of the desired transactions
 * (common transaction, pay in transaction, pay out transaction).
 */
//TODO: not used?
public class HistoryEmailRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public static final int TRANSACTION_HISTORY = 0;
	public static final int PAY_IN_HISTORY = 1;
	public static final int PAY_OUT_HISTORY = 2;
	
	public HistoryEmailRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/history/getByEmail", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject to) throws Exception {
		JSONObject jsonObject = new JSONObject();
		to.encode(jsonObject);
		return execPost(jsonObject);
	}
}
