package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.MainRequestObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to get the exchange rate BTC to CHF.
 */
public class MainActivityRequestTask extends RequestTask<TransferObject, MainRequestObject> {
	
	public MainActivityRequestTask(IAsyncTaskCompleteListener<MainRequestObject> cro, TransferObject input, MainRequestObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/user/mainActivityRequests", cro);
	}

	@Override
	protected MainRequestObject responseService(TransferObject tro) {
		return execGet();
	}
	
}
