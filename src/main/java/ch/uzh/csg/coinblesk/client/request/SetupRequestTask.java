package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to payout a defined amount of bitcoins to the
 * inserted bitcoin-address.
 */
public class SetupRequestTask extends RequestTask<TransferObject, SetupRequestObject> {

	public SetupRequestTask(IAsyncTaskCompleteListener<SetupRequestObject> cro, Context context) {
		super(new TransferObject(), new SetupRequestObject(), Constants.BASE_URI_SSL + "/info/setupInfo", cro, context);
	}

	@Override
	protected SetupRequestObject responseService(TransferObject tro) {
		return execGet();
	}

}
