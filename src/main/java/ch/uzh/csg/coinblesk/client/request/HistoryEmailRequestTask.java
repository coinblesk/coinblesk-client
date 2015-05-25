package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to get an email of the desired transactions
 * (common transaction, pay in transaction, pay out transaction).
 */
public class HistoryEmailRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public static final int TRANSACTION_HISTORY = 0;
	public static final int PAY_IN_HISTORY = 1;
	public static final int PAY_IN_HISTORY_UNVERIFIED = 2;
	public static final int PAY_OUT_HISTORY = 3;
	
	public HistoryEmailRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/history/getByEmail", cro, context);
	}

	@Override
	protected TransferObject responseService(TransferObject to) throws Exception {
		JSONObject jsonObject = new JSONObject();
		to.encode(jsonObject);
		return execPost(jsonObject);
	}
}
