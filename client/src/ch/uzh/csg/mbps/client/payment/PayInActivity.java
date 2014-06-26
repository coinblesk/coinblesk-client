package ch.uzh.csg.mbps.client.payment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SendPayInAddressByEmail;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class is the view for pay ins. It displays the bitcoin address where a
 * user has to transfer bitcoins to in order to pay in into our system.
 */
public class PayInActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
	public String payInAddress;
	private Button copyClipboardBtn;
	private Button sendAsMailBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pay_in);
		setScreenOrientation();
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		copyClipboardBtn = (Button) findViewById(R.id.payIn_copyButton);
		sendAsMailBtn = (Button) findViewById(R.id.payIn_sendMail);
		
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
	}
	
	private void launchRequest() {
		showLoadingProgressDialog();
		RequestTask sendToEmail = new SendPayInAddressByEmail(this);
		sendToEmail.execute();
	}
	
	public void onTaskComplete(CustomResponseObject response) {
    	dismissProgressDialog();
    	displayResponse(response.getMessage());
    }
	
    private void setPaymentAddress() {
    	TextView tv = (TextView) findViewById(R.id.payIn_address);
		payInAddress = ClientController.getStorageHandler().getUserAccount().getPaymentAddress();
		tv.setText(payInAddress);
	}

}