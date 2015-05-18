package ch.uzh.csg.coinblesk.client.wallet;

import android.content.Context;

import net.minidev.json.JSONObject;

import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.responseobject.SignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 5/17/15.
 *
 * This class is responsible for sending the data to the client that is required
 * for fully signing the partially signed transaction that was created by the client.
 */
public class SignatureRequestTask extends RequestTask<SignatureRequestTransferObject, TransferObject> {


    public SignatureRequestTask(IAsyncTaskCompleteListener<TransferObject> cro, SignatureRequestTransferObject input, TransferObject output, Context context) {
        super(input, output, Constants.BASE_URI_SSL + "/transaction/sign", cro, context);
    }

    @Override
    protected TransferObject responseService(SignatureRequestTransferObject tro) throws Exception {
        JSONObject jsonObject = new JSONObject();
        tro.encode(jsonObject);
        return execPost(jsonObject);
    }
}
