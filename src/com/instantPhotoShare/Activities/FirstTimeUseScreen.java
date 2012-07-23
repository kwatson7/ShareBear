package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.HashSet;

import com.instantPhotoShare.Person;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.tools.CustomActivity;
import com.tools.TwoStrings;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class FirstTimeUseScreen
extends com.tools.CustomActivity {

	// pointers to graphics objects
	private Button createNewAcccountButton;
	private Button dontCreateNewAccountButton;
	private Button signInButton;
	private CustomActivity act = this;

	// constants
	private static final int CANT_CREATE_ACCOUNT_TOAST_LENGTH = 4;

	// enums for possible activity calls
	private enum ACTIVITY_CALLS { 										
		DONT_CREATE_ACCOUNT, CREATE_ACCOUNT, SIGN_IN;
		private static ACTIVITY_CALLS convert(int value)
		{
			return ACTIVITY_CALLS.class.getEnumConstants()[value];
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

		// if no main phone, then we cannot use this options
		if (self.getMainPhone() == null || self.getMainPhone().length() == 0){
			Toast.makeText(
					this,
					"Phone number not present on device to be used as account identifier. You must create an account with username/password",
					CANT_CREATE_ACCOUNT_TOAST_LENGTH).show();
			return;
		}

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
				self.getMainPhone());
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
				self.getMainPhone());
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

		// load the intent for entering this info and pre-load with known info
		Intent intent = new Intent(this, LoginScreen.class);

		// load the activity
		startActivityForResult(intent, ACTIVITY_CALLS.SIGN_IN.ordinal());
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
		person.setMainPhone(mainPhone);
		/*
		if (contactIds != null && contactIds.size() != 0){
			// add first contact id to person
			for (Integer item : contactIds) {
				person.setContactsDatabaseId(item);
				break;
			}
		}
		*/
			
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
			returnFromCreateAccount(requestCode, resultCode, data); break;
		case CREATE_ACCOUNT:
			returnFromCreateAccount(requestCode, resultCode, data); break;
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
	private void returnFromCreateAccount(int requestCode, int resultCode, Intent data){
		//TODO: user info is being saved to prefs, even when we just did a login
		
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
			// we are good, so go back to initial launch
			try{
				Intent intent = new Intent(this, InitialLaunch.class);
				startActivity(intent);
				finish();
				
			}catch(Exception e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return;
			}
		}
	}

	private void returnFromSignIn(int requestCode, int resultCode, Intent data){
		// switch across resultCodes
		switch (resultCode){

		// canceled
		case RESULT_CANCELED:
			// do nothing
			break;

			// ok pressed and a good login	
		case RESULT_OK:
			// we are good, so go back to initial launch
			try{
				Intent intent = new Intent(this, InitialLaunch.class);
				startActivity(intent);
				finish();
				
			}catch(Exception e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return;
			}
		}
	}

	@Override
	protected void additionalConfigurationStoring() {
	}

	@Override
	protected void onDestroyOverride() {
		// recyle the old bitmap if one exists
		View view = findViewById(R.id.backgroundImage);
		if (view == null)
			return;
		ImageView image = (ImageView) view;
		Drawable drawable = image.getDrawable();
		Bitmap bitmapOld = null;
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			bitmapOld = bitmapDrawable.getBitmap();
			if (bitmapOld != null)
				bitmapOld.recycle();
		}
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		
	}
}