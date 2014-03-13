package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.model.PayOutTransaction;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a request to payout a defined amount of bitcoins to the
 * inserted bitcoin-address.
 */
public class PayOutRequestTask extends RequestTask {
	
	private PayOutTransaction pot;
	
	public PayOutRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, PayOutTransaction pot) {
		this.callback = cro;
		this.pot = pot;
		this.url = Constants.BASE_URI_SSL + "/transaction/payOut";
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeaderPOT(pot);
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
