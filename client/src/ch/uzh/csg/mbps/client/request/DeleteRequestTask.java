package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends the request to delete the account of the authenticated user.
 */
public class DeleteRequestTask extends RequestTask {
	
	public DeleteRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro) {
		this.callback = cro;
		this.url = Constants.BASE_URI_SSL + "/user/delete/";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
