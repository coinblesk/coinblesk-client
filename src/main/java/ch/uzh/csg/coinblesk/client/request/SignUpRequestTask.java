package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.ServerWatchingKeyTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;

/**
 * This class sends a request to register the user with the inserted
 * information.
 */
public class SignUpRequestTask extends RequestTask<UserAccountObject, ServerWatchingKeyTransferObject> {

	public SignUpRequestTask(IAsyncTaskCompleteListener<ServerWatchingKeyTransferObject> cro, UserAccountObject input, ServerWatchingKeyTransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/user/create", cro, context);
	}

	@Override
	protected ServerWatchingKeyTransferObject responseService(UserAccountObject tro)  throws Exception {
		JSONObject jsonObject = new JSONObject();
		tro.encode(jsonObject);
		return execPost(jsonObject);
	}

}