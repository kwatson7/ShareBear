package com.instantPhotoShare;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashSet;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.Tasks.CreateNewAccountTask;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * A class for storing person information.
 * @author Kyle
 *
 */
public class Person {
	
	// private class variables
	private long rowId = -1; 								// the row id of the person used for identification
	private long serverId = -1; 							// the server id of the person
	private long contactsDatabaseId = -1; 					// the contact id from the phones address book
	private String firstName = "";							// the first name of the friend
	private String lastName = ""; 							// the last name of the friend
	private Location location;  							// the location of the friend
	private long lastUpdated; 								// The time in milliseconds of the last location update
	private HashSet<String> emailArray; 					// the list of emails for this person
	private String mainPhone = ""; 							// main phone number that is identifying as user account
	private String userName = ""; 							// The userName of the person
	private String password = ""; 							// The password of the user
	
	// static variables
	private static float SHRINK_PICTURE_VALUE = 2; 			// factor to shrink picture
	private static String PHOTO_SHARE_ID_COLUMN = "data1";	// The column to store in sql database for contacts
	private static String MIME_TYPE = "'vnd.android.cursor.item/instantPhotoShareMIME_571asd87'";	// The mime type for this app
	private static String DELIM = ",";						// delimiter(s) for phone and email strings 					
	
	/**
	 * Initialize a person, but no id yet (defaults to -1)
	 * @param firstName The person's first name
	 * @param lastName The person's last name
	 */
	public Person(String firstName, String lastName){
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public Person() {
	}

	
	/**
	 * Set the identifying main phone number
	 * @param phone
	 */
	public void setMainPhone(String phone){
		mainPhone = com.tools.Tools.fixPhoneString(phone);
	}
	/**
	 * Return the identifying main phone number
	 * @return
	 */
	public String getMainPhone(){
		return mainPhone;
	}
	
	/**
	 * Set the user name
	 * @param userName
	 */
	public void setUserName(String userName){
		this.userName = userName;
	}
	/**
	 * get the user name
	 * @return
	 */
	public String getUserName(){
		return userName;
	}
	
	/**
	 * Set the password
	 * @param password
	 */
	public void setPassword(String password){
		this.password = password;
	}
	/**
	 * get the password
	 * @return
	 */
	public String getPassword(){
		return password;
	}
	
	/**
	 * Change the name of the person
	 * @param name
	 */
	public void setFirstName(String name){
		this.firstName = name;
	}
	/**
	 * The name of the friend in question 
	 * @return
	 */
	public String getFirstName(){
		return firstName;
	}
	
	/**
	 * Change the name of the person
	 * @param name
	 */
	public void setLastName(String name){
		this.lastName = name;
	}
	/**
	 * The name of the friend in question 
	 * @return
	 */
	public String getLastName(){
		return lastName;
	}
	
	/**
	 * Set the server id of the person (the id given from the server)
	 * @param l
	 */
	public void setServerId(long l){
		this.serverId = l;
	}
	/**
	 * 
	 * @return the id of the person (the id given from the server)
	 */
	public long getServerId(){
		return serverId;
	}
	
	/**
	 * Set the row id of the person
	 * @param l
	 */
	public void setRowId(long l){
		this.rowId = l;
	}
	/**
	 * 
	 * @return the row id of the person 
	 */
	public long getRowId(){
		return rowId;
	}
	
	/**
	 * Set the list of email addresses
	 * @param input
	 */
	public void setEmailArray(HashSet<String> input){
		emailArray = input;
	}
	/**
	 * GSet the list of email addresses
	 * @param input
	 */
	public HashSet<String> getEmailArray(){
		return emailArray;
	}
	/**
	 * Get the list of emails as a string separated by commas
	 * @return
	 */
	public String getEmailsAsString(){
		String out = "";
		for (String email : emailArray)
			out += email + DELIM;
		if (out.length() > 1)
			out = out.substring(0, out.length()-1);
		
		return out;
	}
	/**
	 *  Take as input to emails a long string with commas separating addresses. Store them as a hashset however.
	 * @param emailString
	 */
	public void setEmailArray(String emailString){
		if (emailString == null)
			return;
		
		// break up string by commas
		String delim = DELIM;
		String[] tokens = emailString.split(delim);
		HashSet<String> emailArrayTmp = new HashSet<String>(tokens.length);
		
		// fill array
		for (int i = 0; i < tokens.length; i++){
			emailArrayTmp.add(tokens[i]);
		}
		
		emailArray = emailArrayTmp;
	}
	
	/**
	 * Set the location of the friend
	 * @param loc
	 */
	public void setLocation(Location loc){
		location = loc;
		lastUpdated = Calendar.getInstance().getTimeInMillis();
	}
	/**
	 * Get the friends' location
	 * @return location
	 */
	public Location getLocation(){
		return location;
	}
	
	/**
	 * Return a list of parameter value pairs required to create a new user on the server
	 * @return a JSOBObject
	 * @throws JSONException 
	 */
	public JSONObject getNewUserInfo() throws JSONException{		
		
		// add values to json object
		JSONObject json = new JSONObject();
		json.put(CreateNewAccountTask.PERSON_FIRST_NAME, getFirstName());
		json.put(CreateNewAccountTask.PERSON_LAST_NAME, getLastName());
		json.put(CreateNewAccountTask.PERSON_EMAIL, getEmailsAsString());
		json.put(CreateNewAccountTask.PERSON_PHONE, getMainPhone());
		if (getUserName().length() != 0){
			json.put(CreateNewAccountTask.USER_NAME, getUserName());
			json.put(CreateNewAccountTask.PASSWORD, getPassword());
		}
		return json;
	}
	
	/**
	 * Return geopoint of location where lat and lon are in microdegrees.
	 * @return geopoint of location
	 */
	/*
	public GeoPoint getGeoPoint(){
		if (location == null)
			return null;
		else
			return new GeoPoint(
				(int)(location.getLatitude()*1e6), 
				(int)(location.getLongitude()*1e6));
	}
	*/
	
	/**
	 * Grab the drawable that is the users picture id stored in the address book of the phone. Returns null if nothing found
	 * ***** NOT FUNCTIONAL RIGHT NOW, BECAUSE IDS ARE NOT STORED IN DATABASE ********
	 * @param act
	 * @return
	 */
	public Drawable getBmpNOTFINISHED(Activity act){
		
		// create string for searching
		String selection = ContactsContract.Data.MIMETYPE + " = " + MIME_TYPE + 
			" AND " +
			PHOTO_SHARE_ID_COLUMN + " = '" + getRowId() + "'";
		
		String[] projection = {ContactsContract.Data.RAW_CONTACT_ID};

		//selection arguments
		String[] selectionArgs = null;

		// grab URI	  
		Uri uri = ContactsContract.Data.CONTENT_URI;

		// sort order alphabetical
		String sortOrder = null;

		// grab cursor from search result
		Cursor cursor = act.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		
		// null cursor
		if (cursor == null){
			return null;
		}
		
		// empty cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return null;
		}
		
		// raw id column
		int rawIdColumn = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
		
		// grab the phone id of this contact and load bitmap
		long rawId = cursor.getLong(rawIdColumn);
		
		// get the input stream for the picture
		Uri uri2 = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, rawId);
		InputStream input = ContactsContract.Contacts.
		openContactPhotoInputStream(act.getContentResolver(), uri2);
		if (input == null) {
			cursor.close();
			return null;
		}

		// read bmp and convert to drawable
		Drawable photo = new BitmapDrawable(act.getResources(), BitmapFactory.decodeStream(input));
		photo.setBounds((int)((-photo.getIntrinsicWidth()/2)/SHRINK_PICTURE_VALUE),
				(int)((-photo.getIntrinsicHeight())/SHRINK_PICTURE_VALUE),
				(int)(photo.getIntrinsicWidth()/2/SHRINK_PICTURE_VALUE),
				0);
		
		// close cursor
		cursor.close();
		
		// return the photo
		return photo;
	}

	public void setContactsDatabaseId(long contactsDatabaseId) {
		this.contactsDatabaseId = contactsDatabaseId;
	}

	public long getContactsDatabaseId() {
		return contactsDatabaseId;
	}
}
