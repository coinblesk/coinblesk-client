package ch.uzh.csg.coinblesk.client.storage;

/**
 * Created by draft on 19.08.15.
 */
public interface StorageHandlerCallback {
    void storageHandlerSet(StorageHandler storageHandler);

    void failed();
}
