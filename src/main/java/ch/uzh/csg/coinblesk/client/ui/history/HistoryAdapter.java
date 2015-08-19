package ch.uzh.csg.coinblesk.client.ui.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CoinBleskApplication;
import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.client.wallet.TransactionHistory;
import ch.uzh.csg.coinblesk.responseobject.ExchangeRateTransferObject;

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
    public void onBindViewHolder(final TransactionHolder holder, int position) {
        Preconditions.checkElementIndex(position, txHistory.getAllTransactions().size());

        final TransactionMetaData tx = txHistory.getAllTransactions().get(position);

        CurrencyViewHandler.setBTC(holder.vAmount, tx.getAmount(), context);
        String time = DateUtils.getRelativeDateTimeString(context, tx.getTimestamp().getTime(), DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString();
        holder.vDate.setText(time);

        Drawable icon;
        final String description;
        final String btcAmount = CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context);

        switch (tx.getType()) {
            case PAY_OUT:
            case COINBLESK_PAY_OUT:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.darkred));
                icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pay_out, null);
                description = String.format(context.getText(R.string.transaction_pay_out).toString(), btcAmount, tx.getReceiver());
                break;
            case PAY_IN:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.green));
                icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pay_in, null);
                description = String.format(context.getText(R.string.transaction_pay_in).toString(), btcAmount);
                break;
            case PAY_IN_UNVERIFIED:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.green));
                icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pay_in_un, null);
                description = String.format(context.getText(R.string.transaction_pay_in_unverified).toString(), btcAmount, tx.getConfirmations());
                break;
            case COINBLESK_PAY_IN:
                holder.vAmount.setTextColor(context.getResources().getColor(R.color.darkgreen));
                icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pay_in, null);
                description = String.format(context.getText(R.string.transaction_coinblesk_instant_pay_in).toString(), btcAmount, tx.getSender());
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type " + tx.getType());
        }

        // set icon on the right of the card
        holder.vAmount.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
        holder.vDescription.setText(description);

        // set description with CHF amounts...
        ((CoinBleskApplication) context.getApplicationContext()).getMerchantModeManager().getExchangeRate(new RequestCompleteListener<ExchangeRateTransferObject>() {

            class DescriptionAndAmount {
                public String description;
                public String amount;
            }

            @Override
            public void onTaskComplete(final ExchangeRateTransferObject response) {

                if (!response.isSuccessful()) {
                    return;
                }

                AsyncTask<TransactionHolder, Void, DescriptionAndAmount> task = new AsyncTask<TransactionHolder, Void, DescriptionAndAmount>() {

                    private TransactionHolder v;

                    @Override
                    protected DescriptionAndAmount doInBackground(TransactionHolder[] params) {

                        v = params[0];
                        DescriptionAndAmount descAndAmount = new DescriptionAndAmount();

                        if (response.isSuccessful()) {
                            BigDecimal exchangeRate = new BigDecimal(response.getExchangeRate(Constants.CURRENCY));
                            descAndAmount.amount = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, tx.getAmount(), context);
                            switch (tx.getType()) {
                                case PAY_OUT:
                                    descAndAmount.description = String.format(context.getText(R.string.transaction_pay_out_receiver_unknown).toString(), btcAmount);
                                    break;
                                case COINBLESK_PAY_OUT:
                                    descAndAmount.description = String.format(context.getText(R.string.transaction_pay_out).toString(), descAndAmount.amount, tx.getReceiver());
                                    break;
                                case PAY_IN:
                                    descAndAmount.description = String.format(context.getText(R.string.transaction_pay_in).toString(), descAndAmount.amount);
                                    break;
                                case PAY_IN_UNVERIFIED:
                                    descAndAmount.description = String.format(context.getText(R.string.transaction_pay_in_unverified).toString(), descAndAmount.amount, tx.getConfirmations());
                                    break;
                                case COINBLESK_PAY_IN:
                                    descAndAmount.description = String.format(context.getText(R.string.transaction_coinblesk_instant_pay_in).toString(), descAndAmount.amount, tx.getSender());
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown transaction type " + tx.getType());
                            }
                        }

                        return descAndAmount;
                    }

                    @Override
                    protected void onPostExecute(DescriptionAndAmount btcAndChfAmount) {
                        v.vDescription.setText(btcAndChfAmount.description);
                        v.vAmount.setText(btcAndChfAmount.amount);
                    }
                };

                task.execute(holder);
            }
        });

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
        protected TextView vDescription;
        protected TextView vDate;

        public TransactionHolder(View v) {
            super(v);
            vAmount = (TextView) v.findViewById(R.id.transaction_amount);
            vDescription = (TextView) v.findViewById(R.id.transaction_description);
            vDate = (TextView) v.findViewById(R.id.transaction_date);
        }
    }
}