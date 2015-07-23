package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to get the exchange from a currency to the reference currency.
 * The reference currency is determined by the server's settings.
 */
public class ExchangeRateRequestTask extends RequestTask<TransferObject, ExchangeRateTransferObject> {
	
	public ExchangeRateRequestTask(String symbol, RequestCompleteListener<ExchangeRateTransferObject> cro, Context context) {
		super(new ExchangeRateTransferObject(), Constants.BASE_URI_SSL + "/wallet/exchangeRate/" + symbol, cro, context);
	}
}
