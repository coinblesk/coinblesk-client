package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.coinblesk.responseobject.HistoryTransferRequestObject;

/**
 * This class sends a request to retrieve the transactions of the authenticated user.
 */
public class HistoryRequestTask extends RequestTask<HistoryTransferRequestObject, GetHistoryTransferObject> {
	
	public HistoryRequestTask(IAsyncTaskCompleteListener<GetHistoryTransferObject> cro, HistoryTransferRequestObject input, GetHistoryTransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/history", cro, context);
	}

	@Override
	protected GetHistoryTransferObject responseService(HistoryTransferRequestObject tro)  throws Exception {
		JSONObject jsonObject = new JSONObject();
		tro.encode(jsonObject);
		return execPost(jsonObject);
	}
}
