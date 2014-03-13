package ch.uzh.csg.mbps.client.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import ch.uzh.csg.mbps.client.LoginActivity;

/**
 * This class is responsible for the time session on the client site. This class
 * checks after a predefined time if the time for the logged user is expired.
 */
public class TimeHandler extends HandlerThread{

	private static TimeHandler mTimeHandler;
	private static Handler mHandler;
	private static long mStartTime;
	private static Activity activity;
	
	public TimeHandler() {
		super("TimeSessionHandler");
		start();
	}
	
	public static TimeHandler getInstance(){
		if(mTimeHandler == null){
			mTimeHandler = new TimeHandler();
		}
		return mTimeHandler;
	}
	
	/**
	 * Sets the start time when a request to the server was successful.
	 */
	public void setStartTime(){
		mStartTime = System.currentTimeMillis();
		if(mHandler == null){
			mHandler = new Handler(getLooper());
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, Constants.CLIENT_CONNECTION_TIMEOUT);
		}
	}

	/**
	 * Sets the activity. This is used to terminate the session.
	 * 
	 * @param activity
	 *            The activity in which the MBPS starts.
	 */
	public void setStartActivity(Activity startActivity){
		activity = startActivity;
	}
	
	/**
	 * Removes active runnable and sets the handler back to null.
	 */
	public void terminateSession(){
		if(mHandler != null){
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
			if((now-mStartTime) >= Constants.CLIENT_SESSION_TIMEOUT){
				InternalStorageXML.writeUserAccountIntoFile(activity);
				Intent intent = new Intent(activity, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS );
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
				ClientController.clear();
				terminateSession();
			}else{
				mHandler.postDelayed(mUpdateTimeTask, Constants.CLIENT_CHECK_TIME_INTERVAL);
			}
		}
	};

}