package ch.uzh.csg.mbps.client.request;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to sign in a user.
 */
public class SignInRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	final private String username;
	final private String password;
	
	public SignInRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, String username, String password) {
		super(null, new TransferObject(), Constants.BASE_URI_SSL + "/j_spring_security_check", cro);
		this.username = username;
		this.password = password;
	}

	@Override
	protected TransferObject responseService(TransferObject tro)  throws Exception {
		return execPost(createLogin(username, password));
	}
	
	private static List<NameValuePair> createLogin(String username, String password) {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
	    postParameters.add(new BasicNameValuePair("j_username", username));
	    postParameters.add(new BasicNameValuePair("j_password", password));
	    return postParameters;
	}
}
