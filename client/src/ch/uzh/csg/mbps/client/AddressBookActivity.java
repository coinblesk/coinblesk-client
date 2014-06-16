package ch.uzh.csg.mbps.client;

import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import ch.uzh.csg.mbps.client.util.ClientController;

public class AddressBookActivity extends Activity {
	private MenuItem menuWarning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_address_book);
		setUpGui();
	}


	@Override
	public void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(ch.uzh.csg.mbps.client.R.menu.send_payment, menu);
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

	private void setUpGui() {
		Button addContact = (Button) findViewById(R.id.addressBook_addButton);
		addContact.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
				alert.setTitle(getString(R.string.addressBook_addContact_title));
				alert.setMessage(getString(R.string.addressBook_addContact_message));

				final EditText input = new EditText(v.getContext());
				alert.setView(input);

				alert.setPositiveButton(getString(R.string.addressBook_addContact_save),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String username = input.getText().toString();
								boolean saved = ClientController.getStorageHandler().addAddressBookEntry(username);
								if (!saved) {
									Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_xmlSave_failed), Toast.LENGTH_LONG).show(); 
								}
								createAddressBookEntries();
							}
						});
				alert.setNegativeButton(getString(R.string.addressBook_addContact_cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Dialog canceled
							}
						});
				alert.show();
			}
		});
		
		Button removeUntrusted = (Button) findViewById(R.id.addressBook_removeButton);
		removeUntrusted.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				boolean saved = ClientController.getStorageHandler().removeAllUntrustedAddressBookEntries();
				if (!saved) {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_xmlSave_failed), Toast.LENGTH_LONG).show();
				}
				createAddressBookEntries();
			}
		});
		createAddressBookEntries();
	}

	private void createAddressBookEntries() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout parent = (LinearLayout) findViewById(R.id.addressBookActivity_linearLayout);
		parent.removeAllViews();

		int i = 0;
		for (Iterator<String> it = ClientController.getStorageHandler().getAddressBook().iterator(); it.hasNext();) {
			final String username = it.next();
			View custom = inflater.inflate(R.layout.addressbook_entry_layout, null);
			TextView tv = (TextView) custom.findViewById(R.id.textView1);
			tv.setGravity(Gravity.LEFT);

			final ImageView delete = (ImageView) custom.findViewById(R.id.imageView1);
			delete.setImageResource(R.drawable.ic_navigation_cancel_light);
			delete.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Builder builder = new AlertDialog.Builder(v.getContext());
					builder.setTitle(R.string.addressBook_dialogTitle)
					.setMessage(username)
					.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							boolean saved = ClientController.getStorageHandler().removeAddressBookEntry(username);
							if (!saved) {
								Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_xmlSave_failed), Toast.LENGTH_LONG).show();
							}
							createAddressBookEntries();
						}
					})
					.setNegativeButton(R.string.dialog_no, null);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			});

			final ImageView trusted = (ImageView) custom.findViewById(R.id.imageView2);
			setTrustedImage(trusted, username);
			trusted.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (ClientController.getStorageHandler().isTrustedContact(username)){
						boolean saved = ClientController.getStorageHandler().removeTrustedAddressBookEntry(username);
						if (!saved) {
							Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_xmlSave_failed), Toast.LENGTH_LONG).show();
						}
						trusted.setImageResource(R.drawable.ic_not_starred);
						showToast(getString(R.string.addressBook_removeContact));
					}
					else{
						boolean saved = ClientController.getStorageHandler().addTrustedAddressBookEntry(username);
						if (!saved) {
							Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_xmlSave_failed), Toast.LENGTH_LONG).show();
						}
						trusted.setImageResource(R.drawable.ic_starred);
						showToast(getString(R.string.addressBook_addContact));
					}
				}
			});

			tv.setText(username);
			tv.setTextColor(Color.BLACK);
			if(i % 2 == 0){
				custom.setBackgroundColor(Color.LTGRAY);
			}
			parent.addView(custom);
			i++;
		}
	}

	private void showToast(String message){
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private void setTrustedImage(ImageView img, String username) {
		if (ClientController.getStorageHandler().isTrustedContact(username)) {
			img.setImageResource(R.drawable.ic_starred);
		} else {
			img.setImageResource(R.drawable.ic_not_starred);
		}
	}

}
