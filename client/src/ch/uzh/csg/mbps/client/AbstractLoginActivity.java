package ch.uzh.csg.mbps.client;

import java.security.KeyPair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
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
 * Abstract class with sign in request functionality. Takes care of logging in and saving
 * relevant information to local xml file.
 */
public abstract class AbstractLoginActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
	protected static String username;
	protected static String password;
	
	private MenuItem menuWarning;
	private MenuItem sessionCountdownMenuItem;
	private MenuItem sessionRefreshMenuItem;
	private TextView sessionCountdown;
	private CountDownTimer timer;
	
	protected static CustomKeyPair customKeyPair;
	
	protected boolean clientControllerInitialized = false;
	
	// TODO simon: sets abstractloginactivity as startActivity for TimeHandler,
	// app crashes after timeout if launched from DrawerItemClickListener
	
	protected void launchSignInRequest() {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(this);
		UserAccount user = new UserAccount(username, null, password);
		RequestTask signIn = new SignInRequestTask(this, user);
		signIn.execute();
	}
	
	//TODO simon: create one menutemplate for all activities
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		initializeMenuItems(menu);
		invalidateOptionsMenu();
		return true;
	}
	
	protected void initializeMenuItems(Menu menu){
		menuWarning = menu.findItem(R.id.action_warning);
		menuWarning.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchSignInRequest();
				return false;
			}
		});

		//setup timer
		sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
		sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
		sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);
		sessionRefreshMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchSignInRequest();
				return false;
			}
		});
	}
	
	@Override
	public void invalidateOptionsMenu() {
		if(menuWarning != null){
			if(ClientController.isOnline()) {
				menuWarning.setVisible(false);
				sessionCountdownMenuItem.setVisible(true);
				sessionRefreshMenuItem.setVisible(true);
			} else {
				menuWarning.setVisible(true);
				sessionCountdownMenuItem.setVisible(false);
				sessionRefreshMenuItem.setVisible(false);
			}
		}
	}
	
	protected void startTimer(long duration, long interval) {
		if(timer != null){
			timer.cancel();
		}
		timer = new CountDownTimer(duration, interval) {

			@Override
			public void onFinish() {
				//Session Timeout is already handled by TimeHandler
			}

			@Override
			public void onTick(long millisecondsLeft) {
				int secondsLeft = (int) Math.round((millisecondsLeft / (double) 1000));
				sessionCountdown.setText(getResources().getString(R.string.menu_sessionCountdown) + " " + TimeHandler.getInstance().formatCountdown(secondsLeft));
			}
		};

		timer.start();
	}
	
	public void onTaskComplete(CustomResponseObject response, Context context) {
		if (!clientControllerInitialized) {
			try {
				boolean init = ClientController.init(context, username, password);
				if (!init) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}
				clientControllerInitialized = true;
			} catch (WrongPasswordException e) {
				displayResponse(context.getResources().getString(R.string.invalid_password));
				return;
			}
		}
		
		if (response.isSuccessful()) {
			if (response.getType() == Type.LOGIN) {
				launchReadRequest();
			} else if (response.getType() == Type.AFTER_LOGIN) {
				dismissProgressDialog();
				if (response.getClientVersion() != Constants.CLIENT_VERSION) {
					showDialog(context.getResources().getString(R.string.invalid_client_version_title), R.drawable.ic_alerts_and_states_warning, context.getResources().getString(R.string.invalid_client_version));
					return;
				}
				
				boolean saved = ClientController.getStorageHandler().saveServerPublicKey(response.getServerPublicKey());
				if (!saved) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}
				saved = ClientController.getStorageHandler().saveUserAccount(response.getReadAccountTO().getUserAccount());
				if (!saved) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}
				
				CustomKeyPair ckp = ClientController.getStorageHandler().getKeyPair();
				if (ckp == null) {
					try {
						KeyPair keyPair = KeyHandler.generateKeyPair();
						ckp = new CustomKeyPair(PKIAlgorithm.DEFAULT.getCode(), (byte) 0, KeyHandler.encodePublicKey(keyPair.getPublic()), KeyHandler.encodePrivateKey(keyPair.getPrivate()));
						customKeyPair = ckp;
						
						launchCommitKeyRequest(ckp);
						return;
					} catch (Exception e) {
						displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
					}
				}
				ClientController.setOnlineMode(true);
				launchMainActivity(context);
			} else if (response.getType() == Type.SAVE_PUBLIC_KEY) {
				String keyNr = response.getMessage();
				byte keyNumber = Byte.parseByte(keyNr);
				
				CustomKeyPair ckp = new CustomKeyPair(customKeyPair.getPkiAlgorithm(), keyNumber, customKeyPair.getPublicKey(), customKeyPair.getPrivateKey());
				boolean saved = ClientController.getStorageHandler().saveKeyPair(ckp);
				if (!saved) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}
				
				dismissProgressDialog();
				ClientController.setOnlineMode(true);
				launchMainActivity(context);
			}
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			dismissProgressDialog();
			launchOfflineMode(context);
		} else {
			dismissProgressDialog();
			displayResponse(response.getMessage());
		}
	}
	
	/**
	 * This method is called, when the server request fails. The user
	 * informations are retrieved from the internal storage.
	 */
	private void launchOfflineMode(Context context) {
		if (ClientController.getStorageHandler().getUserAccount() != null) {
			launchMainActivity(context);
		} else {
			displayResponse(context.getResources().getString(R.string.establish_internet_connection));
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
	
	private void launchMainActivity(Context context){
		storeUsernameIntoSharedPref(context);
		Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
		finish();
	}
	
	/**
	 * Stores the username of the authenticated user.
	 */
	private void storeUsernameIntoSharedPref(Context context) {
		SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.stored_username), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(context.getResources().getString(R.string.stored_username), username);
		editor.commit();
	}
	
}
