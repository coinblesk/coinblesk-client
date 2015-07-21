package ch.uzh.csg.coinblesk.client.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SyncProgress {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncProgress.class);

    public interface SyncProgressFinishedListener {
        void onFinished();
    }

    private double percentage;

    private boolean started;
    private boolean finished;

    private List<SyncProgressFinishedListener> listeners = new LinkedList<>();

    public SyncProgress() {
        this.percentage = -1;
        this.finished = false;
    }

    public void setProgress(double percentage) {
        this.started = true;
        this.percentage = percentage;
    }

    public void setFinished() {
        this.started = true;
        this.finished = true;
        notifyListeners();
    }

    public boolean isFinished() {
        return finished;
    }

    public void addSyncCompleteListener(SyncProgressFinishedListener listener) {
        listeners.add(listener);
        if(isFinished()) {
            listener.onFinished();
        }
    }

    private void notifyListeners() {
        for(SyncProgressFinishedListener listener : listeners) {
            listener.onFinished();
        }
    }

    /**
     * TODO: more detailed explanation
     * @return the relative blockchain download progress.
     */
    public double getProgress() {

        if(finished) {
            return 1d;
        }

        if (!started) {
            LOGGER.warn("get progress called too early!");
            return -1d;
        }

        return percentage;
    }
    
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%,.1f%%", getProgress());
    }

}
