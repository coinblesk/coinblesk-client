package ch.uzh.csg.mbps.client.payment.nfc.messages;

import org.apache.commons.lang3.ArrayUtils;

/**
 * This is the abstract base type of the protocol messages send via NFC to
 * accomplish a transaction. The ProtocolMessage consists of a header with a
 * length of one byte and an arbitrary long payload.
 */
public abstract class ProtocolMessage {
	
	// consists of header + payload
	private byte[] data;
	private int headerLength;
	
	/**
	 * Returns the status code contained in the first byte of the header. Each
	 * {@link ProtocolMessage} has in its first byte of the header some kind of
	 * status information of that message. If the data is null, then 0xFF is
	 * returned.
	 */
	public byte getStatus() {
		if (data == null)
			return (byte) 0xFF;
		else
			return data[0];
	}
	
	/**
	 * Sets the status of this ProtocolMessage.
	 */
	public void setStatus(byte status) {
		if (data != null && data.length > 0)
			data[0] = status;
	}
	
	/**
	 * Sets the header length, i.e. how many bytes in the data belong to the
	 * header.
	 * 
	 * @param length
	 *            the length of the header
	 */
	public void setHeaderLength(int length) {
		headerLength = length;
	}
	
	/**
	 * Returns the complete data (header+payload).
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Sets the data, consisting of the header plus payload.
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
	
	/**
	 * Returns the payload only (excluding the header) or null.
	 */
	public byte[] getPayload() {
		if (data == null)
			return null;
		else
			return ArrayUtils.subarray(data, headerLength, data.length);
	}
	
	/**
	 * Returns the length of the payload.
	 */
	public int getPayloadLength() {
		if (data == null)
			return 0;
		else
			return data.length-headerLength; 
	}
	
}
