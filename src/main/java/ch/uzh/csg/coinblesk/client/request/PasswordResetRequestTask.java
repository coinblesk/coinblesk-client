package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import net.minidev.json.JSONObject;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to reset the password. The user gets a link to
 * reset the password by email if the request was successful.
 */
public class PasswordResetRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public PasswordResetRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/user/resetPasswordRequest", cro, context);
	}

	@Override
	protected TransferObject responseService(TransferObject tro) throws Exception {
		JSONObject jsonObject = new JSONObject();
		tro.encode(jsonObject);
		return execResetPassword(jsonObject);
	}

}
