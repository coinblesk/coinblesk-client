package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a request to retrieve the defined payout rules of the
 * authenticated user.
 */
public class PayOutRuleGetRequestTask extends RequestTask{

	public PayOutRuleGetRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro){
		this.callback = cro;
		this.url = Constants.BASE_URI_SSL + "/rules/get";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.GET, requestEntity);
	}

}
