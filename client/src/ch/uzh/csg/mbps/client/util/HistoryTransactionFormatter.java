package ch.uzh.csg.mbps.client.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Context;
import ch.uzh.csg.mbps.client.CurrencyViewHandler;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.model.AbstractHistory;
import ch.uzh.csg.mbps.model.HistoryPayInTransaction;
import ch.uzh.csg.mbps.model.HistoryPayOutTransaction;
import ch.uzh.csg.mbps.model.HistoryTransaction;

public class HistoryTransactionFormatter {
	private static  SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy' 'HH:mm:ss", Locale.getDefault());

	public static String formatHistoryTransaction(AbstractHistory tx, Context context){
		//TODO simon: check
//		sdf.setTimeZone(TimeZone.getDefault());

		if(tx instanceof HistoryPayInTransaction)
			return formatHistoryPayInTransaction((HistoryPayInTransaction) tx, context);
		else if (tx instanceof HistoryPayOutTransaction)
			return formatHistoryPayOuTransaction((HistoryPayOutTransaction) tx, context);
		else
			return formatHistoryNormalTransaction((HistoryTransaction) tx, context);
	}
	
	private static String formatHistoryPayInTransaction(HistoryPayInTransaction tx, Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append(sdf.format(tx.getTimestamp()));
		sb.append("\n");
		sb.append(context.getResources().getString(R.string.history_payIn) + " \n");
		sb.append(CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context));
		return sb.toString();
	}
	
	private static String formatHistoryPayOuTransaction(HistoryPayOutTransaction tx, Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append(sdf.format(tx.getTimestamp()));
		sb.append("\n");
		sb.append(context.getResources().getString(R.string.history_payOut) + " ");
		sb.append(CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context));
		sb.append(" " + context.getResources().getString(R.string.history_payOutTo) + " ");
		sb.append(tx.getBtcAddress());
		return sb.toString();
		
	}
	
	private static String formatHistoryNormalTransaction(HistoryTransaction tx, Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append(sdf.format(tx.getTimestamp()));
		sb.append("\n");
		sb.append(context.getResources().getString(R.string.history_from) + " ");
		sb.append(tx.getBuyer());
		sb.append(", " + context.getResources().getString(R.string.history_to) + " ");
		sb.append(tx.getSeller());
		sb.append(", " + context.getResources().getString(R.string.history_amount) + " ");
		sb.append(CurrencyViewHandler.formatBTCAsString(tx.getAmount(), context));
		return sb.toString();
	}
}
