package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.CustomPublicKeyObject;
import ch.uzh.csg.coinblesk.responseobject.ReadRequestObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class RequestFactory {

    public RequestTask<TransferObject, TransferObject> loginRequest(IAsyncTaskCompleteListener<TransferObject> completeListener, String username, String password, Context context) {
        return new SignInRequestTask(completeListener, username, password, context);
    }

    public RequestTask<TransferObject, ReadRequestObject> readRequest(IAsyncTaskCompleteListener<ReadRequestObject> completeListener, TransferObject input, ReadRequestObject output, Context context) {
        return new ReadRequestTask(completeListener, input, output, context);
    }

    public RequestTask<CustomPublicKeyObject, TransferObject> commitPublicKeyRequest(IAsyncTaskCompleteListener<TransferObject> completeListener, CustomPublicKeyObject input, CustomPublicKeyObject output, Context context) {
        return new CommitPublicKeyRequestTask(completeListener, input, output, context);
    }

    public RequestTask<ServerSignatureRequestTransferObject, TransferObject> refundTxRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
        return new RefundTxRequestTask(cro, input, output, context);
    }

    public RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context) {
        return new PayOutRequestTask(cro, input, output, context);
    }

}