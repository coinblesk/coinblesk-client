package ch.uzh.csg.coinblesk.client.internalstorage;

/**
 * This exception is thrown when the xml file in the shared preferences cannot be
 * decrypted because of an invalid password.
 */
public class WrongPasswordException extends Exception {
	private static final long serialVersionUID = 1052621981449233496L;
	
	public WrongPasswordException(String pw) {
		super(pw);
	}

}
