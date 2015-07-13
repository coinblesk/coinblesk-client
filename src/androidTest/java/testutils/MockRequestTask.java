package testutils;

import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class MockRequestTask<O extends TransferObject, I extends TransferObject> extends RequestTask<I, O> {

    private O mockResponse;

    public MockRequestTask(RequestCompleteListener<O> callback, O mockResponse) {
        super(null, null, null, callback, null);
        this.mockResponse = mockResponse;
    }

    @Override
    protected O doInBackground(Void... params) {
        return mockResponse;
    }
}
