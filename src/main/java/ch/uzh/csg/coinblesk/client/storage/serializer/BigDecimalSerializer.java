package ch.uzh.csg.coinblesk.client.storage.serializer;

import com.activeandroid.serializer.TypeSerializer;

import java.math.BigDecimal;

/**
 * Created by rvoellmy on 8/1/15.
 */
public class BigDecimalSerializer extends TypeSerializer {

    @Override
    public Class<?> getDeserializedType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return String.class;
    }

    @Override
    public String serialize(Object o) {
        if(o == null) {
            return null;
        }

        return o.toString();
    }

    @Override
    public BigDecimal deserialize(Object o) {
        if(o == null) {
            return null;
        }

        return new BigDecimal(o.toString());
    }
}
