package ch.uzh.csg.mbps.client.util;

import android.widget.NumberPicker.Formatter;

/**
 * This class has a format of two digits to represent a time slot in the dialog.
 */
public class TwoDigitFormatter implements Formatter {

	final StringBuilder mBuilder = new StringBuilder();
	final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
	final Object[] mArgs = new Object[1];

	public String format(int value) {
		mArgs[0] = value;
		mBuilder.delete(0, mBuilder.length());
		mFmt.format("%02d", mArgs);
		return mFmt.toString();
	}

}
