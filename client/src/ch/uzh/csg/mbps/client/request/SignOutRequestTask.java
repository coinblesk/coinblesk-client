package ch.uzh.csg.mbps.client.request;

import net.minidev.json.JSONObject;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to sign out the authenticated user.
 */
public class SignOutRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public SignOutRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/spring_security_logout", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject tro)  throws Exception {
		JSONObject jsonObject = new JSONObject();
		tro.encode(jsonObject);
		CookieHandler.deleteCookie();
		return execPost(jsonObject);
	}
}