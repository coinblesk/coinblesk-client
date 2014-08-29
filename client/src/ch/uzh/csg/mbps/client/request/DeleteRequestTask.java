package ch.uzh.csg.mbps.client.request;

import android.content.Context;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends the request to delete the account of the authenticated user.
 */
public class DeleteRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public DeleteRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/user/delete/", cro, context);
	}

	@Override
	protected TransferObject responseService(TransferObject to) {
		return execGet();
	}

}
