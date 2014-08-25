package ch.uzh.csg.mbps.client.payment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SendPayInAddressByEmail;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.responseobject.TransferObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * This class is the view for pay ins. It displays the bitcoin address where a
 * user has to transfer bitcoins to in order to pay in into our system.
 */
public class PayInActivity extends AbstractAsyncActivity {
	public String payInAddress;
	private Button copyClipboardBtn;
	private Button sendAsMailBtn;
	private Button showQRButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pay_in);
		setScreenOrientation();

		getActionBar().setDisplayHomeAsUpEnabled(true);

		copyClipboardBtn = (Button) findViewById(R.id.payIn_copyButton);
		sendAsMailBtn = (Button) findViewById(R.id.payIn_sendMail);
		showQRButton = (Button) findViewById(R.id.payIn_showQRButton);

		initClickListener();

		setPaymentAddress();

		if(!ClientController.isOnline())
			sendAsMailBtn.setEnabled(false);
	}

	@Override
	public void onResume(){
		super.onResume();
		invalidateOptionsMenu();

		if(!ClientController.isOnline())
			sendAsMailBtn.setEnabled(false);
	}

	private void initClickListener() {
		copyClipboardBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("BTC Address", payInAddress);
				clipboard.setPrimaryClip(clip);
				displayResponse(getResources().getString(R.string.copy_clipboard));
			}
		});

		sendAsMailBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchRequest();
			}
		});

		showQRButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showQRCode();
			}
		});
	}

	private void showQRCode() {
		Bitmap qrCode;
		try {
			qrCode = generateQRCode_general("bitcoin:" + payInAddress);
			if (qrCode != null) {
				showDialog(qrCode);
			}
		} catch (WriterException e) {
			displayResponse("Problem creating QR Code");
		}
	}

	private void showDialog(Bitmap qrCode) {
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup group = (ViewGroup) findViewById(R.id.qr_code_popup);
		final View layout = inflater.inflate(R.layout.qr_code_popup, group);
		final PopupWindow popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

		final ImageView qrCodeImageView = (ImageView) layout.findViewById(R.id.qr_code_imageView);
		qrCodeImageView.setImageBitmap(qrCode);
		
		layout.post(new Runnable() {
			public void run() {
				popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
			}
		});

		final Button closeButton = (Button) layout.findViewById(R.id.qr_code_closeButton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				popupWindow.dismiss();
			}
		});
	}

	/**
	 * Generate QR Code as from http://stackoverflow.com/questions/22371626/android-generate-qr-code-and-barcode-using-zxing
	 * 
	 * @param data bitcoin address to write
	 * @return Bitmap with QR Code
	 * @throws WriterException
	 */
	private Bitmap generateQRCode_general(String data)throws WriterException {
		Writer writer = new QRCodeWriter();
		int size = 800;
		BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
		Bitmap ImageBitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);

		for (int i = 0; i < size; i++) {//width
			for (int j = 0; j < size; j++) {//height
				ImageBitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK: Color.WHITE);
			}
		}

		if (ImageBitmap != null) {
			return ImageBitmap;
		} else {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unexcepted_error),
					Toast.LENGTH_SHORT).show(); 
			return null;
		}
	}

	private void launchRequest() {
		showLoadingProgressDialog();
		RequestTask<TransferObject, TransferObject> sendToEmail = new SendPayInAddressByEmail(new IAsyncTaskCompleteListener<TransferObject>() {

			public void onTaskComplete(TransferObject response) {
				dismissProgressDialog();
				displayResponse(response.getMessage());
	            
            }
		}, new TransferObject(), new TransferObject());
		sendToEmail.execute();
	}

	private void setPaymentAddress() {
		TextView tv = (TextView) findViewById(R.id.payIn_address);
		payInAddress = ClientController.getStorageHandler().getUserAccount().getPaymentAddress();
		tv.setText(payInAddress);
	}

}