package com.instantPhotoShare.Activities;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.instantPhotoShare.Tasks.LoginTask;
import com.tools.CustomActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginScreen
extends CustomActivity {
	
	// widget pointers
	private static EditText yourUserNameEdit;
	private static EditText yourPasswordEdit;
	
	//enums for async calls
	private enum ASYNC_CALLS {
		LOGIN;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreateOverride(Bundle savedInstanceState) {
        
        initializeLayout();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	com.tools.Tools.hideKeyboard(this, yourUserNameEdit);
    }
    
    /**
     * Called when ok is clicked. Sends info back to calling activity
     * @param view
     */
    public void okClicked(View view){
    	
    	// check that info is entered in all fields
    	String user = yourUserNameEdit.getText().toString();
    	String pass = yourPasswordEdit.getText().toString();
    	if (user == null || user.length() == 0
    			|| pass == null || pass.length() == 0){
    		Toast.makeText(this,
    				R.string.allFieldsRequired,
    				Toast.LENGTH_LONG).show();
    		return;
    	}

		// launch the async task and add to array of tasks to be managed
		LoginTask<LoginScreen> task =  new LoginTask<LoginScreen>(
				this,
				ASYNC_CALLS.LOGIN.ordinal(),
				user,
				pass);
		task.execute();
    }
    
    /**
     * Called when cancel clicked, sends the fact that cancel was clicked back to calling activity
     * @param view
     */
    public void cancelClicked(View view){
    	setResult(RESULT_CANCELED);
    	finish();
    }

	@Override
	public void onAsyncExecute(int requestCode,
			AsyncTypeCall asyncTypeCall,
			Object data) {
		// convert request to enum
		ASYNC_CALLS request = ASYNC_CALLS.convert(requestCode);

		// switch over all the request codes
		switch (request){

		// create a new account
		case LOGIN:

			// switch over possible calls
			switch (asyncTypeCall){
			case POST:

				// check success, important values were already saved to prefs
				try{
					LoginTask<LoginScreen>.ReturnFromLoginTask returnVal =
						(LoginTask<LoginScreen>.ReturnFromLoginTask) data;
					if (returnVal.isSuccess()){
						setResult(RESULT_OK);
				    	finish();
					}
				}catch(Exception e){
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					return;
				}
				break;
			case PRE:
				break;
			case PROGRESS:
				break;
			}
			break;
		}
		
	}

	@Override
	protected void initializeLayout() {
		// initialize the layout
        setContentView(R.layout.login);
        
        // find variables for widgets
        yourUserNameEdit = (EditText)findViewById(R.id.userName);
        yourPasswordEdit = (EditText)findViewById(R.id.password);
		
	}

	@Override
	protected void additionalConfigurationStoring() {
	
	}

	@Override
	protected void onDestroyOverride() {		
	}
}
