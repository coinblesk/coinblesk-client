package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;

/**
 * This class sends an update request. The update requests are changing the email
 * address or defining a new password.
 */
public class UpdateRequestTask extends RequestTask<UserAccountObject, TransferObject> {	
	
	public UpdateRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, UserAccountObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/user/update", cro, context);
	}

	@Override
	protected TransferObject responseService(UserAccountObject muao)  throws Exception {		
		JSONObject jsonObject = new JSONObject();
		muao.encode(jsonObject);
		return execPost(jsonObject);
	}

}
