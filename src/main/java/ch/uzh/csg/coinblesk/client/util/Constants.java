package ch.uzh.csg.coinblesk.client.util;

import java.math.BigDecimal;

import ch.uzh.csg.coinblesk.Currency;


/**
 * Class for saving static constants of MBPS.
 */
public class Constants {

	/**
	 * The base URL of the server.
	 */
	public static final String BASE_URL = "http://192.168.1.209:8080/coinblesk-server";

	/**
	 * Amount of confirmations a transaction needs to be considered as confirmed
	 */
	public static final int MIN_CONFIRMATIONS = 4;
	
	public static final String BTC = "-1";
	public static final String MILI_BTC = "0";
	public static final String MICRO_BTC = "1";

	/**
	 * Decimal points of CHF amounts
	 */
	public static final int SCALE_CHF = 2;

	/**
	 * Decimal points of BTC amounts
	 */
	public static final int SCALE_BTC = 8;


	public static BigDecimal inputValueCalculator = BigDecimal.ZERO;
	public static String inputUnit = "CHF";
	public static Currency CURRENCY = Currency.CHF;
	
	//TODO: Mensa Test Run Method
	//shows (true) or hides (false) buttons specially created for shops etc.
	public static boolean IS_MENSA_MODE = false;

	// Date format
	public static String DATE_FORMAT = "dd-MM-yyyy' 'HH:mm:ss";

	/**
	 * Determines the earlies creation date of a coin blesk backup phrase (mnemonic).
	 * This will speed up blockchain synchronization significantly, as we don't have
	 * to download block earlier than this date.
	 */
	public static long EARLIEST_COINBLESK_KEY = 1427846400;

	public static final BigDecimal DEFAULT_FEE = new BigDecimal("0.0001");

}
