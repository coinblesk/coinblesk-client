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
 * This class sends a request to retrieve the transactions of the authenticated user.
 */
public class HistoryRequestTask extends RequestTask {
	private static final String TX_PAGE_STRING = "txPage";
	private static final String TX_PAY_IN_PAGE_STRING = "txPayInPage";
	private static final String TX_PAY_OUT_PAGE_STRING = "txPayOutPage";
	
	public HistoryRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, int txPage, int txPayInPage, int txPayOutPage) {
		this.callback = cro;
		setParamteres(txPage, txPayInPage, txPayOutPage);
	}
	
	private void setParamteres(int txPage, int txPayInPage, int txPayOutPage) {
		this.url = UriComponentsBuilder.fromUriString(Constants.BASE_URI_SSL)
			    .path("/transaction/history")
			    .queryParam(TX_PAGE_STRING, txPage)
			    .queryParam(TX_PAY_IN_PAGE_STRING, txPayInPage)
			    .queryParam(TX_PAY_OUT_PAGE_STRING, txPayOutPage)
			    .build()
			    .toUriString();
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		return restTemplate.exchange(url, HttpMethod.GET, requestEntity);
	}

}
