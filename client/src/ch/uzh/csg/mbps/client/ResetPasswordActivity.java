package ch.uzh.csg.mbps.client;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ch.uzh.csg.mbps.client.request.PasswordResetRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.CheckFormatHandler;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class allows to reset a user's password.
 */
public class ResetPasswordActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject> {

	private Button resetPasswordBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reset_password);
		setScreenOrientation();
		
		resetPasswordBtn = (Button) findViewById(R.id.resetPW_resetButton);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		initClickListener();
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
	
	private void initClickListener(){
		
		resetPasswordBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchRequest();
			}
		});
		
	}

    private void launchRequest() {
    	String email = ((EditText)findViewById(R.id.resetPW_emailEditText)).getText().toString();
    	
		if (CheckFormatHandler.isEmailValid(email)) {
			showLoadingProgressDialog();
			UserAccount user = new UserAccount(null, email, null);
			RequestTask resetPW = new PasswordResetRequestTask(this, user);
			resetPW.execute();
		} else {
			displayResponse(Constants.EMAIL_IS_VALID_FALSE);
		}
	}
    
    public void onTaskComplete(CustomResponseObject response) {
    	dismissProgressDialog();
    	if (response.isSuccessful()) {
    		displayResponse(response.getMessage());
    		launchLoginActivity();
    	} else {
    		displayResponse(response.getMessage());
    	}
    }

	private void launchLoginActivity(){
		super.finish();
		launchActivity(this, LoginActivity.class);
	}
	
}
