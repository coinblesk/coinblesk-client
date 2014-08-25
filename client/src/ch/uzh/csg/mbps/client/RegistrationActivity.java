package ch.uzh.csg.mbps.client;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.request.SignUpRequestTask;
import ch.uzh.csg.mbps.client.util.CheckFormatHandler;
import ch.uzh.csg.mbps.responseobject.TransferObject;
import ch.uzh.csg.mbps.responseobject.UserAccountObject;

/**
 * This class is the view where a user can create an account.
 */
public class RegistrationActivity extends AbstractAsyncActivity {
	private PopupWindow popupWindow;
	private String username;
	private String email;
	private String password;
	private String confirmPassword;
	private CheckBox termOfUseChecked;
	private Button createAccountBtn;
  	private TextView termOfUse;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		setScreenOrientation();
	  	
		createAccountBtn = (Button) findViewById(R.id.signUpAgreeBtn);
		termOfUse = (TextView) findViewById(R.id.signUpTermOfUseTextView);
		
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

	private void initClickListener() {
		
	  	createAccountBtn.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
	  			initInputInformation();
	  			Pair<Boolean, String> responseContent = CheckFormatHandler.checkRegistrationInputs(getApplicationContext(), username, email, password, confirmPassword, termOfUseChecked);
				if (responseContent.first) {
					launchCreateRequest();
				} else {
					displayResponse(responseContent.second);
				}
	  		}
	  	});
	  	
	  	termOfUse.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showPopupWindow();
			}
		});
		
	}

	private void initInputInformation() {
		username = ((EditText) findViewById(R.id.signUpEditUsernameText)).getText().toString();
		email = ((EditText) findViewById(R.id.signUpEditEmailText)).getText().toString();
		password = ((EditText) findViewById(R.id.signUpEditPasswordText)).getText().toString();
		confirmPassword = ((EditText) findViewById(R.id.signUpEditConfirmPasswordText)).getText().toString();
		termOfUseChecked = (CheckBox)findViewById(R.id.signUpCheckBox);
	}
	
	private void launchCreateRequest() {
		showLoadingProgressDialog();
		UserAccountObject user = new UserAccountObject();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(password);
		
		RequestTask<UserAccountObject, TransferObject> signUp = new SignUpRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
			
			public void onTaskComplete(TransferObject response) {
				dismissProgressDialog();
				if (response.isSuccessful()) {
					buildDialog(getResources().getString(R.string.registration_successful));
				}else{
					displayResponse(response.getMessage());			
				}
				
			}
		}, user, new TransferObject());
		signUp.execute();
	}
	
	private void buildDialog(String message) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.createAccountSuccessful)
				.setMessage(message)
				.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	private void showPopupWindow(){	
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup group = (ViewGroup) findViewById(R.id.termOfUseLayout);
		View layout = inflater.inflate(R.layout.activity_popup_layout, group);
		popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
		popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
		
		final ImageButton closeBtn = (ImageButton) layout.findViewById(R.id.termOfUseImageCloseBtn);
		closeBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				popupWindow.dismiss();
			}
		});
	}
	
	@Override
	public void finish(){
		Intent data = new Intent();
		data.setClass(RegistrationActivity.this, LoginActivity.class);
		data.putExtra("username", this.username);
		setResult(RESULT_OK,data);
		startActivity(data);
		super.finish();
	}

}