package ch.uzh.csg.coinblesk.client.util;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.format.DateFormat;
import ch.uzh.csg.coinblesk.client.IAsyncTaskCompleteListener;

/**
 * This class creates the dialog for the time slot selection.
 */
@SuppressLint("ValidFragment")
public class TimePickerRule extends DialogFragment implements CustomTimePickerDialog.OnTimeSetListener{

	protected IAsyncTaskCompleteListener<Integer> callback;
	
	public TimePickerRule(IAsyncTaskCompleteListener<Integer> time) {
		callback = time;
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);

        return new CustomTimePickerDialog(getActivity(), this, hour, DateFormat.is24HourFormat(getActivity()));
    }

	/**
	 * After the time is selected the time is send to the parent activity.
	 */
	public void onTimeSet(CustomTimePicker view, int hourOfDay) {
		callback.onTaskComplete(hourOfDay);
	}
	
}
