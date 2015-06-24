package ch.uzh.csg.coinblesk.client.persistence;

import android.content.Context;

public class PersistenceFactory {
    
    public static PersistentData getCloudStorage(String username, String password, Context context) {
        return new CoinBleskCloudData(username, password, context);
    }
}