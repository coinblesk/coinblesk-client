package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Factory for creating server requests.
 */
public interface RequestFactory {

    RequestTask<ServerSignatureRequestTransferObject, TransferObject> refundTxRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context);

    RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(IAsyncTaskCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context);

    RequestTask<TransferObject, SetupRequestObject> setupRequest(IAsyncTaskCompleteListener<SetupRequestObject> cro, Context context);
}
