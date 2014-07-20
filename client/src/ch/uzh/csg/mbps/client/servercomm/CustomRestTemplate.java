package ch.uzh.csg.mbps.client.servercomm;

import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * By customizing the {@link RestTemplate} we can set a custom connection
 * timeout.
 */
public class CustomRestTemplate extends RestTemplate {
	
    public CustomRestTemplate() {
    	HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(new DefaultHttpClient());
    	requestFactory.setReadTimeout(Constants.CLIENT_READ_TIMEOUT);
    	requestFactory.setConnectTimeout(Constants.CLIENT_CONNECTION_TIMEOUT);
    		
    	setRequestFactory(requestFactory);
    }
    
	/**
	 * Calls the exchange method of the super class. If it throws an exception,
	 * than the client is set to offline mode.
	 * 
	 * @param url
	 *            the URL
	 * @param httpMethod
	 *            the HTTP method (GET, POST, etc)
	 * @param requestEntity
	 *            the entity (headers and/or body) to write to the request, may
	 *            be null
	 * @return the response as CustomResponseObject
	 */
    @SuppressWarnings("rawtypes")
    public CustomResponseObject exchange(String url, HttpMethod httpMethod, HttpEntity requestEntity) {
		if (requestEntity == null)
			return new CustomResponseObject(false, Constants.NO_COOKIE_STORED, Type.OTHER);

		try {
			return super.exchange(url, httpMethod, requestEntity, CustomResponseObject.class).getBody();
		} catch (Exception e) {
			ClientController.setOnlineMode(false);
			TimeHandler.getInstance().setStartTime();
			return new CustomResponseObject(false, Constants.REST_CLIENT_ERROR, Type.OTHER);
		}
    }
}
