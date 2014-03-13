package ch.uzh.csg.mbps.client.request;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This class sends a request to sign in a user.
 */
public class SignInRequestTask extends RequestTask {
	
	private UserAccount user;
	
	public SignInRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, UserAccount user) {
		this.callback = cro;
		this.user = user;
		this.url = Constants.BASE_URI_SSL + "/j_spring_security_check";
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		//Add fields for spring authentication
		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
		bodyMap.add("j_username", this.user.getUsername());
		bodyMap.add("j_password", this.user.getPassword());

		ResponseEntity<String> response = null;
		try {
			response = restTemplate.postForEntity(url, bodyMap, String.class);
			
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				CookieHandler.storeCookie(response.getHeaders().entrySet());
				return new CustomResponseObject(true, "OK", Type.LOGIN);
			}
		} catch (RestClientException e) {
			if (e instanceof HttpClientErrorException) {
				if (((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
					return new CustomResponseObject(false, "Invalid username or password!", Type.LOGIN);
				}
			}
		}
		return new CustomResponseObject(false, Constants.REST_CLIENT_ERROR, Type.LOGIN);
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
