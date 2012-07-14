package com.instantPhotoShare.Adapters;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.tools.ThreeObjects;
import com.tools.TwoObjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

public class UsersInGroupsAdapter
extends TableAdapter<UsersInGroupsAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "UsersInGroupsInfo";

	// group table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_USER_ID = "userId";
	public static final String KEY_GROUP_ID = "groupId";
	private static final String KEY_IS_UPDATING = "isUpdating";
	private static final String KEY_IS_SYNCED = "isSynced";
	
	// types for some of the keys
	private static final String LAST_UPDATE_ATTEMPT_TIME_TYPE = " text not null DEFAULT '1900-01-01 01:00:00'";
	private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";

	/** Table creation string */
	public static final String TABLE_CREATE = 
			"create table "
					+TABLE_NAME +" ("
					+KEY_ROW_ID +" integer primary key autoincrement, "
					+KEY_USER_ID +" integer not null, "
					+KEY_GROUP_ID +" integer not null, "
					+KEY_LAST_UPDATE_ATTEMPT_TIME + LAST_UPDATE_ATTEMPT_TIME_TYPE + ", "
					+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
					+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
					+"foreign key(" +KEY_USER_ID +") references " +UsersAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
					+"foreign key(" +KEY_GROUP_ID +") references " +GroupsAdapter.TABLE_NAME +"(" +GroupsAdapter.KEY_ROW_ID + ")" 
					+");";

	public UsersInGroupsAdapter(Context context) {
		super(context);
	}

	/**
	 * Add a new contact group link
	 * @param ctx the context needed to perform manipulations
	 * @param userId The user Id to add to group
	 * @param groupId The group id to add to.
	 * @return The rowId that was created/updated
	 */
	public long addUserToGroup(
			long userId,
			long groupId){

		// initialize items to insert.
		ContentValues values = new ContentValues();
		values.put(KEY_GROUP_ID, groupId);
		values.put(KEY_USER_ID, userId);
		values.put(KEY_IS_UPDATING, false);

		// The where clause
		String where = 
				KEY_USER_ID + " = ? AND " + KEY_GROUP_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(userId), String.valueOf(groupId)};

		// update the row, insert it if not possible
		long newRow = -1;
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0)
			newRow = database.insert(TABLE_NAME, null, values);
		if (affected == 1)
			newRow = getRowId(userId, groupId);
		if (affected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to update more than one row. This should never happen");

		return newRow;
	}
	
	/**
	 * Return a list of SQL statements to perform upon an upgrade from oldVersion to newVersion.
	 * @param oldVersion old version of database
	 * @param newVersion new version of database
	 * @return The arraylist of sql statements. Will not be null.
	 */
	public static ArrayList<String> upgradeStrings(int oldVersion, int newVersion){
		// initialize array
		ArrayList<String> out = new ArrayList<String>(1);
		
		// add columns
		addColumn(out, TABLE_NAME, oldVersion, newVersion, 11, KEY_LAST_UPDATE_ATTEMPT_TIME, LAST_UPDATE_ATTEMPT_TIME_TYPE);
		
		// finished
		return out;
	}

	/**
	 * Remove a user from a group locally.
	 * @param userId The user Id to remove from the group
	 * @param groupId The group id to remove from
	 */
	public void removeUserFromGroup(
			long userId,
			long groupId){

		// The where clause
		final String where = KEY_USER_ID + " =? AND " + KEY_GROUP_ID + " =?";

		// The selection args
		String[] selectionArgs = {String.valueOf(userId), String.valueOf(groupId)};

		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				where,
				selectionArgs);
		
		if (effected != 1)
			Log.e(Utils.LOG_TAG, "user " + userId + " and group " + groupId + " not deleted correctly from group. " + effected + " deletions");
	}

	/**
	 * Update all the links that contain any of these users, and change them to the main user
	 * @param userRowIdToKeep The user to overwrite all the other users
	 * @param otherUserRowIds The users that will be overwritten
	 */
	protected void combineLinksForUsers(long userRowIdToKeep, ArrayList<Long> otherUserRowIds){

		// first remove userRowIdToKeep from otherUserRowIds
		while(otherUserRowIds.remove(userRowIdToKeep));

		// if empty, then return
		if (otherUserRowIds.size() == 0)
			return;

		// the query items
		TwoObjects<String, String[]> selection = TableAdapter.createSelection(KEY_USER_ID, otherUserRowIds);

		// the new values
		ContentValues values = new ContentValues(1);
		values.put(KEY_USER_ID, userRowIdToKeep);

		// do the update
		int nRowsUpdated = -1;
		synchronized (database) {
			// update the rows
			nRowsUpdated = database.update(TABLE_NAME, values, selection.mObject1, selection.mObject2);
		}

		if (nRowsUpdated > 0)
			Log.w(Utils.LOG_TAG, "updated " + nRowsUpdated + " rows for user id " + userRowIdToKeep + " in UsersInGroupsAdapter");
	}

	/**
	 * Delete all connections to the given user rowId
	 * @param userId
	 * @return the number of connections deleted
	 */
	public int deleteUserDebug(long userId){

		// The where clause
		String where = 
				KEY_USER_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(userId)};

		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				where,
				selectionArgs);

		return effected;
	}

	/**
	 * Delete all links. This should only be used for debugging
	 * @return the number of links deleted
	 */
	public int deleteAllLinksDebug(){

		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				null,
				null);

		return effected;
	}

	/**
	 * Get the rowId of the connection given the userId and groupId
	 * @param userId
	 * @param groupId
	 * @return the rowId
	 */
	private long getRowId(long userId, long groupId){

		// default output
		long output = -1;

		// the selection string
		String selection = 
				KEY_USER_ID + " = ? AND " + KEY_GROUP_ID + " = ?";

		// the selection args
		String[] selectionArgs = {String.valueOf(userId), String.valueOf(groupId)};

		// the project
		String[] projection = {KEY_ROW_ID};

		// the query
		Cursor cursor = database.query(
				TABLE_NAME,
				projection,
				selection,
				selectionArgs,
				null,
				null,
				null);

		// check for null
		if (cursor == null)
			return output;

		// error check
		if (!Prefs.debug.allowMultipleUpdates && cursor.getCount() > 1)
			throw new IllegalArgumentException("The user / group connection appears more than once. This should never happen");

		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// grab the row id
		output =  cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROW_ID));
		cursor.close();
		return output;
	}

	/**
	 * Return if the user is a member of the group
	 * @param userId
	 * @param groupId
	 * @return true if member, false otherwise
	 */
	public boolean isUserMemberOfGroup(long userId, long groupId){
		long rowId = getRowId(userId, groupId);
		if (rowId == -1)
			return false;
		else 
			return true;
	}
	
	/**
	 * If we are synced to the server, then set this field to true.
	 * Also if we set to synced, then we know we aren't updating, so that is set to false.
	 * @param rowId the rowId of the picture to update.
	 * @param isSynced boolean if we are synced
	 */
	public void setIsSynced(long rowId, boolean isSynced){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_SYNCED, isSynced);	
		if (isSynced)
			values.put(KEY_IS_UPDATING, false);

		// make the update
		if( database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) <= 0)
			Log.e(Utils.LOG_TAG, "Could not update row in UsersInGroupsAdapter");
	}

	/**
	 * If we are updating to the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * If isUpdating is true, then we know we are not synced, so we will set sync to false as well.
	 * @param rowId the rowId of the picture to update.
	 * @param isUpdating
	 */
	public void setIsUpdating(long rowId, boolean isUpdating){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_UPDATING, isUpdating);	
		if (isUpdating)
			values.put(KEY_IS_SYNCED, false);
		if (isUpdating)
			values.put(KEY_LAST_UPDATE_ATTEMPT_TIME, Utils.getNowTime());
		final String where = KEY_ROW_ID + " =?";

		// make the update
		if( database.update(
				TABLE_NAME,
				values,
				where, new String[]{String.valueOf(rowId)}) <= 0)	
				Log.e(Utils.LOG_TAG, "Could not update row in UsersInGroupsAdapter");
	}
	
	/**
	 * Update an entire list of rowIds
	 * @param rowIdList the rows to set as updating or not
	 * @param isUpdating are we updating
	 */
	public void setIsUpdating(ArrayList<Long> rowIdList, boolean isUpdating){
		try{
			// loop over all values in database
			database.beginTransaction();
			for (int i = 0; i < rowIdList.size(); i++) {
				setIsUpdating(rowIdList.get(i), isUpdating);
			}
			database.setTransactionSuccessful();
		}catch(SQLException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}finally{
			database.endTransaction();
		}
	}
	
	/**
	 * Fetch the links that still need to be uploaded to the server for a given group
	 * @param groupId the group we will search over
	 */
	public void fetchLinksNeedUploading(long groupId){
		// links that haven't been synced but need to be have the following
		// isUpdating = 0 or last_update is old
		// isSynced = 0

		// GRAB current time
		String timeAgo = Utils.getTimeSecondsAgo((int) Utils.SECONDS_SINCE_UPDATE_RESET);

		// build the where clause
		String where = 
			"((" + KEY_IS_UPDATING + " =? OR " + KEY_IS_UPDATING + " =?) OR " +
			"(Datetime(" + KEY_LAST_UPDATE_ATTEMPT_TIME + ") < Datetime('" + timeAgo + "'))) AND " +
			"(" + KEY_IS_SYNCED + " =? OR " + KEY_IS_SYNCED + " =?) AND " +
			KEY_GROUP_ID + " =?";

		// where args
		String[] whereArgs = {
				String.valueOf(0), "FALSE",
				String.valueOf(0), "FALSE",
				String.valueOf(groupId)};
		
		Cursor cursor = database.query(TABLE_NAME, null, where, whereArgs, null, null, null);

		// set cursor
		setCursor(cursor);
	}
	
	/**
	 * Return the user row id at this given link, or -1 if none
	 * @return
	 */
	public long getUserRowId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_USER_ID);
	}
	
	/**
	 * Return the row id at this given link, or -1 if none
	 * @return
	 */
	public long getRowId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_ROW_ID);
	}
}
