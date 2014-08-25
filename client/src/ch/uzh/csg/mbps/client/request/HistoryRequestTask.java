package ch.uzh.csg.mbps.client.request;

import net.minidev.json.JSONObject;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.mbps.responseobject.HistoryTransferRequestObject;

/**
 * This class sends a request to retrieve the transactions of the authenticated user.
 */
public class HistoryRequestTask extends RequestTask<HistoryTransferRequestObject, GetHistoryTransferObject> {
	
	public HistoryRequestTask(IAsyncTaskCompleteListener<GetHistoryTransferObject> cro, HistoryTransferRequestObject input, GetHistoryTransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/history", cro);
	}

	@Override
	protected GetHistoryTransferObject responseService(HistoryTransferRequestObject tro)  throws Exception {
		JSONObject jsonObject = new JSONObject();
		tro.encode(jsonObject);
		return execPost(jsonObject);
	}
}
