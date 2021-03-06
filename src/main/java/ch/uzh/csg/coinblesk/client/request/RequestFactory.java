package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;
import ch.uzh.csg.coinblesk.responseobject.RefundTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.ServerSignatureRequestTransferObject;
import ch.uzh.csg.coinblesk.responseobject.SetupRequestObject;
import ch.uzh.csg.coinblesk.responseobject.SignedTxTransferObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.WatchingKeyTransferObject;

/**
 * Factory for creating server requests.
 */
public interface RequestFactory {

    RequestTask<ServerSignatureRequestTransferObject, RefundTxTransferObject> refundTxRequest(RequestCompleteListener<RefundTxTransferObject> cro, ServerSignatureRequestTransferObject input, RefundTxTransferObject output, Context context);

    RequestTask<ServerSignatureRequestTransferObject, SignedTxTransferObject> payOutRequest(RequestCompleteListener<SignedTxTransferObject> cro, ServerSignatureRequestTransferObject input, Context context);

    RequestTask<WatchingKeyTransferObject, TransferObject> saveWatchingKeyRequest(RequestCompleteListener<TransferObject> cro, WatchingKeyTransferObject input, Context context);

    RequestTask<TransferObject, SetupRequestObject> setupRequest(RequestCompleteListener<SetupRequestObject> cro, Context context);

    /**
     *
     * @param symbol the symbol of the currency to get the exchange rate (e.g. "USD" or "EUR")
     * @param cro
     * @param context
     * @return
     */
    RequestTask<TransferObject, ExchangeRateTransferObject> exchangeRateRequest(String symbol, RequestCompleteListener<ExchangeRateTransferObject> cro, Context context);

}
