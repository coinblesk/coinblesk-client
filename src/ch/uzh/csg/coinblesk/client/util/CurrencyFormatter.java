package ch.uzh.csg.coinblesk.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class CurrencyFormatter {

	private static DecimalFormat DisplayFormatCHF = new DecimalFormat("#.##");
	private static DecimalFormat DisplayFormatBTC = new DecimalFormat("#.########");
	
	/**
	 * Matches the format of the value to bitcoins. The precision is rounded to
	 * eight digits.
	 * 
	 * @param value
	 *            The amount in bitcoins.
	 * @return Returns the value with the correct format.
	 */
	public static String formatBTC(BigDecimal value) {
		if (value == null)
			return "";
		else
			return DisplayFormatBTC.format(value);
	}
	
	/**
	 * Matches the format of the value to CHF. The precision is rounded to two
	 * digits.
	 * 
	 * @param value
	 *            The amount in CHF.
	 * @return Returns the value with the correct format.
	 */
	public static String formatChf(BigDecimal value) {
		if (value == null)
			return "";
		else
			return DisplayFormatCHF.format(value);
	}
	
	/**
	 * Returns the bictoin amount in the correct format.
	 * 
	 * @param amount
	 *            The bitcoin amount in string format.
	 * @return Returns Bigdecimal.
	 */
	public static BigDecimal getBigDecimalBtc(String amount){
		BigDecimal btc = new BigDecimal(amount);
		btc = btc.setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP);
		return btc;
	}

	/**
	 * Returns the CHF amount in the correct format.
	 * 
	 * @param amount
	 *            The CHF amount in string format.
	 * @return Returns BigDecimal.
	 */
	public static BigDecimal getBigDecimalChf(String amount){
		BigDecimal chf = new BigDecimal(amount);
		chf = chf.setScale(Constants.SCALE_CHF, RoundingMode.HALF_UP);
		return chf;
	}
	
}
