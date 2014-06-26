package ch.uzh.csg.mbps.client.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.UpdateRequestTask;
import ch.uzh.csg.mbps.client.util.CheckFormatHandler;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class is the view to set an new email address.
 */
public class EditEmailAccountProfileActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
	private Button saveChangeBtn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setScreenOrientation();
		
		setContentView(R.layout.activity_edit_email_account_profile);
		
		saveChangeBtn = (Button)findViewById(R.id.updateEmailBtn);
		saveChangeBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchRequest();
			}
		});
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		checkOnlineModeAndProceed();
	}
	
    @Override
    public void onResume(){
    	super.onResume();
    	checkOnlineModeAndProceed();
    	invalidateOptionsMenu();
    }
    
	/**
	 * The request for the new email address is called. The request is only
	 * accepted when the email address has an valid form and is does not equal
	 * to the existing email address.
	 */
    private void launchRequest() {
    	EditText email = (EditText) findViewById(R.id.updateEmailEditText);
    	
    	String emailString = email.getText().toString();
    	if (emailString.isEmpty() || !CheckFormatHandler.isEmailValid(emailString)) {
    		displayResponse(getResources().getString(R.string.email_address_invalid));
    	} else if (emailString.equals(ClientController.getStorageHandler().getUserAccount().getEmail())) {
    		displayResponse(getResources().getString(R.string.same_email_address));
    	} else {
    		showLoadingProgressDialog();
    		RequestTask update = new UpdateRequestTask(this, new UserAccount(null, emailString, null));
    		update.execute();
    	}
	}
    
    public void onTaskComplete(CustomResponseObject response) {
		if (response.isSuccessful()) {
			String saveEmail = ((EditText) findViewById(R.id.updateEmailEditText)).getText().toString();
			
			boolean saved = ClientController.getStorageHandler().setUserEmail(saveEmail);
			if (!saved) {
				displayResponse(getResources().getString(R.string.error_xmlSave_failed));
			}
			
			finish();
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		}
		dismissProgressDialog();
		displayResponse(response.getMessage());
	}
	
	private void checkOnlineModeAndProceed() {
		if(!ClientController.isOnline()){
			saveChangeBtn.setEnabled(false);
		}
	}
	
}