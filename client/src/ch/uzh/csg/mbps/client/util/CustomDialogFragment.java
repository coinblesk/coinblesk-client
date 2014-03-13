package ch.uzh.csg.mbps.client.util;

import ch.uzh.csg.mbps.client.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Creates the dialog window to pick the time slot in the payout rule view.
 */
public class CustomDialogFragment extends DialogFragment {
	
    public static CustomDialogFragment create(String title, int icon, String message) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("icon", icon);
		args.putString("message", message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        int icon = getArguments().getInt("icon");

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setIcon(icon)
                .setPositiveButton(R.string.dialog_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	dismiss();
                        }
                    }
                )
                .create();
    }
}