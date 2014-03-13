package ch.uzh.csg.mbps.client.payment.nfc.messages;

/**
 * This is the upper layer protocol message. It contains the whole message to be
 * delivered to the given client (seller or buyer). The header says if the
 * protocol can be proceeded or if the counterpart has encountered an error and
 * asks to abort the transaction.
 */
public class PaymentMessage extends ProtocolMessage {
	
	public static final int HEADER_LENGTH = 1;
	
	// send between seller and buyer
	public static final byte PROCEED = 0x20;
	public static final byte ERROR = 0x21;
	
	// indicates on the buyer side that it needs to wait for another process in
	// order to proceed. Not send between seller and buyer
	public static final byte WAIT_WITH_ANSWER = 0x22;
	
	public PaymentMessage(byte[] data) {
		setHeaderLength(HEADER_LENGTH);
		setData(data);
	}
	
	public PaymentMessage(byte status, byte[] payload) {
		setHeaderLength(HEADER_LENGTH);
		
		byte[] data;
		if (payload != null && payload.length > 0) {
			data = new byte[payload.length+HEADER_LENGTH];
			System.arraycopy(payload, 0, data, HEADER_LENGTH, payload.length);
		} else {
			data = new byte[HEADER_LENGTH];
		}
		data[0] = status;
		
		setData(data);
	}

}
