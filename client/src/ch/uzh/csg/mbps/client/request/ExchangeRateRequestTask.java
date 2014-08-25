package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to get the exchange rate BTC to CHF.
 */
public class ExchangeRateRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public ExchangeRateRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/exchange-rate/", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject to) {
		return execGet();
	}
}
