package ch.uzh.csg.mbps.client.request;

import java.io.IOException;
import java.text.ParseException;

import net.minidev.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class sends a transaction request to the server.
 */
public class TransactionRequestTask extends RequestTask {

	private CreateTransactionTransferObject ctto;
	
	private long time;

	public TransactionRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, CreateTransactionTransferObject ctto) {
		this.callback = cro;
		this.ctto = ctto;
		this.url = Constants.BASE_URI_SSL + "/transaction/create";
		this.time = System.currentTimeMillis();
		
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		long start = System.currentTimeMillis();
		
		try {
        	//request
        	HttpClient httpclient = new DefaultHttpClient();
    		JSONObject jsonObject = new JSONObject();
    		jsonObject.put("payloadBase64", ctto.getPayloadBase64());
        	HttpPost post = createPost(url, jsonObject);
        	HttpResponse response = httpclient.execute(post);
        	//reply
        	
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            	HttpEntity entity1 = response.getEntity();
            	String responseString = EntityUtils.toString(entity1);
            	
            	CustomResponseObject cro = new CustomResponseObject();
            	cro.decode(responseString);
            	
            	long diff1 = System.currentTimeMillis() - time;
        		long diff2 = System.currentTimeMillis() - start;
        		System.err.println("got from server: in:"+diff1+"ms"+"/"+diff2+"ms");
            	return cro;
            	
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                CustomResponseObject cro = new CustomResponseObject(false, statusLine.getReasonPhrase());
                return cro;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            CustomResponseObject cro = new CustomResponseObject(false, e.getMessage());
            return cro;
        } catch (IOException e) {
        	e.printStackTrace();
        	CustomResponseObject cro = new CustomResponseObject(false, e.getMessage());
        	return cro;
        } catch (ParseException e) {
	        e.printStackTrace();
	        CustomResponseObject cro = new CustomResponseObject(false, e.getMessage());
        	return cro;
        }
	}
}
