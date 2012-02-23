package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.HashSet;

import com.instantPhotoShare.Person;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Tasks.CreateNewAccountTask;
import com.instantPhotoShare.Tasks.ReturnFromCreateNewAccountTask;
import com.tools.CustomActivity;
import com.tools.TwoStrings;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class FirstTimeUseScreen extends com.tools.CustomActivity {
	
	// pointers to graphics objects
	private Button createNewAcccountButton;
	private Button dontCreateNewAccountButton;
	private Button signInButton;
	private CustomActivity act = this;
	
	// enums for possible activity calls
	private enum ACTIVITY_CALLS { 										
		DONT_CREATE_ACCOUNT, CREATE_ACCOUNT, SIGN_IN;
		private static ACTIVITY_CALLS convert(int value)
		{
			return ACTIVITY_CALLS.class.getEnumConstants()[value];
		}
	}
	
	//enums for async calls
	private enum ASYNC_CALLS {
		CREATE_ACCOUNT;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}
	
	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		initializeLayout();
	}
    
    /**
     * initialize the layout and grab pointers for widgets
     */
	@Override
    protected void initializeLayout(){

		// allow for advanced features in title bar
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    // status in title
	   // callingActivity.setProgressBarIndeterminateVisibility(true);
		
    	// set the content view
    	setContentView(R.layout.first_time_use);

    	// grab pointers for graphics objects
    	createNewAcccountButton = (Button) findViewById(R.id.createNewAccountButton);
    	dontCreateNewAccountButton = (Button) findViewById(R.id.dontCreateAccountButton);
    	signInButton = (Button) findViewById(R.id.signInButton);
    }
    
    /**
     * Performed when don't create account button clicked
     */
    public void dontCreateAccountButtonClicked(View view){
    	
    	// find the self in address book
    	Person self = findSelfInAddressBook();   	
    	
    	// write main phone number to prefs	
    	Prefs.setMainPhone(this, self.getMainPhone());
    	
    	// load the intent for entering this info and pre-load with known info
    	Intent intent = new Intent(this, EnterIdentifyingInformation.class);
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_FIRST_NAME,
    			self.getFirstName());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_LAST_NAME,
    			self.getLastName());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_PHONE,
    			self.getPhonesAsString());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_EMAIL,
    			self.getEmailsAsString());
    	intent.putExtra(
    			EnterIdentifyingInformation.ALSO_ENTER_USER_NAME_AND_PASSWORD,
    			false);
    	
    	// load the activity
		startActivityForResult(intent, ACTIVITY_CALLS.DONT_CREATE_ACCOUNT.ordinal());
    }
    
    /**
     * Performed when create new account button clicked
     * @param view
     */
    public void createNewAccountButtonClicked(View view){
    	// find the self in address book
    	Person self = findSelfInAddressBook();  
    	
    	// write main phone number to prefs	
    	Prefs.setMainPhone(this, self.getMainPhone());
    	
    	// load the intent for entering this info and pre-load with known info
    	Intent intent = new Intent(this, EnterIdentifyingInformation.class);
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_FIRST_NAME,
    			self.getFirstName());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_LAST_NAME,
    			self.getLastName());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_PHONE,
    			self.getPhonesAsString());
    	intent.putExtra(
    			EnterIdentifyingInformation.YOUR_EMAIL,
    			self.getEmailsAsString());
    	intent.putExtra(
    			EnterIdentifyingInformation.ALSO_ENTER_USER_NAME_AND_PASSWORD,
    			true);
    	
    	// load the activity
		startActivityForResult(intent, ACTIVITY_CALLS.CREATE_ACCOUNT.ordinal());
    }
    
    /**
     * Called when sign in button is clicked
     * @param view
     */
    public void signInButtonClicked(View view){
    	
    }
    
    /** 
     * Look up in the address book to try and find the user of phone, based on phone number and/or user account info
     * @return
     */
    private Person findSelfInAddressBook(){
    	
    	// grab phone number from phone
    	String phone = com.tools.Tools.getMyStrippedPhoneNumber(this);
    	String mainPhone = phone;
    	if (mainPhone == null)
    		mainPhone = "";
    	
    	// now grab user email from accounts list, just grab the first one
    	ArrayList<String> googleAccounts = com.tools.CustomCursors.getGoogleAccountNames(this);
    	
    	// if empty, then add a null "account" for searching
    	if (googleAccounts.size() == 0)
    		googleAccounts.add(null);
    	
    	// find all the matches in address book that have both phone and email looping across googleAccounts
    	HashSet<Integer> contactIds = new HashSet<Integer>();
    	for (String account : googleAccounts){
    		HashSet<Integer> tmp = 
    			com.tools.CustomCursors.getCursorFromPhoneAndEmail(this, phone, account, true);
    		if (tmp != null)
    			contactIds.addAll(tmp);
    	}
    		
    	// if there are none that both match, then do either or
    	if (contactIds.size() == 0){
    		for (String account : googleAccounts){
        		HashSet<Integer> tmp = 
        			com.tools.CustomCursors.getCursorFromPhoneAndEmail(this, phone, account, false);
        		if (tmp != null)
        			contactIds.addAll(tmp);
        	}
    	}
    	
    	// find relevant contact info from database
    	HashSet<String> phoneNumberArray = new HashSet<String>();
    	HashSet<String> emailAddressArray = new HashSet<String>();
    	String displayName = "";
    	TwoStrings fullName = new TwoStrings("", "");
    	
    	// add phone and google accounts
    	if (phone != null && phone.length() != 0)
    		phoneNumberArray.add(phone);
    	if (googleAccounts != null)
    		for (String account : googleAccounts)
    			if (account != null && account.length() != 0)
    				emailAddressArray.add(account);
    	
    	// find phone numbers and display name
    	if (contactIds != null && contactIds.size() != 0){
    		// build search string
    		String selection = com.tools.CustomCursors.buildQueryFromArray
    			(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, contactIds);

    		String[] selectionArgs = null;

    		// find user info of contacts
    		String[] projection = {
    				CommonDataKinds.Phone.DISPLAY_NAME, 
    				CommonDataKinds.Phone.NUMBER,
    				CommonDataKinds.Phone.CONTACT_ID,
    				CommonDataKinds.Phone.RAW_CONTACT_ID};

    		// grab cursor from search result grabbing names of interest
    		Cursor cursor = this.getContentResolver().query(
    				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    				projection,
    				selection,
    				selectionArgs,
    				null);

    		// grab phone numbers and/or name
    		if (cursor != null){
    			int numberIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
    			int nameIndex = cursor.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME);
    			if (cursor.moveToFirst()){
    				do{
    					// grab display name
    					if (displayName.length() == 0){
    						String name = cursor.getString(nameIndex);
    						if (name != null)
    							displayName = name;
    					}
    					
    					// grab full name
    					TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor, this);
    					if ((fullName.mObject1 == null ||
    							fullName.mObject1.length() == 0) &&
    							(fullNameTmp.mObject1 != null &&
    									fullNameTmp.mObject1.length() != 0))
    						fullName.mObject1 = fullNameTmp.mObject1;
    					
    					if ((fullName.mObject2 == null ||
    							fullName.mObject2.length() == 0) &&
    							(fullNameTmp.mObject2 != null &&
    									fullNameTmp.mObject2.length() != 0))
    						fullName.mObject2 = fullNameTmp.mObject2;
    						
    					// add phone numbers
    					String number = cursor.getString(numberIndex);
    					if (number != null)
    						phoneNumberArray.add(number);
    				}while (cursor.moveToNext());
    			}
    			cursor.close();
    		}

    		// now repeat for email addresses

    		// find user info of contacts
    		String[] projection2 = {
    				CommonDataKinds.Email.DISPLAY_NAME, 
    				CommonDataKinds.Email.DATA,
    				CommonDataKinds.Email.CONTACT_ID,
    				CommonDataKinds.Email.RAW_CONTACT_ID};

    		// grab cursor from search result grabbing names of interest
    		Cursor cursor2 = this.getContentResolver().query(
    				ContactsContract.CommonDataKinds.Email.CONTENT_URI,
    				projection2,
    				selection,
    				selectionArgs,
    				null);

    		// grab email and/or name
    		if (cursor2 != null){
    			int emailIndex = cursor2.getColumnIndex(CommonDataKinds.Email.DATA);
    			int nameIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.DISPLAY_NAME);
    			if (cursor2.moveToFirst()){
    				do{
    					// grab display name
    					if (displayName.length() == 0){
    						String name = cursor2.getString(nameIndex2);
    						if (name != null)
    							displayName = name;
    					}
    					
    					// grab full name
    					TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor2, this);
    					if ((fullName.mObject1 == null ||
    							fullName.mObject1.length() == 0) &&
    							(fullNameTmp.mObject1 != null &&
    									fullNameTmp.mObject1.length() != 0))
    						fullName.mObject1 = fullNameTmp.mObject1;
    					
    					if ((fullName.mObject2 == null ||
    							fullName.mObject2.length() == 0) &&
    							(fullNameTmp.mObject2 != null &&
    									fullNameTmp.mObject2.length() != 0))
    						fullName.mObject2 = fullNameTmp.mObject2;
    					
    					// add email addresses
    					String email = cursor2.getString(emailIndex);
    					if (email != null)
    						emailAddressArray.add(email);
    				}while (cursor2.moveToNext());
    			}
    			cursor2.close();
    		}
    	}
		
    	// figure out which name parts to use
    	if (fullName.mObject1 == null
    			|| fullName.mObject1.length() == 0
    			|| fullName.mObject2 == null
    			|| fullName.mObject2.length() == 0 
    			&& (displayName != null && displayName.length() != 0)){
    		fullName.mObject1 = displayName;
    		fullName.mObject2 = "";
    	}
    	
    	// initialize a person with name, phones and emails.
		Person person = new Person(fullName.mObject1, fullName.mObject2);
		person.setEmailArray(emailAddressArray);
		person.setPhoneArray(phoneNumberArray);
		person.setMainPhone(mainPhone);
    	return person;
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// convert to enum
		ACTIVITY_CALLS request = ACTIVITY_CALLS.convert(requestCode);
		
		// switch of possible requestCodes
		switch(request){
		case DONT_CREATE_ACCOUNT:
			returnFromDontCreateAccountPersonalInfo(requestCode, resultCode, data); break;
		case CREATE_ACCOUNT:
			returnFromCreateAccountPersonalInfo(requestCode, resultCode, data); break;
		case SIGN_IN:
			returnFromSignIn(requestCode, resultCode, data); break;
		}

	}
	
	/**
	 * When we return from entering personal info on the not creating account call, we need to save the info and 
	 * create a new account on the server with just phone number and no password
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	private void returnFromDontCreateAccountPersonalInfo(int requestCode, int resultCode, Intent data){
		
		// switch across resultCodes
		switch (resultCode){

		// canceled
		case RESULT_CANCELED:
			// show toast that nothing happened
			Toast.makeText(this,
					R.string.registrationNotCompleted,
					Toast.LENGTH_LONG).show();
			break;

			// ok pressed	
		case RESULT_OK:
			// save info
			if (!savePersonalInfo(data)){
				Toast.makeText(this,
						"Personal info not saved for unknown reason. Cannot Continue !!",
						Toast.LENGTH_LONG).show();
				break;
			}
			
			// create new account on server
			createNewAccountOnServer();
		}
	}
	
	private void returnFromCreateAccountPersonalInfo(int requestCode, int resultCode, Intent data){
		// switch across resultCodes
		switch (resultCode){

		// canceled
		case RESULT_CANCELED:
			// show toast that nothing happened
			Toast.makeText(this,
					R.string.registrationNotCompleted,
					Toast.LENGTH_LONG).show();
			break;

			// ok pressed	
		case RESULT_OK:
			// save info
			if (!savePersonalInfo(data)){
				Toast.makeText(this,
						"Personal info not saved for unknown reason. Cannot Continue !!",
						Toast.LENGTH_LONG).show();
				break;
			}
			
			// create new account on server
			createNewAccountOnServer();
		}
	}

	private void returnFromSignIn(int requestCode, int resultCode, Intent data){
		//TODO: complete
	}
	
	/**
	 * Save personal info with intent data sent back from EnterIdentifyingInfo.java
	 * @param data
	 * @return return true if data saved successfully, and false otherwise
	 */
	private boolean savePersonalInfo(Intent data){
		
		// null data, return false
		if (data == null)
			return false;
		
		// read extras from intent
		Bundle extras = data.getExtras();
		if (extras != null){
			
			// grab info
			String firstName = extras.getString(EnterIdentifyingInformation.YOUR_FIRST_NAME);
			String lastName = extras.getString(EnterIdentifyingInformation.YOUR_LAST_NAME);
			String phones = extras.getString(EnterIdentifyingInformation.YOUR_PHONE);
			String emails = extras.getString(EnterIdentifyingInformation.YOUR_EMAIL);
			String userName = extras.getString(EnterIdentifyingInformation.USER_NAME);
			String password = extras.getString(EnterIdentifyingInformation.PASSWORD);
			
			// Assign info
			Prefs.setFirstName(this, firstName);
			Prefs.setLastName(this, lastName);
			Prefs.setPhoneNumbers(this, phones);
			Prefs.setEmailAddresses(this, emails);
			Prefs.setUserName(this, userName);
			Prefs.setPassword(this, password);
			
			return true;
		}else
			return false;
	}
	
	/**
	 * Call this to launch a new activity to create a new account on the server
	 */
	private void createNewAccountOnServer(){
		
		// grab person from preferences
		Person me = Prefs.getPerson(this);
		
		// launch the async task and add to array of tasks to be managed
		CreateNewAccountTask task =  new CreateNewAccountTask(
				act,
				ASYNC_CALLS.CREATE_ACCOUNT.ordinal(),
				me);
		this.asyncArray.add(task);
		task.execute();
	}


	/**
	 * Deal with returns from asynctask
	 */
	@Override
	public void onAsyncExecute(
			int requestCode,
			AsyncTypeCall asyncTypeCall,
			Object data) {
		
		// convert request to enum
		ASYNC_CALLS request = ASYNC_CALLS.convert(requestCode);
		
		// switch over all the request codes
		switch (request){
		
		// create a new account
		case CREATE_ACCOUNT:
			
			// switch over possible calls
			switch (asyncTypeCall){
			case POST:
				// check success, important values were already saved to prefs
				try{
					ReturnFromCreateNewAccountTask returnVal = (ReturnFromCreateNewAccountTask) data;
					if (returnVal.isSuccess()){
						Intent intent = new Intent(this, InitialLaunch.class);
						startActivity(intent);
						finish();
					}
				}catch(Exception e){
					
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
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub		
	}

	@Override
	protected void onDestroyOverride() {
		
	}
}