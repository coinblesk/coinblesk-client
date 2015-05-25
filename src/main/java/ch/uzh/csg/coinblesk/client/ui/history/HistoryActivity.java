package ch.uzh.csg.coinblesk.client.ui.history;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;

/**
 * This class is a UI class, showing the history of transactions of the
 * authenticated user.
 */
public class HistoryActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        RecyclerView recList = (RecyclerView) findViewById(R.id.recent_transactions_recycler_view);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
    }
}
