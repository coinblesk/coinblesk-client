package ch.uzh.csg.mbps.client.util;

import java.math.BigDecimal;


/**
 * Class for saving static constants of MBPS.
 */
public class Constants {
//	public static final String BASE_URI = "http://bitcoin.csg.uzh.ch/server";
	public static final String BASE_URI_SSL = "https://bitcoin.csg.uzh.ch/ws/server";
//	public static final String BASE_URI = "http://bitcoin-clone2.csg.uzh.ch/server";
//	public static final String BASE_URI_SSL = "http://bitcoin-clone2.csg.uzh.ch/server";
	
//	public static final String BASE_URI = "http://192.168.1.49:8080/server";
//	public static final String BASE_URI_SSL = "http://192.168.1.49:8080/server";
	
	/*
	 * The client version number helps to check if a version has been published
	 * in order to force the user to update the app. This assures that
	 * compatible versions are used. (see also client Constants.java)
	 */
	public static final int CLIENT_VERSION = 1;
	
	public static final long RESUME_TIMEOUT = 10 * 1000; 
	public static final long BUYER_ACK_TIMEOUT = 3 * 1000; // in ms
	public static final int CLIENT_READ_TIMEOUT = 5000; // in ms
	public static final int CLIENT_CONNECTION_TIMEOUT = 5000; // in ms
	public static final long CLIENT_SESSION_TIMEOUT = 10*60*1000; //10 minutes
	public static final long CLIENT_CHECK_TIME_INTERVAL = 15 * 1000;
	
	public static final String CONNECTION_ERROR = "Unable to resolve host";
	
	/*
	 * When a remote NFC device wants to talk to your service, it sends a
	 * so-called "SELECT AID" APDU as defined in the ISO/IEC 7816-4
	 * specification.
	 */
	public static final byte[] CLA_INS_P1_P2 = { 0x00, (byte) 0xA4, 0x04, 0x00 };
	public static final byte[] AID_MBPS = { (byte) 0xF0, (byte) 0xF0, 0x07, 0x77, (byte) 0xFF, 0x55, 0x11 };
	public static final byte[] AID_MBPS_RESUME = { (byte) 0xF0, (byte) 0xF0, 0x07, 0x77, (byte) 0xFF, 0x55, 0x12 };
	
	public static final byte[] READ_BINARY = new byte[]{0, (byte)0xb0,0,0,1};
	
	public static final String BTC = "-1";
	public static final String MILI_BTC = "0";
	public static final String MICRO_BTC = "1";
	
	public static final String USERNAME_PATTERN = "^[A-Za-z0-9_-]{3,25}$";
	public static final String TIME_SESSION_EXPIRED = "Time is expired";
	public static final String NO_COOKIE_STORED = "No Cookie stored!";
	
	//minimum value for payouts
	public static final String MIN_VALUE_PAYOUT = "0.0001";

	
	public static final int SCALE_CHF = 2;
	public static final int SCALE_BTC = 8;
	
	public static BigDecimal inputValueCalculator = BigDecimal.ZERO;
	public static String inputUnit = "CHF";
	
	//TODO simon: put to false when generating apk for play-store
	//shows (true) or hides (false) buttons specially created for shops etc.
	public static boolean IS_MENSA_MODE = false;

}
