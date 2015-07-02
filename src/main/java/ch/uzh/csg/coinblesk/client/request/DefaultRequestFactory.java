package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class DefaultRequestFactory implements RequestFactory {

    @Override
    public RequestTask<ServerSignatureRequestTransferObject, TransferObject> refundTxRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
        return new RefundTxRequestTask(cro, input, output, context);
    }

    @Override
    public RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
        return new PayOutRequestTask(cro, input, output, context);
    }

    @Override
    public RequestTask<TransferObject, SetupRequestObject> setupRequest(IAsyncTaskCompleteListener<SetupRequestObject> cro, Context context) {
        return new SetupRequestTask(cro, context);
    }

}