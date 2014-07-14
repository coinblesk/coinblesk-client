package ch.uzh.csg.mbps.client.util;

import android.content.Context;
import ch.uzh.csg.mbps.client.internalstorage.InternalStorageHandler;
import ch.uzh.csg.mbps.client.internalstorage.WrongPasswordException;

/**
 * This class stores the user information as long as the user is logged in. It
 * also provides the information if the user is in only mode or not.
 * Furthermore, it holds the reference to the {@link InternalStorageHandler}.
 */
public class ClientController {
	private static boolean isOnline = false;
	private static InternalStorageHandler internalStorageHandler;
	
	/**
	 * Returns the {@link InternalStorageHandler}.
	 */
	public static InternalStorageHandler getStorageHandler() {
		return internalStorageHandler;
	}
	
	public static void setOnlineMode(boolean mode){
		isOnline = mode;
	}
	
	public static boolean isOnline(){
		return isOnline;
	}
	
	/**
	 * Initializes the {@link ClientController} in order to be able to read and
	 * write to the local storage.
	 * 
	 * @param context
	 *            the application's context
	 * @param username
	 *            the username of the authenticated user (needed to create a so
	 *            named xml file)
	 * @param password
	 *            the password of the authenticaed user (needed to encrypt the
	 *            file in on the local storage)
	 * @return true if the existing xml could be read or a new xml file could be
	 *         created, false if there was an exception thrown in
	 *         reading/writing/encoding/decoding the xml
	 * @throws WrongPasswordException
	 *             if the password provided does not match the password of the
	 *             given username
	 */
	public static boolean init(Context context, String username, String password) throws WrongPasswordException {
		internalStorageHandler = new InternalStorageHandler(context, username, password);
		try {
			return internalStorageHandler.init();
		} catch (Exception e) {
			if (e instanceof WrongPasswordException) {
				throw new WrongPasswordException(e.getMessage());
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * Deletes the temporary information stored in this class.
	 */
	public static void clear(){
		isOnline = false;
		internalStorageHandler = null;
	}

}
