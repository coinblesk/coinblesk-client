package ch.uzh.csg.coinblesk.client.ui.history;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.CurrencyFormatter;
import ch.uzh.csg.coinblesk.client.wallet.TransactionHistory;
import ch.uzh.csg.coinblesk.model.Transaction;

/**
 * Created by rvoellmy on 5/25/15.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.TransactionHolder> implements Filterable {

    private TransactionHistory txHistory;

    // Provide a suitable constructor (depends on the kind of dataset)
    public HistoryAdapter(TransactionHistory txHistory) {
        this.txHistory = txHistory;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TransactionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.card_payment, parent, false);

        return new TransactionHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(TransactionHolder holder, int position) {
        Preconditions.checkElementIndex(position, txHistory.getAllTransactions().size());

        Transaction tx = txHistory.getAllTransactions().get(position);

        String amount = CurrencyFormatter.formatBTC(tx.getAmount());
        String date = tx.getTimestamp().toString();
        String type = tx.getType().toString();

        holder.vAmount.setText(amount);
        holder.vDate.setText(date);
        holder.vType.setText(type);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return txHistory.getAllTransactions().size();
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    public static class TransactionHolder extends RecyclerView.ViewHolder {
        protected TextView vAmount;
        protected TextView vDate;
        protected TextView vType;

        public TransactionHolder(View v) {
            super(v);
            vAmount =  (TextView) v.findViewById(R.id.transaction_amount);
            vDate =  (TextView) v.findViewById(R.id.transaction_date);
            vType =  (TextView) v.findViewById(R.id.transaction_type);
        }
    }
}