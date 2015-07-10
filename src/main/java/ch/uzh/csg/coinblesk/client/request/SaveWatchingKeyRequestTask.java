package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;

/**
 * This class sends a request to payout a defined amount of bitcoins to the
 * inserted bitcoin-address.
 */
public class SaveWatchingKeyRequestTask extends RequestTask<WatchingKeyTransferObject, TransferObject> {

	public SaveWatchingKeyRequestTask(RequestCompleteListener<TransferObject> cro, WatchingKeyTransferObject input, Context context) {
		super(input, new SetupRequestObject(), Constants.BASE_URI_SSL + "/wallet/saveWatchingKey", cro, context);
	}

}
