package ch.uzh.csg.coinblesk.client.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SyncProgress {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncProgress.class);

    private int totalBlockToDownload;
    private int blocksRemaining;

    private boolean started;
    private boolean finished;

    public SyncProgress() {
        this.totalBlockToDownload = -1;
        this.blocksRemaining = Integer.MAX_VALUE;
        this.finished = false;
    }

    public void setBlocksRemaining(int blocksRemaining) {
        this.blocksRemaining = blocksRemaining;
        this.started = true;
    }

    public boolean hasStarted() {
        return started;
    }

    public void setFinished() {
        this.started = true;
        this.finished = true;
    }

    public boolean isFinished() {
        return blocksRemaining == 0 || finished;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlockToDownload = totalBlocks;
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

        // minimum progress == 0
        return Math.max(1d - ((double) blocksRemaining) / totalBlockToDownload, 0);
    }
    
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%,.1f%%", getProgress());
    }

}
