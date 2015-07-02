package ch.uzh.csg.coinblesk.client.testutils;

import net.minidev.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import java.io.IOException;

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
    protected TransferObject doInBackground(Void... params) {
        return mockResponse;
    }

    @Override
    protected TransferObject responseService(TransferObject restTemplate) throws Exception {
        return null;
    }

    @Override
    public HttpResponse executePost(JSONObject jsonObject) throws ClientProtocolException, IOException {
        return null;
    }

}
