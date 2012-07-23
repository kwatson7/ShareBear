package com.instantPhotoShare.Adapters;

import java.util.ArrayList;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.tools.TwoObjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class PicturesInGroupsAdapter
extends TableAdapter <PicturesInGroupsAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "PicturesInGroupsInfo";
	
	// group table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_GROUP_ID = "groupId";
	public static final String KEY_PICTURE_ID = "pictureId";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	
	/** Table creation string */
	public static final String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_GROUP_ID +" integer not null, "
		+KEY_PICTURE_ID +" integer not null, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_GROUP_ID +") references " +GroupsAdapter.TABLE_NAME +"(" +GroupsAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public PicturesInGroupsAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Add a new picture group link
	 * @param ctx the context needed to perform manipulations
	 * @param pictureId The picture Id to add to group
	 * @param groupId The group id to add to.
	 * @return The rowId that was created/updated
	 */
	public long addPictureToGroup(
			Context ctx,
			long pictureId,
			long groupId){

		// initialize items to insert.
		ContentValues values = new ContentValues();
		values.put(KEY_GROUP_ID, groupId);
		values.put(KEY_PICTURE_ID, pictureId);

		// The where clause
		String where = 
			KEY_PICTURE_ID + " = ? AND " + KEY_GROUP_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(pictureId), String.valueOf(groupId)};
		
		// update the row, insert it if not possible
		long newRow = -1;
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0)
			newRow = database.insert(TABLE_NAME, null, values);
		if (affected == 1)
			newRow = getRowId(pictureId, groupId);
		if (affected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to update more than one row. This should never happen");

		// change the most recent picture number in group
		GroupsAdapter groups = new GroupsAdapter(ctx);
		groups.incrementLastPictureNumber(groupId);
		
		return newRow;
	}
	
	/**
	 * Remove all links for the given picture from all groups
	 * @param pictureRowId
	 * @see PicturesAdapter.removePictureFromDatabase
	 */
	protected void removePictureFromAllGroups(long pictureRowId){
		
		// find all the groups this picture is in
		Cursor cursor = database.query(
				TABLE_NAME,
				new String[] {KEY_GROUP_ID},
				KEY_PICTURE_ID + " = ?",
				new String[] {String.valueOf(pictureRowId)},
				null,
				null,
				null);	
		if (cursor == null)
			return;
		
		// loop decrementing groups
		GroupsAdapter groups = new GroupsAdapter(ctx);
		while(cursor.moveToNext()){
			long groupId = cursor.getLong(0);
			groups.decrementPictureNumber(groupId);
		}
		cursor.close();
		
		// delete all rows where it appears
		int effected = database.delete(
				TABLE_NAME,
				KEY_PICTURE_ID + " =?",
				new String[] {String.valueOf(pictureRowId)});
		Log.v(Utils.LOG_TAG, effected + " pictures removed from pic-group link table");
	}
	
	/**
	 * Remove the given picture from the given group
	 * @param pictureRowId
	 * @param groupRowId
	 * @see PicturesAdapter.removePictureFromDatabase
	 */
	public void removePictureFromGroup(long pictureRowId, long groupRowId){
		
		// delete all rows where it appears
		int effected = database.delete(
				TABLE_NAME,
				KEY_PICTURE_ID + " =? AND " + KEY_GROUP_ID,
				new String[] {String.valueOf(pictureRowId)});
		Log.v(Utils.LOG_TAG, effected + " pictures removed from pic-group link table");
		
		// if we had a removal, then decrement group
		if (effected > 0){
			GroupsAdapter groups = new GroupsAdapter(ctx);
			groups.decrementPictureNumber(groupRowId);
		}
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
			KEY_PICTURE_ID + " = ? AND " + KEY_GROUP_ID + " ?";
		
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

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {
		
	}
}
