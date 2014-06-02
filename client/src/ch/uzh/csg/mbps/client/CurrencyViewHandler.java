package ch.uzh.csg.mbps.client;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.CurrencyFormatter;

/**
 * The CurrencyViewHandler handles all aspects that includes currencies. The
 * format of currency represented in the view is handled. The different units of
 * bitcoins represented in the view are performed and the change from one
 * currency to another is also handled in this class.
 */
public class CurrencyViewHandler {
	
	/**
	 * The swiss currency is represented in decimal format with an precision of
	 * two digits. The CHF balance is achieved by converting a user's bitcoin
	 * balance using the given exchange rate. The view depends on the
	 * accessibility of the exchange rate. When exchange rate is not accessible
	 * the text view is not initialized.
	 * 
	 * @param textView
	 *            The TextView for CHF currency amount in the activity.
	 * @param exchangeRate
	 *            The rate of one bitcoin in relation to CHF.
	 * @param amountBtc
	 *            The user's bitcoin balance.
	 */
	public static void setToCHF(TextView textView, BigDecimal exchangeRate, BigDecimal amountBtc) {
		BigDecimal chf = amountBtc.multiply(exchangeRate);
		textView.setText(CurrencyFormatter.formatChf(chf) + " CHF");
	}
	
	/**
	 * The swiss currency is represented in decimal format with an precision of
	 * two digits. Returns a string to represent a bitcoin amount in CHF.
	 * 
	 * @param exchangeRate The rate of one bitcoin in relation to CHF.
	 * @param amountBtc  The amount to transform to CHF.
	 * @return the given bitcoin amount represented in CHF.
	 */
	public static String amountInCHF(BigDecimal exchangeRate, BigDecimal amountBtc) {
		BigDecimal chf = amountBtc.multiply(exchangeRate);
		return CurrencyFormatter.formatChf(chf) + " CHF";
	}
	
	/**
	 * The bitcoin balance of a user is represented in decimal format with an
	 * precision of eight digits. The bitcoin balance can be shown in three
	 * different units.
	 * 
	 * @param textView
	 *            The TextView for bitcoin amount in the activity.
	 * @param amountBtc
	 *            The user's bitcoin balance.
	 * @param context
	 *            Needed to get the predefined bitcoin unit from the shared
	 *            preferences.
	 */
	public static void setBTC(TextView textView, BigDecimal amountBtc, Context context) {
		BigDecimal amount = getBTCAmountInDefinedUnit(amountBtc, context);
		textView.setText(CurrencyFormatter.formatBTC(amount) + " " + getBitcoinUnit(context));
	}
	
	/**
	 * Converts the the entered amount from CHF to bitcoin.
	 * 
	 * @param exchangeRate
	 *            The rate of one bitcoin in relation to swiss currency.
	 * @param amountChf
	 *            The entered amount in CHF
	 * @return Returns the value of the amount in bitcoins.
	 */
	public static BigDecimal getBitcoinExchangeValue(BigDecimal exchangeRate, BigDecimal amountChf){
		if(exchangeRate.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;

		MathContext mc = new MathContext(25);
		return amountChf.divide(exchangeRate, mc).setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP);
	}
	
	/**
	 * Gets the currency description of the bitcoin unit (milli, micro, etc.).
	 * 
	 * @param context
	 *            Needed to get the predefined bitcoin unit from the shared
	 *            preferences.
	 * @return String of the predefined bitcoin unit.
	 */
	public static String getBitcoinUnit(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String bitcoinUnit = settings.getString("bitcoin_list", "");

		if (bitcoinUnit.equals(Constants.MICRO_BTC)) {
			return "μBTC";
		} else if (bitcoinUnit.equals(Constants.MILI_BTC)) {
			return "mBTC";
		} else {
			return "BTC";
		}
	}
	
	/**
	 * Returns the BTC value, by taking into consideration any given predifined
	 * unit like milli or micro BTC.
	 * 
	 * @param amountBtc
	 *            entered bitcoin value
	 * @param contexts
	 *            Needed to get the predefined bitcoin unit from the shared
	 *            preferences.
	 * @return the plain BTC value without any transformation
	 */
	public static BigDecimal getBitcoinsRespectingUnit(BigDecimal amountBtc, Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String bitcoinUnit = settings.getString("bitcoin_list", "");
		if (bitcoinUnit.equals(Constants.MICRO_BTC)) {
			return amountBtc.divide(new BigDecimal("1000000"));
		} else if (bitcoinUnit.equals(Constants.MILI_BTC)) {
			return amountBtc.divide(new BigDecimal("1000"));
		} else {
			return amountBtc;
		}
	}

	/**
	 * Sets the value of the TextView to the given exchange Rate. It calculates
	 * the amount of CHF in proportion to one BTC.
	 * 
	 * @param exchangeRate
	 *            The rate of one bitcoin in relation to CHF
	 * @param display
	 *            The TextView in the activity which represents the exchange
	 *            rate.
	 */
	public static void setExchangeRateView(BigDecimal exchangeRate, TextView display) {
		display.setText("1 BTC = " + CurrencyFormatter.formatChf(exchangeRate) + " CHF");
	}
	
	/**
	 * The text string in the view is overriden by a blank string.
	 *
	 * @param 	view The TextView in which the description is
	 * 			deleted. 
	 */
	public static void clearTextView(TextView view){
		view.setText("");
	}
	
	/**
	 * Returns the amount of BTC in the corresponding defined bitcoin unit.
	 * 
	 * @param amountBtc in Bitcoin
	 * @param context Application context.
	 * @return amount BTC converted to the unit defined in usersettings
	 */
	public static BigDecimal getBTCAmountInDefinedUnit(BigDecimal amountBtc, Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String bitcoinUnit = settings.getString("bitcoin_list", "");
		BigDecimal factor;
		if (bitcoinUnit.equals(Constants.MICRO_BTC)) {
			factor = new BigDecimal("1000000");
		} else if (bitcoinUnit.equals(Constants.MILI_BTC)) {
			factor = new BigDecimal("1000");
		} else {
			factor = new BigDecimal("1");
		}
		BigDecimal amount = factor.multiply(amountBtc).setScale(Constants.SCALE_BTC, RoundingMode.HALF_UP);
		return amount;
	}
	
}
