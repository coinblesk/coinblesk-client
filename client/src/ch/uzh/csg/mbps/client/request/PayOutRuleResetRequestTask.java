package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a request to delete the defined payout rules.
 */
public class PayOutRuleResetRequestTask extends RequestTask {

	public PayOutRuleResetRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro){
		this.callback = cro;
		this.url = Constants.BASE_URI_SSL + "/rules/reset";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
