package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Factory for creating server requests.
 */
public interface RequestFactory {

    RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundTxRequest(RequestCompleteListener<RefundTxTransferObject> cro, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context);

    RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(RequestCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, TransferObject output, Context context);

    RequestTask<TransferObject, SetupRequestObject> setupRequest(RequestCompleteListener<SetupRequestObject> cro, Context context);
}
