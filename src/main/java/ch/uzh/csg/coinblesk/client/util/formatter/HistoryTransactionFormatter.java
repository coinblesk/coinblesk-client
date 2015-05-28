package ch.uzh.csg.coinblesk.client.util.formatter;

import android.content.Context;

import ch.uzh.csg.coinblesk.client.CurrencyViewHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.model.Transaction;

public class HistoryTransactionFormatter {

	/**
	 * Returns a String representation of the HistoryTransaction depending on
	 * its type in the proper language.
	 * 
	 * @param tx which shall be formatted
	 * @param context ApplicationContext to access String ressources
	 * @return String representation of HistoryTransaction
	 */
	public static String formatHistoryTransaction(Transaction tx, Context context){
		StringBuilder sb = new StringBuilder();
		sb.append(DateFormatter.formatDate(tx.getTimestamp()));
		sb.append("\n");
		switch(tx.getType()) {
			case PAY_IN:
				sb.append(context.getResources().getString(R.string.history_payIn) + " \n");
				break;
			case PAY_IN_UNVERIFIED:
				sb.append(context.getResources().getString(R.string.history_payInUn) + " \n");
				break;
			case PAY_OUT:
				sb.append(context.getResources().getString(R.string.history_payOut) + " \n");
				break;
		}

		sb.append(CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context));
		return sb.toString();
	}
	
}
