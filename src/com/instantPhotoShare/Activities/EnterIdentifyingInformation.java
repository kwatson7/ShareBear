package com.instantPhotoShare.Activities;

import com.instantPhotoShare.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

public class EnterIdentifyingInformation extends Activity {
	
	// static variables for passing in info
	public static String YOUR_FIRST_NAME = "YOUR_FIRST_NAME";
	public static String YOUR_LAST_NAME = "YOUR_LAST_NAME";
	public static String YOUR_PHONE = "YOUR_PHONE";
	public static String YOUR_EMAIL = "YOUR_EMAIL";
	public static String ALSO_ENTER_USER_NAME_AND_PASSWORD = "ALSO_ENTER_USER_NAME_AND_PASSWORD";
	public static String USER_NAME = "USER_NAME";
	public static String PASSWORD = "PASSWORD";
	
	// widget pointers
	private static EditText yourFirstNameEdit;
	private static EditText yourLastNameEdit;
	private static EditText yourPhoneEdit;
	private static EditText yourEmailEdit;
	private static EditText yourUserNameEdit;
	private static EditText yourPasswordEdit;
	
	// keep track if we need username and pass
	private boolean isAlsoEnterUserNameAndPassword = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // initialize the layout
        setContentView(R.layout.enter_identifying_information);
        
        // find variables for widgets
        yourFirstNameEdit = (EditText)findViewById(R.id.yourFirstName);
        yourLastNameEdit = (EditText)findViewById(R.id.yourLastName);
        yourPhoneEdit = (EditText)findViewById(R.id.yourPhoneNumbers);
        yourEmailEdit = (EditText)findViewById(R.id.yourEmailAddresses);
        yourUserNameEdit = (EditText)findViewById(R.id.yourUserName);
        yourPasswordEdit = (EditText)findViewById(R.id.yourPassword);
        
           
        // load in passed info
        Bundle extras = getIntent().getExtras(); 
        if(extras !=null)
        {
        	// first name
        	String firstName = extras.getString(YOUR_FIRST_NAME);
        	if (firstName != null)
        		yourFirstNameEdit.setText(firstName);
        	
        	// first name
        	String lastName = extras.getString(YOUR_LAST_NAME);
        	if (lastName != null)
        		yourLastNameEdit.setText(lastName);
        	
        	// phone
        	String phone = extras.getString(YOUR_PHONE);
        	if (phone != null)
        		yourPhoneEdit.setText(phone);
        	
        	// email
        	String email = extras.getString(YOUR_EMAIL);
        	if (email != null)
        		yourEmailEdit.setText(email);
        	
        	// should we be entering username and password
        	isAlsoEnterUserNameAndPassword = extras.getBoolean(ALSO_ENTER_USER_NAME_AND_PASSWORD, false);
        	if (isAlsoEnterUserNameAndPassword){
        		yourUserNameEdit.setVisibility(View.VISIBLE);
        		yourPasswordEdit.setVisibility(View.VISIBLE);
        		yourPasswordEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        		yourEmailEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        	}else{
        		yourUserNameEdit.setVisibility(View.GONE);
        		yourPasswordEdit.setVisibility(View.GONE);
        		yourEmailEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        	}
        	
        }
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	com.tools.Tools.hideKeyboard(this, yourFirstNameEdit);
    }
    
    /**
     * Called when ok is clicked. Sends info back to calling activity
     * @param view
     */
    public void okClicked(View view){
    	
    	// check that info is entered in all fields
    	String firstName =  yourFirstNameEdit.getText().toString();
    	String lastName = yourLastNameEdit.getText().toString();
    	String phone = yourPhoneEdit.getText().toString();
    	String email = yourEmailEdit.getText().toString();
    	String user = yourUserNameEdit.getText().toString();
    	String pass = yourPasswordEdit.getText().toString();
    	if (((firstName == null || firstName.length() == 0) && (lastName == null || lastName.length() == 0)) || 
    			phone == null || phone.length() == 0 ||
    			email == null || email.length() == 0){
    		Toast.makeText(this,
    				R.string.allFieldsRequired,
    				Toast.LENGTH_LONG).show();
    		return;
    	} else if (isAlsoEnterUserNameAndPassword &&
    			(user == null || user.length() == 0 ||
    			pass == null || pass.length() == 0)){
    		Toast.makeText(this,
    				R.string.allFieldsRequired,
    				Toast.LENGTH_LONG).show();
    		return;
    	}
    			
    	
    	// send back info to calling activity
    	Intent resultIntent = new Intent();
    	resultIntent.putExtra(YOUR_FIRST_NAME, firstName);
    	resultIntent.putExtra(YOUR_LAST_NAME, lastName);
    	resultIntent.putExtra(YOUR_PHONE, phone);
    	resultIntent.putExtra(YOUR_EMAIL, email);
    	resultIntent.putExtra(USER_NAME, user);
    	resultIntent.putExtra(PASSWORD, pass);
    	setResult(RESULT_OK, resultIntent);
    	finish();
    }
    
    /**
     * Called when cancel clicked, sends the fact that cancel was clicked back to calling activity
     * @param view
     */
    public void cancelClicked(View view){
    	setResult(RESULT_CANCELED);
    	finish();
    }
}
