package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to sign out the authenticated user.
 */
public class SignOutRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public SignOutRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/spring_security_logout", cro, context);
	}

	@Override
	protected TransferObject responseService(TransferObject tro)  throws Exception {
		return execLogout();
	}
	
}