package ch.uzh.csg.coinblesk.client.ui.history;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.WalletActivity;
import ch.uzh.csg.coinblesk.client.wallet.TransactionHistory;

/**
 * This class is a UI class, showing the history of transactions of the
 * authenticated user.
 */
public class HistoryActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryActivity.class);

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        super.onServiceConnected(name, service);

        TransactionHistory txHistory = getWalletService().getTransactionHistory();

        // specify an adapter (see also next example)
        mAdapter = new HistoryAdapter(txHistory);
        mRecyclerView.setAdapter(mAdapter);

        dismissProgressDialog();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mRecyclerView = (RecyclerView) findViewById(R.id.recent_transactions_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        showLoadingProgressDialog();
    }
}
