package ch.uzh.csg.coinblesk.client.ui.history;

import android.widget.Filter;

import com.google.common.collect.Lists;

import java.util.List;

import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * Created by rvoellmy on 5/25/15.
 */
public class PaymentFilter extends Filter {

    private final List<TransactionMetaData> originalTransactions;
    private final List<TransactionMetaData> filteredTransactions;

    public PaymentFilter(HistoryAdapter adapter, List<TransactionMetaData> originalTransactions) {
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
