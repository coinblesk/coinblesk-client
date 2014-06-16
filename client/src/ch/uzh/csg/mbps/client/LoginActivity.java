package ch.uzh.csg.mbps.client;

import java.security.KeyPair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.internalstorage.WrongPasswordException;
import ch.uzh.csg.mbps.client.request.CommitPublicKeyRequestTask;
import ch.uzh.csg.mbps.client.request.ReadRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SignInRequestTask;
import ch.uzh.csg.mbps.client.security.KeyHandler;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.client.util.TimeHandler;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.keys.CustomPublicKey;
import ch.uzh.csg.mbps.model.UserAccount;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;

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
	
	private CustomKeyPair customKeyPair;
	
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
	
	private void launchSignInRequest() {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(this);
		UserAccount user = new UserAccount(username, null, password);
		RequestTask signIn = new SignInRequestTask(this, user);
		signIn.execute();
	}
	
	public void onTaskComplete(CustomResponseObject response) {
		try {
			boolean init = ClientController.init(getApplicationContext(), username, password);
			if (!init) {
				//TODO: display message that not saved to xml --> not able to use offline!
			}
		} catch (WrongPasswordException e) {
			displayResponse(getResources().getString(R.string.invalid_password));
			return;
		}
		
		if (response.isSuccessful()) {
			if (response.getType() == Type.LOGIN) {
				launchReadRequest();
			} else if (response.getType() == Type.AFTER_LOGIN) {
				boolean saved = ClientController.getStorageHandler().saveServerPublicKey(response.getServerPublicKey());
				if (!saved) {
					//TODO: display message that not saved to xml --> not able to use offline!
				}
				saved = ClientController.getStorageHandler().saveUserAccount(response.getReadAccountTO().getUserAccount());
				if (!saved) {
					//TODO: display message that not saved to xml --> not able to use offline!
				}
				
				CustomKeyPair ckp = ClientController.getStorageHandler().getKeyPair();
				if (ckp == null) {
					try {
						KeyPair keyPair = KeyHandler.generateKeyPair();
						ckp = new CustomKeyPair(PKIAlgorithm.DEFAULT.getCode(), (byte) 0, KeyHandler.encodePublicKey(keyPair.getPublic()), KeyHandler.encodePrivateKey(keyPair.getPrivate()));
						this.customKeyPair = ckp;
						
						launchCommitKeyRequest(ckp);
						return;
					} catch (Exception e) {
						//TODO: show error that keys not created, cannot use the app for payment (if you see this message repeatedly, try uninstalling and installing again, you will loose no information)
					}
				}
				ClientController.setOnlineMode(true);
				launchMainActivity();
			} else if (response.getType() == Type.SAVE_PUBLIC_KEY) {
				String keyNr = response.getMessage();
				byte keyNumber = Byte.parseByte(keyNr);
				
				CustomKeyPair ckp = new CustomKeyPair(customKeyPair.getPkiAlgorithm(), keyNumber, customKeyPair.getPublicKey(), customKeyPair.getPrivateKey());
				boolean saved = ClientController.getStorageHandler().saveKeyPair(ckp);
				if (!saved) {
					//TODO: display message that not saved to xml --> not able to use offline!
				}
				
				dismissProgressDialog();
				ClientController.setOnlineMode(true);
				launchMainActivity();
			}
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			dismissProgressDialog();
			launchOfflineMode();
		} else {
			dismissProgressDialog();
			displayResponse(response.getMessage());
		}
	}
	
	private void launchReadRequest() {		
		RequestTask read = new ReadRequestTask(this);
		read.execute();
	}
	
	private void launchCommitKeyRequest(CustomKeyPair ckp) {
		CustomPublicKey cpk = new CustomPublicKey(ckp.getKeyNumber(), ckp.getPkiAlgorithm(), ckp.getPublicKey());
		new CommitPublicKeyRequestTask(this, cpk).execute();
	}
	
	/**
	 * This method is called, when the server request fails. The user
	 * informations are retrieved from the internal storage.
	 */
	private void launchOfflineMode() {
		if (ClientController.getStorageHandler().getUserAccount() != null) {
			launchMainActivity();
		} else {
			displayResponse(getResources().getString(R.string.establish_internet_connection));
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