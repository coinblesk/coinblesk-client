package ch.uzh.csg.mbps.client.payment.nfc;

/**
 * This is a Utility class containing error codes and messages for the p2p nfc
 * communication between two clients.
 */
public class CommUtils {
	public static final int SETUP_CATEGORY = 0;
	public static final int NFC_LAYER_CATEGORY = 1;
	public static final int PAYMENT_LAYER_CATEGORY = 2;
	
	public enum Message {
		ERROR_NO_NFC_MSG(SETUP_CATEGORY, 1, "Your device does not support NFC. Unfortunately, you can't use this functionality without NFC."),
		ACTIVATE_NFC_MSG(SETUP_CATEGORY, 2, "Please activate NFC and press back to return to the application!"),
		ERROR_NFC_NOT_ACTIVATED_MSG(SETUP_CATEGORY, 3, "You can't use this functionality without NFC."),
		
		NFC_INIT_ERROR(NFC_LAYER_CATEGORY, 2, "Could not initialize a NFC connection. Please try again by holding devices together."),
		NFC_WRITE_ERROR(NFC_LAYER_CATEGORY, 3, "Error occurred while communicating over NFC. Please try again by holding devices together."),
		NFC_PASS_TO_UPPER_LAYER(NFC_LAYER_CATEGORY, 4, ""),
		
		PAYMENT_TRANSCEIVE_PACKET(PAYMENT_LAYER_CATEGORY, 1, ""),
		PAYMENT_ERROR_NO_AMOUNT_ENTERED(PAYMENT_LAYER_CATEGORY, 2, "Please enter an amount greater than 0 to proceed."),
		PAYMENT_ERROR_BUYER_REJECTED(PAYMENT_LAYER_CATEGORY, 3, "%s refused to pay %s BTC"),
		PAYMENT_ERROR_SERVER_REFUSED(PAYMENT_LAYER_CATEGORY, 4, "SERVER_REFUSED"),
		PAYMENT_ERROR_UNEXPECTED(PAYMENT_LAYER_CATEGORY, 5, "An unexpected error occurred. Please try again by holding devices together."),
		
		PAYMENT_UPDATE_GUI_AMOUNT(PAYMENT_LAYER_CATEGORY, 6, ""),
		PAYMENT_ACCEPT_PRESSED(PAYMENT_LAYER_CATEGORY, 7, "ACCEPT_PAYMENT"),
		PAYMENT_REJECT_PRESSED(PAYMENT_LAYER_CATEGORY, 8, "REJECT_PAYMENT"),
		PAYMENT_SUCCESS_BUYER(PAYMENT_LAYER_CATEGORY, 9, "You paid %s BTC to %s"),
		PAYMENT_SUCCESS_SELLER(PAYMENT_LAYER_CATEGORY, 10, "You received %s BTC from %s"),
		
		PAYMENT_OTHERDEVICE_UNEXPECTED(PAYMENT_LAYER_CATEGORY, 11, "");
		
		private final int category;
		private final int code;
		private final String msg;
		
		private Message(int category, int code, String msg) {
			this.category = category;
			this.code = code;
			this.msg = msg;
		}
		
		public int getCategory() {
			return category;
		}
		
		public int getCode() {
			return code;
		}
		
		public String getMessage() {
			return msg;
		}
		
		@Override
		public String toString() {
			return getCategory() + ", " + getCode() + ", " + getMessage();
		}
	}
	
}
