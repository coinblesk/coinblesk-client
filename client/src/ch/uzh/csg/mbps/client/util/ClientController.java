package ch.uzh.csg.mbps.client.util;

import java.math.BigDecimal;

import android.content.Context;
import ch.uzh.csg.mbps.model.Transaction;
import ch.uzh.csg.mbps.model.UserAccount;

/**
 * This class stores the user information as long as the user is logged in. The
 * status if the user is online mode is retrieved from this class.
 */
public class ClientController {
	private final static String SERVERPKFILENAME = "serverPK.txt";
	private static String serverPublicKey;
	private static UserAccount user;
	private static String filename;
	private static boolean isOnline = false;
	
	public static UserAccount getUser(){
		return user;
	}
	
	/**
	 * The retrieved user information from the server or from the internal
	 * storage is stored.
	 * 
	 * @param password
	 *            The plain password as string.
	 * @param username
	 *            The username as string.
	 * @param newUser
	 *            The user informations.
	 * @param context
	 *            The context of the current view (activity).
	 */
	public static void setUser(String password, String username, UserAccount newUser, Context context){
		user = newUser;
		
		if(user != null){
			if (username != null)
				user.setUsername(username);

			if (password != null)
				user.setPassword(password);
		}
		// The user informations are stored into the internal storage
		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
	}
	
	public static String getFilename(){
		return filename;
	}
	
	public static void setFielname(String name){
		filename = name;
	}
	
	public static String getServerPublicKeyFilename(){
		return SERVERPKFILENAME;
	}
	
	public static void setServerPublicKey(String key){
		serverPublicKey = key;
	}
	
	public static String getServerPublicKey(){
		return serverPublicKey;
	}
	
	public static void setOnlineMode(boolean mode){
		isOnline = mode;
	}
	
	public static boolean isOnline(){
		return isOnline;
	}
	
	/**
	 * Deletes the temporary information stored in this class.
	 */
	public static void clear(){
		user = null;
		filename  = null;
		isOnline = false;
		serverPublicKey = null;
	}

	/**
	 * The balance of the buyer and seller is updated after a transaction is
	 * accomplished successfully.
	 * 
	 * @param isSeller
	 *            if the user is seller.
	 * @param tx
	 *            The transaction information's from the accomplished
	 *            transaction.
	 * @param context
	 *            The context of the current view (activity).
	 */
	public static void updateUserAfterTransaction(boolean isSeller, Transaction tx, Context context) {
		user.setTransactionNumber(user.getTransactionNumber()+1);
		if (isSeller)
			user.setBalance(user.getBalance().add(tx.getAmount()));
		else
			user.setBalance(user.getBalance().subtract(tx.getAmount()));
		
		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
	}

	public static void setUserPassword(String password, Context context) {
		user.setPassword(password);
		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
	}

	public static void setUserEmail(String saveEmail, Context context) {
		user.setEmail(saveEmail);
		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
	}

	public static void setUserBalance(BigDecimal balance, Context context) {
		user.setBalance(balance);
		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
	}
	
}
