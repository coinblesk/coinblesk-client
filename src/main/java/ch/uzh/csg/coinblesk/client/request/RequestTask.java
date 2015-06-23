package ch.uzh.csg.coinblesk.client.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
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
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.tools.CookieHandler;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.TimeHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This abstract class is used to send asynchronous requests to the server and
 * to notify the caller when the response arrives. 
 */
public abstract class RequestTask<I extends TransferObject, O extends TransferObject> extends AsyncTask<Void, Void, O> {
	
	private static final int HTTP_CONNECTION_TIMEOUT = 3 * 1000;
	private static final int HTTP_SOCKET_TIMEOUT = 5 * 1000;
	
	final private I requestObject;
	final private O responseObject;
	final private String url;
	final private IAsyncTaskCompleteListener<O> callback;
	final private Context context;
	
	public RequestTask(I requestObject, O responseObject, String url, IAsyncTaskCompleteListener<O> callback, Context context) {
		this.requestObject = requestObject;
		this.responseObject = responseObject;
		this.url = url;
		this.callback = callback;
		this.context = context;
	}
	
	@Override
	protected O doInBackground(Void... params) {
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
        
		if (postParameters != null && !postParameters.isEmpty()) {
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
		HttpClient httpclient = createDefaultHttpsClient();
		HttpPost post = createPost(url, null, postParameters);
		HttpResponse response = httpclient.execute(post);
		return response;
    }
	
	public HttpResponse executePost(JSONObject jsonObject) throws ClientProtocolException, IOException {
		HttpClient httpclient = createDefaultHttpsClient();
		HttpPost post = createPost(url, jsonObject, null);
		HttpResponse response = httpclient.execute(post);
		return response;
	}
	
	public HttpResponse executeGet() throws ClientProtocolException, IOException {
		HttpClient httpclient = createDefaultHttpsClient();
		HttpUriRequest request = createGet(url);
		HttpResponse response = httpclient.execute(request);
		return response;
	}
	
	/*private DefaultHttpClient createDefaultHttpClient() {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, HTTP_SOCKET_TIMEOUT);
		return new DefaultHttpClient(httpParams);
	}*/
	
	private DefaultHttpClient createDefaultHttpsClient() {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, HTTP_SOCKET_TIMEOUT);
		return new DefaultHttpClient(new MyHttpClient(context).getConnectionManager(), httpParams);
	}
	
	public O execPost(JSONObject jsonObject) {
		try {
        	//request
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
            } else if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            	ClientController.setOnlineMode(false);
	        	TimeHandler.getInstance().setStartTime();
	        	
	        	responseObject.setUnauthorized();
        		responseObject.setMessage(statusLine.getReasonPhrase());
        		responseObject.setVersion(-1);
        		return responseObject;
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	ClientController.setOnlineMode(false);
        	TimeHandler.getInstance().setStartTime();
        	e.printStackTrace();
        	return createFailed(e.getMessage());
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
            } else if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            	ClientController.setOnlineMode(false);
	        	TimeHandler.getInstance().setStartTime();
	        	
	        	responseObject.setUnauthorized();
        		responseObject.setMessage(statusLine.getReasonPhrase());
        		responseObject.setVersion(-1);
        		return responseObject;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	ClientController.setOnlineMode(false);
        	TimeHandler.getInstance().setStartTime();
        	e.printStackTrace();
        	return createFailed(e.getMessage());
        }
	}
	
	private void parseSessionID(HttpResponse response) {
	    try {
	        Header header = response.getFirstHeader("Set-Cookie");

	        String value = header.getValue();
	        if (value.contains("JSESSIONID")) {
	            int index = value.indexOf("JSESSIONID=");
	            int endIndex = value.indexOf(";", index);
	            String sessionID = value.substring(index + "JSESSIONID=".length(), endIndex);

	            if (sessionID != null) {
	            	CookieHandler.setCookie(sessionID);
	            }
	        }
	    } catch (Exception e) {
	    }
	}

	public O execGet( ) {
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
            } else if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            	ClientController.setOnlineMode(false);
	        	TimeHandler.getInstance().setStartTime();
	        	
	        	responseObject.setUnauthorized();
        		responseObject.setMessage(statusLine.getReasonPhrase());
        		responseObject.setVersion(-1);
        		return responseObject;
            } else {
                //Closes the connection.
            	ClientController.setOnlineMode(false);
            	TimeHandler.getInstance().setStartTime();
                response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	ClientController.setOnlineMode(false);
        	TimeHandler.getInstance().setStartTime();
        	e.printStackTrace();
        	return createFailed(e.getMessage());
        }
	}
	
	public O execLogout() {
		try {
        	//request
			HttpResponse response = executeGet();
        	//reply
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            	TimeHandler.getInstance().setStartTime();
            	responseObject.setSuccessful(true);
            	return responseObject;
            } else {
                //Closes the connection.
            	response.getEntity().getContent().close();
                return createFailed(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
        	ClientController.setOnlineMode(false);
        	TimeHandler.getInstance().setStartTime();
        	e.printStackTrace();
        	return createFailed(e.getMessage());
        }
	}
	
	public O execResetPassword( JSONObject jsonObject) {
		try {
	    	//request
			HttpClient httpclient = createDefaultHttpsClient();
			HttpPost post = new HttpPost(url);
        	post.addHeader("Content-Type", "application/json;charset=UTF-8");
            post.addHeader("Accept", "application/json");
        	post.setEntity(new StringEntity(jsonObject.toString(), "UTF-8"));
			
			HttpResponse response = httpclient.execute(post);
			
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
	        } else if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
	        	ClientController.setOnlineMode(false);
	        	TimeHandler.getInstance().setStartTime();
	        	
	        	responseObject.setUnauthorized();
        		responseObject.setMessage(statusLine.getReasonPhrase());
        		responseObject.setVersion(-1);
        		return responseObject;
            } else {
	            //Closes the connection.
	            response.getEntity().getContent().close();
	            return createFailed(statusLine.getReasonPhrase());
	        }
	    } catch (Exception e) {
	    	ClientController.setOnlineMode(false);
        	TimeHandler.getInstance().setStartTime();
	    	e.printStackTrace();
	    	return createFailed(e.getMessage());
	    }
	}
	
}

class  MyHttpClient extends DefaultHttpClient {
 
    final Context context;
 
    public MyHttpClient(Context context) {
        this.context = context;
    }
 
    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        // Register for port 443 our SSLSocketFactory with our keystore
        // to the ConnectionManager
        registry.register(new Scheme("https", newSslSocketFactory(), 443));
        
        HttpParams params = new BasicHttpParams(); 
        return new ThreadSafeClientConnManager(params, registry);
    }
 
    private SSLSocketFactory newSslSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            InputStream in = context.getResources().openRawResource(R.raw.bitcoinkeystore);
            try {
                // Initialize the keystore with the provided trusted certificates
                // Also provide the password of the keystore
                trusted.load(in, "changeit".toCharArray());
            } finally {
                in.close();
            }
            // Pass the keystore to the SSLSocketFactory. The factory is responsible
            // for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            //sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
