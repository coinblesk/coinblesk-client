package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;

/**
 * This class sends a request to store the defined payout rules.
 */
public class PayOutRuleRequestTask extends RequestTask {

	private PayOutRulesTransferObject porto;
	
	public PayOutRuleRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, PayOutRulesTransferObject porto){
		this.callback = cro;
		this.porto = porto;
		this.url = Constants.BASE_URI_SSL + "/rules/create";
	}
	
	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeaderPORTO(porto);
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
