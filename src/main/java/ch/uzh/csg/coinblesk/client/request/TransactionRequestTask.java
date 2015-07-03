package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.TransactionObject;

/**
 * This class sends a transaction request to the server.
 */
public class TransactionRequestTask extends RequestTask<TransactionObject, TransactionObject> {

	public TransactionRequestTask(IAsyncTaskCompleteListener<TransactionObject> cro, TransactionObject input, TransactionObject output, Context context) {
		super(input, output, Constants.BASE_URI_SSL + "/transaction/create", cro, context);
	}
}
