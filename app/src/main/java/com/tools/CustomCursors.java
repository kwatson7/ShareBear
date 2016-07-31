package com.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.Data;

/**
 * A helper class with many different static methods for calling database methods to extract user info.
 * @author Kyle
 *
 */
//TODO: finish commenting this class
public class CustomCursors {

	// some variables
	/** account type for google accounts */
	public static final String GOOGLE_ACCOUNT_TYPE = "com.google";
	public static final String YOU_TUBE_DOMAIN = "@youtube.com";

	/**
	 * Get a cursor that has all group names and ids
	 * @param act The activity that this cursor call happens within
	 * @return cursor with these fields:
	 * ContactsContract.Groups.TITLE, 
				ContactsContract.Groups._ID,
				ContactsContract.Groups.SYSTEM_ID
	 */
	public static Cursor getGroupNamesAndIdsCursor(Activity act){

		//columns to return
		String[] projection = {ContactsContract.Groups.TITLE, 
				ContactsContract.Groups._ID,
				ContactsContract.Groups.SYSTEM_ID,  
				ContactsContract.Groups.ACCOUNT_NAME,
				ContactsContract.Groups.ACCOUNT_TYPE};

		// sql where clause - used to require to be visible, but this is not good and will block groups t
		// that shouldn't be blocked
		String selection = null;

		// sort order alphabetical
		String sortOrder = ContactsContract.Groups.TITLE + " COLLATE LOCALIZED ASC";

		// perform the cursor call
		Cursor cursor = act.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, 
				projection, selection, null, sortOrder);

		return cursor;
	}

	public static Cursor getGroupNamesAndIdsCursor(ContentResolver cr){

		//columns to return
		String[] projection = {ContactsContract.Groups.TITLE, 
				ContactsContract.Groups._ID,
				ContactsContract.Groups.SYSTEM_ID,  
				ContactsContract.Groups.ACCOUNT_NAME,
				ContactsContract.Groups.ACCOUNT_TYPE};

		// sql where clause - used to require to be visible, but this is not good and will block groups t
		// that shouldn't be blocked
		String selection = null;

		// sort order alphabetical
		String sortOrder = ContactsContract.Groups.TITLE + " COLLATE LOCALIZED ASC";

		// perform the cursor call
		Cursor cursor = cr.query(ContactsContract.Groups.CONTENT_URI, 
				projection, selection, null, sortOrder);

		return cursor;
	}

	/**
	 * Grab all the account names on the phone and the account types.
	 * @param ctx A context used to implement AccountManager
	 * @return An arrayList of two Strings, with name, and type
	 */
	public static  ArrayList<TwoStrings> getAccountNamesAndType(Context ctx){
		// grab the accounts
		Account[] accounts = AccountManager.get(ctx).getAccounts(); 

		// initialize list
		ArrayList<TwoStrings> accountList = new ArrayList<TwoStrings>(accounts.length);

		// loop across accounts grouping them together
		for (Account acc : accounts){
			accountList.add(new TwoStrings(acc.name, acc.type, "Account names and types"));
		}

		return accountList;
	}

	public static  ArrayList<String> getGoogleAccountNames(Context ctx){

		// grab the accounts
		Account[] accounts = AccountManager.get(ctx).getAccounts(); 

		// initialize list
		ArrayList<String> accountList = new ArrayList<String>();

		// loop across accounts finding groups that are google and NOT youtube
		for (Account acc : accounts){
			if (acc.type.equals(GOOGLE_ACCOUNT_TYPE) 
					&& !acc.name.contains(YOU_TUBE_DOMAIN))
				accountList.add(acc.name);
		}

		return accountList;
	}

	/** Get a twoobject element where each element is a listarray. 
	 * One list array is the titles of the groups, and the other is the IDs */
	public static TwoObjects<ArrayList<String>, ArrayList<String>> getGroupNamesAndIdsList0(Activity act){

		// grab cursor
		Cursor cursor = getGroupNamesAndIdsCursor(act);
		int N = cursor.getCount();

		// initialize outputs
		ArrayList<String> titles = new ArrayList<String>(N);
		ArrayList<String> ids = new ArrayList<String>(N);

		// columns to read
		int colTitle = cursor.getColumnIndex(ContactsContract.Groups.TITLE);
		int colId = cursor.getColumnIndex(ContactsContract.Groups._ID);
		int systemId = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID);

		// loop through cursor
		String tmpTitle;
		if (cursor != null && cursor.moveToFirst()){
			do{
				// see if we want systemid name or title
				tmpTitle = cursor.getString(systemId);
				if (tmpTitle == null)
					tmpTitle = cursor.getString(colTitle);

				// don't add duplicate names
				//	if (titles.contains(tmpTitle))
				//		continue;	

				// add title and id to array	
				titles.add(tmpTitle);
				ids.add(cursor.getString(colId));
			}while(cursor.moveToNext());
		}

		if (cursor != null)
			cursor.close();

		// sort by title		

		// put array list into output
		return new TwoObjects<ArrayList<String>, ArrayList<String>>(titles, ids);
	}

	/** Get a twoobject element where each element is a listarray. 
	 * One list array is the titles of the groups, and the other is the IDs */
	public static ArrayList<TwoStrings> getGroupNamesAndIdsList(Activity act){

		// grab cursor
		Cursor cursor = getGroupNamesAndIdsCursor(act);
		int N = cursor.getCount();

		// initialize outputs
		ArrayList<TwoStrings> titlesAndIds = new ArrayList<TwoStrings>(N);

		// columns to read
		int colTitle = cursor.getColumnIndex(ContactsContract.Groups.TITLE);
		int colId = cursor.getColumnIndex(ContactsContract.Groups._ID);
		int systemId = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID);

		// loop through cursor
		String tmpTitle;
		if (cursor != null && cursor.moveToFirst()){
			do{
				// object that holds id and title
				TwoStrings input = new TwoStrings("", "0");

				// see if we want systemid name or title
				tmpTitle = cursor.getString(systemId);
				if (tmpTitle == null)
					tmpTitle = cursor.getString(colTitle);

				// put title into input object
				input.mObject1 = tmpTitle;

				//if still null, then break out
				if (tmpTitle == null)
					continue;

				// don't add duplicate names
				//	if (titlesAndIds.contains(input))
				//		continue;	

				// put the id into input object
				input.mObject2 = cursor.getString(colId);

				// check if this group is empty, if so don't return it
				Cursor cursorEmpty = act.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, 
						new String[]{ContactsContract.Groups.SUMMARY_COUNT}, 
						ContactsContract.Groups._ID + " = "+input.mObject2, 
						null, null);
				if (cursorEmpty == null || !cursorEmpty.moveToFirst()){
					if (cursorEmpty != null)
						cursorEmpty.close();
					continue;
				}

				if (cursorEmpty.getInt(cursorEmpty.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT)) < 1){
					cursorEmpty.close();
					continue;			
				}

				// add title and id to array	
				cursorEmpty.close();
				titlesAndIds.add(input);
			}while(cursor.moveToNext());
		}

		// sort by title	
		Collections.sort(titlesAndIds);

		if (cursor != null)
			cursor.close();

		// put array list into output
		return titlesAndIds;
	}

	public static ArrayList<TwoStrings> getGroupNamesAndIdsList(ContentResolver cr){

		// grab cursor
		Cursor cursor = getGroupNamesAndIdsCursor(cr);
		int N = cursor.getCount();

		// initialize outputs
		ArrayList<TwoStrings> titlesAndIds = new ArrayList<TwoStrings>(N);

		// columns to read
		int colTitle = cursor.getColumnIndex(ContactsContract.Groups.TITLE);
		int colId = cursor.getColumnIndex(ContactsContract.Groups._ID);
		int systemId = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID);

		// loop through cursor
		String tmpTitle;
		if (cursor != null && cursor.moveToFirst()){
			do{
				// object that holds id and title
				TwoStrings input = new TwoStrings("", "0");

				// see if we want systemid name or title
				tmpTitle = cursor.getString(systemId);
				if (tmpTitle == null)
					tmpTitle = cursor.getString(colTitle);

				// put title into input object
				input.mObject1 = tmpTitle;

				//if still null, then break out
				if (tmpTitle == null)
					continue;

				// don't add duplicate names
				//	if (titlesAndIds.contains(input))
				//		continue;	

				// put the id into input object
				input.mObject2 = cursor.getString(colId);

				// check if this group is empty, if so don't return it
				Cursor cursorEmpty = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, 
						new String[]{ContactsContract.Groups.SUMMARY_COUNT}, 
						ContactsContract.Groups._ID + " = "+input.mObject2, 
						null, null);

				if (cursorEmpty == null || !cursorEmpty.moveToFirst()){
					if (cursorEmpty != null)
						cursorEmpty.close();
					continue;
				}

				if (cursorEmpty.getInt(cursorEmpty.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT)) < 1){
					cursorEmpty.close();
					continue;			
				}

				// add title and id to array	
				titlesAndIds.add(input);
				cursorEmpty.close();
			}while(cursor.moveToNext());
		}

		// sort by title	
		Collections.sort(titlesAndIds);

		if (cursor != null)
			cursor.close();

		// put array list into output
		return titlesAndIds;
	}

	public static Cursor getGroupProjection(Activity act, String[] projection){

		// sql where clause
		String selection = ContactsContract.Groups.GROUP_VISIBLE + " = '1'";

		// sort order alphabetical
		String sortOrder = ContactsContract.Groups.TITLE + " COLLATE LOCALIZED ASC";

		Cursor cursor = act.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, 
				projection, selection, null, sortOrder);

		return cursor;
	}

	public static Cursor getPhoneProjection(Activity act, String[] projection, String srchName){

		// replace any "'" in srchName with "''" to search properly in sql
		srchName = srchName.replace("'", "''");

		// create string for searching
		String selection ="(UPPER(" + CommonDataKinds.Phone.DISPLAY_NAME + ") LIKE UPPER('" + srchName + "%') OR UPPER(" 
		+ CommonDataKinds.Phone.DISPLAY_NAME + ") LIKE UPPER('% " + srchName + "%'))";
		//ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '0' "+

		//selection arguments
		String[] selectionArgs =new String[] {	ContactsContract.Contacts.IN_VISIBLE_GROUP,
				CommonDataKinds.Phone.DISPLAY_NAME,
				CommonDataKinds.Phone.DISPLAY_NAME};
		selectionArgs = null;

		// grab URI	  
		Uri uri = CommonDataKinds.Phone.CONTENT_URI;

		// sort order alphabetical
		String sortOrder = CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

		// grab cursor from search result
		Cursor cursor = act.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		return cursor;
	}
	
	public static Cursor getContactProjection(
			Activity act,
			String[] projection,
			String srchName,
			boolean onlyReturnInVisibleGroup){

		// replace any "'" in srchName with "''" to search properly in sql
		srchName = srchName.replace("'", "''");

		// create string for searching
		String selection ="(UPPER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE UPPER('" + srchName + "%') OR UPPER(" 
			+ ContactsContract.Contacts.DISPLAY_NAME + ") LIKE UPPER('% " + srchName + "%'))";
		if (onlyReturnInVisibleGroup)
			selection += " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";

		//selection arguments
		String[] selectionArgs = null;

		// grab URI	  
		Uri uri = ContactsContract.Contacts.CONTENT_URI;

		// sort order alphabetical
		String sortOrder =ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

		// grab cursor from search result
		Cursor cursor = act.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		return cursor;
	}

	public static Cursor getContactInfoInGroup(ContentResolver cr, String group, Boolean isIdFlag, String[] projection){

		// if isn't Id, then we must find the id and return null if not found
		if (!isIdFlag){

			// read the names and ids
			ArrayList<TwoStrings> namesAndIds = getGroupNamesAndIdsList(cr);

			// find the id that matches the name and return null if no match
			String tmpGroup = null;
			TwoStrings current;
			for (int i=0; i < namesAndIds.size(); i++){
				current = namesAndIds.get(i);
				if (current.mObject1.compareToIgnoreCase(group)==0){
					tmpGroup = current.mObject2;
					break;
				}
			}
			if (tmpGroup == null)
				return null;
			group = tmpGroup;
		}


		String selection = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+ 
		" = '"+String.valueOf(group)+"'";

		Cursor cursor = cr.query(Data.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	public static Cursor getContactInfoInGroup(Activity act, String group, Boolean isIdFlag, String[] projection){

		// if isn't Id, then we must find the id and return null if not found
		if (!isIdFlag){

			// read the names and ids
			ArrayList<TwoStrings> namesAndIds = getGroupNamesAndIdsList(act);

			// find the id that matches the name and return null if no match
			String tmpGroup = null;
			TwoStrings current;
			for (int i=0; i < namesAndIds.size(); i++){
				current = namesAndIds.get(i);
				if (current.mObject1.compareToIgnoreCase(group)==0){
					tmpGroup = current.mObject2;
					break;
				}
			}
			if (tmpGroup == null)
				return null;
			group = tmpGroup;
		}


		String selection = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+ 
		" = '"+String.valueOf(group)+"'";

		Cursor cursor = act.getContentResolver().query(Data.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	public static Cursor getRowsFromContactId(Activity act, int contactId, String[] projection){
		String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID+ 
		" = '"+String.valueOf(contactId)+"'";

		Cursor cursor = act.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	public static String getRawContactIdFromPhoneId(Activity act, long contactId){
		String selection = ContactsContract.CommonDataKinds.Phone._ID+
		" = '"+String.valueOf(contactId)+"'";
		String[] projection = {ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID};
		String out;

		Cursor cursor = act.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, projection,
				selection, null, null);	
		if (cursor != null && cursor.moveToFirst()){
			out = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
		}else
			out = "";

		if (cursor != null)
			cursor.close();

		return out;
	}

	public static Cursor getRowsFromContactId(Activity act, long contactId, String[] projection){
		String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID+ 
		" = '"+String.valueOf(contactId)+"'";

		Cursor cursor = act.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	public static Cursor getRowsFromId(Activity act, long contactId, String[] projection){
		String selection = ContactsContract.CommonDataKinds.Phone._ID+ 
		" = '"+String.valueOf(contactId)+"'";

		Cursor cursor = act.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	public static Cursor getRowsFromId(ContentResolver cr, long contactId, String[] projection){
		String selection = ContactsContract.CommonDataKinds.Phone._ID+ 
		" = '"+String.valueOf(contactId)+"'";

		Cursor cursor = cr.query(CommonDataKinds.Phone.CONTENT_URI, projection,
				selection, null, null);

		return cursor;
	}

	/**
	 * Grab the person's first name from teh contacts database from the current position in the cursor.
	 * Cursor must contain CommonDataKinds.Phone.RAW_CONTACT_ID column
	 * @param cursor The cursor positioned at the correct id
	 * @param act The activity to use to search on.
	 * @return The user's nickname.
	 */
	public static String getFirstName(Cursor cursor, Activity act){
		TwoStrings fullName = getFirstAndLastName(cursor, act);

		return fullName.mObject1;
	}
	
	/**
	 * Grab the person's first name from the contacts database from the current position in the cursor.
	 * Cursor must contain CommonDataKinds.Phone.RAW_CONTACT_ID column
	 * @param cursor The cursor positioned at the correct id
	 * @param ctx The context to use to search with.
	 * @return The user's first and last name in a TwoString object
	 */
	public static TwoStrings getFirstAndLastName(Cursor cursor, Context ctx){
		
		// find id in database
		long contactId = cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Phone.RAW_CONTACT_ID));
		
		return getFirstAndLastName(contactId, ctx);
		
	}
	
	/**
	 * Grab the person's first name from the contacts database from the contactId.
	 * @param contactId The contact id
	 * @param ctx The context to use to search with.
	 * @return The user's first and last name in a TwoString object
	 */
	public static TwoStrings getFirstAndLastName(long contactId, Context ctx){
		
		// initialize names
		String selectedName = null;
		String lastName = null;
		
		// search parameters
		Uri uri = ContactsContract.Data.CONTENT_URI;
		String selection =
			ContactsContract.Data.RAW_CONTACT_ID+ " = " + contactId
			+" AND "
			+ContactsContract.Data.MIMETYPE+ " = '"
			+ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE+"'";

		// grab cursor from search result
		Cursor cursor2 = ctx.getContentResolver().query
			(uri, null, selection, null, null);

		// grab first and last name
		if (cursor2 != null && cursor2.moveToFirst()){
			selectedName = cursor2.getString(cursor2.getColumnIndex
					(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
			lastName = cursor2.getString(cursor2.getColumnIndex
					(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
		}

		// remove traling white
		if (selectedName != null)
			selectedName.trim();
		
		// remove traling white
		if (lastName != null)
			lastName.trim();
		
		// if we have no last name and a space in the first name, then
		// it's very likely the last name is actually falsly in the first,
		// so throw out everything pass the first space
		if (selectedName != null && selectedName.length() != 0 && 
				(lastName == null || lastName.length() == 0)){
			int lastSpace = selectedName.lastIndexOf(" ");
			if (lastSpace != -1){
				String selectedNameSafe = selectedName.substring(0);
				selectedName = selectedName.substring(0, lastSpace).replaceAll("\\s+$", "");
				if (lastSpace+1 < selectedNameSafe.length())
					lastName = selectedNameSafe.substring(lastSpace+1, selectedNameSafe.length()).replaceAll("\\s+$", "");
			}
		}

		if (cursor2 != null)
			cursor2.close();

		return new TwoStrings(selectedName, lastName);
	}
	
	

	/**
	 * Grab the person's nickname from teh contacts database from the current position in the cursor.
	 * Cursor must contain CommonDataKinds.Phone.CONTACT_ID column
	 * @param cursor The cursor positioned at the correct id
	 * @param act The activity to use to search on.
	 * @return The user's nickname.
	 */
	public static String getNickname(Cursor cursor, Activity act){

		//grab the id we are currently at
		int id = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.CONTACT_ID));

		// the string to search on, the id, nickname mimetype and custom type
		String selection = CommonDataKinds.Nickname.CONTACT_ID + " = " + id + " AND "+
		CommonDataKinds.Nickname.MIMETYPE + " = '" + CommonDataKinds.Nickname.CONTENT_ITEM_TYPE + "'";
		//	+ " AND (" + CommonDataKinds.Nickname.TYPE + " = " + CommonDataKinds.Nickname.TYPE_CUSTOM + ")";

		// the columns to grab back
		String[] projection = {CommonDataKinds.Nickname.TYPE, 
				CommonDataKinds.Nickname.NAME};

		// do the search
		Cursor cursorNickname = act.getContentResolver().query(Data.CONTENT_URI, 
				projection,
				selection, null, null);

		// grab the columns of interest
		int typeCol = cursorNickname.getColumnIndex(CommonDataKinds.Nickname.TYPE);
		int nameCol = cursorNickname.getColumnIndex(CommonDataKinds.Nickname.NAME);

		// the string to output
		String nickname = null;

		// search across all matches
		if (cursorNickname != null && cursorNickname.moveToFirst()){
			do{
				//must be a custom type (which is the nickname i think)
				if (cursorNickname.getInt(typeCol) == CommonDataKinds.Nickname.TYPE_CUSTOM){

					//grab the nickname
					nickname = cursorNickname.getString(nameCol);

					// if we have a value, then stop searching
					if (nickname != null)
						break;
				}
			}while(cursorNickname.moveToNext());
		}

		if (cursorNickname != null)
			cursorNickname.close();

		// return the nickname
		return nickname;
	}

	public static Uri insertNewNickname(Cursor cursor, Activity act, String nickname){

		//grab the id we are currently at
		int rawId = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.RAW_CONTACT_ID));

		// initialize values
		ContentValues values = new ContentValues();
		values.put(Nickname.RAW_CONTACT_ID, rawId);
		values.put(CommonDataKinds.Nickname.MIMETYPE, CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
		values.put(CommonDataKinds.Nickname.NAME, nickname);
		values.put(CommonDataKinds.Nickname.TYPE, CommonDataKinds.Nickname.TYPE_CUSTOM);

		// insert the value
		Uri url = act.getContentResolver().insert(Data.CONTENT_URI, values);

		return url;
	}

	public static int getNicknameID(Cursor cursor, Activity act){

		//grab the id we are currently at
		int id = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.CONTACT_ID));

		// the string to search on, the id, nickname mimetype and custom type
		String selection = CommonDataKinds.Nickname.CONTACT_ID + " = " + id + " AND "+
		CommonDataKinds.Nickname.MIMETYPE + " = '" + CommonDataKinds.Nickname.CONTENT_ITEM_TYPE + "'";
		//	+ " AND (" + CommonDataKinds.Nickname.TYPE + " = " + CommonDataKinds.Nickname.TYPE_CUSTOM + ")";

		// the columns to grab back
		String[] projection = {CommonDataKinds.Nickname.TYPE, 
				CommonDataKinds.Nickname.NAME, 
				CommonDataKinds.Nickname._ID};

		// do the search
		Cursor cursorNickname = act.getContentResolver().query(Data.CONTENT_URI, 
				projection,
				selection, null, null);

		// grab the columns of interest
		int typeCol = cursorNickname.getColumnIndex(CommonDataKinds.Nickname.TYPE);
		int nameCol = cursorNickname.getColumnIndex(CommonDataKinds.Nickname.NAME);
		int idCol = cursorNickname.getColumnIndex(CommonDataKinds.Nickname._ID);

		// the string to output
		String nickname = null;
		int id1 = -1;

		// search across all matches
		if (cursorNickname != null && cursorNickname.moveToFirst()){
			do{
				//must be a custom type (which is the nickname i think)
				if (cursorNickname.getInt(typeCol) == CommonDataKinds.Nickname.TYPE_CUSTOM){

					//grab the nickname
					nickname = cursorNickname.getString(nameCol);

					// if we have a value, then stop searching
					if (nickname != null){
						id1 = cursorNickname.getInt(idCol);
						break;
					}
				}
			}while(cursorNickname.moveToNext());
		}

		if (cursorNickname != null)
			cursorNickname.close();

		// return the nickname
		return id1;
	}

	/**
	 * Return a cursor that contains all contacts with the passed phone number and/or email.
	 * Only contacts where both phone and email match will return (if isRequireBothIfNotNull is true, else only 1 requried).
	 * If one is null (or empty), then only one must match, if both are null (or empty), then a null cursor is returned.
	 * @param act The calling activity is used to perform query
	 * @param Phone The phone number to search
	 * @param email The email address to search
	 * @param isRequireBothIfNotNull if true, then both phone and email are required (if both are non-null / empty) and if false, then either/or is required.
	 * @return A hashset of all the contact ids that match the phone and/or email
	 */
	public static HashSet<Integer> getCursorFromPhoneAndEmail(Activity act, String phone, String email, boolean isRequireBothIfNotNull){

		// store all the contact ids for matches
		HashSet<Integer> contactIdArrayEmail = new HashSet<Integer>();
		HashSet<Integer> contactIdArrayPhone = new HashSet<Integer>();
		
		// make empty strings null
		if (email != null && email.length() == 0)
			email = null;
		if (phone != null && phone.length() == 0)
			phone = null;	

		// if both null, return
		if (phone == null && email == null)
			return null;

		// replace any "'" in srchName with "''" to search properly in sql
		if (email != null)
			email = email.replace("'", "''");
		if (phone != null)
			phone = phone.replace("'", "''");

		// check phone numbers first
		if (phone != null){
			// define the columns I want the query to return
			String[] projection = new String[] {
					ContactsContract.PhoneLookup._ID};

			// encode the phone number and build the filter URI
			Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));

			// query time
			Cursor cursor = act.getContentResolver().query(
					contactUri,
					projection,
					null,
					null,
					null);

			// if cursor is null, then no phone number found, so return null if we require both
			if (cursor == null && isRequireBothIfNotNull)
				return null;

			// read the contact ids for non null cursor
			else if (cursor != null){

				// move to beginning of cursor
				if (cursor.moveToFirst()){

					// grab the index of the column to read contact id
					int columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID);

					// loop over cursor, grabbing the contact ids
					do{
						contactIdArrayPhone.add(cursor.getInt(columnIndex));
					}while (cursor.moveToNext());
					cursor.close();

				// if no count in cursor, then again no phone number so return null	
				}else if (isRequireBothIfNotNull){
					cursor.close();
					return null;
				}
			}
		}

		// done searching for phone numbers, now search emails
		if (email != null){
			
			// define the columns I want the query to return
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Email.DATA, 
					ContactsContract.CommonDataKinds.Email.CONTACT_ID};

			// encode the phone number and build the filter URI
			Uri contactUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(email));

			// query time
			Cursor cursor = act.getContentResolver().query(
					contactUri,
					projection,
					null,
					null,
					null);

			// if we require both and no cursor then return null
			if (cursor == null && isRequireBothIfNotNull)
				return null;

			// read the contact ids for non null cursor
			else if (cursor != null){

				// move to beginning of cursor
				if (cursor.moveToFirst()){

					// grab the index of the column to read contact id
					int columnIndex2 = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);

					// loop over cursor, grabbing the contact ids
					do{
						contactIdArrayEmail.add(cursor.getInt(columnIndex2));
					}while (cursor.moveToNext());
					cursor.close();

				// if no count in cursor, then again no phone number so return null	
				}else if (isRequireBothIfNotNull){
					cursor.close();
					return null;
				}
			}
		}

		// now combine ids either as unique union or unique intersection
		if (isRequireBothIfNotNull)
			contactIdArrayEmail.retainAll(contactIdArrayPhone);
		else{
			contactIdArrayEmail.addAll(contactIdArrayPhone);
		}
		
		// now we do a query returning a cursor for all matches to the set of contact ids.
		if (contactIdArrayEmail.size() == 0)
			return null;
		else
			return contactIdArrayEmail;
	}
	
	/**
	 * Input an array of selection fields that an sql can match. For example:<p>
	 * if field = "contactId", and collection = an array of integers, {1, 18, 6}
	 * Then an sql selection query string will be built that searces on all of those integers.
	 * The type of collection must be convertable to a string.
	 * @param <T>
	 * @param field The column name to search on
	 * @param collection The array of items where any can match the column name
	 * @return The query string.
	 */
	public static <T> String buildQueryFromArray(String field, Collection<T> collection){
		
		// initialize output string
		String output = "";
		
		// replace any ' in field with ''
		field = field.replace("'", "''");
		
		// loop over collection building string
		for (T item : collection) {
			String string = item.toString().replace("'", "''");
			output += 
				field + " = " + "'" + string + "'" + " OR ";
		}
		
		// clip last OR
		if (output.length() != 0)
			output = output.substring(0, output.length()-4);
		
		return output;	
	}
	
	/**
	 * Find a list of phone numbers, email addresses, and the name of the contact with the given contactId
	 * @param ctx A context to perform the search on
	 * @param rowId The contactId to search on
	 * @return A ThreeObjects where mObject1 is a hashSet of TwoStrings of phone numbers and type, 
	 * mObjec 2 is a hashSet of TwoStrings of email addresses and type,
	 * and mObject3 is a TwoString with firstName and lastName.
	 */
	public static ThreeObjects<HashSet<TwoStrings>, HashSet<TwoStrings>, TwoStrings> getContactPhoneArrayEmailArrayAndName(
			Context ctx,
			long rowId){
		
    	// initialize output arrays
    	HashSet<TwoStrings> phoneNumberArray = new HashSet<TwoStrings>();
    	HashSet<TwoStrings> emailAddressArray = new HashSet<TwoStrings>();
    	TwoStrings fullName = new TwoStrings("", "", "Full name of contact, where mObject1 is first name and mObject2 is second name");
    	
    	// helper string
    	String displayName = "";
    	
    	// build the selection string and args
    	String selection = 
    		ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
    	String[] selectionArgs = {String.valueOf(rowId)};

    	// find user info of contacts for the phone
    	String[] projection = {
    			CommonDataKinds.Phone.DISPLAY_NAME, 
    			CommonDataKinds.Phone.NUMBER,
    			CommonDataKinds.Phone.TYPE,
    			CommonDataKinds.Phone.LABEL,
    			CommonDataKinds.Phone.CONTACT_ID,
    			CommonDataKinds.Phone.RAW_CONTACT_ID};  // This is needed for finding first and last name, and phone is not a bug

    	// grab cursor from search result grabbing the phones
    	Cursor cursor = ctx.getContentResolver().query(
    			ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    			projection,
    			selection,
    			selectionArgs,
    			null);

    	// grab phone numbers and/or name
    	if (cursor != null){
    		int numberIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
    		int nameIndex = cursor.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME);
    		int typeIndex = cursor.getColumnIndex(CommonDataKinds.Phone.TYPE);
    		int labelIndex = cursor.getColumnIndex(CommonDataKinds.Phone.LABEL);
    		if (cursor.moveToFirst()){
    			do{
    				// grab display name
    				if (displayName.length() == 0){
    					String name = cursor.getString(nameIndex);
    					if (name != null)
    						displayName = name;
    				}

    				// grab full name
    				TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor, ctx);
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

    				// add phone numbers and types
    				String number = cursor.getString(numberIndex);
    				int typeInt = cursor.getInt(typeIndex);
    				String type = "";
    				switch (typeInt){
    				case CommonDataKinds.Phone.TYPE_HOME: type = "Home"; break;
    				case CommonDataKinds.Phone.TYPE_MOBILE: type = "Mobile"; break;
    				case CommonDataKinds.Phone.TYPE_WORK_MOBILE: type = "Work Mobile"; break;
    				case CommonDataKinds.Phone.TYPE_WORK: type = "Work"; break;
    				case CommonDataKinds.Phone.TYPE_PAGER: type = "Pager"; break;
    				case CommonDataKinds.Phone.TYPE_CUSTOM: {
    					String label = cursor.getString(labelIndex);
    					if (label == null)
    						label = "Other";
    					else if (label.equalsIgnoreCase("GrandCentral"))
    						label = "Google Voice";
    					type = label;
    					break;
    				}
    				default: type = "Other"; break;
    				}
    				if (number != null)
    					phoneNumberArray.add(new TwoStrings(com.tools.Tools.formatPhoneNumber(number), type));
    			}while (cursor.moveToNext());
    		}
    		cursor.close();
    	}

    	// now repeat for email addresses

    	// find user info of contacts
    	String[] projection2 = {
    			CommonDataKinds.Email.DISPLAY_NAME, 
    			CommonDataKinds.Email.DATA,
    			CommonDataKinds.Email.TYPE,
    			CommonDataKinds.Email.LABEL,
    			CommonDataKinds.Email.CONTACT_ID,
    			CommonDataKinds.Phone.RAW_CONTACT_ID}; // This is needed for finding first and last name, and phone is not a bug

    	// grab cursor from search result grabbing names of interest
    	Cursor cursor2 = ctx.getContentResolver().query(
    			ContactsContract.CommonDataKinds.Email.CONTENT_URI,
    			projection2,
    			selection,
    			selectionArgs,
    			null);

    	// grab email and/or name
    	if (cursor2 != null){
    		int emailIndex = cursor2.getColumnIndex(CommonDataKinds.Email.DATA);
    		int nameIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.DISPLAY_NAME);
    		int typeIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.TYPE);
    		int labelIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.LABEL);
    		if (cursor2.moveToFirst()){
    			do{
    				// grab display name
    				if (displayName.length() == 0){
    					String name = cursor2.getString(nameIndex2);
    					if (name != null)
    						displayName = name;
    				}

    				// grab full name
    				TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor2, ctx);
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
    				int typeInt = cursor2.getInt(typeIndex2);
    				String type = "";
    				switch (typeInt){
    				case CommonDataKinds.Email.TYPE_HOME: type = "Home"; break;
    				case CommonDataKinds.Email.TYPE_MOBILE: type = "Mobile"; break;
    				case CommonDataKinds.Email.TYPE_WORK: type = "Work"; break;
    				case CommonDataKinds.Email.TYPE_CUSTOM: {
    					String label = cursor2.getString(labelIndex2);
    					if (label == null)
    						label = "Other";
    					type = label;
    					break;
    				}
    				default: type = "Other"; break;
    				}
    				if (email != null)
    					emailAddressArray.add(new TwoStrings(email, type));
    			}while (cursor2.moveToNext());
    		}
    		cursor2.close();
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

    	// make output
    	ThreeObjects<HashSet<TwoStrings>, HashSet<TwoStrings>, TwoStrings> output = new
    		ThreeObjects<HashSet<TwoStrings>, HashSet<TwoStrings>, TwoStrings>
    		(phoneNumberArray, emailAddressArray, fullName,
    				"mObject1 is hashSet of TwoStrings phone numbers with number and type, "+
    				"mObject2 is hashSet of TwoStrinsg emails of email and type, and "+
    				"mObject3 is the full name of the contact stored in first and last names");
    	return output;
	}
	
	/** 
	 * Look up in the address book to try and find the user of phone, based on phone number and/or user account info
	 * @return first and last name, email hashset, and phonenumber hashset.
	 */
	public static ThreeObjects<TwoStrings, HashSet<String>, HashSet<String>>
		findSelfInAddressBook(Activity act){

		// grab phone number from phone
		String phone = com.tools.Tools.getMyStrippedPhoneNumber(act);
		String mainPhone = phone;
		if (mainPhone == null)
			mainPhone = "";

		// now grab user email from accounts list, just grab the first one
		ArrayList<String> googleAccounts = com.tools.CustomCursors.getGoogleAccountNames(act);

		// if empty, then add a null "account" for searching
		if (googleAccounts.size() == 0)
			googleAccounts.add(null);

		// find all the matches in address book that have both phone and email looping across googleAccounts
		HashSet<Integer> contactIds = new HashSet<Integer>();
		for (String account : googleAccounts){
			HashSet<Integer> tmp = 
				com.tools.CustomCursors.getCursorFromPhoneAndEmail(act, phone, account, true);
			if (tmp != null)
				contactIds.addAll(tmp);
		}

		// if there are none that both match, then do either or
		if (contactIds.size() == 0){
			for (String account : googleAccounts){
				HashSet<Integer> tmp = 
					com.tools.CustomCursors.getCursorFromPhoneAndEmail(act, phone, account, false);
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
			Cursor cursor = act.getContentResolver().query(
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
						TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor, act);
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
			Cursor cursor2 = act.getContentResolver().query(
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
						TwoStrings fullNameTmp = com.tools.CustomCursors.getFirstAndLastName(cursor2, act);
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
		ThreeObjects<TwoStrings, HashSet<String>, HashSet<String>> output = 
				new ThreeObjects<TwoStrings, HashSet<String>, HashSet<String>>(
						fullName,
						emailAddressArray,
						phoneNumberArray);
			
		return output;
	}
	
	/**
	 * Find a list of phone numbers and email addresses of the contact with the given contactId
	 * @param ctx A context to perform the search on
	 * @param rowId The contactId to search on
	 * @return A TwoObjects where mObject1 is a hashSet of TwoStrings of phone numbers and type, 
	 * mObject2 is a hashSet of TwoStrings of email addresses and type
	 */
	public static TwoObjects<HashSet<TwoStrings>, HashSet<TwoStrings>> getContactPhoneAndEmailArray(
			Context ctx,
			long rowId){
		
    	// initialize output arrays
    	HashSet<TwoStrings> phoneNumberArray = new HashSet<TwoStrings>();
    	HashSet<TwoStrings> emailAddressArray = new HashSet<TwoStrings>();
    	
    	// build the selection string and args
    	String selection = 
    		ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
    	String[] selectionArgs = {String.valueOf(rowId)};

    	// find user info of contacts for the phone
    	String[] projection = {CommonDataKinds.Phone.NUMBER,
    			CommonDataKinds.Phone.TYPE,
    			CommonDataKinds.Phone.LABEL};

    	// grab cursor from search result grabbing the phones
    	Cursor cursor = ctx.getContentResolver().query(
    			ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    			projection,
    			selection,
    			selectionArgs,
    			null);

    	// grab phone numbers and/or name
    	if (cursor != null){
    		int numberIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
    		int typeIndex = cursor.getColumnIndex(CommonDataKinds.Phone.TYPE);
    		int labelIndex = cursor.getColumnIndex(CommonDataKinds.Phone.LABEL);
    		if (cursor.moveToFirst()){
    			do{
    				
    				// add phone numbers and types
    				String number = cursor.getString(numberIndex);
    				int typeInt = cursor.getInt(typeIndex);
    				String type = "";
    				switch (typeInt){
    				case CommonDataKinds.Phone.TYPE_HOME: type = "Home"; break;
    				case CommonDataKinds.Phone.TYPE_MOBILE: type = "Mobile"; break;
    				case CommonDataKinds.Phone.TYPE_WORK_MOBILE: type = "Work Mobile"; break;
    				case CommonDataKinds.Phone.TYPE_WORK: type = "Work"; break;
    				case CommonDataKinds.Phone.TYPE_PAGER: type = "Pager"; break;
    				case CommonDataKinds.Phone.TYPE_CUSTOM: {
    					String label = cursor.getString(labelIndex);
    					if (label == null)
    						label = "Other";
    					else if (label.equalsIgnoreCase("GrandCentral"))
    						label = "Google Voice";
    					type = label;
    					break;
    				}
    				default: type = "Other"; break;
    				}
    				if (number != null)
    					phoneNumberArray.add(new TwoStrings(com.tools.Tools.formatPhoneNumber(number), type));
    			}while (cursor.moveToNext());
    		}
    		cursor.close();
    	}

    	// now repeat for email addresses

    	// find user info of contacts
    	String[] projection2 = {
    			CommonDataKinds.Email.DATA,
    			CommonDataKinds.Email.TYPE,
    			CommonDataKinds.Email.LABEL};

    	// grab cursor from search result grabbing names of interest
    	Cursor cursor2 = ctx.getContentResolver().query(
    			ContactsContract.CommonDataKinds.Email.CONTENT_URI,
    			projection2,
    			selection,
    			selectionArgs,
    			null);

    	// grab email and/or name
    	if (cursor2 != null){
    		int emailIndex = cursor2.getColumnIndex(CommonDataKinds.Email.DATA);
    		int typeIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.TYPE);
    		int labelIndex2 = cursor2.getColumnIndex(CommonDataKinds.Email.LABEL);
    		if (cursor2.moveToFirst()){
    			do{
    				
    				// add email addresses
    				String email = cursor2.getString(emailIndex);
    				int typeInt = cursor2.getInt(typeIndex2);
    				String type = "";
    				switch (typeInt){
    				case CommonDataKinds.Email.TYPE_HOME: type = "Home"; break;
    				case CommonDataKinds.Email.TYPE_MOBILE: type = "Mobile"; break;
    				case CommonDataKinds.Email.TYPE_WORK: type = "Work"; break;
    				case CommonDataKinds.Email.TYPE_CUSTOM: {
    					String label = cursor2.getString(labelIndex2);
    					if (label == null)
    						label = "Other";
    					type = label;
    					break;
    				}
    				default: type = "Other"; break;
    				}
    				if (email != null)
    					emailAddressArray.add(new TwoStrings(email, type));
    			}while (cursor2.moveToNext());
    		}
    		cursor2.close();
    	}
    	
    	// make output
    	TwoObjects<HashSet<TwoStrings>, HashSet<TwoStrings>> output = new
    		TwoObjects<HashSet<TwoStrings>, HashSet<TwoStrings>>
    		(phoneNumberArray, emailAddressArray,
    				"mObject1 is hashSet of TwoStrings phone numbers with number and type, "+
    				"mObject2 is hashSet of TwoStrinsg emails of email and type, and ");
    	return output;
	}
}
