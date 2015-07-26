package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class sends a request to to send the pay in address to the current user's email.
 */
public class SendPayInAddressByEmail extends RequestTask<TransferObject, TransferObject> {

	public SendPayInAddressByEmail(RequestCompleteListener<TransferObject> cro, TransferObject input, TransferObject output, Context context) {
		super(input, output, Constants.BASE_URL + "/transaction/payIn/getByEmail", cro, context);
	}

}
