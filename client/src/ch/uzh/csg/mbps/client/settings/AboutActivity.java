package ch.uzh.csg.mbps.client.settings;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ch.uzh.csg.mbps.client.AbstractAsyncActivity;
import ch.uzh.csg.mbps.client.R;
import ch.uzh.csg.mbps.client.util.ClientController;

/**
 * This class is the view that informs the user about the involved parties to
 * build this application.
 */
public class AboutActivity extends AbstractAsyncActivity {

	private MenuItem menuWarning;
	
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
    	if(menuWarning != null){
        	if(ClientController.isOnline()) {
                menuWarning.setVisible(false);
            } else {
            	menuWarning.setVisible(true);
            }
    	}
    }

}
