package ch.uzh.csg.mbps.client.util;

import java.math.BigDecimal;

import android.content.Context;
import ch.uzh.csg.mbps.model.UserAccount;

/**
 * This class stores the user information as long as the user is logged in. It
 * also provides the information if the user is in only mode or not.
 * Furthermore, it holds the reference to the {@link InternalStorageHandler}.
 */
public class ClientController {
	private static UserAccount userAccount;
	private static boolean isOnline = false;
	private static InternalStorageHandler internalStorageHandler;
	
	/**
	 * Returns the {@link InternalStorageHandler}.
	 */
	public static InternalStorageHandler getStorageHandler() {
		return internalStorageHandler;
	}
	
	public static void setOnlineMode(boolean mode){
		isOnline = mode;
	}
	
	public static boolean isOnline(){
		return isOnline;
	}
	
	public static UserAccount getUser(){
		return userAccount;
	}
	
	/**
	 * Initializes the {@link ClientController} in order to be able to read and
	 * write to the local storage.
	 * 
	 * @param context
	 *            the application's context
	 * @param username
	 *            the username of the authenticated user (needed to create a so
	 *            named xml file)
	 * @param password
	 *            the password of the authenticaed user (needed to encrypt the
	 *            file in on the local storage)
	 * @throws Exception
	 *             if the application can't write to the internal storage or if
	 *             a decryption exception is thrown
	 */
	public static void init(Context context, String username, String password) throws Exception {
		internalStorageHandler = new InternalStorageHandler(context, username, password);
	}
	
	/**
	 * Sets a new (or updated) {@link UserAccount} object.
	 * 
	 * @param newUser
	 *            the new object
	 * @param persist
	 *            if true, the {@link UserAccount} object is saved to the local
	 *            storage
	 * @throws Exception
	 *             if the {@link UserAccount} object could not be persisted
	 */
	public static void setUser(UserAccount newUser, boolean persist) throws Exception {
		//TODO jeton: persist always?
		userAccount = newUser;
		if (persist)
			getStorageHandler().saveUserAccount(userAccount);
	}
	
	/**
	 * Deletes the temporary information stored in this class.
	 */
	public static void clear(){
		userAccount = null;
		isOnline = false;
		internalStorageHandler = null;
	}

	public static void setUserPassword(String password) throws Exception {
		userAccount.setPassword(password);
		internalStorageHandler.saveUserAccount(userAccount);
	}

	public static void setUserEmail(String saveEmail) throws Exception {
		userAccount.setEmail(saveEmail);
		internalStorageHandler.saveUserAccount(userAccount);
	}

	public static void setUserBalance(BigDecimal balance) throws Exception {
		userAccount.setBalance(balance);
		internalStorageHandler.saveUserAccount(userAccount);
	}
	
	public static boolean loadUserAccountFromStorage() throws Exception {
		UserAccount ua = internalStorageHandler.getUserAccount();
		if (ua == null) {
			return false;
		} else {
			userAccount = ua;
			return true;
		}
	}
	
//	//TODO: refactor, since no Transaction model class anymore
////	/**
////	 * The balance of the buyer and seller is updated after a transaction is
////	 * accomplished successfully.
////	 * 
////	 * @param isSeller
////	 *            if the user is seller.
////	 * @param tx
////	 *            The transaction information's from the accomplished
////	 *            transaction.
////	 * @param context
////	 *            The context of the current view (activity).
////	 */
////	public static void updateUserAfterTransaction(boolean isSeller, Transaction tx, Context context) {
////		user.setTransactionNumber(user.getTransactionNumber()+1);
////		if (isSeller)
////			user.setBalance(user.getBalance().add(tx.getAmount()));
////		else
////			user.setBalance(user.getBalance().subtract(tx.getAmount()));
////		
////		InternalStorageXML.writeUserAccountIntoFile(context.getApplicationContext());
////	}
	
}
