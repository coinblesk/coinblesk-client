package ch.uzh.csg.mbps.client.request;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class sends a request to sign out the authenticated user.
 */
public class SignOutRequestTask extends RequestTask {
	
	public SignOutRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro) {
		this.callback = cro;
		this.url = Constants.BASE_URI_SSL + "/spring_security_logout";
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {

		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeader();
		if (requestEntity != null) {
			CookieHandler.deleteCookie();
		}

		try {
			restTemplate.postForEntity(url, null, String.class);
		} catch (Exception e) {
		}
		// It does not matter if the request was successful as long the cookie
		// of the user which authorizes the user is deleted.
		return new CustomResponseObject(true, Constants.REST_CLIENT_ERROR, Type.LOGOUT);
	}
	
	@Override
	protected CustomResponseObject requestService() {
		CustomRestTemplate restTemplate = new CustomRestTemplate();
		
		//Create a list for the message converters
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		//Add the Jackson Message converter
		messageConverters.add(new FormHttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		
		return responseService(restTemplate);
	}
	
}