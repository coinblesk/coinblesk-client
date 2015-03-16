package ch.uzh.csg.coinblesk.client.settings;

import android.os.Bundle;
import ch.uzh.csg.coinblesk.client.AbstractAsyncActivity;
import ch.uzh.csg.coinblesk.client.R;

/**
 * This class is the view that informs the user about the involved parties to
 * build this application.
 */
public class AboutActivity extends AbstractAsyncActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		setScreenOrientation();
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

    @Override
    public void onResume(){
    	super.onResume();
    	invalidateOptionsMenu();
    }
    
}
