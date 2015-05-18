package ch.uzh.csg.coinblesk.client;

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
import ch.uzh.csg.coinblesk.client.persistence.WrongPasswordException;
import ch.uzh.csg.coinblesk.client.request.CommitPublicKeyRequestTask;
import ch.uzh.csg.coinblesk.client.request.ReadRequestTask;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.request.SignInRequestTask;
import ch.uzh.csg.coinblesk.client.security.KeyHandler;
import ch.uzh.csg.coinblesk.client.ui.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.Constants;
import ch.uzh.csg.coinblesk.client.util.TimeHandler;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.customserialization.PKIAlgorithm;
import ch.uzh.csg.coinblesk.keys.CustomKeyPair;
import ch.uzh.csg.coinblesk.keys.CustomPublicKey;
import ch.uzh.csg.coinblesk.responseobject.CustomPublicKeyObject;
import ch.uzh.csg.coinblesk.responseobject.ReadRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * Abstract class with sign in request functionality. Takes care of logging in and saving
 * relevant information to local xml file.
 */
public abstract class AbstractLoginActivity extends WalletActivity {
	protected static String username;
	protected static String password;

	private MenuItem menuWarning;
	private MenuItem offlineMode;
	private MenuItem sessionCountdownMenuItem;
	private MenuItem sessionRefreshMenuItem;
	private TextView sessionCountdown;
	private CountDownTimer timer;
	private boolean wrongPassword = false;

	protected static CustomKeyPair customKeyPair;

	private boolean clientControllerInitialized = false;

	public static void updatePassword(){
		password = ClientController.getStorageHandler().getUserAccount().getPassword();
	}

	/**
	 * Launch Sign in request to connect to server and launch session.
	 */
	protected void launchSignInRequest(final Context context) {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(context);
		RequestTask<TransferObject, TransferObject> signIn = new SignInRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
			@Override
			public void onTaskComplete(TransferObject response) {
				init(context);
				if(response.isSuccessful()) {
					launchReadRequest(context);
				} else if (response.isUnauthorized()) {
					dismissProgressDialog();
					displayResponse(context.getResources().getString(R.string.error_login));
				} else {
					launchOfflineMode(context);
				}
			}
		}, username, password, context);
		signIn.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		initializeMenuItems(menu, getApplicationContext());
		invalidateOptionsMenu();
		return true;
	}

	/**
	 * Initializes option menu items for indicating offline mode, refresh
	 * session and session countdown.
	 * 
	 * @param menu
	 */
	protected void initializeMenuItems(Menu menu, final Context context){
		menuWarning = menu.findItem(R.id.action_warning);
		offlineMode = menu.findItem(R.id.menu_offlineMode);
		TextView offlineModeTV = (TextView) offlineMode.getActionView();
		offlineModeTV.setText(getResources().getString(R.string.menu_offlineModeText));

		menuWarning.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchSignInRequest(context);
				return false;
			}
		});

		//setup timer
		sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
		sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
		sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);
		sessionRefreshMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				launchSignInRequest(context);
				return false;
			}
		});
	}

	@Override
	public void invalidateOptionsMenu() {
		if(menuWarning != null){
			if(ClientController.isOnline()) {
				menuWarning.setVisible(false);
				offlineMode.setVisible(false);
				sessionCountdownMenuItem.setVisible(true);
				sessionRefreshMenuItem.setVisible(true);
			} else {
				menuWarning.setVisible(true);
				offlineMode.setVisible(true);
				sessionCountdownMenuItem.setVisible(false);
				sessionRefreshMenuItem.setVisible(false);
			}
		}
	}

	/**
	 * Start Timer for Session Countdown in Options Menu.
	 * 
	 * @param duration Time left in milliseconds.
	 * @param interval Interval in which timer is updated in milliseconds.
	 */
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
	
	public void init(Context context) {	    
	    if (!clientControllerInitialized) {
			try {
				boolean init = ClientController.init(context, username, password);
				clientControllerInitialized = init;
			} catch (WrongPasswordException e) {
				wrongPassword = true;
				displayResponse(context.getResources().getString(R.string.invalid_password));
				clientControllerInitialized = false;
			}
		}
	}

	/**
	 * This method is called, when the server request fails. The user
	 * informations are retrieved from the internal storage.
	 */
	protected void launchOfflineMode(Context context) {
		dismissProgressDialog();
		if (ClientController.getStorageHandler().getUserAccount() != null) {
			launchMainActivity(context);
		} else {
			if (!wrongPassword)
				displayResponse(context.getResources().getString(R.string.establish_internet_connection));
		}
	}

	private void launchReadRequest(final Context context) {		
		RequestTask<TransferObject, ReadRequestObject> read = new ReadRequestTask(new IAsyncTaskCompleteListener<ReadRequestObject>() {
			
			public void onTaskComplete(ReadRequestObject response) {
				dismissProgressDialog();
				
				 if (!response.isSuccessful()) {
					 displayResponse(response.getMessage());
					 return;
				 }
				
				if (response.getVersion() != Constants.CLIENT_VERSION) {
					showDialog(context.getResources().getString(R.string.invalid_client_version_title), R.drawable.ic_alerts_and_states_warning, context.getResources().getString(R.string.invalid_client_version));
					return;
				}

				boolean saved = ClientController.getStorageHandler().saveServerPublicKey(response.getCustomPublicKey().getCustomPublicKey());
				if (!saved) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}
				saved = ClientController.getStorageHandler().saveUserAccount(response.getUserAccount());
				if (!saved) {
					displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
				}

				CustomKeyPair ckp = ClientController.getStorageHandler().getKeyPair();
				if (ckp == null) {
					try {
						KeyPair keyPair = KeyHandler.generateKeyPair();
						ckp = new CustomKeyPair(PKIAlgorithm.DEFAULT.getCode(), (byte) 0, KeyHandler.encodePublicKey(keyPair.getPublic()), KeyHandler.encodePrivateKey(keyPair.getPrivate()));
						customKeyPair = ckp;

						launchCommitKeyRequest(context, ckp);
						return;
					} catch (Exception e) {
						displayResponse(context.getResources().getString(R.string.error_xmlSave_failed));
					}
				} else if (response.getMessage() == null) {
					customKeyPair = ckp;
					launchCommitKeyRequest(context, ckp);
					return;
				}
				ClientController.setOnlineMode(true);
				launchMainActivity(context);
			}
		}, new TransferObject(), new ReadRequestObject(), context);
		read.execute();
	}

	private void launchCommitKeyRequest(final Context context, CustomKeyPair ckp) {
		showLoadingProgressDialog();
		
		CustomPublicKey cpk = new CustomPublicKey(ckp.getKeyNumber(), ckp.getPkiAlgorithm(), ckp.getPublicKey());
		CustomPublicKeyObject cpko = new CustomPublicKeyObject();
		cpko.setCustomPublicKey(cpk);
		
		RequestTask<CustomPublicKeyObject, TransferObject> task = new CommitPublicKeyRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
			@Override
			public void onTaskComplete(TransferObject response) {
				dismissProgressDialog();
				if (response.isSuccessful()) {
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
				} else if (response.isUnauthorized()) {
					//TODO: what else? fix
					dismissProgressDialog();
				} else {
					//TODO: what else? fix
					dismissProgressDialog();
				}
			}
		}, cpko, new CustomPublicKeyObject(), context);
		task.execute();
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
