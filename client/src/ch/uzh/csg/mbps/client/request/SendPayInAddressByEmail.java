package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a request to to send the pay in address to the current user's email.
 */
public class SendPayInAddressByEmail extends RequestTask {

	public SendPayInAddressByEmail(IAsyncTaskCompleteListener<CustomResponseObject> cro){
		this.callback = cro;
		this.url = Constants.BASE_URI_SSL + "/transaction/payIn/getByEmail";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
