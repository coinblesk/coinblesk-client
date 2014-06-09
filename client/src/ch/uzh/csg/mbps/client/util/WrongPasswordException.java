package ch.uzh.csg.mbps.client.util;

/**
 * This exception is thrown when a user enters a wrong password or username
 * while singing in.
 */
public class WrongPasswordException extends Exception {
	private static final long serialVersionUID = 1052621981449233496L;
	
	public WrongPasswordException(String pw) {
		super(pw);
	}

}
