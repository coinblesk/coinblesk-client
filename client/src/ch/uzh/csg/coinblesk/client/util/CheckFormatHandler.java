package ch.uzh.csg.coinblesk.client.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.uzh.csg.coinblesk.client.profile.EditPasswordAccountProfileActivity;
import ch.uzh.csg.coinblesk.client.R;
import android.content.Context;
import android.util.Pair;
import android.widget.CheckBox;

/**
 * This class is used to check the inputs inserted by the user. Checks if the
 * format is correct and valid.
 */
public class CheckFormatHandler {

	private static boolean fieldsNotEmpty(ArrayList<String> fields){
		for (String field : fields) {
			if (field.isEmpty())
				return false;
		}
		return true;
	}
	
	private static boolean isUsernameValid(CharSequence username){
		Matcher matcher = Pattern.compile(Constants.USERNAME_PATTERN).matcher(username);
		return  matcher.matches();
	}
	
	private static boolean isPasswordLengthValid(CharSequence password){
		return (password.length() >= 4 && password.length() <= 20) ? true : false;
	}
	
	private static boolean passwordsAreEqual(CharSequence email, CharSequence confirm){
		return email.equals(confirm);
	}
	
	private static boolean isTermOfUseAccepted(CheckBox accept){
		return accept.isChecked();
	}
	
	/**
	 * Checks if the inserted inputs fulfill the expected requirements.
	 * 
	 * @param username
	 *            The inserted username string.
	 * @param email
	 *            The inserted email string.
	 * @param password
	 *            The inserted password string.
	 * @param confirmPassword
	 *            The inserted password string for the second time.
	 * @param termOfUseChecked
	 *            The checkbox of the term of use.
	 * @return Returns true if all requirements are met.
	 */
	public static Pair<Boolean,String> checkRegistrationInputs(Context context, String username, String email, String password, String confirmPassword, CheckBox termOfUseChecked){
		ArrayList<String> editTexts = new ArrayList<String>();
		editTexts.add(username);
		editTexts.add(email);
		editTexts.add(password);
		editTexts.add(confirmPassword);

		// Checks if any field is empty
		if (!fieldsNotEmpty(editTexts)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_field_not_empty));
		}
		// Checks if username has correct format
		else if (!isUsernameValid(username)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_username_error));
		}
		// Checks if the email address has a valid form
		else if (!isEmailValid(email)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_email_not_valid));
		}
		// checks if the password has a minimum length of 4 characters
		else if (!isPasswordLengthValid(password)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_password_lenght_error));
		}
		// Checks if password and confirm password are the same
		else if (!passwordsAreEqual(password, confirmPassword)) {
			return new Pair<Boolean, String>(false,context.getResources().getString(R.string.registration_password_not_equal));
		}
		// Checks if term of use is accepted
		else if (!isTermOfUseAccepted(termOfUseChecked)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_tou_not_accepted));
		} else {
			return new Pair<Boolean, String>(true, "OK");
		}
	}

	/**
	 * Checks if the password and the confirmation password fulfill the
	 * requirements. This method is only used by the update password actvity
	 * {@link EditPasswordAccountProfileActivity}.
	 * 
	 * @param password
	 *            The inserted password string.
	 * @param confirmPassword
	 *            The inserted password string as second time.
	 * @return Returns true if all fields are filled, have length of four
	 *         characters and password and confirmation password are the same.
	 */
	public static Pair<Boolean, String> checkUpdateInputs(Context context, String password, String confirmPassword) {
		// Checks if any field is empty
		if (password.isEmpty() || confirmPassword.isEmpty()) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_field_not_empty));
		}
		// checks if the password has a minimum length of 4 characters
		else if (!isPasswordLengthValid(password)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_password_lenght_error));
		}
		// Checks if password and confirm password are the same
		else if (!passwordsAreEqual(password, confirmPassword)) {
			return new Pair<Boolean, String>(false, context.getResources().getString(R.string.registration_password_not_equal));
		} else {
			return new Pair<Boolean, String>(true, "OK");
		}
	}
	
	/**
	 * Checks if the email has a valid form.
	 * 
	 * @param email
	 *            The inserted email string.
	 * @return Returns true if format is valid.
	 */
	public static boolean isEmailValid(String email){
		return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}

}
