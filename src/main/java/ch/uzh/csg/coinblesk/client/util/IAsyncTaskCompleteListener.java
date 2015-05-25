package ch.uzh.csg.coinblesk.client.util;

/**
 * This interface is used to return a response to / to notify a caller once an
 * asynchronous task is completed.
 * 
 * @param <T> generic placeholder for the returned type
 */
public interface IAsyncTaskCompleteListener<T> {
	/**
	 * Handles response form server after http server call.
	 */
	public void onTaskComplete(T response);

}
