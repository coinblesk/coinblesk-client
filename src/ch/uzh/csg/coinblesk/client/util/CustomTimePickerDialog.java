/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.uzh.csg.coinblesk.client.util;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import ch.uzh.csg.coinblesk.client.util.CustomTimePicker.CustomOnTimeChangedListener;
import ch.uzh.csg.coinblesk.client.R;

/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}.
 */
public class CustomTimePickerDialog extends AlertDialog implements OnClickListener, CustomOnTimeChangedListener {

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnTimeSetListener {

        /**
         * @param view The view associated with this listener.
         * @param hourOfDay The hour that was set.
         */
        void onTimeSet(CustomTimePicker view, int hourOfDay);
    }

    private static final String HOUR = "hour";
    private static final String IS_24_HOUR = "is24hour";
    
    private final CustomTimePicker mTimePicker;
    private final OnTimeSetListener mCallback;
    private final Calendar mCalendar;
    
    int mInitialHourOfDay;
    boolean mIs24HourView;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param hourOfDay The initial hour.
     * @param is24HourView Whether this is a 24 hour view, or AM/PM.
     */
    public CustomTimePickerDialog(Context context,
            OnTimeSetListener callBack,
            int hourOfDay, boolean is24HourView) {
        this(context, R.style.Theme_Dialog_Alert,
                callBack, hourOfDay, is24HourView);
    }

    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     * @param hourOfDay The initial hour.
     * @param is24HourView Whether this is a 24 hour view, or AM/PM.
     */
    public CustomTimePickerDialog(Context context,
            int theme,
            OnTimeSetListener callBack,
            int hourOfDay, boolean is24HourView) {
        super(context, theme);
        mCallback = callBack;
        mInitialHourOfDay = hourOfDay;
        mIs24HourView = is24HourView;

        mCalendar = Calendar.getInstance();
        updateTitle(mInitialHourOfDay);
        setButton(DialogInterface.BUTTON_NEUTRAL, context.getText(R.string.date_time_set), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getText(R.string.cancel), (OnClickListener) null);
        setIcon(R.drawable.ic_device_access_time);
        
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_time_picker_dialog, null);
        setView(view);
        mTimePicker = (CustomTimePicker) view.findViewById(R.id.customTimePicker);

        // initialize state
        mTimePicker.setCurrentHour(mInitialHourOfDay);
        mTimePicker.setIs24HourView(mIs24HourView);
        mTimePicker.setOnTimeChangedListener(this);
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            mTimePicker.clearFocus();
            mCallback.onTimeSet(mTimePicker, mTimePicker.getCurrentHour());
        }
    }

    public void onTimeChanged(CustomTimePicker view, int hourOfDay) {
        updateTitle(hourOfDay);
    }
    
    public void updateTime(int hourOfDay, int minutOfHour) {
        mTimePicker.setCurrentHour(hourOfDay);
    }

    private void updateTitle(int hour) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hour);
        setTitle("Select pay out time");
    }
    
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(HOUR, mTimePicker.getCurrentHour());
        state.putBoolean(IS_24_HOUR, mTimePicker.is24HourView());
        return state;
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int hour = savedInstanceState.getInt(HOUR);
        updateTitle(hour);
    }

}
