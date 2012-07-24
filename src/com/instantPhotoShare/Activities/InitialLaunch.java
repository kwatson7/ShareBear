/**
 * 
 */
package com.instantPhotoShare.Activities;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Adapters.UsersAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

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
        
        if(createHomeScreenIconIfNeedBe())
        	return;
        
        // launch the correct activity
        launchCorrectActivity();
        
        // quit the activity, no need to keep in memory
        this.finish();
    }
    
    private boolean createHomeScreenIconIfNeedBe(){
		// determine if we are supposed to just create a home screen icon
		final Intent intent0 = getIntent();
        final String action = intent0.getAction();
        
        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
        	Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
            shortcutIntent.setClassName(this, this.getClass().getName());
            
            // Then, set up the container intent (the response to the caller)
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
            Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                    this,  getApplicationInfo().icon);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Now, return the result to the launcher
            setResult(RESULT_OK, intent);
            finish();
            return true;
        } 	
        return false;
	}
    
    /**
     * Check if the phone has a user id and secret code.
     * @return true if user id and secret code are available, and false otherwise
     */
    public static boolean isUserAccountInfoAvailable(Context ctx){
    	// grab user id and secret code
    	long id = Prefs.getUserServerId(ctx);
    	String secretCode = Prefs.getSecretCode(ctx);
    	if (Prefs.debug.forceId){
    		id = 2;
    		secretCode = "secret";
    	}
    	
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
        if (isUserAccountInfoAvailable(this)){
        	Intent intent = new Intent(this, MainScreen.class);
        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
        }else{
        	Intent intent = new Intent(this, FirstTimeUseScreen.class);
        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    		startActivity(intent);
        }
    }
}
