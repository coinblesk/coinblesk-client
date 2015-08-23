package ch.uzh.csg.coinblesk.client.ui.adressbook;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.storage.model.AddressBookEntry;
import ch.uzh.csg.coinblesk.client.ui.baseactivities.BaseActivity;

/**
 * Activity for showing and modifying address book contacts. 
 *
 */
public class AddressBookActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_address_book);
		setUpGui();
		setupActionBar();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	private void setUpGui() {

		Button removeUntrusted = (Button) findViewById(R.id.addressBook_removeButton);
		removeUntrusted.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
				alert.setTitle(getString(R.string.addressBook_removeAllUntrustedButton));
				alert.setMessage(getString(R.string.addressBook_removeAllUntrusted_question));

				alert.setPositiveButton(getString(R.string.dialog_yes),
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						getCoinBleskApplication().getStorageHandler().removeAllUntrustedAddressBookEntries();
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
		createAddressBookEntries();
	}

	@SuppressLint("InflateParams")
	private void createAddressBookEntries() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout parent = (LinearLayout) findViewById(R.id.addressBookActivity_linearLayout);
		parent.removeAllViews();

		int i = 0;
		for (final AddressBookEntry entry : getCoinBleskApplication().getStorageHandler().getAddressBook()) {
			final String username = entry.getName();
			View custom = inflater.inflate(R.layout.addressbook_entry_layout, null);
			TextView tv = (TextView) custom.findViewById(R.id.textView1);
			tv.setGravity(Gravity.LEFT);

			final ImageView delete = (ImageView) custom.findViewById(R.id.imageView1);
			delete.setImageResource(R.drawable.ic_navigation_cancel_light);
			delete.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
					builder.setTitle(R.string.addressBook_dialogTitle)
					.setMessage(username)
					.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							getCoinBleskApplication().getStorageHandler().deleteAddressBookEntry(entry.getPublicKey());
							createAddressBookEntries();
						}
					})
					.setNegativeButton(R.string.dialog_no, null);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			});

			final ImageView trustedImage = (ImageView) custom.findViewById(R.id.imageView2);
			setTrustedImage(trustedImage, entry.isTrusted());

			trustedImage.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (entry.isTrusted()) {
						entry.setTrusted(false);
						trustedImage.setImageResource(R.drawable.ic_not_starred);
						showToast(getString(R.string.addressBook_removeContact));
					} else {
						entry.setTrusted(true);
						trustedImage.setImageResource(R.drawable.ic_starred);
						showToast(getString(R.string.addressBook_addContact));
					}

					getCoinBleskApplication().getStorageHandler().saveAddressBookEntry(entry);

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

	private void setTrustedImage(ImageView img, boolean isTrusted) {
		if (isTrusted) {
			img.setImageResource(R.drawable.ic_starred);
		} else {
			img.setImageResource(R.drawable.ic_not_starred);
		}
	}

}
