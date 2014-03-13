package ch.uzh.csg.mbps.client.request;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import android.os.AsyncTask;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

/**
 * This abstract class is used to send asynchronous requests to the server and
 * to notify the caller when the response arrives. 
 */
public abstract class RequestTask extends AsyncTask<Void, Void, CustomResponseObject> {
	protected IAsyncTaskCompleteListener<CustomResponseObject> callback;
	protected String url;
	
	@Override
	final protected CustomResponseObject doInBackground(Void... params) {
		return requestService();
	}
	
	@Override
	final protected void onPostExecute(CustomResponseObject result){
		responseResult(result);
	}

	/**
	 * Creates the message converter for sending the request.
	 * 
	 * @return the result of the request
	 */
	protected CustomResponseObject requestService() {
		if(TimeHandler.getInstance().determineIfLessThanFiveSecondsLeft()){
			cancel(true);
		}
		
		CustomRestTemplate restTemplate = new CustomRestTemplate();
		//Create a list for the message converters
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		//Add the Jackson Message converter
		messageConverters.add(new MappingJacksonHttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		
		return responseService(restTemplate);
	}
	
	/**
	 * Gets the response and sends it to the object that launched the request.
	 * 
	 * @param result
	 *            Response of the request. The result is an
	 *            {@link CustomResponseObject} and includes always a boolean and
	 *            message.
	 */
	protected void responseResult(CustomResponseObject result) {
		if(ClientController.isOnline() || (result.getType() == Type.LOGIN && result.isSuccessful()))
			TimeHandler.getInstance().setStartTime();
		
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
	protected abstract CustomResponseObject responseService(CustomRestTemplate restTemplate);
	
}