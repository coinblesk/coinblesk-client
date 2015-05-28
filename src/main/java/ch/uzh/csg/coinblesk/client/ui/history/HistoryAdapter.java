package ch.uzh.csg.coinblesk.client.ui.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.formatter.DateFormatter;
import ch.uzh.csg.coinblesk.client.wallet.TransactionHistory;
import ch.uzh.csg.coinblesk.model.Transaction;

/**
 * Created by rvoellmy on 5/25/15.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.TransactionHolder> implements Filterable {

    private TransactionHistory txHistory;
    private Context context;

    // Provide a suitable constructor (depends on the kind of dataset)
    public HistoryAdapter(Context context, TransactionHistory txHistory) {
        this.txHistory = txHistory;
        this.context = context;
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

        CurrencyViewHandler.setBTC(holder.vAmount, tx.getAmount(), context);
        String date = DateFormatter.formatDate(tx.getTimestamp());

        holder.vDate.setText(date);

        Drawable icon;

        switch(tx.getType()) {
            case PAY_OUT:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.darkred));
                icon = context.getResources().getDrawable(R.drawable.ic_pay_out, null);
                break;
            case PAY_IN:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.darkgreen));
                icon = context.getResources().getDrawable(R.drawable.ic_pay_in, null);
                break;
            case PAY_IN_UNVERIFIED:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.green));
                icon = context.getResources().getDrawable(R.drawable.ic_pay_in_un, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type " + tx.getType());
        }

        // set icon on the right of the card
        holder.vAmount.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
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

        public TransactionHolder(View v) {
            super(v);
            vAmount =  (TextView) v.findViewById(R.id.transaction_amount);
            vDate =  (TextView) v.findViewById(R.id.transaction_date);
        }
    }
}