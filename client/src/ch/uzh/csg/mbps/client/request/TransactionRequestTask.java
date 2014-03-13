package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a transaction request to the server.
 */
public class TransactionRequestTask extends RequestTask {

	private CreateTransactionTransferObject ctto;

	public TransactionRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, CreateTransactionTransferObject ctto) {
		this.callback = cro;
		this.ctto = ctto;
		this.url = Constants.BASE_URI_SSL + "/transaction/create";
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		HttpEntity requestEntity = CookieHandler.getAuthHeaderCTTO(ctto);
		CustomResponseObject retVal = restTemplate.exchange(url, HttpMethod.POST, requestEntity);
		System.err.println("got from server: "+retVal.getMessage());
		return retVal;
	}
	
}
