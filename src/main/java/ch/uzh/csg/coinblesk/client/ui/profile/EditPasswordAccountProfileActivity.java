package ch.uzh.csg.coinblesk.client.ui.profile;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.AbstractAsyncActivity;
import ch.uzh.csg.coinblesk.client.ui.authentication.AbstractLoginActivity;
import ch.uzh.csg.coinblesk.client.util.IAsyncTaskCompleteListener;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.request.UpdateRequestTask;
import ch.uzh.csg.coinblesk.client.util.CheckFormatHandler;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.model.UserAccount;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;
import ch.uzh.csg.coinblesk.responseobject.UserAccountObject;

/**
 * This class is the view to set an new password.
 */
public class EditPasswordAccountProfileActivity extends AbstractAsyncActivity {
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
    	
	private void checkOnlineModeAndProceed() {
		if(!ClientController.isConnectedToServer()){
			saveChangeBtn.setEnabled(false);
		}
	}
    
    private void prepareLaunchRequest() {
		password = ((EditText) findViewById(R.id.updatePasswordEditText)).getText().toString();
		String confirmPassword = ((EditText) findViewById(R.id.updatePasswordConfirmEditText)).getText().toString();
		// Checks if the format is corrected and if the confirmation password equals to the inserted password 
		Pair<Boolean, String> responseContent = CheckFormatHandler.checkUpdateInputs(getApplicationContext(), password, confirmPassword);
		if (responseContent.first) {
			UserAccount user = new UserAccount(null, null, password);
			launchRequest(user);
		} else {
			displayResponse(responseContent.second);
		}
	}
    
	public void launchRequest(UserAccount user) {
		showLoadingProgressDialog();

		UserAccountObject userObject = new UserAccountObject();
		userObject.setUsername(user.getUsername());
		userObject.setPassword(user.getPassword());
		RequestTask<UserAccountObject, TransferObject> request = new UpdateRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
			public void onTaskComplete(TransferObject response) {
				if (!response.isSuccessful()) {
					displayResponse(response.getMessage());
					reload(getIntent());
					invalidateOptionsMenu();
					dismissProgressDialog();
					return;
				}
				boolean saved = ClientController.getStorageHandler().setUserPassword(password);
				AbstractLoginActivity.updatePassword();
				if (!saved) {
					displayResponse(getResources().getString(R.string.error_xmlSave_failed));
				}
				displayResponse(getResources().getString(R.string.updateAccount_successful));
				dismissProgressDialog();
				finish();
			}
		}, userObject, new TransferObject(), getApplicationContext());
		request.execute();
	}
    
}
