package com.instantPhotoShare.Adapters;

import java.util.ArrayList;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.tools.TwoObjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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

	/** Table creation string */
	public static final String TABLE_CREATE = 
			"create table "
					+TABLE_NAME +" ("
					+KEY_ROW_ID +" integer primary key autoincrement, "
					+KEY_USER_ID +" integer not null, "
					+KEY_GROUP_ID +" integer not null, "
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

		// verify we updated the correct number of rows
		if (nRowsUpdated != otherUserRowIds.size())
			Log.e(Utils.LOG_TAG, "Did not update the correct number of rows for id " + userRowIdToKeep);
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

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");

	}
}
