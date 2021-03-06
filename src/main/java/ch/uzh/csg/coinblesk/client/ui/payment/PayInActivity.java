package ch.uzh.csg.coinblesk.client.ui.payment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;
import ch.uzh.csg.coinblesk.client.util.ConnectionCheck;
import ch.uzh.csg.coinblesk.client.util.Mailer;

/**
 * This class is the view for pay ins. It displays the bitcoin address where a
 * user has to transfer bitcoins to in order to pay in into our system.
 */
public class PayInActivity extends BaseActivity {

    private final static Logger LOGGER = LoggerFactory.getLogger(PayInActivity.class);

    public String payInAddress;
    private Button copyClipboardBtn;
    private Button sendAsMailBtn;
    private Button showQRButton;
    
    private class GetReceiveAddressTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            return getWalletService().getBitcoinAddress();
        }
        
        @Override
        protected void onPostExecute(String address) {
            TextView tv = (TextView) findViewById(R.id.payIn_address);
            tv.setText(address);
        }
        
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        new GetReceiveAddressTask().execute();
        payInAddress = getWalletService().getBitcoinAddress();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pay_in);
        setScreenOrientation();

        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        copyClipboardBtn = (Button) findViewById(R.id.payIn_copyButton);
        sendAsMailBtn = (Button) findViewById(R.id.payIn_sendMail);
        showQRButton = (Button) findViewById(R.id.payIn_showQRButton);

        initClickListener();

        if (!ConnectionCheck.isNetworkAvailable(this))
            sendAsMailBtn.setEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();

        if (!ConnectionCheck.isNetworkAvailable(this))
            sendAsMailBtn.setEnabled(false);
    }

    private void initClickListener() {
        copyClipboardBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BTC Address", payInAddress);
                clipboard.setPrimaryClip(clip);
                displayResponse(getResources().getString(R.string.copy_clipboard));
                LOGGER.debug("Copied Bitcoin address to clipboard: {}", payInAddress);
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
     * Generate QR Code as from
     * http://stackoverflow.com/questions/22371626/android
     * -generate-qr-code-and-barcode-using-zxing
     * 
     * @param data
     *            bitcoin address to write
     * @return Bitmap with QR Code
     * @throws WriterException
     */
    private Bitmap generateQRCode_general(String data) throws WriterException {
        Writer writer = new QRCodeWriter();
        int size = 800;
        BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
        Bitmap ImageBitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);

        for (int i = 0; i < size; i++) {// width
            for (int j = 0; j < size; j++) {// height
                ImageBitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }

        if (ImageBitmap != null) {
            return ImageBitmap;
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.unexcepted_error), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void launchRequest() {

        showLoadingProgressDialog();
        Mailer mailer = new Mailer();
        mailer.setSubject(getString(R.string.payIn_bitcoinAddressEmailSubject));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(getString(R.string.payIn_bitcoinAddressEmailBody), getWalletService().getBitcoinAddress()));

        mailer.setMessage(sb.toString());
        mailer.sendEmail(this);

    }


}
