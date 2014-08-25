package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to reset the password. The user gets a link to
 * reset the password by email if the request was successful.
 */
public class PasswordResetRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public PasswordResetRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/user/resetPasswordRequest", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject tro) {
		return execGet();
	}

}
