package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

public class PersistenceFactory {
    
    public static PersistentStorage getCloudStorage(Context context) {
        return new CoinBleskCloudData(context);
    }
}
