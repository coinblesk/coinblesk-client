package ch.uzh.csg.mbps.client.profile;

import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
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
 * This class is the view to set an new password.
 */
public class EditPasswordAccountProfileActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {
	private MenuItem menuWarning;
	private Button saveChangeBtn;
	private String password;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_password_account_profile);
		setScreenOrientation();
		
		saveChangeBtn = (Button)findViewById(R.id.updatePasswordBtn);
		saveChangeBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				prepareLaunchRequest();
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
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit_password_account_profile, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuWarning = menu.findItem(R.id.action_warning);
		menuWarning.setVisible(false);
		return true;
	}

    @Override
    public void invalidateOptionsMenu(){
    	if(menuWarning != null){
        	if(ClientController.isOnline()) {
                menuWarning.setVisible(false);
            } else {
            	menuWarning.setVisible(true);
            }
    	}
    }
	
	private void checkOnlineModeAndProceed() {
		if(!ClientController.isOnline()){
			saveChangeBtn.setEnabled(false);
		}
	}
    
    private void prepareLaunchRequest() {
		password = ((EditText) findViewById(R.id.updatePasswordEditText)).getText().toString();
		String confirmPassword = ((EditText) findViewById(R.id.updatePasswordConfirmEditText)).getText().toString();
		// Checks if the format is corrected and if the confirmation password equals to the inserted password 
		Pair<Boolean, String> responseContent = CheckFormatHandler.checkUpdateInputs(password, confirmPassword);
		if (responseContent.first) {
			UserAccount user = new UserAccount(null, null, password);
			launchRequest(user);
		} else {
			displayResponse(responseContent.second);
		}
	}
    
    private void launchRequest(UserAccount user) {
		showLoadingProgressDialog();
		RequestTask update = new UpdateRequestTask(this, user);
		update.execute();
	}
    
    public void onTaskComplete(CustomResponseObject response) {
		if (response.isSuccessful()) {
			boolean saved = ClientController.getStorageHandler().setUserPassword(password);
			if (!saved) {
				//TODO: display message that not saved to xml --> not able to use offline!
			}
			finish();
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		}
		dismissProgressDialog();
		displayResponse(response.getMessage());
	}
    
}