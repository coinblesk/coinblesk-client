package ch.uzh.csg.coinblesk.client.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by rvoellmy on 7/12/15.
 */
public class ConnectionCheck {

    /**
     * @param context
     * @return true if the client is connected to the internet
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager  = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
