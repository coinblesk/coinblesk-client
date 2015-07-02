package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

public class PersistenceFactory {
    
    public static PersistentData getCloudStorage(Context context) {
        return new CoinBleskCloudData(context);
    }
}
