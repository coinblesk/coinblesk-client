package ch.uzh.csg.mbps.client.profile;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.LoginActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.request.DeleteRequestTask;
import ch.uzh.csg.mbps.client.request.RequestTask;
import ch.uzh.csg.mbps.client.util.ClientController;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

/**
 * This class is the view for a user's profile informations. The password and
 * email address are editable.
 */
public class AccountProfileActivity extends AbstractAsyncActivity implements IAsyncTaskCompleteListener<CustomResponseObject>{
    private MenuItem menuWarning; 
    private ImageButton editEmailImgBtn;
  	private ImageButton editPasswordImgBtn;
	private Button deleteAccountBtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_account);
		setScreenOrientation();
		
		editEmailImgBtn = (ImageButton) findViewById(R.id.profileEditEmailBtn);
	  	editPasswordImgBtn = (ImageButton) findViewById(R.id.profileEditPasswordBtn);
		deleteAccountBtn = (Button)findViewById(R.id.profileEditDeletedUserAccountBtn);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setFields();
		initClickListener();
		checkOnlineModeAndProceed();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setFields();
	    checkOnlineModeAndProceed();
	    invalidateOptionsMenu();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.offline_mode, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        invalidateOptionsMenu();
        return true;
    }
    
	@Override
	public void invalidateOptionsMenu() {
		if (menuWarning != null) {
			if (ClientController.isOnline()) {
				menuWarning.setVisible(false);
			} else {
				menuWarning.setVisible(true);
			}
		}
	}
	
	private void setFields() {
		TextView username = (TextView) findViewById(R.id.profileUsernameText);
		TextView email = (TextView) findViewById(R.id.profileEmailText);

		username.setText(ClientController.getStorageHandler().getUserAccount().getUsername());
		email.setText(ClientController.getStorageHandler().getUserAccount().getEmail());
	}
	
	private void initClickListener() {
    	editEmailImgBtn.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
	  			launchActivity(AccountProfileActivity.this, EditEmailAccountProfileActivity.class);
	  		}
	  	});
	  	
	  	editPasswordImgBtn.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
	  			launchActivity(AccountProfileActivity.this, EditPasswordAccountProfileActivity.class);
	  		}
	  	});
	  	
	  	deleteAccountBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				buildDialog(v);
			}
		});
	}
	
	private void buildDialog(View v) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_dialog_title)
		.setMessage(R.string.delete_dialog_message)
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				launchDeleteRequest();
			}
		})
		.setNegativeButton(R.string.dialog_no, null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	private void launchDeleteRequest() {
		showLoadingProgressDialog();
		RequestTask delete = new DeleteRequestTask(this);
		delete.execute();
	}
	
	public void onTaskComplete(CustomResponseObject response) {
		if (response.isSuccessful()) {
			ClientController.clear();
			launchActivity();
		} else if (response.getMessage().equals(Constants.REST_CLIENT_ERROR)) {
			reload(getIntent());
			invalidateOptionsMenu();
		}
		dismissProgressDialog();
		displayResponse(response.getMessage());
	}
	
    private void launchActivity() {
    	Intent intent = new Intent(this, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS );
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		finish();
		startActivity(intent);
	}

	private void checkOnlineModeAndProceed() {
    	if(!ClientController.isOnline()){
			editEmailImgBtn.setEnabled(false);
			editPasswordImgBtn.setEnabled(false);
			deleteAccountBtn.setEnabled(false);
			deleteAccountBtn.setBackgroundColor(Color.GRAY);
		}
	}

}