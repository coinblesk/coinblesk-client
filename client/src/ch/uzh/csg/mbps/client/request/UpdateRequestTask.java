package ch.uzh.csg.mbps.client.request;

import net.minidev.json.JSONObject;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;
import ch.uzh.csg.mbps.responseobject.UserAccountObject;

/**
 * This class sends an update request. The update requests are changing the email
 * address or defining a new password.
 */
public class UpdateRequestTask extends RequestTask<UserAccountObject, TransferObject> {	
	public UpdateRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, UserAccountObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/user/update", cro);
	}

	@Override
	protected TransferObject responseService(UserAccountObject muao)  throws Exception {		
		JSONObject jsonObject = new JSONObject();
		muao.encode(jsonObject);
		return execPost(jsonObject);
	}

}
