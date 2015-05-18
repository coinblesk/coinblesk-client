package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

public class PersistenceFactory {
    
    @Deprecated
    public static PersistentData getXMLStorage(String username, String password, Context context) {
        return new InternalXMLData(username, password, context.getFilesDir());
    }
    public static PersistentData getCloudStorage(String username, String password, Context context) {
        return new CoinBleskCloudData(username, password, context);
    }
}
