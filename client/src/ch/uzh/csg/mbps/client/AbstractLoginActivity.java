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
	private MenuItem offlineMode;
	private MenuItem sessionCountdownMenuItem;
	private MenuItem sessionRefreshMenuItem;
	private TextView sessionCountdown;
	private CountDownTimer timer;

	protected static CustomKeyPair customKeyPair;

	private boolean clientControllerInitialized = false;

	public static void updatePassword(){
		password = ClientController.getStorageHandler().getUserAccount().getPassword();
	}

	/**
	 * Launch Sign in request to connect to server and launch session.
	 */
	protected void launchSignInRequest() {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(this);
		UserAccount user = new UserAccount(username, null, password);
		RequestTask signIn = new SignInRequestTask(this, user);
		signIn.execute();
	}

	/**
	 * Launches Sign In Request with Context as parameter. Used for Reconnecting
	 * to Server from Navigation Drawer.
	 * 
	 * @param Appliation Context
	 */
	protected void launchSignInRequest(Context context) {
		showLoadingProgressDialog();
		TimeHandler.getInstance().setStartActivity(context);
		UserAccount user = new UserAccount(username, null, password);
		RequestTask signIn = new SignInRequestTask(this, user);
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
		initializeMenuItems(menu);
		invalidateOptionsMenu();
		return true;
	}

	/**
	 * Initializes option menu items for indicating offline mode, refresh
	 * session and session countdown.
	 * 
	 * @param menu
	 */
	protected void initializeMenuItems(Menu menu){
		menuWarning = menu.findItem(R.id.action_warning);
		offlineMode = menu.findItem(R.id.menu_offlineMode);
		TextView offlineModeTV = (TextView) offlineMode.getActionView();
		offlineModeTV.setText(getResources().getString(R.string.menu_offlineModeText));

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

	/**
	 * Handles response from http server call. Differentiates the server call
	 * based on given Type.
	 * 
	 * @param response from server
	 * @param context (application context)
	 */
	public void onTaskComplete(CustomResponseObject response, Context context) {
		boolean wrongPassword = false;
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
				} else if (response.getMessage() == null) {
					customKeyPair = ckp;
					launchCommitKeyRequest(ckp);
					return;
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
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR) || response.getMessage().equals(Constants.CONNECTION_ERROR) ) {
			dismissProgressDialog();
			displayResponse(getResources().getString(R.string.no_connection_server));
			launchOfflineMode(context);
		} else if (response.getMessage().equals("UNAUTHORIZED")) {
			dismissProgressDialog();
			if (!wrongPassword)
				displayResponse(getResources().getString(R.string.error_login));
		} else {
			dismissProgressDialog();
			displayResponse(response.getMessage());
		}
	}

	/**
	 * This method is called, when the server request fails. The user
	 * informations are retrieved from the internal storage.
	 */
	protected void launchOfflineMode(Context context) {
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
