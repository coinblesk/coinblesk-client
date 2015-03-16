package ch.uzh.csg.coinblesk.client;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ch.uzh.csg.coinblesk.client.request.PasswordResetRequestTask;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.util.CheckFormatHandler;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

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

			TransferObject request = new TransferObject();
			request.setMessage(email);

			RequestTask<TransferObject, TransferObject> resetPW = new PasswordResetRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
				@Override
				public void onTaskComplete(TransferObject response) {
					dismissProgressDialog();
					if (response.isSuccessful()) {
						displayResponse(response.getMessage());
						launchLoginActivity();
					} else if (response.getMessage().contains(Constants.CONNECTION_ERROR)){
						displayResponse(getResources().getString(R.string.error_no_connection_before_login));
					}
					else { 
						displayResponse(response.getMessage());
					}
				}
			}, request, new TransferObject(), getApplicationContext());
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
