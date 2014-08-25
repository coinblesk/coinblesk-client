package ch.uzh.csg.mbps.client;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ch.uzh.csg.mbps.client.request.PasswordResetRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.CheckFormatHandler;
import ch.uzh.csg.mbps.responseobject.TransactionObject;
import ch.uzh.csg.mbps.responseobject.TransferObject;

/**
 * This class allows to reset a user's password.
 */
public class ResetPasswordActivity extends AbstractAsyncActivity {

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
			RequestTask<TransferObject, TransferObject> resetPW = new PasswordResetRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
				
				public void onTaskComplete(TransferObject response) {
					dismissProgressDialog();
			    	if (response.isSuccessful()) {
			    		displayResponse(response.getMessage());
			    		launchLoginActivity();
			    	} else {
			    		displayResponse(response.getMessage());
			    	}
				}
			}, new TransactionObject(), new TransactionObject());
			resetPW.execute();
		} else {
			displayResponse(getResources().getString(R.string.registration_email_not_valid));
		}
	}

	private void launchLoginActivity(){
		super.finish();
		launchActivity(this, LoginActivity.class);
	}
}
