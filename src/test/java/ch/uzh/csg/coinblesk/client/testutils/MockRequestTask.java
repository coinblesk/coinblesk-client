package ch.uzh.csg.coinblesk.client.testutils;

import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class MockRequestTask<I extends TransferObject, O extends TransferObject> extends RequestTask<I, O> {

    private RequestCompleteListener<TransferObject> callback;
    private O mockResponse;

    public MockRequestTask(RequestCompleteListener callback, O mockResponse) {
        super(null, null, null, callback, null);
        this.callback = callback;
        this.mockResponse = mockResponse;
    }

    @Override
    protected O doInBackground(Void... params) {
        return mockResponse;
    }


}
