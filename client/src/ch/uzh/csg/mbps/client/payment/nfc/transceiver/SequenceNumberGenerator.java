package ch.uzh.csg.mbps.client.payment.nfc.transceiver;

import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;

/**
 * This class is responsible for providing increasing sequence numbers and
 * storing that counter temporarily. It is used to provide sequence numbers for
 * the {@link NfcMessage} header.
 */
public class SequenceNumberGenerator {

	private static byte sequenceNumber = 0;
	
	/**
	 * Increments the counter and returns the sequence number which has to be
	 * included in the header of the {@link NfcMessage} to be send.
	 */
	public static byte getNextSequenceNumber() {
		return ++sequenceNumber;
	}
	
	/**
	 * Returns the sequence number of the last {@link NfcMessage} send.
	 */
	public static byte getCurrentSequenceNumber() {
		return sequenceNumber;
	}
	
	/**
	 * Resets the counter so it can be used in the next session.
	 */
	public static void resetSequenceNumber() {
		sequenceNumber = 0;
	}
	
}
