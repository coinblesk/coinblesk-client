package ch.uzh.csg.mbps.client;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.request.ReadRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SignInRequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.client.util.WrongPasswordException;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * The Login Activity is the first view of the application. The user has to sign
 * in with the username and password to use the application.
 */
public class LoginActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
	// constant to determine which sub-activity returns
	private static final int REQUEST_CODE = 1;
	
	private String username;
	private String password;
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
    
	private void retrieveLastSignedUsername() {
		
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		String storedUsername = sharedPref.getString(getString(R.string.stored_username), "");
		EditText usernameEditText = (EditText) findViewById(R.id.loginUsernameEditText);
		usernameEditText.setText(storedUsername);
		if(!storedUsername.isEmpty()){
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
				else
					launchSignInRequest();
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
	
	private void launchSignInRequest() {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(this);
		UserAccount user = new UserAccount(username, null, password);
		RequestTask signIn = new SignInRequestTask(this, user);
		signIn.execute();
	}

	public void onTaskComplete(CustomResponseObject response){
		// The response object was successful
		if (response.isSuccessful()) {
			/* Checks the getReadAccountTO method to 
			 * find out if it was a read account request
			 */
			if (response.getReadAccountTO() != null) {
				writeServerPublicKey(response.getEncodedServerPublicKey());
				
				try {
					ClientController.init(getApplicationContext(), username, password);
					ClientController.setUser(response.getReadAccountTO().getUserAccount(), true);
				} catch (Exception e) {
					//TODO jeton: handle exception
				}
				ClientController.setOnlineMode(true);
				launchMainActivity();
			} else {
				launchReadRequest();
			}
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			launchOfflineMode();
			dismissProgressDialog();
		} else {
			displayResponse(response.getMessage());
			dismissProgressDialog();
		}
		
	}
	
	private void launchReadRequest() {		
		RequestTask read = new ReadRequestTask(this);
		read.execute();
	}
	
	/**
	 * After the user signed in successfully, the retrieved public key of the
	 * server is written into the internal Storage.
	 * 
	 * @param encodedServerPublicKey
	 *            the encoded server public key
	 */
	private void writeServerPublicKey(String encodedServerPublicKey) {
		//TODO jeton: we need a new UserPublicKey request/resposne here!!!
//		InternalStorageXML.writePublicKeyIntoFile(getApplicationContext(), encodedServerPublicKey);
		//TODO jeton: delegate to ClientController
	}
	
	/**
	 * This method is called, when the server request fails. The user
	 * informations are retrieved from the internal storage.
	 */
	private void launchOfflineMode() {
		try {
			if (ClientController.loadUserAccountFromStorage()) {
				launchMainActivity();
			} else {
				displayResponse(getResources().getString(R.string.establish_internet_connection));
			}
		} catch (WrongPasswordException e) {
			displayResponse(getResources().getString(R.string.invalid_password));
		} catch (IOException e) {
			displayResponse(getResources().getString(R.string.establish_internet_connection));
		} catch (Exception e) {
			//TODO jeton: handle exception
		}
	}
	
	private void launchMainActivity(){
		storeUsernameIntoSharedPref();
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}
	
	/**
	 * Stores the username of the authenticated user.
	 */
	private void storeUsernameIntoSharedPref() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(getString(R.string.stored_username), username);
		editor.commit();
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