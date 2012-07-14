package com.instantPhotoShare.Adapters;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.tools.ThreeObjects;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class UsersAdapter
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

	private static final String DELIM = ","; // cannot change this as hashset converter does commas

	private static final String LOOKUP_KEY_TYPE = "String not null default ''";

	private Context ctx;

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

	public UsersAdapter(Context context) {
		super(context);
		ctx = context;
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
	 * Make a new contact in the database or update an existing one.
	 * Puts isUpdating to false and doesn't overwrite serverId if -1 is input.
	 * @param ctx The context to perform searches on
	 * @param contactId The contactId from the address book
	 * @param lookupKey the lookup key used to find the user later
	 * @param defaultContactMethod The default contact method for the user. If empty, then it will not overwrite
	 * @return The row id added or updated.
	 */
	public long makeNewUser(
			Context ctx,
			long contactId,
			String lookupKey,
			String defaultContactMethod){

		// initialize items to insert.
		ContentValues values = new ContentValues();

		// misc values
		values.put(KEY_CONTACTS_ROW_ID, contactId);
		if (defaultContactMethod != null && defaultContactMethod.length() > 0)
			values.put(KEY_DEFAULT_CONTACT, defaultContactMethod);
		values.put(KEY_IS_UPDATING, false);
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

		// The where clause
		String where = 
				KEY_CONTACTS_ROW_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(contactId)};

		// update the row, insert it if not possible
		long newRow = -1;
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0)
			newRow = database.insert(TABLE_NAME, null, values);
		if (affected == 1){
			UsersAdapter users = new UsersAdapter(ctx);
			users.fetchUserByContactsId(contactId);
			newRow = users.getRowId();
			users.close();
		}
		if (affected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to update more than one row. This should never happen");

		return newRow;
	}

	/**
	 * Make a new contact in the database for a given serverId
	 * If the user already exists, then nothing happens adn the original rowId is returned
	 * @param serverId The server id 
	 * @return The row id added or updated.
	 */
	public long makeNewUser(
			long serverId){

		synchronized (UsersAdapter.class) {

			// initialize items to insert.
			ContentValues values = new ContentValues();

			// misc values
			values.put(KEY_SERVER_ID, serverId);

			// The where clause
			String where = 
					KEY_SERVER_ID + " = ?";

			// The selection args
			String[] selectionArgs = {String.valueOf(serverId)};

			// update the row, insert it if not possible
			long newRow = -1;
			int affected = database.update(TABLE_NAME,
					values,
					where,
					selectionArgs);
			if (affected == 0)
				newRow = database.insert(TABLE_NAME, null, values);
			if (affected == 1){
				UsersAdapter users = new UsersAdapter(ctx);
				users.fetchUserByServerId(serverId);
				newRow = users.getRowId();
				users.close();
			}
			if (affected > 1 && !Prefs.debug.allowMultipleUpdates)
				throw new IllegalArgumentException("attempting to update more than one row. This should never happen");

			return newRow;
		}
	}

	/**
	 * Insert a new user into the database. Return the new rowId or -1 if unsuccessful.
	 * This user will not have a serverId attached
	 * to it, and its isSynced column will be false. Use setIsSynced to set these.
	 * @param ctx Context used for various calls
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
			Context ctx,
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

	/**
	 * Return a user for the given rowId
	 * 
	 * @param rowId id of the user to retrieve
	 * @return User object, null if none found at rowId
	 */
	public User getUser(long rowId){

		// default
		User output = null;

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

		// check null
		if (cursor == null || !cursor.moveToFirst())
			return output;

		// check if we are accessing more than one row, this shouuldn't happen
		if (cursor.getCount() > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to access more than one row. This should never happen");

		// make the group from cursor
		output = new User(cursor);

		// close and return
		cursor.close();
		return output;
	}

	/**
	 * Get an array of all users stored in database
	 * @return
	 */
	public ArrayList<User> getAllUsers(){

		// grab the cursor over all groups
		Cursor cursor = fetchAllCursor(TABLE_NAME);

		// initalize output
		ArrayList<User> output = new ArrayList<User>();

		// check null
		if (cursor == null)
			return output;

		// loop over cursor
		while (cursor.moveToNext()){
			output.add(new User(cursor));
		}

		cursor.close();
		return output;
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
	 * @param rowId the rowId of the user to update.
	 * @param isSynced boolean if we are synced
	 * @param newServerId, input -1, if not known and nothing will be saved
	 * @param hasAccount, does the user have an account or is this a temp placeholder?
	 * @return boolean if we updated successfully to sql table.
	 */
	public boolean setIsSynced(long rowId, boolean isSynced, long newServerId, boolean hasAccount){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_SYNCED, isSynced);	
		if (isSynced)
			values.put(KEY_IS_UPDATING, false);
		if (newServerId != -1)
			values.put(KEY_SERVER_ID, newServerId);

		boolean didWeUpdate = false;
		synchronized (database) {

			// update
			didWeUpdate = database.update(TABLE_NAME,values,KEY_ROW_ID + "='" + rowId + "'", null) > 0;

			// check that we don't have multiple serverIds
			combineCommonServerIds(newServerId);
		}

		return didWeUpdate;
	}

	/**
	 * Search the database and combine any rows that have this same serverid. If 0 or -1, we ignore and just return
	 * @param serverId The serverId to check
	 */
	private void combineCommonServerIds(long serverId){
		//TODO: make sure this is working correctly
		
		
		UsersAdapter helper = new UsersAdapter(ctx);

		// no check on -1 or 0
		if (serverId == 0 || serverId == -1)
			return;

		// find all users with this serverId
		helper.fetchUserByServerId(serverId);

		// none or only 1, then nothing to combine, so just leave
		if (helper.size() == 0 || helper.size() == 1){
			helper.close();
			return;
		}

		// initialize items to insert.
		ContentValues values = new ContentValues();		

		/*
		 *  loop over the users finding one with the following criteria that takes precedent over others
		 *  1. have contact id AND phonelookup 64 points
		 *  2. have email  or phone gives 32 points
		 *  3. 
		 *  
		 *  1. if we have email and / or phone highest priority
		 *  2. if #1, then highest row id wins
		 *  3. if not #1, then 
		 */
		boolean bool = false;
		int points = 0;
		int bestPoints = 0;
		long rowToKeep = 0;
		ArrayList<Long> allUserIds = new ArrayList<Long>();
		do{
			// reset points
			points = 0;

			// keep track of all ids, so we can combine them later
			allUserIds.add(helper.getRowId());

			// calculate points for this user
			// contact id and lookup key
			long contactId = helper.getContactDatabaseRowId();
			String lookupKey = helper.getLookupKey();
			if (contactId != -1 && contactId != 0
					&& lookupKey != null && lookupKey.length() != 0)
				points +=64;

			// email and phone
			String email = helper.getEmails();
			String phone = helper.getPhones();
			if (email != null && email.length() > 0 &&
					phone != null && phone.length() > 0)
				points += 32;

			// update values, but if conflict, then user higher point
			updateHelper(values, points, bestPoints, KEY_LOOKUP_KEY, lookupKey);
			updateHelper(values, points, bestPoints, KEY_FIRST_NAME, helper.getFirstName());
			updateHelper(values, points, bestPoints, KEY_LAST_NAME, helper.getLastName());
			updateHelper(values, points, bestPoints, KEY_EMAILS, email);
			updateHelper(values, points, bestPoints, KEY_PHONES, phone);
			updateHelper(values, points, bestPoints, KEY_DEFAULT_CONTACT, helper.getDefaultContactInfo());
			updateHelper(values, points, bestPoints, KEY_CONTACTS_ROW_ID, contactId);
			updateHelper(values, points, bestPoints, KEY_PICTURE_ID, helper.getPictureId());

			// has account
			bool = helper.getHasAccount();
			if (bool)
				values.put(KEY_HAS_ACCOUNT, bool);

			// update bestPoints
			if (points >= bestPoints){
				bestPoints = points;
				rowToKeep = helper.getRowId();
			}

			//TODO: finish the below fields
			//TODO: we need some way to know if we've added new fields to this database
			//private static final String KEY_DATE_JOINED = "dateJoined";
			//private static final String KEY_IS_UPDATING = "isUpdating";
			//private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";
			//private static final String KEY_IS_SYNCED = "isSynced";
		}while(helper.moveToNext());

		helper.close();

		// update the database with combination of all the best values
		if(database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "=?", new String[] {String.valueOf(rowToKeep)}) > 0){

			// update database to remove all old users
			combineLinksForUsers(rowToKeep, allUserIds);

		}else
			Log.e(Utils.LOG_TAG, "database did not update correctly");
	}

	/**
	 * Update the values of a content value at a given key with the new value only if: <br>
	 * newValue != null && newValue.length() != 0  && (previousValue == null || points >= bestPoints)
	 * @param values The contentValues to update
	 * @param points The validity ranking of the new value (bigger takes precedence)
	 * @param bestPoints the validity ranking of the old value
	 * @param key The key to update
	 * @param newValue The new value to update with
	 */
	private static void updateHelper(ContentValues values, int points, int bestPoints, String key, String newValue){
		String previousValue = values.getAsString(key);
		if (newValue != null && newValue.length() != 0  && 
				(previousValue == null || points >= bestPoints))
			values.put(key, newValue);
	}

	/**
	 * Update the values of a content value at a given key with the new value only if: <br>
	 * newValue != -1 && newValue != 0  && (previousValue == null || points >= bestPoints)
	 * @param values The contentValues to update
	 * @param points The validity ranking of the new value (bigger takes precedence)
	 * @param bestPoints the validity ranking of the old value
	 * @param key The key to update
	 * @param newValue The new value to update with
	 */
	private static void updateHelper(ContentValues values, int points, int bestPoints, String key, long newValue){
		Long previousValue = values.getAsLong(key);
		if (newValue != -1 && newValue != 0  && 
				(previousValue == null || points >= bestPoints))
			values.put(key, newValue);
	}

	/**
	 * Update all the links that contain any of these users, and change them to the main user
	 * @param userRowIdToKeep The user to overwirte all the other users
	 * @param otherUserRowIds The users that will be overwritten
	 */
	private void combineLinksForUsers(long userRowIdToKeep, ArrayList<Long> otherUserRowIds){
		// first remove userRowIdToKeep from otherUserRowIds
		while(otherUserRowIds.remove(userRowIdToKeep));

		// if empty, then return
		if (otherUserRowIds.size() == 0)
			return;

		// the query items
		TwoObjects<String, String[]> selection = TableAdapter.createSelection(KEY_ROW_ID, otherUserRowIds);

		// synchronize over the database
		synchronized (database) {
			// first delete the rows in this database
			int effected = database.delete(
					TABLE_NAME,
					selection.mObject1,
					selection.mObject2);
			if (effected != otherUserRowIds.size())
				Log.e(Utils.LOG_TAG, "did not delete the same number of rows as we entered");

			// now update all the rows in the groups adapter
			GroupsAdapter groups = new GroupsAdapter(ctx);
			groups.combineLinksForUsers(userRowIdToKeep, otherUserRowIds);

			// now update all the rows in the pictures adapter
			PicturesAdapter pictures = new PicturesAdapter(ctx);
			pictures.combineLinksForUsers(userRowIdToKeep, otherUserRowIds);

			// now update all the rows in the usersingroups adapter
			UsersInGroupsAdapter usersInGroups = new UsersInGroupsAdapter(ctx);
			usersInGroups.combineLinksForUsers(userRowIdToKeep, otherUserRowIds);
		}
		//TODO: figured out how to make sure we update everywehre user apperas, for example if we add user to a new databse like commetns for example

	}

	/**
	 * Get the row id from the phone's contact database that aligns with this "user" rowId
	 * @param contactId The rowId in the "user" database
	 * @return the rowId in the contacts database on the phone, -1 if none.
	 */
	public int getRowIdFromContactsDatabaseDONTUSE(long contactId){

		return getIntFrowRowId(contactId, KEY_CONTACTS_ROW_ID);
	}

	/**
	 * Update or create new link between this current user and the contacts database user
	 * @param usersRowId The row id of the "users" database this contact is located at
	 * @param contactsRowId The row id in the contacts database
	 */
	public void setRowIdFromContactsDatabaseDONTUSE(int usersRowId, int contactsRowId){

		// the value to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);	

		// The where clause
		String where = 
				KEY_ROW_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(usersRowId)};

		// update the row
		database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
	}

	/**
	 * Get the default contact info, either an email string or phone string, for this row id.
	 * This searches on the "users" databse rowId. To search on the contacts database row id, see getDefaultContactInfoFromContactsDatabaseRowId
	 * @param rowId the rowId of the contact
	 * @return a string of either an email or phone, null if none found.
	 */
	public String getDefaultContactInfoDONTUSE(int rowId){

		return getStringFrowRowId(rowId, KEY_DEFAULT_CONTACT);
	}

	/**
	 * Get the default contact info, either an email string or phone string, for a given contacts database row id.
	 * This searches on the "contacts" databse rowId. To search on the "users" database row id, see getDefaultContactInfo
	 * @param rowId the rowId of the contact in the contacts databse
	 * @return a string of either an email or phone, null if none found.
	 */
	public String getDefaultContactInfoFromContactsDatabaseRowIdDONTUSE(int rowId){
		ArrayList<String> tmp = getStringsFrowColumnName(KEY_CONTACTS_ROW_ID, String.valueOf(rowId), new String[] {KEY_DEFAULT_CONTACT});
		if (tmp.size() > 0)
			return tmp.get(0);
		else
			return null;
	}

	/**
	 * Set the default contact info based on the id from the contacts database, not the "users" database
	 * @param contactsRowId The contact Id
	 * @param defaultContact The default contact info, ie "johnsmith@gmail.com"
	 */
	public void setDefaultContactInfoFromContactsDatabaseRowIdDONTUSE(int contactsRowId, String defaultContact){
		// the value to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);	
		values.put(KEY_DEFAULT_CONTACT, defaultContact);
		values.put(KEY_IS_UPDATING, false);

		// check if we have a row that we can update


		// The where clause
		String where = 
				KEY_CONTACTS_ROW_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(contactsRowId)};

		// update the row
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0)
			database.insert(TABLE_NAME, null, values);
		if (affected > 1 && !Prefs.debug.allowMultipleUpdates){
			throw new IllegalArgumentException("attempting to update more than one row. This should never happen");
		}
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
	 * Get the rowId in this database given the contacts Database row id
	 * @param contactId The contacts Database rowId
	 * @return the "users" database row id, -1 if not found.
	 */
	public int getRowIdGivenContactsDatabaseRowIdDONTUSE(long contactId){

		// default output
		int output = -1;

		// create the query
		String selection = 
				KEY_CONTACTS_ROW_ID+ " = ?";

		// the selection args
		String[] selectionArgs = {String.valueOf(contactId)};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {KEY_ROW_ID},
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the row id
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROW_ID));
		cursor.close();
		return output;
	}

	/**
	 * Delete the given user. Also delete any connections in user - group database
	 * @param ctx Context needed to delete connections
	 * @param rowId the rowId of the user to delete
	 * @return the number of connections deleted
	 */
	public int deleteUserForDebug(Context ctx, long rowId){

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
	 * Move the cursor to the correct location based on the serverId
	 * @param serverId
	 */
	public void fetchUserByServerId(long serverId){
		// grab the cursor
		Cursor cursor =

				database.query(
						true,
						TABLE_NAME,
						null,
						KEY_SERVER_ID + "='" + serverId +"'",
						null,
						null,
						null,
						null,
						null);

		setCursor(cursor);
		moveToFirst();
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
	 * Return the default contact method for this user. "" or null if none.
	 * @return
	 */
	public String getDefaultContactInfo(){
		if (!checkCursor())
			return "";
		return getString(KEY_DEFAULT_CONTACT);
	}

	/**
	 * Return if this user has an open account, or false if we could not read.
	 * @return
	 */
	public boolean getHasAccount(){
		if (!checkCursor())
			return false;
		else return getBoolean(KEY_HAS_ACCOUNT);
	}

	/**
	 * Return the pictureId for this user or -1 if it couldn't be found
	 * @return
	 */
	public long getPictureId(){
		if (!checkCursor())
			return -1;
		return getLong(KEY_PICTURE_ID);
	}

	/**
	 * Return the users name. Usually first + last. Will query the server if not available
	 * @return the name, will query server if unavailable and if still unavailable will be ""
	 */
	public String getName(){
		String first = getFirstName();
		String last = getLastName();
		String name = "";
		if (first != null)
			name = first;
		if (last != null){
			if (name.length() > 0)
				name += " ";
			name+=last;
		}

		// no name, so grab user from the server
		if (name.length() == 0 && getServerId() > 0){
			
			// the list of users to request info on
			JSONArray array = new JSONArray();
			array.put(getServerId());
			
			// create json to post
			JSONObject json = new JSONObject();
			try {
				json.put("user_id", Prefs.getUserServerId(ctx));
				json.put("secret_code", Prefs.getSecretCode(ctx));
				json.put("user_ids", array);
			} catch (JSONException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return name;
			}
			
			// post the data
			ShareBearServerReturn result = Utils.postToServer("get_users", json, null, null);
			
			// grab the name and save to database
			JSONObject message = result.getMessageObject();
			try {
				String firstName = message.optJSONObject(String.valueOf(getServerId())).optString("first_name");
				String lastName = message.getJSONObject(String.valueOf(getServerId())).optString("last_name");
				name = firstName + " " + lastName;		
			} catch (Exception e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}
		
		return name;
	}
	
	/**
	 * Set the first and last name at the current rowId location
	 * @param firstName The first name to set
	 * @param lastName The last name to set
	 */
	public void setName(String firstName, String lastName){
		// grab the rowId
		long rowId = getRowId();
		if (rowId == -1){
			Log.e(Utils.LOG_TAG, "attempted to setName at a bad rowId");
			return;
		}
		
		// the values to update
		ContentValues values = new ContentValues(2);
		if (firstName != null)
			values.put(KEY_FIRST_NAME, firstName);
		if (lastName != null)
			values.put(KEY_LAST_NAME, lastName);
		
		// update
		if (database.update(TABLE_NAME, values, KEY_ROW_ID + " =?", new String[] {String.valueOf(rowId)}) <=0)
			Log.e(Utils.LOG_TAG, "could not update user " + rowId);
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
	 * This searches the contacts database to confirm we have the correct id, so it can be slow
	 * @return
	 */
	public long getContactDatabaseRowId(){
		//TODO: this method is slow, but currently required to make sure we don't get contact confusion. it can be fixed by outputing a uri, so it doesn't have to be done again in adduserstogroup
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
		String[] projection = {ContactsContract.Contacts._ID};
		Cursor cursor = ctx.getContentResolver().query(
				uri2,
				projection,
				null, null, null);

		// read the id
		int id = -1;
		if (cursor != null && cursor.moveToFirst()){
			id = cursor.getInt(0);
		}
		if (cursor != null)
			cursor.close();

		// if we have a changed id, then we need to update.
		//if (originalId != id){
		//TODO: determine if this should happen or not
		//setRowIdFromContactsDatabaseDONTUSE((int)getRowId(), id);
		//}

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

	// helper functions
	/**
	 * Get an int from the table for a given rowID and a given column. -1 if none found
	 * @param contactId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, -1 if none found.
	 */
	private int getIntFrowRowId(long contactId, String columnName){

		// default output
		int output = -1;

		// create the query
		String selection = 
				KEY_ROW_ID + " = ?";

		// the selection args
		String[] selectionArgs = {String.valueOf(contactId)};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the row id
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}

	/**
	 * Get a string from the table for a given rowID and a given column. null if none found
	 * @param rowId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, null if none found.
	 */
	private String getStringFrowRowId(int rowId, String columnName){

		// default output
		String output = null;

		// create the query
		String selection = 
				KEY_ROW_ID + " = ?";

		// the selection args
		String[] selectionArgs = {String.valueOf(rowId)};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the row id
		output =  cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}

	/**
	 * Get an array of strings returned from grabbing the values at an array of column name.
	 * The rows are determined by searching the columnName == columnValue.
	 * If multiple rows, then only the first is returned
	 * The output array will be empty if there are no matches
	 * @param columnName the column name to search on
	 * @param columnValue the value at the searching column
	 * @param returnColumns, the name of all the columns we want returned
	 * @return the value at all of the columns in returnColumns
	 */
	private ArrayList<String> getStringsFrowColumnName(
			String columnName,
			String columnValue,
			String[] returnColumns){

		// default output
		ArrayList<String> output = new ArrayList<String>(returnColumns.length);

		// create the query
		String selection = 
				columnName + " = ?";

		// the selection args
		String[] selectionArgs = {columnValue};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				returnColumns,
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the values
		for (int i = 0; i < returnColumns.length; i++)
			output.add(i, cursor.getString(cursor.getColumnIndexOrThrow(returnColumns[i])));
		cursor.close();
		return output;
	}

	/**
	 * Get an int of string returned from grabbing the value at a column name.
	 * The rows are determined by searching the columnName == columnValue.
	 * If multiple rows, then only the first is returned.
	 * Default value is -1;
	 * @param columnName the column name to search on
	 * @param columnValue the value at the searching column
	 * @param returnColumn, the name of the column we want returned
	 * @return the value at the column in returnColumn
	 */
	private int getIntFrowColumnName(
			String columnName,
			String columnValue,
			String returnColumn){

		// default output
		int output = -1;

		// create the query
		String selection = 
				columnName + " = ?";

		// the selection args
		String[] selectionArgs = {columnValue};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String[]{returnColumn},
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the value
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(returnColumn));
		cursor.close();
		return output;
	}

	/**
	 * Get a string from the table for a given rowID and a given column. null if none found
	 * @param rowId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, null if none found.
	 */
	private String getStringFrowContactsDatabaseRowId(int rowId, String columnName){

		// default output
		String output = null;

		// create the query
		String selection = 
				KEY_ROW_ID + " = ?";

		// the selection args
		String[] selectionArgs = {String.valueOf(rowId)};

		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the row id
		output =  cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}

	/**
	 * Object that defines a user.
	 *
	 */
	public class User{
		private long rowId = -1;
		private String firstName;
		private String lastName;
		private long serverId = -1;
		private ArrayList<String> emailsArray;
		private String phone;
		private long pictureId;
		private String dateJoined;
		private boolean hasAccount;
		private boolean isUpdating;
		private boolean isSynced;
		private String defaultContactMethod;
		private long contactsRowId;

		private boolean isEmpty = true;
		private String lastUpdateAttemptTime;

		/**
		 * Create a user from the database cursor at the current location.
		 * @param cursor
		 */
		private User(Cursor cursor){

			// check null and size of cursor
			if (cursor == null)
				return;

			// fill fields
			setEmpty(false);	
			setRowId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROW_ID)));
			setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIRST_NAME)));
			setLastName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_NAME)));
			setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SERVER_ID)));
			setEmailsArray(com.tools.Tools.setArrayFromString(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAILS)), DELIM));
			setPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONES)));
			setPictureId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PICTURE_ID)));
			setDateJoined(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE_JOINED)));
			setLastUpdateAttemptTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_UPDATE_ATTEMPT_TIME)));	
			setHasAccount(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_ACCOUNT)) > 0);
			setUpdating(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_UPDATING))>0);
			setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_SYNCED))>0);
			setDefaultContactMethod(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEFAULT_CONTACT)));
			setContactsRowId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CONTACTS_ROW_ID)));
		}

		@Override
		public String toString(){
			return getFullName();
		}

		/**
		 * Return first + last name as full name
		 * @return
		 */
		public String getFullName(){
			String name = getFirstName();
			String last = getLastName();
			if (last != null && last.length() > 0)
				name += " " + last;
			return name;
		}

		/**
		 * These two objects are considered equal if all fields are equal
		 */
		@Override
		public boolean equals(Object o){
			// must be correct type
			if (!( o instanceof User) || o==null)
				return false;

			// convert			
			User input = ((User) o);

			// if empty, they cannot be equal
			if (isEmpty() || input.isEmpty())
				return false;

			// now compare each field
			return equalsHelper(input);
		}

		/**
		 * compare each field in this class to be equal
		 * @param user
		 * @return true if all fields equal, false otherwise.
		 */
		private boolean equalsHelper(User user){
			Class<? extends Object> thisClass = this.getClass();
			Field[] fieldsArray = thisClass.getDeclaredFields();
			for (Field field : fieldsArray){
				if (field.getName().equalsIgnoreCase("this$0"))
					continue;
				try {
					Object tmp = field.get(this);
					if (tmp == null || !this.equals(field.get(user)))
						return false;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			return true;
		}

		@Override
		public int hashCode(){
			if (isEmpty())
				return 0;
			else
				return ((Long)getRowId()).hashCode();
		}

		private void setEmpty(boolean isEmpty) {
			this.isEmpty = isEmpty;
		}

		public boolean isEmpty() {
			return isEmpty;
		}

		private void setUpdating(boolean isUpdating) {
			this.isUpdating = isUpdating;
		}

		public boolean isUpdating() {
			// if the last time we updated is too long ago, then we are not updating any more
			boolean timeout;
			try {
				timeout = Utils.parseMilliseconds(Utils.getNowTime()) - Utils.parseMilliseconds(getLastUpdateAttemptTime()) > 1000*Utils.SECONDS_SINCE_UPDATE_RESET;
			} catch (ParseException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				timeout = true;
			}
			if (timeout) 
				return false;
			else
				return isUpdating;
		}

		private void setLastUpdateAttemptTime(String lastUpdateAttemptTime) {
			this.lastUpdateAttemptTime = lastUpdateAttemptTime;
		}

		public String getLastUpdateAttemptTime() {
			return lastUpdateAttemptTime;
		}

		private void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getFirstName() {
			return firstName;
		}

		private void setRowId(long rowId) {
			this.rowId = rowId;
		}

		public long getRowId() {
			return rowId;
		}

		private void setServerId(long serverId) {
			this.serverId = serverId;
		}

		public long getServerId() {
			return serverId;
		}

		private void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getLastName() {
			return lastName;
		}

		private void setEmailsArray(ArrayList<String> emailsArray) {
			this.emailsArray = emailsArray;
		}

		public ArrayList<String> getEmailsArray() {
			return emailsArray;
		}

		private void setPhone(String phone) {
			this.phone = phone;
		}

		public String getPhone() {
			return phone;
		}

		private void setPictureId(long pictureId) {
			this.pictureId = pictureId;
		}

		public long getPictureId() {
			return pictureId;
		}

		private void setDateJoined(String dateJoined) {
			this.dateJoined = dateJoined;
		}

		public String getDateJoined() {
			return dateJoined;
		}

		private void setHasAccount(boolean hasAccount) {
			this.hasAccount = hasAccount;
		}

		public boolean isHasAccount() {
			return hasAccount;
		}

		private void setSynced(boolean isSynced) {
			this.isSynced = isSynced;
		}

		public boolean isSynced() {
			return isSynced;
		}

		private void setDefaultContactMethod(String defaultContactMethod) {
			this.defaultContactMethod = defaultContactMethod;
		}

		public String getDefaultContactMethod() {
			return defaultContactMethod;
		}

		private void setContactsRowId(long contactsRowId) {
			this.contactsRowId = contactsRowId;
		}

		public long getContactsRowId() {
			return contactsRowId;
		}
	}

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {

	}
}