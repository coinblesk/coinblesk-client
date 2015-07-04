package ch.uzh.csg.coinblesk.client.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.persistence.PersistentStorageHandler;
import ch.uzh.csg.coinblesk.client.persistence.WrongPasswordException;

/**
 * This class stores the user information as long as the user is logged in. It
 * also provides the information if the user is in only mode or not.
 * Furthermore, it holds the reference to the {@link PersistentStorageHandler}.
 */
public class ClientController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClientController.class);

    private static boolean isConnectedToServer = false;
    private static PersistentStorageHandler internalStorageHandler;

    /**
     * Returns the {@link PersistentStorageHandler}.
     */
    public static PersistentStorageHandler getStorageHandler() {
        return internalStorageHandler;
    }

    public static void setOnlineMode(boolean mode) {
        isConnectedToServer = mode;
    }

    public static boolean isConnectedToServer() {
        return isConnectedToServer;
    }

    /**
     * @param context
     * @return true if the client is connected to the internet
     */
    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Initializes the {@link ClientController} in order to be able to read and
     * write to the local storage.
     *
     * @param context  the application's context
     * @return true if the existing xml could be read or a new xml file could be
     * created, false if there was an exception thrown in
     * reading/writing/encoding/decoding the xml
     * @throws WrongPasswordException if the password provided does not match the password of the
     *                                given username
     */
    public static boolean init(Context context) {
        internalStorageHandler = new PersistentStorageHandler(context);
        return true;
    }

    /**
     * Deletes the temporary information stored in this class.
     */
    public static void clear() {
        isConnectedToServer = false;
        internalStorageHandler = null;
    }

}
