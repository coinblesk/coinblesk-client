package ch.uzh.csg.mbps.client.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import net.minidev.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.util.Log;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This abstract class is used to send asynchronous requests to the server and
 * to notify the caller when the response arrives. 
 */
public abstract class RequestTask<I extends TransferObject, O extends TransferObject> extends AsyncTask<Void, Void, O> {
	
	final private I requestObject;
	final private O responseObject;
	final private String url;
	final private IAsyncTaskCompleteListener<O> callback;
	
	//List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
    //postParameters.add(new BasicNameValuePair("param1", "param1_value"));
    //postParameters.add(new BasicNameValuePair("param2", "param2_value"));
	
	// = new LinkedMultiValueMap<String, String>()
	
	
	
	public RequestTask(I requestObject, O responseObject, String url, IAsyncTaskCompleteListener<O> callback) {
		this.requestObject = requestObject;
		this.responseObject = responseObject;
		this.url = url;
		this.callback = callback;
	}
	
	
	
	@Override
	final protected O doInBackground(Void... params) {
		if(TimeHandler.getInstance().determineIfLessThanFiveSecondsLeft()){
			cancel(true);
			return createFailed("timeout");
		}
		try {
			return responseService(requestObject);
		} catch (Exception e) {
        	e.printStackTrace();
        	return createFailed(e.getMessage());
        }
	}
	
	/**
	 * Gets the response and sends it to the object that launched the request.
	 * 
	 * @param result
	 *            Response of the request. The result is an
	 *            {@link CustomResponseObject} and includes always a boolean and
	 *            message.
	 */
	@Override
	final protected void onPostExecute(O result){
		
		callback.onTaskComplete(result);
	}
	
	/**
	 * Checks if the user is authorized to ask for a request and sends a HTTP
	 * request.
	 * 
	 * @param restTemplate
	 *            The defined message template for the communication
	 * @return
	 */
	protected abstract O responseService(I restTemplate) throws Exception;
	
	public String getURL() {
		return url;
	}
	
	private static HttpPost createPost(String url, JSONObject jsonObject, List<NameValuePair> postParameters) throws UnsupportedEncodingException {
		HttpPost post = new HttpPost(url);
        post.addHeader(CookieHandler.COOKIE_STRING, CookieHandler.JSESSIONID_STRING + CookieHandler.getCookie());
        
        
        if(postParameters!=null && !postParameters.isEmpty()) {
        	post.setEntity(new UrlEncodedFormEntity(postParameters));
        } else if (jsonObject != null) {
        	post.addHeader("Content-Type", "application/json;charset=UTF-8");
            post.addHeader("Accept", "application/json");
        	post.setEntity(new StringEntity(jsonObject.toString(), "UTF-8"));
        }
        return post;
	}
	
	private static HttpGet createGet(String url) throws UnsupportedEncodingException {
		HttpGet get = new HttpGet(url);
		get.addHeader(CookieHandler.COOKIE_STRING, CookieHandler.JSESSIONID_STRING + CookieHandler.getCookie());
		//get.addHeader("Content-Type", "application/json;charset=UTF-8");
		get.addHeader("Accept", "application/json");
        return get;
	}
	
	public O createFailed(String failedMessage) {
		responseObject.setSuccessful(false);
		responseObject.setMessage(failedMessage);
		responseObject.setVersion(-1);
		return responseObject;
	}
	
	private HttpResponse executePost(List<NameValuePair> postParameters) throws ClientProtocolException, IOException {
		final long start = System.currentTimeMillis();
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost post = createPost(url, null, postParameters);
		HttpResponse response = httpclient.execute(post);
		final long diff = System.currentTimeMillis() - start;
		Log.i("ch.uzh.csg.mbps.client.request.RequestTask", "request Post completed in "+diff+"ms");
		return response;
    }
	
	public HttpResponse executePost(JSONObject jsonObject) throws ClientProtocolException, IOException {
		final long start = System.currentTimeMillis();
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost post = createPost(url, jsonObject, null);
		HttpResponse response = httpclient.execute(post);
		final long diff = System.currentTimeMillis() - start;
		Log.i("ch.uzh.csg.mbps.client.request.RequestTask", "request Post completed in "+diff+"ms");
		return response;
	}
	
	public HttpResponse executeGet() throws ClientProtocolException, IOException {
		final long start = System.currentTimeMillis();
		HttpClient httpclient = new DefaultHttpClient();
		HttpUriRequest request = createGet(url);
		HttpResponse response = httpclient.execute(request);
		final long diff = System.currentTimeMillis() - start;
		Log.i("ch.uzh.csg.mbps.client.request.RequestTask", "request Get completed in "+diff+"ms");
		return response;
	}
	
	public O execPost(JSONObject jsonObject) {
		try {
        	//request
			requestObject.encode(jsonObject);
        	HttpResponse response = executePost(jsonObject);
        	//reply
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            	TimeHandler.getInstance().setStartTime();
            	HttpEntity entity1 = response.getEntity();
            	String responseString = EntityUtils.toString(entity1);
            	if(responseString != null && responseString.trim().length() > 0) {
            		responseObject.decode(responseString);
            	}
            	return responseObject;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	return createFailed( e.getMessage());
        }
	}
	
	public O execPost(List<NameValuePair> postParameters) {
		try {
        	//request
			HttpResponse response = executePost(postParameters);
			//reply
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            	TimeHandler.getInstance().setStartTime();
            	HttpEntity entity1 = response.getEntity();
            	String responseString = EntityUtils.toString(entity1);
            	
            	if(responseString != null && responseString.trim().length() > 0) {
            		responseObject.decode(responseString);
            	}
            	if(this.url.contains("j_spring_security_check")) {
            		parseSessionID(response);
            		responseObject.setSuccessful(true);
            	}
            	return responseObject;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	return createFailed( e.getMessage());
        }
	}
	
	private void parseSessionID(HttpResponse response) {
	    try {

	        Header header = response.getFirstHeader("Set-Cookie");

	        String value = header.getValue();
	        if (value.contains("JSESSIONID")) {
	            int index = value.indexOf("JSESSIONID=");

	            int endIndex = value.indexOf(";", index);

	            String sessionID = value.substring(
	                    index + "JSESSIONID=".length(), endIndex);

	            if (sessionID != null) {
	            	CookieHandler.setCookie(sessionID);
	            }

	        }
	    } catch (Exception e) {
	    }

	}
	
	

	public O execGet() {
		try {
        	//request
			HttpResponse response = executeGet();
        	//reply
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            	TimeHandler.getInstance().setStartTime();
            	HttpEntity entity1 = response.getEntity();
            	String responseString = EntityUtils.toString(entity1);
            	if(responseString != null && responseString.trim().length() > 0) {
            		responseObject.decode(responseString);
            	}
            	return responseObject;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	return createFailed( e.getMessage());
        }
	}
}