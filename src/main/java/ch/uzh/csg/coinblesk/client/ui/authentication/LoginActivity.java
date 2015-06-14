package ch.uzh.csg.coinblesk.client.ui.authentication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import ch.uzh.csg.coinblesk.client.R;

/**
 * The Login Activity is the first view of the application. The user has to sign
 * in with the username and password to use the application.
 */
public class LoginActivity extends AbstractLoginActivity {
	// constant to determine which sub-activity returns
	
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String FILE_REQUEST = "request.coinblesk";
	private static final String FILE_TOKEN = "token.coinblesk";
	
	private static final int REQUEST_CODE = 1;
	private Button signInBtn;
	private Button signUpBtn;
	private TextView resetPassword;
	
	private static File lastToken;
	
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

    	initUSBToken();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        initSignInBtn();
    }

    private void initUSBToken() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addDataScheme("file");
		filter.setPriority(999);
		
		final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

			@Override
		    public void onReceive(Context context, Intent intent) {
				String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		        String action = intent.getAction();
		        if(action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
		        	String mountPoint = intent.getDataString();
		        	if(mountPoint != null) {
		        		//search for e.g., file in file:///storage/usbdisk0
		        		File token = file(mountPoint, FILE_TOKEN);
		        		if(token != null) {
		        			lastToken = token;
		        		}
		        		File request = file(mountPoint, FILE_REQUEST);
		        		if(request!=null && token!=null && request.exists() && !token.exists() && token.canWrite()) {
		        			request.delete();
		        			String username = ((EditText) findViewById(  R.id.loginUsernameEditText)).getText().toString();
		        			String password = ((EditText) findViewById(  R.id.loginPasswordEditText)).getText().toString();
		        			storeToken(token, androidId, username, password);
		        		}
		        		
		        		if(token!=null && token.exists() && token.canRead() && token.length() > 0) {
		        			Properties p = readToken(token, androidId);
		        			if(p!=null) {
		        				((EditText) findViewById(R.id.loginUsernameEditText)).setText(p.getProperty(USERNAME));
		        				((EditText) findViewById(R.id.loginPasswordEditText)).setText(p.getProperty(PASSWORD));
		        				((Button)findViewById(R.id.loginSignInBtn)).performClick();
		        			}
		        		}
		        	}
		            
		        }
		    }

			
		};
		registerReceiver(broadcastReceiver, filter);
    }
    
    private static File file(String mountPoint, String name) {
		String tokenFile = mountPoint + File.separator+ name;
		File file;
        try {
            file = new File(new URI(tokenFile));
            return file;
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return null;
        }
	}
	
	private static void storeToken(File token, String tokenPassword, String username, String password) {
		try {
			final Properties p = new Properties();
			p.setProperty("username", username);
			p.setProperty("password", password);
			StringWriter sw = new StringWriter();
			p.store(sw, "login-data");
			byte[] data3 = encrypt(tokenPassword.getBytes(), sw.toString().getBytes());
			FileOutputStream fos = new FileOutputStream(token);
			fos.write(data3);
			fos.close();
			System.err.println("wrote password!");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	private static Properties readToken(File token, String password) {
		byte[] data = new byte[(int) token.length()];
		try {
			FileInputStream fis = new FileInputStream(token);
			fis.read(data);
			fis.close();
			//
			byte[] data2 = decrypt(password.getBytes(), data);
			final Properties p = new Properties();
			StringReader sr = new StringReader(new String(data2));
		    p.load(sr);
			sr.close();
			return p;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
    
    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
    
	private void retrieveLastSignedUsername() {
		SharedPreferences sharedPref = getSharedPreferences(getResources().getString(R.string.stored_username), MODE_PRIVATE);
		String storedUsername = sharedPref.getString(getString(R.string.stored_username), "");
		EditText usernameEditText = (EditText) findViewById(R.id.loginUsernameEditText);
		usernameEditText.setText(storedUsername);
		if (!storedUsername.isEmpty()) {
			username = storedUsername;
			EditText password = (EditText) findViewById(R.id.loginPasswordEditText);
			password.requestFocus();
		}
	}

	private void initSignInBtn() {
		signInBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				username = ((EditText) findViewById(R.id.loginUsernameEditText)).getText().toString();
				password = ((EditText) findViewById(R.id.loginPasswordEditText)).getText().toString();

				if (lastToken != null && lastToken.exists() && lastToken.canRead() && lastToken.length() > 0 &&
						(username.isEmpty() || password.isEmpty())) {
					String androidId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
					Properties p = readToken(lastToken, androidId);
					if (p != null) {
						if (username.isEmpty()) {
							username = p.getProperty(USERNAME);
						}
						if (password.isEmpty()) {
							password = p.getProperty(PASSWORD);
						}
					}
				}

				if (username.isEmpty() || password.isEmpty())
					displayResponse(getResources().getString(R.string.enter_username_password));
				else {
					// hide virtual keyboard
					InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					launchSignInRequest(getApplicationContext());
				}
			}
		});

		signInBtn.setEnabled(true);
	}

	private void initClickListener() {

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