package ch.uzh.csg.mbps.client.request;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class sends a request to to send the pay in address to the current user's email.
 */
public class SendPayInAddressByEmail extends RequestTask<TransferObject, TransferObject> {

	public SendPayInAddressByEmail(IAsyncTaskCompleteListener<TransferObject> cro, TransferObject input, TransferObject output) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/payIn/getByEmail", cro);
	}

	@Override
	protected TransferObject responseService(TransferObject to) {
		return execGet();
	}

}
