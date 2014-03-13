package ch.uzh.csg.mbps.client.request;

import org.springframework.http.ResponseEntity;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class sends a request to reset the password. The user gets a link to
 * reset the password by email if the request was successful.
 */
public class PasswordResetRequestTask extends RequestTask {
	
	private UserAccount user;
	
	public PasswordResetRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cb, UserAccount user){
		this.callback = cb;
		this.user = user;
		this.url = Constants.BASE_URI_SSL + "/user/resetPasswordRequest";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		ResponseEntity<CustomResponseObject> response = null;
		try {
			response = restTemplate.postForEntity(url, user.getEmail(), CustomResponseObject.class);
			return new CustomResponseObject(response.getBody().isSuccessful(), response.getBody().getMessage(), Type.OTHER);
		} catch (Exception e) {
			return new CustomResponseObject(false, Constants.REST_CLIENT_ERROR, Type.OTHER);
		}
	}

}
