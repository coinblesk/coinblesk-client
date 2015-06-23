package testutils;

import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Created by rvoellmy on 6/22/15.
 */
public class MockRequestTask extends RequestTask {

    private IAsyncTaskCompleteListener<TransferObject> callback;
    private TransferObject mockResponse;

    public MockRequestTask(IAsyncTaskCompleteListener callback, TransferObject mockResponse) {
        super(null, null, null, callback, null);
        this.callback = callback;
        this.mockResponse = mockResponse;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        return mockResponse;
    }

    @Override
    protected TransferObject responseService(TransferObject restTemplate) throws Exception {
        return null;
    }
}
