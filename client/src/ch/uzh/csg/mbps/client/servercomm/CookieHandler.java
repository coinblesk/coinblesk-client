package ch.uzh.csg.mbps.client.servercomm;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is responsible for storing the cookie return by the server in
 * order to launch future requests in the same session. It also returns
 * appropriate HTTPHeaders containing the header and the needed HTTPEntity.
 */
public class CookieHandler {
	public static final String COOKIE_STRING = "Cookie";
	public static final String JSESSIONID_STRING = "JSESSIONID=";
	public static final String SET_COOKIE_STRING = "Set-Cookie";
	
	private static String COOKIE;
	
	/**
	 * Returns the cookie.
	 * 
	 * @return the cookie, might return null
	 */
	public static String getCookie() {
		return COOKIE;
	}
	
	/**
	 * Stores the cookie temporarily.
	 * 
	 * @param newCookie
	 *            the cookie to be stored
	 */
	public static void setCookie(String newCookie) {
		COOKIE = newCookie;
	}

	/**
	 * Stores the cookie which is contained in the header of the server
	 * response. If the set does not contain a cookie entry, nothing is done.
	 * 
	 * @param entrySet
	 *            the entry set
	 */
	public static void storeCookie(Set<Entry<String, List<String>>> entrySet) {
		for (Entry<String, List<String>> entry : entrySet) {
			if (entry.getKey().equals(SET_COOKIE_STRING)) {
				String value = entry.getValue().get(0);
				CookieHandler.setCookie(value.substring(value.indexOf("=")+1, value.indexOf(";")));
			}
		}
	}
	
	/**
	 * Sets the stored cookie to the empty string.
	 */
	public static void deleteCookie(){
		setCookie("");
	}
	
}
