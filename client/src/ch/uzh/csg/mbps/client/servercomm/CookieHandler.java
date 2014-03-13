package ch.uzh.csg.mbps.client.servercomm;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import ch.uzh.csg.mbps.client.model.PayOutTransaction;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.PayOutRulesTransferObject;

/**
 * This class is responsible for storing the cookie return by the server in
 * order to launch future requests in the same session. It also returns
 * appropriate HTTPHeaders containing the header and the needed HTTPEntity.
 */
public class CookieHandler {
	private static final String COOKIE_STRING = "Cookie";
	private static final String JSESSIONID_STRING = "JSESSIONID=";
	private static final String SET_COOKIE_STRING = "Set-Cookie";
	
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
	
	/**
	 * Returns a HttpEntity with the cookie in the entity header. Returns null
	 * if no cookie is stored.
	 */
	@SuppressWarnings("rawtypes")
	public static HttpEntity getAuthHeader() {
		return getAuthHeader(null);
	}
	
	/**
	 * Returns a HttpEntity with the cookie in the entity header and a
	 * {@link UserAccount} object in the entity body. Returns null if no cookie
	 * is stored.
	 * 
	 * @param updatedAccount
	 *            the {@link UserAccount} object to be added to the HttpEntity
	 *            body
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HttpEntity getAuthHeader(UserAccount updatedAccount) {
		if (getCookie() == null || getCookie().equals(""))
			return null;
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add(COOKIE_STRING, JSESSIONID_STRING + getCookie());
		return new HttpEntity(updatedAccount, requestHeaders);
	}
	
	/**
	 * Returns a HttpEntity with the cookie in the entity header and a
	 * {@link PayOutTransaction} object in the entity body. Returns null if no
	 * cookie is stored.
	 * 
	 * @param pot
	 *            the {@link PayOutTransaction} object to be added to the
	 *            HttpEntity body
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HttpEntity getAuthHeaderPOT(PayOutTransaction pot) {
		if (getCookie() == null || getCookie().equals(""))
			return null;
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add(COOKIE_STRING, JSESSIONID_STRING + getCookie());
		return new HttpEntity(pot, requestHeaders);
	}
	
	/**
	 * Returns a HttpEntity with the cookie in the entity header and a
	 * {@link CreateTransactionTransferObject} object in the entity body.
	 * Returns null if no cookie is stored.
	 * 
	 * @param ctto
	 *            the {@link CreateTransactionTransferObject} object to be added
	 *            to the HttpEntity body
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HttpEntity getAuthHeaderCTTO(CreateTransactionTransferObject ctto) {
		if (getCookie() == null || getCookie().equals(""))
			return null;
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add(COOKIE_STRING, JSESSIONID_STRING + getCookie());
		return new HttpEntity(ctto, requestHeaders);
	}

	/**
	 * Returns a HttpEntity with the cookie in the entity header and a
	 * {@link PayOutRulesTransferObject} object in the entity body. Returns null
	 * if no cookie is stored.
	 * 
	 * @param porto
	 *            the {@link PayOutRulesTransferObject} object to be added to
	 *            the HttpEntity body
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HttpEntity getAuthHeaderPORTO(PayOutRulesTransferObject porto) {
		if (getCookie() == null || getCookie().equals(""))
			return null;

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add(COOKIE_STRING, JSESSIONID_STRING + getCookie());
		return new HttpEntity(porto, requestHeaders);
	}
	
}
