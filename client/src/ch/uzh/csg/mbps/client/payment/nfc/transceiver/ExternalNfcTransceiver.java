package ch.uzh.csg.mbps.client.payment.nfc.transceiver;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.util.Log;
import ch.uzh.csg.mbps.client.payment.AbstractPaymentActivity;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils;
import ch.uzh.csg.mbps.client.payment.nfc.CommUtils.Message;
import ch.uzh.csg.mbps.client.payment.nfc.SellerRole;
import ch.uzh.csg.mbps.client.payment.nfc.messages.NfcMessage;
import ch.uzh.csg.mbps.client.payment.nfc.messages.PaymentMessage;

import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.ReaderException;

/**
 * Accordingly to the {@link InternalNfcTransceiver} this class handles the NFC
 * communication for the external NFC sensor. This class makes only sense for
 * the ACR122u device and has only been tested with that specific device.
 */
public class ExternalNfcTransceiver extends NfcTransceiver {
	
	/*
	 * 64 is the maximum due to a sequence bug in the ACR122u
	 * http://musclecard.996296
	 * .n3.nabble.com/ACR122U-response-frames-contain-wrong
	 * -sequence-numbers-td5002.html --> if larger than 64, then I get a
	 * com.acs.smartcard.CommunicationErrorException: The sequence number (4) is
	 * invalid.
	 */
	public static final int MAX_WRITE_LENGTH = 53;
	
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
//	private static final String[] stateStrings = { "Unknown", "Absent", "Present", "Swallowed", "Powered", "Negotiable", "Specific" };
	
	private Activity activity;
	private Reader reader;
	private NfcMessageSplitter messageSplitter;
	
	private boolean permissionRequested = false;
	
	private volatile boolean initDone = false;
	
	private static final String TAG = "ExternalNfcTransceiver";

	public ExternalNfcTransceiver(Handler handler, Context applicationContext) {
		super(handler, applicationContext);
	}

	@Override
	public void enable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter) {
		Log.d(TAG, "enable NFC");
		
		activity = abstractPaymentActivity;
		role = new SellerRole();
		
		UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		reader = new Reader(manager);
		
		UsbDevice externalDevice = null;
		for (UsbDevice device : manager.getDeviceList().values()) {
			if (reader.isSupported(device)) {
				externalDevice = device;
				break;
			}
		}
		
		PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		activity.registerReceiver(broadcastReceiver, filter);
		
		if (!permissionRequested) {
			manager.requestPermission(externalDevice, permissionIntent);
			permissionRequested = true;
		}
		
		setOnStateChangedListener();
	}
	
	private long lastAbsent = 0;
	
	private void setOnStateChangedListener() {
		reader.setOnStateChangeListener(new OnStateChangeListener() {
			
			public void onStateChange(int slotNum, int prevState, int currState) {
                try {
                	Log.d(TAG, "statechange from: " + prevState + " to: " + currState);
                	if (currState == Reader.CARD_PRESENT) {
                		// 2 == present
                		
                		long diff = System.currentTimeMillis() - lastAbsent;
                		initCard();
                		if(diff < 100) {
                			Log.e(TAG, "retry because delay: "+diff+"ms");
                			initNfcResume();
                			
							if (toSend != null && !toSend.isEmpty()) {
								for (NfcMessage msg : toSend) {
									msg.getData()[1] = SequenceNumberGenerator.getNextSequenceNumber();
								}
								send(toSend);
							}
                			return;
                		} else {
                			Log.e(TAG, "no retry because delay: "+diff+"ms");
                		}
                		
                		if(!initDone) {
                			initDone = initNfc();
                		}
                	} else if (prevState == Reader.CARD_SPECIFIC && currState == Reader.CARD_ABSENT) {
                		lastAbsent = System.currentTimeMillis();
                		initDone = false;
                		if (role.isStateEnd()) {
                			role = new SellerRole();
                		}
                	} 
                } catch (Exception e) {
                	Message m = CommUtils.Message.NFC_WRITE_ERROR;
                	protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
                }
			}
		});
	}
	
	private void initCard() {
		try {
			reader.power(0, Reader.CARD_WARM_RESET);
			reader.setProtocol(0, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
		} catch (ReaderException re) {
			re.printStackTrace();
		}
	}
	
	private boolean initNfcResume() throws InterruptedException {
		messageSplitter = new NfcMessageSplitter(MAX_WRITE_LENGTH);
		int length = 0;

		for (int i = 0; i < 10; i++) {
			long start = System.currentTimeMillis();
			try {
				byte[] recvBuffer = new byte[MAX_WRITE_LENGTH];
				byte[] sendBuffer = createSelectAidApduResume();
				Log.d(TAG, "send init NFC resume");
				length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
				Log.d(TAG, "got init NFC resume:"+length);
				if (length > 0) {
					byte[] result = new byte[length];
					System.arraycopy(recvBuffer, 0, result, 0, length);
					handleAidApduResponseResume(result);
					return true;
				}
			} catch (Throwable t) {
				Log.e(TAG, "could not init in loop/resume", t);
			}
			
			long waitTime = 100 - (System.currentTimeMillis() - start);
			if(waitTime > 0) {
				Thread.sleep(waitTime);
			}
		}

		if (length == 0) {
			Message m = CommUtils.Message.NFC_INIT_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
		return false;
	}
	

	private boolean initNfc() throws InterruptedException {
		messageSplitter = new NfcMessageSplitter(MAX_WRITE_LENGTH);
		int length = 0;

		for (int i = 0; i < 10; i++) {
			long start = System.currentTimeMillis();
			try {
				byte[] recvBuffer = new byte[MAX_WRITE_LENGTH];
				byte[] sendBuffer = createSelectAidApdu();
				Log.d(TAG, "send init NFC");
				length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
				Log.d(TAG, "got init NFC:"+length);
				if (length > 0) {
					byte[] result = new byte[length];
					System.arraycopy(recvBuffer, 0, result, 0, length);
					handleAidApduResponse(result);
					return true;
				}
			} catch (Throwable t) {
				Log.e(TAG, "could not init in loop", t);
			}
			
			long waitTime = 100 - (System.currentTimeMillis() - start);
			if(waitTime > 0) {
				Thread.sleep(waitTime);
			}
		}

		if (length == 0) {
			Message m = CommUtils.Message.NFC_INIT_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
		return false;
	}

	@Override
	public void disable(AbstractPaymentActivity abstractPaymentActivity, NfcAdapter nfcAdapter) {
		cancel();
		activity.unregisterReceiver(broadcastReceiver);
		if (reader != null && reader.isOpened()) {
			reader.close();
		}
	}

	@Override
	public void cancel() {
		initDone = false;
		role = new SellerRole();
	}
	
	private List<NfcMessage> toSend;

	@Override
	public void write(PaymentMessage paymentMessage) {
		Log.e(TAG, Arrays.toString(paymentMessage.getData()));
		
		toSend = messageSplitter.getFragments(paymentMessage.getData());
		send(toSend);
	}
	
	private void send (List<NfcMessage> toSend) {
		for (Iterator<NfcMessage> iterator= toSend.iterator();iterator.hasNext();) {
			NfcMessage msg = iterator.next();
			if(!transmit(msg.getData())) {
				return;
			} else {
				iterator.remove();
			}
		}
	}
	
	@Override
	public boolean write(NfcMessage nfcMessage) throws IllegalArgumentException {
		if (nfcMessage.getData().length > MAX_WRITE_LENGTH)
			throw new IllegalArgumentException("The argument length exceeds the maximum capacity of "+MAX_WRITE_LENGTH+" bytes.");
			
		Log.d(TAG, "send msg:"+nfcMessage);
		transmit(nfcMessage.getData());
		return true;
	}
	
	private boolean transmit(byte[] bytes) {
		if (reader.isOpened()) {
			return write(bytes);
		} else {
			Message m = CommUtils.Message.NFC_WRITE_ERROR;
			protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		}
		return false;
	}
	
	protected boolean write(byte[] bytes) {
		byte[] result = null;
		int length = 0;

		//for (int i = 0; i < 10 && !cancelWrite; i++) {
			long start = System.currentTimeMillis();
			try {
				byte[] recvBuffer = new byte[MAX_WRITE_LENGTH];
				Log.d(TAG, "transmit " + bytes.length +"/s:"+bytes[1]);
				length = reader.transmit(0, bytes, bytes.length, recvBuffer, recvBuffer.length);
				Log.d(TAG, "got from transmit " + length);
				if (length > 0) {
					result = new byte[length];
					System.arraycopy(recvBuffer, 0, result, 0, length);
					handleResponse(new NfcMessage(result));
					return true;
				}
			} catch (Throwable t) {
				Log.e(TAG, "could not write in loop", t);
			}

			long waitTime = 100 - (System.currentTimeMillis() - start);
			if (waitTime > 0) {
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		//}
		
		//if (length == 0 ) {
		//	Message m = CommUtils.Message.NFC_WRITE_ERROR;
		//	protocolHandler.obtainMessage(m.getCategory(), m.getCode(), 0).sendToTarget();
		//}
		return false;
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

	        if (ACTION_USB_PERMISSION.equals(action)) {
	            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					if (device != null) {
						try {
							reader.open(device);
						} catch (Exception e) {
						}
					}
	            }
	        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
	            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	            if (device != null && device.equals(reader.getDevice())) {
	            	reader.close();
	            }
	        }
		}
		
	};
	
}
