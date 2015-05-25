package ch.uzh.csg.coinblesk.client.ui.history;

import android.widget.Filter;

import com.google.common.collect.Lists;

import java.util.List;

import ch.uzh.csg.coinblesk.model.Transaction;

/**
 * Created by rvoellmy on 5/25/15.
 */
public class PaymentFilter extends Filter {

    private final List<Transaction> originalTransactions;
    private final List<Transaction> filteredTransactions;

    public PaymentFilter(HistoryAdapter adapter, List<Transaction> originalTransactions) {
        this.originalTransactions = originalTransactions;
        this.filteredTransactions = Lists.newArrayListWithExpectedSize(originalTransactions.size());
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        return null;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {

    }
}
