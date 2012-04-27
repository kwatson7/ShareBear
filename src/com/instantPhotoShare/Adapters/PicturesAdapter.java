package com.instantPhotoShare.Adapters;

import java.util.ArrayList;
import java.util.Date;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.tools.TwoObjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class PicturesAdapter 
extends TableAdapter<PicturesAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "pictureInfo";

	// picture table keys
	public static final String KEY_ROW_ID = "_id";
	private static final String KEY_SERVER_ID = "serverId";
	public static final String KEY_PATH = "picturePath";
	public static final String KEY_THUMBNAIL_PATH = "pictureThumbnailPath";	
	private static final String KEY_DATE_TAKEN = "dateTaken";
	private static final String KEY_USER_ID_TOOK = "userIdWhoTookPicture";
	private static final String KEY_LATITUDE = "pictureLatitude";
	private static final String KEY_LONGITUE = "pictureLongitude";
	private static final String KEY_IS_UPDATING = "isUpdating";
	private static final String KEY_IS_SYNCED = "isSynced";
	private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";
	
	// types for some of the keys
	private static final String LAST_UPDATE_ATTEMPT_TIME_TYPE = " text not null DEFAULT '1900-01-01 01:00:00'";
	
	// some other constants
	private static String SORT_ORDER = KEY_DATE_TAKEN + " DESC"; 			// sort the picture by most recent
	
	/** table creation string */
	public static final String TABLE_CREATE =
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_SERVER_ID +" integer DEFAULT '-1', "
		+KEY_PATH +" text, "
		+KEY_THUMBNAIL_PATH +" text, "
		+KEY_DATE_TAKEN +" text, "
		+KEY_USER_ID_TOOK +" integer not null, "
		+KEY_LATITUDE +" DOUBLE, "
		+KEY_LONGITUE +" DOUBLE, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_LAST_UPDATE_ATTEMPT_TIME + LAST_UPDATE_ATTEMPT_TIME_TYPE + ", "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_USER_ID_TOOK +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + ")"
		+");";
	
	public PicturesAdapter(Context context) {
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
		if (oldVersion < 2 && newVersion >= 2){
			String upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_LAST_UPDATE_ATTEMPT_TIME + " "+
				LAST_UPDATE_ATTEMPT_TIME_TYPE;
			out.add(upgradeQuery);
		}
		
		return out;
	}
	
	/**
	 * Insert a new picture using the info. If the picture is
	 * successfully created return the new rowId for that picture, otherwise return
	 * a -1 to indicate failure.
	 * This picture will not have a serverId attached
	 * to it, and its isSynced column will be false. Use setIsSynced to set these.
	 * @param ctx Context used for various calls
	 * @param path The path to the picture on the phone
	 * @param pathThumbnail The path to the thumbnail on the phone
	 * @param dateTaken Date the picture was taken, use Utils.getNowTime(), or input null for right now.
	 * @param idWhoTook The id of the user who took the picture. Insert null for id of person on phone
	 * @param latitude latitude of pictures in degrees, or null
	 * @param longitude longitude of picture in degrees, or null
	 * @return rowId or -1 if failed
	 */
	public long createPicture(
			Context ctx,
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			Long idWhoTook, 
			Double latitude,
			Double longitude){	
		
		// override some values and/or check
		if (dateTaken == null || dateTaken.length() == 0)
			dateTaken = Utils.getNowTime();
		if (idWhoTook == null || idWhoTook == -1)
			idWhoTook = Prefs.getUserRowId(ctx);
		
		// all the values
		ContentValues values = new ContentValues();
		values.put(KEY_PATH, path);
		values.put(KEY_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_DATE_TAKEN, dateTaken);
		values.put(KEY_USER_ID_TOOK, idWhoTook);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LONGITUE, longitude);

		// insert the values
		return database.insert(TABLE_NAME, null, values);
	}
	
	/**
	 * If we are synced to the server, then set this field to true.
	 * Also if we set to synced, then we know we aren't updating, so that is set to false.
	 * @param rowId the rowId of the picture to update.
	 * @param isSynced boolean if we are synced
	 * @param newServerId, input -1, if not known and nothing will be saved
	 * @return boolean if we updated successfully to sql table.
	 */
	public boolean setIsSynced(long rowId, boolean isSynced, long newServerId){

		ContentValues values = new ContentValues();
		values.put(KEY_IS_SYNCED, isSynced);	
		if (isSynced)
			values.put(KEY_IS_UPDATING, false);
		if (newServerId != -1)
			values.put(KEY_SERVER_ID, newServerId);

		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}
	
	/**
	 * If we are updating to the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * If isUpdating is true, then we know we are not synced, so we will set sync to false as well.
	 * @param rowId the rowId of the picture to update.
	 * @param isUpdating
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
	 * Delete the picture with the given rowId
	 * 
	 * @param rowId id of picture to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deletePictureNOTFINISHED(long rowId) {

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
	private Cursor fetchAllPicturesPrivate() {

		return database.query(TABLE_NAME, null, null, null, null, null, SORT_ORDER);		
	}	

	/**
	 * Return an arraylist of all the picture file names
	 * 
	 * @return ArrayList of all picture names
	 */
	public ArrayList<String> fetchAllPictureFilePaths() {

		// grab cursor to pictures
		Cursor cursor = fetchAllPicturesPrivate();

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
	 * @param rowId id of picture to retrieve
	 * @return Cursor positioned to matching picture, if found
	 */
	private Cursor fetchPicturePrivate(long rowId){

		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_ROW_ID + "='" + rowId +"'",
					null,
					null,
					null,
					SORT_ORDER,
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
	public boolean updatePictureNOTFINISHED(
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
	private Cursor fetchPicturesInGroupPrivate(long groupId){

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
			+"=?"
			+" ORDER BY " + SORT_ORDER;

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// return the cursor
		return cursor;
	}
	
	/**
	 * Get a cursor for all the pictures that are linked to the group groupId
	 * in PicturesInGroup table
	 * @param groupId
	 * @return A cursor on the pictures that match the groupId in PictursInGroupsAdapter
	 */
	public boolean isPictureInGroup(long pictureId, long groupId){

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
			+"=?"
			+" AND "
			+"pics." + PicturesAdapter.KEY_ROW_ID
			+"=?"
			+" ORDER BY " + SORT_ORDER;

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId), String.valueOf(pictureId)});

		// return the false if there is no good cursor, and true otherwise.
		if (cursor == null || !cursor.moveToFirst()){
			if (cursor != null)
				cursor.close();
			return false;
		}else
			return true;
	}
	
	// public methods for accessing outside this class
	
	// column numbers for various fields
	private int rowIdCol;
	private int serverIdCol;
	private int pathCol;
	private int thumbnailPathCol;
	private int dateTakenCol;
	private int userIdWhoTookCol;
	private int latCol;
	private int lonCol;
	private int updatingCol;
	private int syncedCol;
	private int lastUpdateAttemptTimeCol;
	
	/**
	 * Load into the cursor 
	 * @param groupId The rowId of the groupd we search on
	 */
	public void fetchPicturesInGroup(long groupId){
		setCursor(fetchPicturesInGroupPrivate(groupId));
	}	
	
	/**
	 * Fetch the picture with the given rowId. <br>
	 * *** Make sure to close cursor when finished with closeCursor ***
	 * @param rowId The rowId of the picture
	 */
	public void fetchPicture(long rowId){
		setCursor(fetchPicturePrivate(rowId));
		moveToFirst();
	}
	
	/**
	 * Fetch random pictures. <br>
	 * If nPictures <= 1, then we start of positioned at the first (and only)
	 * item. If nPictures > 1, then we are positioned before the first item, and moveToFirst()
	 * or moveToNext() must be called.
	 * @param nPictures Number of pictures to fetch.
	 */
	public void fetchRandomPicture(Context ctx, int nPictures){
		// no pictures to fetch, so no need to do query
		if (nPictures < 1){
			setCursor(null);
			return;
		}
		
		// find all the active groups
		GroupsAdapter groups = new GroupsAdapter(ctx);
		groups.fetchAllGroups();
		ArrayList<Long> rowIds = new ArrayList<Long>();
		while (groups.moveToNext()){
			rowIds.add(groups.getRowId());
		}
		groups.close();
		
		// create the querey to only return pictures in active groups
		TwoObjects<String, String[]> selection = createSelection("groups." + PicturesInGroupsAdapter.KEY_GROUP_ID, rowIds);
		
		String query = 
			"SELECT pics.* FROM "
			+PicturesAdapter.TABLE_NAME + " pics "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
			+"groups." + PicturesInGroupsAdapter.KEY_PICTURE_ID;
		if (selection.mObject1.length() > 0){
			query+=
				" WHERE "
				+selection.mObject1;
		}
		query+=" ORDER BY RANDOM() LIMIT '" + nPictures + "'";
		
		// do the query
		Cursor cursor = database.rawQuery(
				query,
				selection.mObject2);

		// return the cursor
		setCursor(cursor);
		
		// move to correct location
		if (nPictures == 1)
			moveToFirst();

		/*
		// create the query where we match up all the pictures that are in the group
		String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY RANDOM() LIMIT '" + nPictures + "'";

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				null);

		// return the cursor
		setCursor(cursor);
		
		// move to correct location
		if (nPictures == 1)
			moveToFirst();
			*/
	}
	
	/**
	 * Return the path to the picture thumbnail, "" if there is none or cursor cannot be read. <br>
	 * @see getThumbnail
	 * @see setThumbnail
	 * @return
	 */
	public String getThumbnailPath(){
		if (!checkCursor())
			return "";
		else{
			return getString(thumbnailPathCol);
		}
	}
	
	/**
	 * Return the path to the picture, "" if there is none or cursor cannot be read. <br>
	 * @see getThumbnail
	 * @see setThumbnail
	 * @see getThumbnailPath
	 * @return
	 */
	public String getFullPicturePath(){
		if (!checkCursor())
			return "";
		else{
			return getString(pathCol);
		}
	}
	
	/**
	 * Get the bitmap for the thumbnnail. null if there is none.
	 * @return
	 */
	public Bitmap getThumbnail(){
		
		// read the path
		Bitmap out = null;
		String path = getThumbnailPath();
		if (path.length() == 0)
			return out;
		
		// decode the file
		return BitmapFactory.decodeFile(path);
	}
	
	/**
	 * Set the input imageView to the current thumbnail.
	 * @param view The imageView in question
	 * @return True if successful and their is a thumbnail, false otherwise (if no thumbnail exists)
	 */
	public boolean setThumbnail(ImageView view){
		Bitmap bmp = getThumbnail();
		if (bmp == null)
			return false;
		else{
			view.setImageBitmap(bmp);
			return true;
		}
	}
	
	/**
	 * Return the date taken as a string, or "" if not accessible.
	 * @return
	 */
	public String getDateTaken(){
		if (!checkCursor())
			return "";
		else{
			return getString(dateTakenCol);
		}
	}
	
	/**
	 * Return the rowId of the current row, or return -1 if not accessible.
	 * @return
	 */
	public long getRowId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(rowIdCol);
	}
	
	/**
	 * Determine if we are currently updating by looking at database. If last update time was too long ago, then we are no longer updating.
	 * @return
	 */
	public boolean isUpdating() {
		// if the last time we updated is too long ago, then we are not updating any more
		if (Date.parse(Utils.getNowTime()) - Date.parse(getLastUpdateAttemptTime()) > 1000*Utils.SECONDS_SINCE_UPDATE_RESET) 
			return false;
		else
			return getBoolean(updatingCol);
	}
	
	/**
	 * Get formatted last update time.
	 * @return
	 */
	public String getLastUpdateAttemptTime(){
		return getString(lastUpdateAttemptTimeCol);
	}
		
	// protected helper methods
	/**
	 * Determine the column numbers from the cursor.
	 * @throws IllegalArgumentException if columns don't exist
	 */
	@Override
	protected void setColumnNumbers(){

		// can only do this with a non null cursor
		if (!isCursorValid())
			return;

		// now find the column numbers for our strings
		try{
			rowIdCol = getColumnIndexOrThrow(KEY_ROW_ID);
			serverIdCol = getColumnIndexOrThrow(KEY_SERVER_ID);
			pathCol = getColumnIndexOrThrow(KEY_PATH);
			thumbnailPathCol = getColumnIndexOrThrow(KEY_THUMBNAIL_PATH);
			dateTakenCol = getColumnIndexOrThrow(KEY_DATE_TAKEN);
			userIdWhoTookCol = getColumnIndexOrThrow(KEY_USER_ID_TOOK);
			latCol = getColumnIndexOrThrow(KEY_LATITUDE);
			lonCol = getColumnIndexOrThrow(KEY_LONGITUE);
			updatingCol = getColumnIndexOrThrow(KEY_IS_UPDATING);
			syncedCol = getColumnIndexOrThrow(KEY_IS_SYNCED);
			lastUpdateAttemptTimeCol = getColumnIndexOrThrow(KEY_LAST_UPDATE_ATTEMPT_TIME);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException("Inputted cursor into PicturesAdapter does not have all the required columns");
		}
	}
}
