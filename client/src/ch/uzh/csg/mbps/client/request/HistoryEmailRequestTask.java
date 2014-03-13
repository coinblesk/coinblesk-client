package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a request to get an email of the desired transactions
 * (common transaction, pay in transaction, pay out transaction).
 */
public class HistoryEmailRequestTask extends RequestTask {
	private static final String TYPE = "type";
	
	public static final int TRANSACTION_HISTORY = 0;
	public static final int PAY_IN_HISTORY = 1;
	public static final int PAY_OUT_HISTORY = 2;
	
	/**
	 * Creates a new Request Task of the given type, but does not launch the
	 * request.
	 * 
	 * @param cb
	 *            the object to be called when the asynchronous call is
	 *            completed
	 * @param type
	 *            0 for transactions, 1 for pay in, 2 for pay out
	 */
	public HistoryEmailRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cb, int type) {
		this.callback = cb;
		setUrl(type);
	}
	
	private void setUrl(int type) {
		this.url = UriComponentsBuilder.fromUriString(Constants.BASE_URI_SSL)
			    .path("/transaction/history/getByEmail")
			    .queryParam(TYPE, type)
			    .build()
			    .toUriString();
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
