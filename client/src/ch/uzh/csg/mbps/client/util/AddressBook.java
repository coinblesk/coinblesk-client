package ch.uzh.csg.mbps.client.util;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class stores the user information as long as the user is logged in. The
 * status if the user is online mode is retrieved from this class.
 */
public class AddressBook {
	
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

