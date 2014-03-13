package ch.uzh.csg.mbps.client.payment.nfc.messages;

import java.util.BitSet;

import android.util.Log;

/**
 * This is the lower layer protocol message. It is responsible for sending
 * Messages between two devices via NFC. It is build to allow message
 * fragmentation and reassembly. The status values in the header can be combined
 * (OR, AND, etc.) to transmit more than one status to the counterpart. The
 * header contains also a sequence number in order to detect multiple
 * transmission of the same messages.
 */
public class NfcMessage extends ProtocolMessage {
	
	public static final int HEADER_LENGTH = 2;
	
	// messages from buyer to seller
	public static final byte AID_SELECTED = 0x01; // 0
	public static final byte RESPONSE_ABORTED = 0x02; // 1
	public static final byte WAIT_FOR_ANSWER = 0x04; // 2
	
	// messages between seller to buyer
	public static final byte DEFAULT = 0x00; // - 
	public static final byte RESPONSE = 0x08; // 3
	public static final byte WAITING_FOR_NEXT = 0x10; // 4
	
	// flag
	public static final byte HAS_MORE_FRAGMENTS = 0x20; // 5
	
	// messages from seller to buyer
	//public static final byte CAN_HANDLE_LARGE_FRAGMENTS = 0x40;
	
	public NfcMessage(byte[] data) {
		setHeaderLength(HEADER_LENGTH);
		setData(data);
	}
	
	/**
	 * Creates a new NfcMessage.
	 * 
	 * @param status
	 *            the status to be contained in the header
	 * @param sequenceNumber
	 *            the sequence number of this message to be contained in the
	 *            header
	 * @param payload
	 *            the payload of this message
	 */
	public NfcMessage(byte status, byte sequenceNumber, byte[] payload) {
		setHeaderLength(HEADER_LENGTH);
		
		byte[] data;
		if (payload != null && payload.length > 0) {
			data = new byte[payload.length+HEADER_LENGTH];
			System.arraycopy(payload, 0, data, HEADER_LENGTH, payload.length);
		} else {
			data = new byte[HEADER_LENGTH];
		}
		data[0] = status;
		data[1] = sequenceNumber;
		
		setData(data);
		
		Log.e("NfcMessage", "generated new message: "+toString());
	}
	
	/**
	 * Returns the sequence number of this {@link NfcMessage}. If the data is
	 * null, then 0xFF is returned.
	 */
	public byte getSequenceNumber() {
		if (getData() == null)
			return (byte) 0xFF;
		else
			return getData()[1];
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("nfc:");
		sb.append("head:");
		BitSet bs = BitSet.valueOf(new byte[]{getData()[0]});
		sb.append(bs).append(", h:").append(((int) getData()[0] & 0xFF)).append(", s:").append(((int) getData()[1] & 0xFF));
		sb.append(", len:").append(getData().length);
		return sb.toString();
	}
	
}
