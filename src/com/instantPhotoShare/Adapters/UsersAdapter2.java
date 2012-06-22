package com.instantPhotoShare.Adapters;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.tools.ThreeObjects;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class UsersAdapter2
extends TableAdapter <UsersAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "userInfo";
	
	// user table keys
	public static final String KEY_ROW_ID = "_id";
	private static final String KEY_FIRST_NAME = "firstName";
	private static final String KEY_LAST_NAME = "lastName";
	private static final String KEY_SERVER_ID = "serverId";
	private static final String KEY_EMAILS = "userEmails";
	private static final String KEY_PHONES = "userPhones";	
	private static final String KEY_PICTURE_ID = "pictureId";
	private static final String KEY_DATE_JOINED = "dateJoined";
	private static final String KEY_HAS_ACCOUNT = "hasAccount";
	private static final String KEY_IS_UPDATING = "isUpdating";
	private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";
	private static final String KEY_IS_SYNCED = "isSynced";
	private static final String KEY_DEFAULT_CONTACT = "defaultContact";
	private static final String KEY_CONTACTS_ROW_ID = "contactsRowId";
	private static final String KEY_LOOKUP_KEY = "KEY_LOOKUP_KEY";
	
	private static final String LOOKUP_KEY_TYPE = "String not null default ''";
	
	/** Table creation string */
	public static String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_FIRST_NAME +" text DEFAULT '', "
		+KEY_LAST_NAME +" text DEFAULT '', "
		+KEY_SERVER_ID +" integer DEFAULT '-1', "
		+KEY_EMAILS +" text, "
		+KEY_PHONES +" text, "
		+KEY_PICTURE_ID +" integer DEFAULT '-1', "
		+KEY_DATE_JOINED +" text, "
		+KEY_HAS_ACCOUNT +" boolean DEFAULT 'FALSE', "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_LAST_UPDATE_ATTEMPT_TIME +" text not null DEFAULT '1900-01-01 01:00:00', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+KEY_DEFAULT_CONTACT + " TEXT, "
		+KEY_CONTACTS_ROW_ID + " integer DEFAULT '-1', "
		+KEY_LOOKUP_KEY + " " + LOOKUP_KEY_TYPE + ", "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public UsersAdapter2(Context context) {
		super(context);
	}
	
	/**
	 * Return a list of SQL statements to perform upon an upgrade from oldVersion to newVersion.
	 * @param oldVersion old version of database
	 * @param newVersion new version of database
	 * @return The arraylist of sql statements. Will not be null.
	 */
	public static ArrayList<String> upgradeStrings(int oldVersion, int newVersion){
		ArrayList<String> out = new ArrayList<String>(1);
		if (oldVersion < 4 && newVersion >= 4){
			String upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_LOOKUP_KEY + " "+
				LOOKUP_KEY_TYPE;
			out.add(upgradeQuery);
		}
		
		return out;
	}
	
	/**
	 * Make a new contact in the database. <br>
	 * Determines names, emails, and phones, from contact database
	 * @param contactId The contactId from the address book
	 * @param lookupKey the lookup key used to find the user later
	 * @param defaultContactMethod The default contact method for the user. If empty, then it will not overwrite
	 * @return The row id added or updated.
	 */
	public long makeNewUser(
			long contactId,
			String lookupKey,
			String defaultContactMethod){
		
		// values to insert
		ContentValues values = new ContentValues(7);
		values.put(KEY_CONTACTS_ROW_ID, contactId);
		if (defaultContactMethod != null && defaultContactMethod.length() > 0)
			values.put(KEY_DEFAULT_CONTACT, defaultContactMethod);
		if (lookupKey != null && lookupKey.length() > 0)
			values.put(KEY_LOOKUP_KEY, lookupKey);

		// find emails, phones, and names
		TwoStrings fullName = null;
		ThreeObjects<HashSet<TwoStrings>, HashSet<TwoStrings>, TwoStrings>  phoneEmailName = 
			com.tools.CustomCursors.getContactPhoneArrayEmailArrayAndName(ctx, contactId);
		if (phoneEmailName != null){
			String phone = phoneEmailName.mObject1.toString();
			String email = phoneEmailName.mObject2.toString();
			// remove []
			if (phone.length()>= 2)
				phone = phone.substring(1, phone.length()-1);
			if (email.length()>= 2)
				email = email.substring(1, email.length()-1);
			fullName = phoneEmailName.mObject3;

			// fill first and last name
			if (fullName != null){
				values.put(KEY_FIRST_NAME, fullName.mObject1);
				values.put(KEY_LAST_NAME, fullName.mObject2);
			}

			// phone and email
			values.put(KEY_PHONES, phone);
			values.put(KEY_EMAILS, email);
		}	

		// make new users
		long newRow = database.insert(TABLE_NAME, null, values);

		return newRow;
	}
	
	/**
	 * Insert a new user into the database. Return the new rowId or -1 if unsuccessful.
	 * This user will not have a serverId attached
	 * to it, and its isSynced column will be false. Use setIsSynced to set these.
	 * @param firstName The first name of this person
	 * @param lastName The last name
	 * @param userEmails a comma separated list of emails 
	 * @param userPhone a phone number for this person
	 * @param pictureId The picture id to the picture for this person, null if not known
	 * @param dateJoined The date this user joined, use Utils.getNowTime() if now.
	 * @param hasAccount boolean if this user has an account, pass false if not known
	 * @param defaultContact the default method of contacting this person, ie user@gmail.com, null if not known
	 * @param contactsRowId The rowId of the link to the user in the phone's adddress book
	 * @return The rowId of the group, -1 if it failed.
	 */
	public long makeNewUser(
			String firstName,
			String lastName,
			String userEmails,
			String userPhone,
			Long pictureId,
			String dateJoined,
			boolean hasAccount,
			String defaultContact,
			Long contactsRowId){		

		// create values
		ContentValues values = new ContentValues();
		values.put(KEY_FIRST_NAME, firstName);
		values.put(KEY_PICTURE_ID, pictureId);
		values.put(KEY_DATE_JOINED, dateJoined);
		values.put(KEY_LAST_NAME, lastName);
		values.put(KEY_EMAILS, userEmails);
		values.put(KEY_PHONES, userPhone);
		values.put(KEY_HAS_ACCOUNT, hasAccount);
		values.put(KEY_DEFAULT_CONTACT, defaultContact);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);
		
		// create new row
		long newRow = database.insert(
				TABLE_NAME,
				null,
				values);
		
		return newRow;
	}	
	
	public long getRowIdGivenContactId(long contactId){
		
		// first find if there is a user that matches directly.
		UsersAdapter2 users = new UsersAdapter2(ctx);
		users.fetchUserByContactsId(contactId);
		long rowId = users.getRowId();
		users.close();
		
		// if we have a match, then just return
		if (rowId != -1)
			return rowId;

		// if no match, we can check by phone number, email, and name
		// but then upon using the lookup key of any matches, must match to contactId
		
		// each for all 3 of these items for the given contact
		ThreeObjects<HashSet<TwoStrings>,HashSet<TwoStrings>,TwoStrings> phonesEmailsName =
				com.tools.CustomCursors.getContactPhoneArrayEmailArrayAndName(ctx, contactId);
		
		// now look at each item individually
		if (phonesEmailsName != null){
			TwoStrings names = phonesEmailsName.mObject3;
			HashSet<TwoStrings> phones = phonesEmailsName.mObject1;
			HashSet<TwoStrings>  emails = phonesEmailsName.mObject2;
			
			// search on name first
			if (names != null){
				Cursor cursor = database.query(
						TABLE_NAME,
						null,
						KEY_FIRST_NAME + "= ? AND " + KEY_LAST_NAME + " = ?",
						new String[] {names.mObject1, names.mObject2},
						null,
						null,
						null);
				users.setCursor(cursor);

				// see if any the possible matches have the correct contactId
				while (users.moveToNext()){
					long newContactId = users.getContactDatabaseRowId();
					if (newContactId == contactId){
						rowId = users.getRowId();
						users.close();
						return rowId;
					}
				}
				users.close();
			}
			
			// search on email next
			if (emails != null){
				Iterator<TwoStrings> iterator = emails.iterator();
				while (iterator.hasNext()){
					String emailItem = iterator.next().mObject1;
					Cursor cursor = database.query(
							TABLE_NAME,
							null,
							KEY_EMAILS + " LIKE %?%",
							new String[] {emailItem},
							null,
							null,
							null);
					users.setCursor(cursor);

					// see if any the possible matches have the correct contactId
					while (users.moveToNext()){
						long newContactId = users.getContactDatabaseRowId();
						if (newContactId == contactId){
							rowId = users.getRowId();
							users.close();
							return rowId;
						}
					}
					users.close();
				}
			}
			
			// finally on phone
			if (phones != null){
				Iterator<TwoStrings> iterator = phones.iterator();
				while (iterator.hasNext()){
					String phoneItem = iterator.next().mObject1;
					Cursor cursor = database.query(
							TABLE_NAME,
							null,
							KEY_PHONES + " LIKE %?%",
							new String[] {phoneItem},
							null,
							null,
							null);
					users.setCursor(cursor);

					// see if any the possible matches have the correct contactId
					while (users.moveToNext()){
						long newContactId = users.getContactDatabaseRowId();
						if (newContactId == contactId){
							rowId = users.getRowId();
							users.close();
							return rowId;
						}
					}
					users.close();
				}
			}
		}
		
		// no match, return -1
		return -1;
	}
	
	/**
	 * If we are updating to the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * If isUpdating is true, then we know we are not synced, so we will set sync to false as well.
	 * @param rowId the rowId of the group to update.
	 * @param isUpdating boolean if we are updating or not
	 * @return boolean if we updated successfully to sql table.
	 */
	public boolean setIsUpdating(long rowId, boolean isUpdating){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_UPDATING, isUpdating);	
		if (isUpdating)
			values.put(KEY_IS_SYNCED, false);
		if (isUpdating)
			values.put(KEY_LAST_UPDATE_ATTEMPT_TIME, Utils.getNowTime());

		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}
	
	/**
	 * If we are synced to the server, then set this field to true.
	 * Also if we set to synced, then we know we aren't updating, so that is set to false.
	 * @param rowId the rowId of the group to update.
	 * @param isSynced boolean if we are synced
	 * @param newServerId, input -1, if not known and nothing will be saved
	 * @return boolean if we updated successfully to sql table.
	 */
	public boolean setIsSynced(long rowId, boolean isSynced, long newServerId){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_SYNCED, isSynced);	
		if (isSynced)
			values.put(KEY_IS_UPDATING, false);
		if (newServerId != -1){
			values.put(KEY_SERVER_ID, newServerId);
			values.put(KEY_LAST_UPDATE_ATTEMPT_TIME, Utils.getNowTime());
		}
		

		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}
	
	/**
	 * Update the contacts database Id and lookupKey at the given users rowId
	 * @param usersRowId The row id of the "users" database this contact is located at
	 * @param contactsRowId The row id in the contacts database
	 * @param lookupKey the lookupKey to update to. If null or "", then one will be found by searching database
	 */
	private void setContactIdAndLookupKey(
			long usersRowId,
			long contactsRowId,
			String lookupKey){
		
		// see if we need to find the lookupKey
		if (lookupKey == null || lookupKey.length() == 0){
				Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactsRowId);
				Cursor cursor = ctx.getContentResolver().query(
						uri,
						new String[]{ContactsContract.Contacts.LOOKUP_KEY},
						null,
						null,
						null);
				if (cursor != null && cursor.moveToFirst())
					lookupKey = cursor.getString(0);
				if (cursor != null)
					cursor.close();
		}
		
		// blank lookupKey, not null
		if (lookupKey == null)
			lookupKey = "";
			
		// the value to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);	
		values.put(KEY_LOOKUP_KEY, lookupKey);
		
		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(usersRowId)};
		
		// update the row
		int updated = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (updated != 1)
			Log.e(Utils.LOG_TAG, "rowId " + usersRowId + " and contactId " + contactsRowId + " not successfully linked");
	}
	
	/**
	 * Set the default contact info based on the rowId
	 * @param rowId The user row Id
	 * @param defaultContact The default contact info, ie "johnsmith@gmail.com"
	 */
	public void setDefaultContactInfo(long rowId, String defaultContact){
		
		// the values to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_DEFAULT_CONTACT, defaultContact);
		values.put(KEY_IS_SYNCED, false);
		
		// check if we have a row that we can update
		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(rowId)};
		
		// update the row
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0){
			Log.e(Utils.LOG_TAG, "Contact info could not be set for rowId = "+rowId);
		}
		if (affected > 1 && !Prefs.debug.allowMultipleUpdates){
			throw new IllegalArgumentException("attempting to update more than one row. This should never happen");
		}
	}
	
	/**
	 * Delete the given user. Also delete any connections in user - group database
	 * @param rowId the rowId of the user to delete
	 * @return the number of connections deleted
	 */
	public int deleteUserForDebug(long rowId){

		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(rowId)};
		
		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				where,
				selectionArgs);

		// throw error if we somehow linked to more than one group
		if (effected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to delete more than one row in groups. This should never happen");
		
		// delte teh connections
		UsersInGroupsAdapter connections = new UsersInGroupsAdapter(ctx);
		return connections.deleteUserDebug(rowId);				
	}
	
	/**
	 * Load the user the given rowId into the cursor for this object
	 * @param rowId
	 */
	public void fetchUser(long rowId){
		
		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_ROW_ID + "='" + rowId +"'",
					null,
					null,
					null,
					null,
					null);
		
		setCursor(cursor);
		moveToFirst();
	}
	
	/**
	 * Fetch all users that match the given rowIds. <br>
	 * *** not guaranteed to return a cursor with the same leng as the input array. ***
	 */
	public void fetchUsers(ArrayList<Long> rowIds){
		
		// the selection
		TwoObjects<String, String[]> selection = createSelection(KEY_ROW_ID, rowIds);
		
		// no inputs, set null cursor
		if (selection.mObject1.length() == 0){
			setCursor(null);
		}
		
		// get the cursor
		Cursor cursor = 
			database.query(
					TABLE_NAME,
					null,
					selection.mObject1,
					selection.mObject2,
					null,
					null,
					null);
		
		// set the cursor
		setCursor(cursor);
	}
	
	/**
	 * Return a user for the given contacts database rowId
	 * 
	 * @param rowId contacts database id of the user to retrieve
	 * @return User object, null if none found at rowId
	 */
	public void fetchUserByContactsId(long rowId){

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_CONTACTS_ROW_ID + "='" + rowId +"'",
					null,
					null,
					null,
					null,
					null);

		setCursor(cursor);
		moveToFirst();
		
		// double check that this matches, if it doens't match, then create a new one
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchUser(this.getRowId());
		if (rowId != users.getContactDatabaseRowId())
			setCursor(null);
		users.close();
	}
	
	/**
	 * Load all users into this cursor
	 * @return
	 */
	public void fetchAllUsers(){
		
		// grab the cursor over all groups
		Cursor cursor = fetchAllCursor(TABLE_NAME);
		
		// return the cursor
		setCursor(cursor);
	}
	
	/**
	 * Get a cursor for all the users that are linked to the group groupId
	 * in UsersInGroups table
	 * @param groupId
	 * @return A cursor on the users that match the groupId in UsersInGroups table
	 */
	public void fetchUsersInGroup(long groupId){

		// create the query where we match up all the pictures that are in the group
		String query = 
			"SELECT users.* FROM "
			+UsersAdapter.TABLE_NAME + " users "
			+" INNER JOIN "
			+UsersInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"users." + UsersAdapter.KEY_ROW_ID + " = "
			+"groups." + UsersInGroupsAdapter.KEY_USER_ID
			+" WHERE "
			+"groups." + UsersInGroupsAdapter.KEY_GROUP_ID
			+"=?";

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// return the cursor
		setCursor(cursor);
	}
	
	/**
	 * Return the users first name
	 * @return
	 */
	public String getFirstName(){
		if (!checkCursor())
			return "";
		return getString(KEY_FIRST_NAME);
	}
	
	/**
	 * Return the users last name
	 * @return
	 */
	public String getLastName(){
		if (!checkCursor())
			return "";
		return getString(KEY_LAST_NAME);
	}
	
	/**
	 * Return the users name. Usually first + last
	 * @return
	 */
	public String getName(){
		String first = getFirstName();
		String last = getLastName();
		String name = "";
		if (first != null)
			name+=first;
		if (last != null){
			if (name.length() > 0)
				name += " ";
			name+=last;
		}
		return name;
	}
	
	/**
	 * Readable string of user, usually getName();
	 */
	public String toString(){
		return getName();
	}
	
	/**
	 * Return a comma delimited list of the default contact methods.
	 * @return
	 */
	public String getDefaultContactMethod(){
		if (!checkCursor())
			return "";
		return getString(KEY_DEFAULT_CONTACT);
	}
	
	/**
	 * Return the contact id from the google address book. <br>
	 * This searches the contacts database to confirm we have the correct id, so it can be slow.
	 * If the contactId stored in this database is incorrect, it will be updated.
	 * @return the contacts Id
	 */
	public long getContactDatabaseRowId(){
		
		if (!checkCursor())
			return -1;
		
		// just use the current contactId if there is no lookupKey
		if (getLookupKey().length() == 0)
			return getLong(KEY_CONTACTS_ROW_ID);
		
		// keep track of original row id
		long originalId = getLong(KEY_CONTACTS_ROW_ID);
		
		// if we have a lookup key, then use it to search database
		Uri uri = ContactsContract.Contacts.getLookupUri(getLong(KEY_CONTACTS_ROW_ID), getLookupKey());
		Uri uri2 = ContactsContract.Contacts.lookupContact(ctx.getContentResolver(), uri);
		String[] projection = {ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY};
		Cursor cursor = ctx.getContentResolver().query(
				uri2,
				projection,
				null, null, null);
		
		// read the id
		long id = -1;
		String lookupKey ="";
		if (cursor != null && cursor.moveToFirst()){
			id = cursor.getLong(0);
			lookupKey = getString(1);
		}
		if (cursor != null)
			cursor.close();
		
		// if we have a changed id, then we need to update.
		if (originalId != id){
			setContactIdAndLookupKey(getRowId(), id, lookupKey);
		}
		
		// return the value
		return id;
	}
	
	/**
	 * Return the lookup key to find the user in the database
	 * @return
	 */
	public String getLookupKey(){
		if (!checkCursor())
			return "";
		return getString(KEY_LOOKUP_KEY);
	}
	
	/**
	 * Return the server id or -1 if none
	 * @return
	 */
	public long getServerId(){
		if (!checkCursor())
			return -1;
		return getLong(KEY_SERVER_ID);
	}
	
	/**
	 * Return the row ID of the contact, -1 if none
	 * @return
	 */
	public long getRowId(){
		if (!checkCursor())
			return -1;
		return getLong(KEY_ROW_ID);
	}
	
	/**
	 * Return the comma separated list of phone numbers for this user.
	 * @return
	 */
	public String getPhones(){
		if (!checkCursor())
			return "";
		return getString(KEY_PHONES);
	}
	
	/**
	 * Return the comma separated list of emails for this user.
	 * @return
	 */
	public String getEmails(){
		if (!checkCursor())
			return "";
		return getString(KEY_EMAILS);
	}
	
	/**
	 * Return the last update time of the user
	 * @return
	 */
	public String getLastUpdateTime(){
		if (!checkCursor())
			return "";
		return getString(KEY_LAST_UPDATE_ATTEMPT_TIME);
	}
	
	/**
	 * Return if we are currently updating. If we are past the maximum timeout time, then we will always return false.
	 * @return
	 */
	public boolean isUpdating() {
		if (!checkCursor())
			return false;
		
		// if the last time we updated is too long ago, then we are not updating any more
		boolean timeout;
		try {
			timeout = Utils.parseMilliseconds(Utils.getNowTime()) - Utils.parseMilliseconds(getLastUpdateTime()) > 1000*Utils.SECONDS_SINCE_UPDATE_RESET;
		} catch (ParseException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			timeout = true;
		}
		if (timeout) 
			return false;
		else
			return getBoolean(KEY_IS_UPDATING);	
	}
}
