package ch.uzh.csg.coinblesk.client.util.formatter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

public class HistoryTransactionFormatter {

    /**
     * Returns a String representation of the HistoryTransaction depending on
     * its type in the proper language.
     *
     * @param tx      which shall be formatted
     * @param context ApplicationContext to access String ressources
     * @return String representation of HistoryTransaction
     */
    public static String formatHistoryTransaction(TransactionMetaData tx, Context context) {
        return formatHistoryTransaction(tx, null, context);
    }

    /**
     * Returns a String representation of the HistoryTransaction depending on
     * its type in the proper language.
     * This method also enhances the bitcoin amounts with the according amount in the default currency (eg CHF)
     *
     * @param tx           which shall be formatted
     * @param exchangeRate the current exchange rate of 1 BTC. If null, only the Bitcoin amount is displayed.
     * @param context      ApplicationContext to access String ressources
     * @return String representation of HistoryTransaction
     */
    public static String formatHistoryTransaction(TransactionMetaData tx, @Nullable BigDecimal exchangeRate, Context context) {
        StringBuilder sb = new StringBuilder();

        String description;
        String btcAmount;

        if (exchangeRate != null) {
            btcAmount = CurrencyViewHandler.getAmountInCHFandBTC(exchangeRate, tx.getAmount(), context);
        } else {
            btcAmount = CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context);
        }

        switch (tx.getType()) {
            case PAY_OUT:
            case COINBLESK_PAY_OUT:
                description = String.format(context.getText(R.string.transaction_pay_out).toString(), btcAmount, tx.getReceiver());
                break;
            case PAY_IN:
                description = String.format(context.getText(R.string.transaction_pay_in).toString(), btcAmount);
                break;
            case PAY_IN_UNVERIFIED:
                description = String.format(context.getText(R.string.transaction_pay_in_unverified).toString(), btcAmount, tx.getConfirmations());
                break;
            case COINBLESK_PAY_IN:
                description = String.format(context.getText(R.string.transaction_coinblesk_instant_pay_in).toString(), btcAmount, tx.getSender());
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type " + tx.getType());
        }

        sb.append(btcAmount);
        sb.append("\n");
        sb.append(description);
        sb.append("\n");
        sb.append(DateUtils.getRelativeDateTimeString(context, tx.getTimestamp().getTime(), DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString());

        return sb.toString();
    }

}
