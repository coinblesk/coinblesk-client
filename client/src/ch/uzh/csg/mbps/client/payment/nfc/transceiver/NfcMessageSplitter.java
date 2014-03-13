package ch.uzh.csg.mbps.client.payment.nfc.transceiver;

import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;

/**
 * Is responsible for NfcMessage (or byte buffer) fragmentation in order to not
 * exceed the maximum allowed message length by the underlying NFC technology.
 */
public class NfcMessageSplitter {
	
	private int isoDepMaxTransceiveLength;
	private int payloadPerFragment;
	
	/**
	 * Returns a new NfcMessageSplitter to handle the fragmentation of
	 * NfcMessages.
	 * 
	 * @param isoDepMaxTransceiveLength
	 *            the maximum number of bytes which can be send at once by the
	 *            underlying NFC technology
	 */
	public NfcMessageSplitter(int isoDepMaxTransceiveLength) {
		this.isoDepMaxTransceiveLength = isoDepMaxTransceiveLength;
		payloadPerFragment = this.isoDepMaxTransceiveLength - NfcMessage.HEADER_LENGTH;
	}
	
	/**
	 * Fragments the payload into a number of NfcMessages so that no NfcMessage
	 * exceeds the isoDepMaxTransceiveLength. If no fragmentation is needed
	 * (because the payload does not reach the threshold), then a list
	 * containing only one NfcMessage is returned.
	 * 
	 * @param payload
	 *            the whole message or byte array to be send by NFC
	 * @return an ArrayList of NfcMessages containing the fragmented payload.
	 */
	public ArrayList<NfcMessage> getFragments(byte[] payload) {
		ArrayList<NfcMessage> list = new ArrayList<NfcMessage>();
		
		if (!isFragmentationNeeded(payload)) {
			list.add(new NfcMessage(NfcMessage.DEFAULT, SequenceNumberGenerator.getNextSequenceNumber(), payload));
			return list;
		}
		
		int totalNofMessagesToSend = getTotalNofFragmentsToSend(payload.length);
		
		for (int i=0; i<totalNofMessagesToSend; i++) {
			int start = i*payloadPerFragment;
			byte[] temp = ArrayUtils.subarray(payload, start, start+payloadPerFragment);
			byte status = (i < (totalNofMessagesToSend-1)) ? NfcMessage.HAS_MORE_FRAGMENTS : NfcMessage.DEFAULT;
			list.add(new NfcMessage(status, SequenceNumberGenerator.getNextSequenceNumber(), temp));
		}
		
		return list;
	}

	/**
	 * Calculates if a message fragmentation is needed based on the payload to
	 * be send and the max message length provided by NFC.
	 * 
	 * @param payload
	 *            the message or byte array to be send by NFC
	 * @return true or false
	 */
	private boolean isFragmentationNeeded(byte[] payload) {
		return (getTotalNofFragmentsToSend(payload.length) > 1) ? true : false;
	}
	
	/**
	 * Returns the number of fragments the whole message needs to be split into
	 * (taking into account protocol headers etc.).
	 * 
	 * @param payloadLength
	 *            the length of the whole message or byte array which should be
	 *            send by NFC
	 * @return
	 */
	private int getTotalNofFragmentsToSend(int payloadLength) {
		int nofMessagesToSend = payloadLength / payloadPerFragment;
		// round since int division truncates the result
		if (payloadPerFragment * nofMessagesToSend < payloadLength)
			nofMessagesToSend++;
		
		return nofMessagesToSend;
	}
	
}
