package ch.uzh.csg.mbps.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * The Login Activity is the first view of the application. The user has to sign
 * in with the username and password to use the application.
 */
public class LoginActivity extends AbstractLoginActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
	// constant to determine which sub-activity returns
	private static final int REQUEST_CODE = 1;
	private Button signInBtn;
	private Button signUpBtn;
	private TextView resetPassword;
	
	
	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            If the activity is being re-initialized after previously being
	 *            shut down then this Bundle contains the data it most recently
	 *            supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it
	 *            is null.</b>
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setScreenOrientation();
        
        signInBtn = (Button) findViewById(R.id.loginSignInBtn);
    	signUpBtn = (Button) findViewById(R.id.loginSignUpBtn);
    	resetPassword = (TextView) findViewById(R.id.loginPasswordOrUsernameForgottenTextView);
        
    	retrieveLastSignedUsername();
    	
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
    
    public void onTaskComplete(CustomResponseObject response) {
    	super.onTaskComplete(response, getApplicationContext());
    }
    
	private void retrieveLastSignedUsername() {
		SharedPreferences sharedPref = getSharedPreferences(getResources().getString(R.string.stored_username), Context.MODE_PRIVATE);
		String storedUsername = sharedPref.getString(getString(R.string.stored_username), "");
		EditText usernameEditText = (EditText) findViewById(R.id.loginUsernameEditText);
		usernameEditText.setText(storedUsername);
		if (!storedUsername.isEmpty()) {
			username = storedUsername;
			EditText password = (EditText) findViewById(R.id.loginPasswordEditText);
			password.requestFocus();
		}
	}

	private void initClickListener() {
    	signInBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				username = ((EditText) findViewById(R.id.loginUsernameEditText)).getText().toString();
				password = ((EditText) findViewById(R.id.loginPasswordEditText)).getText().toString();
				
				if (username.isEmpty() || password.isEmpty())
					displayResponse(getResources().getString(R.string.enter_username_password));
				else{
					// hide virtual keyboard
					InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					launchSignInRequest();
				}
			}
		});
		
		signUpBtn.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				launchSignUpActivity();
			}
		});
		
		
		resetPassword.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				launchActivity(LoginActivity.this, ResetPasswordActivity.class);
			}
		});
		
	}
	
	private void launchSignUpActivity(){
    	Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
    	startActivityForResult(intent, REQUEST_CODE);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
			if (data.hasExtra("username")) {
				// if edit text field is not empty clear it
				if (((EditText) findViewById(R.id.loginPasswordEditText)).getText().length() > 0)
					((EditText) findViewById(R.id.loginPasswordEditText)).setText("");

				((EditText) findViewById(R.id.loginUsernameEditText)).setText(data.getExtras().getString("username"));
			}
		}
	}	
}