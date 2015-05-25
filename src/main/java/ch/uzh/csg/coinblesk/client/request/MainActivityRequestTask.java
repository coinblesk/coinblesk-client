package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.MainRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to get the exchange rate BTC to CHF.
 */
public class MainActivityRequestTask extends RequestTask<TransferObject, MainRequestObject> {
	
	public MainActivityRequestTask(IAsyncTaskCompleteListener<MainRequestObject> cro, TransferObject input, MainRequestObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/user/mainActivityRequests", cro, context);
	}

	@Override
	protected MainRequestObject responseService(TransferObject tro) {
		return execGet();
	}
	
}
