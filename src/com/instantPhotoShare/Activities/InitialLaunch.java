/**
 * 
 */
package com.instantPhotoShare.Activities;

import com.instantPhotoShare.Prefs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author Kyle
 * This is the main activity called when the app is launched.
 * It performs any initialization before deciding which activity to launch next.
 * The 1st check is to see if there is user information stored on the phone:
 * If so, then skip to main app, if not, then user must create an account or sign in.
 */
public class InitialLaunch extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // launch the correct activity
        launchCorrectActivity();
        
        // quit the activity, no need to keep in memory
        this.finish();
    }
    
    /**
     * Check if the phone has a user id and secret code.
     * @return true if user id and secret code are available, and false otherwise
     */
    private boolean isUserAccountInfoAvailable(){
    	// grab user id and secret code
    	long id = Prefs.getUserId(this);
    	String secretCode = Prefs.getSecretCode(this);
    	
    	// check if we have good values
    	if (id == Prefs.DEFAULT_LONG || secretCode == Prefs.DEFAULT_STRING ||
    			secretCode.length() == 0)
    		return false;
    	else
    		return true;
    }
    
    /**
     * Check user account info and launch correct activity
     */
    private void launchCorrectActivity(){
    	
    	// check if user account info is available. Then launch appropriate activity
        if (isUserAccountInfoAvailable()){
        	Intent intent = new Intent(this, MainScreen.class);
			startActivity(intent);
        }else{
        	Intent intent = new Intent(this, FirstTimeUseScreen.class);
    		startActivity(intent);
        }
    }
}
