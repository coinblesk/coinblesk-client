package ch.uzh.csg.mbps.client.util;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * AddressBook is responsible for handling usernames of known and trusted users.
 */
public class AddressBook {
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames stored
	 * in address book.
	 * 
	 * @param context
	 *            Application Context
	 * @return Set<String> with all usernames stored in address book.
	 */
	public static Set<String> getAddressBook(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Set<String> addressBook = new TreeSet<String>(new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return o1.toLowerCase().compareTo(o2.toLowerCase());
		    }
		});
		addressBook.addAll(preferences.getStringSet("addressBook", new TreeSet<String>()));
		return addressBook;
	}
	
	/**
	 * Adds an entry (username) to the address book of known users.
	 * 
	 * @param context Application Context
	 * @param username to store
	 */
	public static void addAddressBookEntry(Context context, String username){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();
		Set<String> addressBook = new TreeSet<String>(new Comparator<String>() {
			public int compare(String o1, String o2) {
		        return o1.toLowerCase().compareTo(o2.toLowerCase());
		    }
		});
		addressBook.addAll(preferences.getStringSet("addressBook", new TreeSet<String>()));
		addressBook.add(username);
		editor.putStringSet("addressBook", addressBook);
		editor.commit();
	}
	
	/**
	 * Returns an alphabetically ordered Set<String> with all usernames of trusted users stored
	 * in the trusted address book.
	 * 
	 * @param context
	 * @return Set<String> with all trusted usernames (alphabetically ordered)
	 */
	public static Set<String> getTrustedAddressBook(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Set<String> trustedAddressBook = new TreeSet<String>(new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return o1.toLowerCase().compareTo(o2.toLowerCase());
		    }
		});
		trustedAddressBook.addAll(preferences.getStringSet("trustedAddressBook", new TreeSet<String>()));
		return trustedAddressBook;
	}
	
	/**
	 * Adds an entry (username) to the address book of trusted users.
	 * 
	 * @param context (Application Context)
	 * @param username to store
	 */
	public static void addTrustedAddressBookEntry(Context context, String username){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();
		Set<String> trustedAddressBook = new TreeSet<String>(new Comparator<String>() {
			public int compare(String o1, String o2) {
		        return o1.toLowerCase().compareTo(o2.toLowerCase());
		    }
		});
		trustedAddressBook.addAll(preferences.getStringSet("trustedAddressBook", new TreeSet<String>()));
		trustedAddressBook.add(username);
		editor.putStringSet("trustedAddressBook", trustedAddressBook);
		editor.commit();
	}
	
	/**
	 * Returns boolean which indicates if a given username is saved in
	 * trustedAddressBook and therefore trusted (true) or not (false).
	 * 
	 * @param context
	 *            (Application Context)
	 * @param username to check
	 * @return boolean if username is trusted
	 */
	public static boolean isTrusted(Context context, String username){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Set<String> trustedAddressBook = new TreeSet<String>(new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return o1.toLowerCase().compareTo(o2.toLowerCase());
		    }
		});
		trustedAddressBook.addAll(preferences.getStringSet("trustedAddressBook", new TreeSet<String>()));
		boolean isTrusted = trustedAddressBook.contains(username);
		return isTrusted;
	}
}

