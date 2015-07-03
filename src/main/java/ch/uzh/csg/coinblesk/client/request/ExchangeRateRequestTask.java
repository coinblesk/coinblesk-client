package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to get the exchange rate BTC to CHF.
 */
public class ExchangeRateRequestTask extends RequestTask<TransferObject, TransferObject> {
	
	public ExchangeRateRequestTask(RequestCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/exchange-rate/", cro, context);
	}
}
