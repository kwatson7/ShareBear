package com.instantPhotoShare.Adapters;

import java.util.ArrayList;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class PicturesAdapter 
extends TableAdapter{

	/** Table name */
	public static final String TABLE_NAME = "pictureInfo";
	
	// picture table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_SERVER_ID = "serverId";
	public static final String KEY_PATH = "picturePath";
	public static final String KEY_THUMBNAIL_PATH = "pictureThumbnailPath";	
	public static final String KEY_DATE_TAKEN = "dateTaken";
	public static final String KEY_USER_ID_TOOK = "userIdWhoTookPicture";
	public static final String KEY_LATITUDE = "pictureLatitude";
	public static final String KEY_LONGITUE = "pictureLongitude";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	
	/** table creation string */
	public static final String TABLE_CREATE =
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_SERVER_ID +" integer, "
		+KEY_PATH +" text, "
		+KEY_THUMBNAIL_PATH +" text, "
		+KEY_DATE_TAKEN +" text, "
		+KEY_USER_ID_TOOK +" integer not null, "
		+KEY_LATITUDE +" DOUBLE, "
		+KEY_LONGITUE +" DOUBLE, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_USER_ID_TOOK +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + ")"
		+");";
	
	public PicturesAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Create a new picture using the info. If the picture is
	 * successfully created return the new rowId for that picture, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param serverId The id of picture on the server
	 * @param path The path to the picture on the phone
	 * @param pathThumbnail The path to the thumbnail on the phone
	 * @param dateTaken Date the picture was taken
	 * @param idWhoTook The id of the user who took the picture
	 * @param latitude latitude of pictures in degrees
	 * @param longitude longitude of picture in degrees
	 * @param isSynced mark true, if we pulled data from server, so we know it's already synced,
	 * or false, if we have just taken the picture and have not yet synced to server.
	 * @return rowId or -1 if failed
	 */
	public long createPicture(
			long serverId, 
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			long idWhoTook, 
			double latitude,
			double longitude,
			boolean isSynced){		
		
		ContentValues values = new ContentValues();
		values.put(KEY_SERVER_ID, serverId);
		values.put(KEY_PATH, path);
		values.put(KEY_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_DATE_TAKEN, dateTaken);
		values.put(KEY_USER_ID_TOOK, idWhoTook);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LONGITUE, longitude);
		values.put(KEY_IS_SYNCED, isSynced);

		return database.insert(TABLE_NAME, null, values);
	}

	/**
	 * Delete the picture with the given rowId
	 * 
	 * @param serverId id of picture to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deletePicture(long rowId) {

		return database.delete(
				TABLE_NAME,
				KEY_ROW_ID + "='" + rowId + "'",
				null) > 0;
	}

	/**
	 * Return a Cursor over the list of all pictures in the database
	 * 
	 * @return Cursor over all pictures
	 */
	public Cursor fetchAllPictures() {

		return database.query(TABLE_NAME, null, null, null, null, null, null);		
	}	

	/**
	 * Return an arraylist of all the picture file names
	 * 
	 * @return ArrayList of all picture names
	 */
	public ArrayList<String> fetchAllPictureNames() {

		// grab cursor to pictures
		Cursor cursor = fetchAllPictures();

		// initialize array
		ArrayList<String> pictures = new ArrayList<String>();

		// null cursor, just return empty arraylist
		if (cursor==null)
			return pictures;

		// loop across cursor grabbing picture names
		while (cursor.moveToNext()){
			pictures.add(cursor.getString(cursor.getColumnIndex(KEY_PATH)));
		}

		cursor.close();
		return pictures;
	}

	/**
	 * Return a Cursor positioned at the picture that matches the given rowId
	 * 
	 * @param serverId id of picture to retrieve
	 * @return Cursor positioned to matching picture, if found
	 */
	public Cursor fetchPicture(long rowId){

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

		return cursor;
	}

	/**
	 * Update the picture using the details provided. The picture to be updated is
	 * specified using the rowId, and it is altered to use the values passed in
	 * 
	 * @param rowId id of picture to update
	 * @return true if the picture was successfully updated, false otherwise
	 */
	public boolean updatePicture(
			long rowId,
			long serverId, 
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			long idWhoTook, 
			double latitude,
			double longitude,
			boolean isSynced){		
		
		ContentValues values = new ContentValues();
		values.put(KEY_SERVER_ID, serverId);
		values.put(KEY_PATH, path);
		values.put(KEY_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_DATE_TAKEN, dateTaken);
		values.put(KEY_USER_ID_TOOK, idWhoTook);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LONGITUE, longitude);
		values.put(KEY_IS_SYNCED, isSynced);	
		
		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}
	
	/**
	 * Get a cursor for all the pictures that are linked to the group groupId
	 * in PicturesInGroup table
	 * @param groupId
	 * @return A cursor on the pictures that match the groupId in PictursInGroupsAdapter
	 */
	public Cursor getPicturesInGroup(long groupId){

		// create the query where we match up all the pictures that are in the group
		String query = 
			"SELECT pics.* FROM "
			+PicturesAdapter.TABLE_NAME + " pics "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
			+"groups." + PicturesInGroupsAdapter.KEY_PICTURE_ID
			+" WHERE "
			+"groups." + PicturesInGroupsAdapter.KEY_GROUP_ID
			+"=?";

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// return the cursor
		return cursor;
	}
}
