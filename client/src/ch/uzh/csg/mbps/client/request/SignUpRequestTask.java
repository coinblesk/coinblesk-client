package ch.uzh.csg.mbps.client.request;

import org.springframework.http.ResponseEntity;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class sends a request to register the user with the inserted
 * information.
 */
public class SignUpRequestTask extends RequestTask {
	
	private UserAccount user;
	
	public SignUpRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, UserAccount user) {
		this.callback = cro;
		this.user = user;
		this.url = Constants.BASE_URI_SSL + "/user/create";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		ResponseEntity<CustomResponseObject> response = null;
		try {
			response = restTemplate.postForEntity(url, user, CustomResponseObject.class);	
			return new CustomResponseObject(response.getBody().isSuccessful(), response.getBody().getMessage(), Type.OTHER);
		} catch (Exception e) {
			return new CustomResponseObject(false, Constants.REST_CLIENT_ERROR, Type.OTHER);
		}
	}

}