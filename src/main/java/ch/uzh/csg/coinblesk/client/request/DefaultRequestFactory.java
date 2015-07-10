package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class DefaultRequestFactory implements RequestFactory {

    @Override
    public RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundTxRequest(RequestCompleteListener<RefundTxTransferObject> cro, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context) {
        return new RefundTxRequestTask(cro, input, output, context);
    }

    @Override
    public RequestTask<ServerSignatureRequestTransferObject, TransferObject> payOutRequest(RequestCompleteListener<TransferObject> cro, ServerSignatureRequestTransferObject input, Context context) {
        return new PayOutRequestTask(cro, input, context);
    }

    @Override
    public RequestTask<WatchingKeyTransferObject, TransferObject> saveWatchingKeyRequest(RequestCompleteListener<TransferObject> cro, WatchingKeyTransferObject input, Context context) {
        return new SaveWatchingKeyRequestTask(cro, input, context);
    }

    @Override
    public RequestTask<TransferObject, SetupRequestObject> setupRequest(RequestCompleteListener<SetupRequestObject> cro, Context context) {
        return new SetupRequestTask(cro, context);
    }

}