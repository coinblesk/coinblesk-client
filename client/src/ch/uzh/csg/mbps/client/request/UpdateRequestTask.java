package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends an update request. The update requests are changing the email
 * address or defining a new password.
 */
public class UpdateRequestTask extends RequestTask {
	
	private UserAccount user;
	
	public UpdateRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cb, UserAccount user) {
		this.callback = cb;
		this.user = user;
		this.url = Constants.BASE_URI_SSL + "/user/update";
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader(user);
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
