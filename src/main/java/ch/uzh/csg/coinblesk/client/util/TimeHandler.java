package ch.uzh.csg.coinblesk.client.util;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.util.Log;
import ch.uzh.csg.coinblesk.client.ui.authentication.LoginActivity;

/**
 * This class is responsible for the time session on the client site. This class
 * checks after a predefined time if the time for the logged user is expired.
 */
public class TimeHandler extends HandlerThread{

	private static TimeHandler mTimeHandler;
	private static Handler mHandler;
	private static long mStartTime;
	private static Context context;
	
	public TimeHandler() {
		super("TimeSessionHandler");
		start();
	}
	
	public static TimeHandler getInstance() {
		if (mTimeHandler == null) {
			mTimeHandler = new TimeHandler();
		}
		return mTimeHandler;
	}
	
	/**
	 * Sets the start time when a request to the server was successful.
	 */
	public void setStartTime() {
		mStartTime = System.currentTimeMillis();
		if (mHandler == null) {
			mHandler = new Handler(getLooper());
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, Constants.CLIENT_CONNECTION_TIMEOUT);
		}
	}

	/**
	 * Sets the context of the activity. This is used to terminate the session.
	 * 
	 * @param context
	 *            Context of the activity in which the MBPS starts.
	 */
	public void setStartActivity(Context startActivity){
		context = startActivity;
	}
	
	/**
	 * Removes active runnable and sets the handler back to null.
	 */
	public void terminateSession() {
		if (mHandler != null) {
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.getLooper().quit();
			mHandler = null;
			mTimeHandler = null;
		}
	}
	
	public void terminateSession2(){
		mHandler.removeCallbacks(mUpdateTimeTask);
	}
	
	/**
	 * Checks if the request to server is done in the last five seconds before
	 * the session expires.
	 * 
	 * @return Returns true if the only five or less seconds are left for session to
	 *         be expired.
	 */
	public boolean determineIfLessThanFiveSecondsLeft(){
		long now = System.currentTimeMillis();
		Log.i("LessThanFive", "Time");
		if ((now - mStartTime) >= (Constants.CLIENT_SESSION_TIMEOUT - Constants.CLIENT_CONNECTION_TIMEOUT) && ClientController.isOnline()) {
			return true;
		}
		return false;
	}
	
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			Long now = System.currentTimeMillis();
			if ((now - mStartTime) >= Constants.CLIENT_SESSION_TIMEOUT) {
				Intent intent = new Intent(context, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
				ClientController.clear();
				terminateSession();
			} else {
				mHandler.postDelayed(mUpdateTimeTask, Constants.CLIENT_CHECK_TIME_INTERVAL);
			}
		}
	};
	
	/**
	 * Returns the remaining time as a long until client session is ended and will be logged out.
	 * @return remaining Session Time as long
	 */
	public long getRemainingTime(){
		long now = System.currentTimeMillis();
		return (Constants.CLIENT_SESSION_TIMEOUT - (now - mStartTime));
	}
	
	/**
	 * Formats input in milliseconds as a string with format mm:ss. Not
	 * applicable for inputs bigger than 60minutes.
	 * 
	 * @param improperSeconds
	 *            (milliseconds to format in mm:ss)
	 * @return String with countdown format
	 */
	public String formatCountdown(int improperSeconds) {
		Time timeLeft = new Time();
		timeLeft.minute = 0;
		timeLeft.second = 0;

		timeLeft.second = improperSeconds;
		timeLeft.normalize(true);

		String minutes = String.valueOf(timeLeft.minute);
		String seconds = String.valueOf(timeLeft.second);

		if (seconds.length() < 2) {
			seconds = "0" + seconds;
		}
		if (minutes.length() < 2) {
			minutes = "0" + minutes;
		}
		return minutes + ":" + seconds;
	}

}
