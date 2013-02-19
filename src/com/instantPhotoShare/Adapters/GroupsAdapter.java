package com.instantPhotoShare.Adapters;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.GetGroupsServerReturn;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ServerKeys;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.instantPhotoShare.Tasks.CreateGroupTask;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ExpiringValue;
import com.tools.ServerPost.PostCallback;
import com.tools.ServerPost.ServerReturn;
import com.tools.ThreeObjects;
import com.tools.Tools;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class GroupsAdapter
extends TableAdapter <GroupsAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "groupInfo";

	// group table keys
	public static final String KEY_ROW_ID = "_id";
	private static final String KEY_NAME = "groupName";
	private static final String KEY_SERVER_ID = "serverId";
	private static final String KEY_PICTURE_ID = "groupPictureId";	
	private static final String KEY_DATE_CREATED = "dateCreated";
	private static final String KEY_USER_ID_CREATED = "userIdWhoCreatedGroup";
	private static final String KEY_ALLOW_OTHERS_ADD_MEMBERS = "allowOthersAddMembers";
	private static final String KEY_LATITUDE = "latitudeGroup";
	private static final String KEY_LONGITUDE = "longitudeGroup";
	private static final String KEY_ALLOW_PUBLIC_WITHIN_DISTANCE = "allowPublicWithinDistance";
	private static final String KEY_LAST_PICTURE_NUMBER = "lastPictureNumber";
	private static final String KEY_KEEP_LOCAL = "keepLocal";
	private static final String KEY_IS_UPDATING = "isUpdating";
	private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";
	private static final String KEY_IS_SYNCED = "isSynced";
	private static final String KEY_GROUP_TOP_LEVEL_PATH = "groupTopLevelPath";
	private static final String KEY_PICTURE_PATH = "picturePath";
	private static final String KEY_THUMBNAIL_PATH = "thumbnailPath";
	private static final String KEY_MARK_FOR_DELETION  = "KEY_MARK_FOR_DELETION";
	private static final String KEY_AM_I_MEMBER = "KEY_AM_I_MEMBER";
	private static final String KEY_NUMBER_OF_PICS = "KEY_NUMBER_OF_PICS";
	private static final String KEY_TIME_LAST_PICTURE_ADDED = "KEY_TIME_LAST_PICTURE_ADDED";
	private static final String KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE = "groupTopLevelPathAlternate";
	private static final String KEY_PICTURE_PATH_ALTERNATE = "picturePathAlternate";
	private static final String KEY_THUMBNAIL_PATH_ALTERNATE = "thumbnailPathAlternate";

	// types
	private static final String MARK_FOR_DELETION_TYPE = " BOOLEAN DEFAULT 'FALSE'";
	private static final String AM_I_MEMBER_TYPE = " BOOLEAN DEFAULT 'TRUE'";
	private static final String NUMBER_OF_PICS_TYPE = " INTEGER NOT NULL DEFAULT '0'";
	private static final String TIME_LAST_PICTURE_ADDED_TYPE = " INTEGER NOT NULL DEFAULT '0'";
	private static final String ALTERNATE_PATH_TYPES = " TEXT NOT NULL DEFAULT ''";

	// private constants
	private static final String DEFAULT_PRIVATE_NAME = "Private"; 			// default group name for the auto-generated private group
	private static final String PRIVATE_GROUP_COLOR = "#ADD8E6"; 			// color for private groups when toString is called
	private static final String NON_SYNCED_PUBLIC_GROUPS_COLOR = "red"; 	// color for non-synced groups when toString is called
	private static final String SOMEONE_ELSE_MADE_GROUPS_COLOR = "#E066FF"; // color for groups other people made
	private static final float MAKE_NEW_FOLDER_TIMOUT = 5; 					// seconds until making a new folder has considered timed out and try again

	private static final String SORT_ORDER = KEY_DATE_CREATED + " DESC"; 	// sort the picture by most recent
	private static final String ADDITIONAL_QUERY_NO_AND = 
		"(" + KEY_AM_I_MEMBER + " ='TRUE' OR " + KEY_AM_I_MEMBER + " = '1') AND (" 
		+ KEY_MARK_FOR_DELETION + " = 'FALSE' OR " + KEY_MARK_FOR_DELETION + " = '0')";
	private static final String ADDITIONAL_QUERY = " AND " + ADDITIONAL_QUERY_NO_AND;

	// error codes
	public static final String GROUP_ACCESS_ERROR = "GROUP_ACCESS_ERROR";

	// static variables
	private static com.tools.ExpiringValue<Boolean> isFetchingGroups = new com.tools.ExpiringValue<Boolean>(50f, false, false);
	private static Map<Long, ExpiringValue<Boolean>> isCreatingNewFolderLock =
		Collections.synchronizedMap(new HashMap<Long, ExpiringValue<Boolean>>(10));

	/** Table creation string */
	public static final String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_NAME +" text not null, "
		+KEY_SERVER_ID +" integer DEFAULT '-1', "
		+KEY_PICTURE_ID +" integer DEFAULT '-1', "
		+KEY_DATE_CREATED +" text not null, "
		+KEY_USER_ID_CREATED +" integer not null, "
		+KEY_ALLOW_OTHERS_ADD_MEMBERS +" boolean DEFAULT 'TRUE', "
		+KEY_LATITUDE +" double, "
		+KEY_LONGITUDE +" double, "
		+KEY_ALLOW_PUBLIC_WITHIN_DISTANCE +" double, "
		+KEY_KEEP_LOCAL +" BOOLEAN DEFAULT 'FALSE', "
		+KEY_LAST_PICTURE_NUMBER +" int NOT NULL DEFAULT '-1', "
		+KEY_GROUP_TOP_LEVEL_PATH +" TEXT NOT NULL, "
		+KEY_PICTURE_PATH +" TEXT NOT NULL, "
		+KEY_THUMBNAIL_PATH +" TEXT NOT NULL, "
		+KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE + ALTERNATE_PATH_TYPES + ", "
		+KEY_PICTURE_PATH_ALTERNATE + ALTERNATE_PATH_TYPES + ", "
		+KEY_THUMBNAIL_PATH_ALTERNATE + ALTERNATE_PATH_TYPES + ", "
		+KEY_MARK_FOR_DELETION + MARK_FOR_DELETION_TYPE + ", "
		+KEY_AM_I_MEMBER + AM_I_MEMBER_TYPE +", "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_NUMBER_OF_PICS + NUMBER_OF_PICS_TYPE + ", "
		+KEY_TIME_LAST_PICTURE_ADDED + TIME_LAST_PICTURE_ADDED_TYPE + ", "
		+KEY_LAST_UPDATE_ATTEMPT_TIME +" text not null DEFAULT '1900-01-01 01:00:00', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_USER_ID_CREATED +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + ")" 
		+");";	

	public GroupsAdapter(Context context) {
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
		if (oldVersion  < 3 && newVersion >= 3){
			String upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_MARK_FOR_DELETION + " "+
				MARK_FOR_DELETION_TYPE;
			out.add(upgradeQuery);

			upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_AM_I_MEMBER + " "+
				AM_I_MEMBER_TYPE;
			out.add(upgradeQuery);
		}

		if (oldVersion  < 10 && newVersion >= 10){
			String upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_NUMBER_OF_PICS + " "+
				NUMBER_OF_PICS_TYPE;
			out.add(upgradeQuery);
		}		

		addColumn(out, TABLE_NAME, oldVersion, newVersion, 12, KEY_TIME_LAST_PICTURE_ADDED, TIME_LAST_PICTURE_ADDED_TYPE);
		addColumn(out, TABLE_NAME, oldVersion, newVersion, 13, KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, ALTERNATE_PATH_TYPES);
		addColumn(out, TABLE_NAME, oldVersion, newVersion, 13, KEY_PICTURE_PATH_ALTERNATE, ALTERNATE_PATH_TYPES);
		addColumn(out, TABLE_NAME, oldVersion, newVersion, 13, KEY_THUMBNAIL_PATH_ALTERNATE, ALTERNATE_PATH_TYPES);

		return out;
	}

	/**
	 * Call this to asynchronously, with progressDialog, upgrade the database to version 10
	 * @param act An activity required to hold the progressDialog
	 */
	public void upgradeToVersion10(CustomActivity act){
		(new UpgradeToVersion10(act)).execute();
	}

	/**
	 * Call this to asynchronously, with progressDialog, upgrade the database to version 10
	 */
	private class UpgradeToVersion10 extends CustomAsyncTask<CustomActivity, Integer, Void>{

		public UpgradeToVersion10(CustomActivity act) {
			super(
					act,
					-1,
					true,
					false,
					null);

		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected Void doInBackground(Void... params) {
			// count how many pictures are in each group
			HashMap<Long, ThreeObjects<Long, Integer, String>> nPictures = getNPicturesInGroupsByServerId();

			try{
				database.beginTransaction();
				final String where = KEY_ROW_ID + " =?";
				int i = 0;
				int total = nPictures.size();

				// loop over all values in database
				for (Entry<Long, ThreeObjects<Long, Integer, String>> entry : nPictures.entrySet()) {
					// grab the row id and pictures stored locally, and serverid
					ThreeObjects<Long, Integer, String> val = entry.getValue();
					long rowId = val.mObject1;
					int localNPics = val.mObject2;;

					// update database
					ContentValues values = new ContentValues(1);
					values.put(KEY_NUMBER_OF_PICS, localNPics);
					database.update(TABLE_NAME, values, where, new String[] {String.valueOf(rowId)});

					publishProgress(((i++)*100)/total);
				}
				database.setTransactionSuccessful();
			}catch(SQLException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}finally{
				database.endTransaction();
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			dialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExectueOverride(Void result) {

		}

		@Override
		protected void setupDialog() {
			// show dialog for this long process
			if (callingActivity != null){
				dialog = new ProgressDialog(callingActivity);
				dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				dialog.setTitle("Upgrading Database");
				dialog.setMessage("Please wait...");
				dialog.setIndeterminate(false);
				dialog.setCancelable(false);
			}
		}
	}

	/**
	 * Update all the links that contain any of these users, and change them to the main user
	 * @param userRowIdToKeep The user to overwirte all the other users
	 * @param otherUserRowIds The users that will be overwritten
	 */
	protected void combineLinksForUsers(long userRowIdToKeep, ArrayList<Long> otherUserRowIds){

		// first remove userRowIdToKeep from otherUserRowIds
		while(otherUserRowIds.remove(userRowIdToKeep));

		// if empty, then return
		if (otherUserRowIds.size() == 0)
			return;

		// the query items
		TwoObjects<String, String[]> selection = TableAdapter.createSelection(KEY_USER_ID_CREATED, otherUserRowIds);

		// the new values
		ContentValues values = new ContentValues(1);
		values.put(KEY_USER_ID_CREATED, userRowIdToKeep);

		int nRowsUpdated = -1;
		synchronized (database) {
			// update the rows
			nRowsUpdated = database.update(TABLE_NAME, values, selection.mObject1, selection.mObject2);
		}

		if (nRowsUpdated > 0)
			Log.w(Utils.LOG_TAG, "updated " + nRowsUpdated + " rows for user id " + userRowIdToKeep + " in GroupsAdapter for user who created");
	}

	/**
	 * Generate a picture path and thumbnail path for a given top level folder and group name
	 * @param groupFolderFullPath The top level path ie. /sdcard/folder1/
	 * @param groupName The acceptable groupName to write to path ie "folder1", not "folder%*1"
	 * @return The picture path and thumbnail path in TwoSrings object
	 */
	private static TwoStrings generatePicturePath(String groupFolderFullPath, String groupName){
		return new TwoStrings(groupFolderFullPath + Utils.getAllowableFileName(groupName) + Utils.pathsep,
				groupFolderFullPath + Utils.THUMBNAIL_PATH);
	}

	/**
	 * Determine the allowable folder name for a given group name. Will start with simple groupName. <br>
	 * If that doesn't work, then append _date. If that doesn't work then append _date_(1, or 2, or 3...). <p>
	 * Check acceptable folder names that work. ie no *&/ etc.
	 * @param groupName
	 * @param dateCreated
	 * @param useAlternate should we use alternate location. If returns null, then will not use alternate
	 * @return
	 * @throws IOException  if there is no where to store the data
	 */
	private static String getAllowableFolderName(
			Context ctx,
			String groupName,
			String dateCreated,
			boolean useAlternate)
	throws IOException{

		// read in some properites to make it faster
		final char pathsep = Utils.pathsep;

		// remove illegal characters
		groupName = Utils.getAllowableFileName(groupName);
		dateCreated = Utils.getAllowableFileName(dateCreated);

		// the top storage place
		String top = Utils.getExternalStorageTopPath();
		if (useAlternate){
			String top2 = Tools.getMirroredExternalPath(top);
			if (top2 == null)
				throw new IOException("No alternate path to create Folder at");
			else
				top = top2;
		}

		// the folder name
		String path = top + groupName + pathsep;
		File theDir = new File(path);

		// check for existence and add date if need be
		if (theDir.exists()){
			path = top + groupName + "_" + dateCreated + pathsep;
			theDir = new File(path);

			// check existence of new file, if that exists, then start iterating
			if (theDir.exists()){
				long newNumber = 2;
				path = top + groupName + "_" + dateCreated + "_";
				while ((new File(path + newNumber + pathsep)).exists())
					newNumber++;
				path = path + newNumber + pathsep;
			}
		}

		// the paht
		return path;
	}	

	/**
	 * Grab the lock object for creating new folders for the given group
	 * @param groupId the group row id
	 * @return the lock for a given group Id - will never be null
	 */
	private ExpiringValue<Boolean> getCreatingNewFolderLock(long groupId){
		// create lock first if not existent
		if (isCreatingNewFolderLock == null){
			isCreatingNewFolderLock = new HashMap<Long, ExpiringValue<Boolean>>(10);
		}

		// grab value
		ExpiringValue<Boolean> isCreating = isCreatingNewFolderLock.get(groupId);

		// if null, then initialize
		if (isCreating == null){
			isCreating = new ExpiringValue<Boolean>(MAKE_NEW_FOLDER_TIMOUT, false, false);
		}

		return isCreating;
	}

	/**
	 * Check across a static array if we are currently attempting to create new folders for this group id
	 * @param groupId the group id to check.
	 * @return If we are creating or not
	 */
	private boolean isCreatingNewFolder(long groupId){

		ExpiringValue<Boolean> isCreating = getCreatingNewFolderLock(groupId);

		// return teh value
		return isCreating.getValue();
	}

	/**
	 * Set that we are creating new folders for this group
	 * @param groupId the group row id
	 * @param isCreatingOrNot if we are creating the new folders, or we finished creating the folders
	 */
	private void setCreatingNewFolder(long groupId, boolean isCreatingOrNot){
		ExpiringValue<Boolean> isCreating = getCreatingNewFolderLock(groupId);
		isCreating.setValue(isCreatingOrNot);
		isCreatingNewFolderLock.put(groupId, isCreating);
	}

	/**
	 * Is the given group based on the serverId present in the database
	 * @param serverId teh serverId to search
	 * @return is the group present
	 */
	public boolean isGroupPresent(long serverId){
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					new String[] {KEY_ROW_ID},
					KEY_SERVER_ID + "=?",
					new String[] {String.valueOf(serverId)},
					null,
					null,
					SORT_ORDER,
					null);
		if (cursor == null)
			return false;
		boolean value = cursor.moveToFirst();
		cursor.close();
		return value;
	}
	
	/**
	 * Clear all alternate paths in database. Used for debugging purposes
	 * @return the number of rows updated
	 */
	public int clearAlternatePathsForDebugging(){
		
		ContentValues values = new ContentValues(3);
		values.put(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, "");
		values.put(KEY_PICTURE_PATH_ALTERNATE, "");
		values.put(KEY_THUMBNAIL_PATH_ALTERNATE, "");
		
		return database.update(TABLE_NAME, values, null, null);
	}

	public boolean canIRemoveMembers(Context ctx){
		return false;
		//return didIMakeGroup(ctx);
	}

	public boolean canIRemoveThisMember(Context ctx, long userId){
		// see if the user is even in the group first
		UsersAdapter users = new UsersAdapter(ctx);
		UsersInGroupsAdapter links = new UsersInGroupsAdapter(ctx);
		boolean isMember = links.isUserMemberOfGroup(userId, getRowId());

		// anyone can delete if not a member
		if (!isMember)
			return true;

		// only the creator can delete others
		return canIRemoveMembers(ctx);
	}

	/**
	 * Marks the group for deletion. Doesn't acutally delete, but just puts a delete flag <br>
	 * Remove the folders, if they are empty
	 * @param rowId the rowId of the group to update
	 * @return true if successful, false if not.
	 */
	public boolean deleteGroup(long rowId){

		ContentValues values = new ContentValues();
		values.put(KEY_MARK_FOR_DELETION, true);	

		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(rowId)};

		// find the path name if we need it later to delete folders
		Group group = getGroup(rowId);
		Group3Folders topPath = group.getDesiredFoldersFromDatabase();
		Group3Folders alternate = group.getBackupFoldersFromDatabase();

		// add marke for deletion to row
		boolean updateVal =  database.update(TABLE_NAME, values, where, selectionArgs) > 0;

		// delete folders if they are empty
		if (topPath.topFolder != null && topPath.topFolder.length() != 0 &&
				com.tools.Tools.isFolderEmpty(topPath.topFolder, ".nomedia"))
			com.tools.Tools.deleteEmptyFolder(new File(topPath.topFolder), ".nomedia");
		if (alternate.topFolder != null && alternate.topFolder.length() != 0 &&
				com.tools.Tools.isFolderEmpty(alternate.topFolder, ".nomedia"))
			com.tools.Tools.deleteEmptyFolder(new File(alternate.topFolder), ".nomedia");

		return updateVal;					
	}

	/**
	 * Delete the group at the given rowId
	 * @param rowId
	 * @return number of rows deleted.
	 */
	public int deleteGroupDONTUSE(long rowId){

		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";

		// The selection args
		String[] selectionArgs = {String.valueOf(rowId)};

		// find the path name if we need it later to delete folders
		Group group = getGroup(rowId);
		Group3Folders topPath = group.getDesiredFoldersFromDatabase();
		Group3Folders alternate = group.getBackupFoldersFromDatabase();

		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				where,
				selectionArgs);

		// throw error if we somehow linked to more than one group
		if (effected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to delete more than one row in groups. This should never happen");

		// delete folders if they are empty
		if (topPath.topFolder != null && topPath.topFolder.length() != 0 &&
				com.tools.Tools.isFolderEmpty(topPath.topFolder, ".nomedia"))
			com.tools.Tools.deleteEmptyFolder(new File(topPath.topFolder), ".nomedia");
		if (alternate.topFolder != null && alternate.topFolder.length() != 0 &&
				com.tools.Tools.isFolderEmpty(alternate.topFolder, ".nomedia"))
			com.tools.Tools.deleteEmptyFolder(new File(alternate.topFolder), ".nomedia");

		return effected;
	}

	public boolean didIMakeGroup(Context ctx){
		if (!checkCursor())
			return false;
		else
			return (getLong(KEY_USER_ID_CREATED) == Prefs.getUserRowId(ctx));
	}

	/**
	 * Fetch all groups and load them into this cursor
	 * @return
	 */
	public void fetchAllGroupsOld(){

		// grab the cursor over all groups
		Cursor cursor = fetAllGroupsCursorSimple();

		setCursor(cursor);
	}

	/**
	 * Fetch all groups and load them into this cursor
	 * @return
	 */
	public void fetchAllGroups(){
		setCursor(fetchAllGroupsCursor());
	}

	/**
	 * Get the total number of groups.
	 * @return
	 */
	public int getNGroups(){
		String query = "select count(*) from " + TABLE_NAME + " where " + ADDITIONAL_QUERY_NO_AND;
		Cursor countCursor = database.rawQuery(query, null);
		if (countCursor == null)
			return 0;
		if (!countCursor.moveToFirst()){
			countCursor.close();
			return 0;
		}
		int count= countCursor.getInt(0);
		countCursor.close();
		return count;
	}

	/**
	 * Grab the cursor that puts the newest group first, then the3 groups with most recent picture additions, and then
	 * alphabetical
	 * @return the cursor, can be null
	 */
	private Cursor fetchAllGroupsCursor(){
		// first find the 3rd newest group
		Cursor cursor = database.query(
				TABLE_NAME,
				new String[] {KEY_TIME_LAST_PICTURE_ADDED},
				null,
				null,
				null,
				null,
				KEY_TIME_LAST_PICTURE_ADDED + " DESC",
				String.valueOf(3));

		if (cursor == null){
			return null;
		}
		if (cursor.getCount() < 2){
			cursor.close();
			return fetAllGroupsCursorSimple();
		}
		cursor.moveToLast();
		long thresholdTime = cursor.getLong(0);
		while(thresholdTime == 0 && cursor.moveToPrevious()){
			thresholdTime = cursor.getLong(0);
		}
		cursor.close();

		// now find the newest group you created 
		// first find the 3rd newest group
		cursor = database.query(
				TABLE_NAME,
				new String[] {KEY_ROW_ID, KEY_DATE_CREATED},
				null,
				null,
				null,
				null,
				KEY_DATE_CREATED + " DESC",
				String.valueOf(1));

		if (cursor == null){
			return null;
		}
		if (cursor.getCount() < 1){
			return fetAllGroupsCursorSimple();
		}
		cursor.moveToFirst();
		long rowId = cursor.getLong(0);
		cursor.close();

		// the query
		String query = 
			"SELECT *," + 
			"CASE WHEN " + KEY_ROW_ID + " = ? THEN 0"
			+ " ELSE 1 END As ORDER_COL1 "+
			", CASE WHEN " + KEY_TIME_LAST_PICTURE_ADDED + " >= ? THEN KEY_TIME_LAST_PICTURE_ADDED"
			+ " ELSE 0 END As ORDER_COL2 "+
			"FROM " + TABLE_NAME + 
			" WHERE " + ADDITIONAL_QUERY_NO_AND + 
			" ORDER BY " + " ORDER_COL1 ASC, ORDER_COL2 DESC, " + KEY_NAME + " COLLATE NOCASE";

		// do the query
		return database.rawQuery(query, new String[] {String.valueOf(rowId), String.valueOf(thresholdTime)});
	}

	/**
	 * Fetch the group with the given rowId into this main cursor
	 * @param rowId
	 */
	public void fetchGroup(long rowId){

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_ROW_ID + "='" + rowId +"'" + ADDITIONAL_QUERY,
					null,
					null,
					null,
					SORT_ORDER,
					null);

		setCursor(cursor);
		moveToFirst();
	}

	/**
	 * Fetch the group with the given rowId into this main cursor
	 * @param serverId the server id of this group
	 */
	public void fetchGroupByServerId(long serverId){

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_SERVER_ID + " =? " + ADDITIONAL_QUERY,
					new String[]{String.valueOf(serverId)},
					null,
					null,
					SORT_ORDER,
					null);

		setCursor(cursor);
		moveToFirst();
	}

	/**
	 * Load the cursor for all groups that have the pictureId as a member
	 * @param pictureId
	 */
	public void fetchGroupsContainPicture(long pictureId){

		// create the query where we match up all groups that have this picture
		String query = 
			"SELECT groups.* FROM "
			+GroupsAdapter.TABLE_NAME + " groups "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " joinner "
			+" ON "
			+"groups." + GroupsAdapter.KEY_ROW_ID + " = "
			+"joinner." + PicturesInGroupsAdapter.KEY_GROUP_ID
			+" WHERE "
			+"joinner." + PicturesInGroupsAdapter.KEY_PICTURE_ID
			+"=?" + " AND " + "groups." + KEY_AM_I_MEMBER + " ='TRUE' AND " + "groups." + KEY_MARK_FOR_DELETION + " = 'FALSE'"
			+" ORDER BY " + SORT_ORDER;

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(pictureId)});

		// save the cursor
		setCursor(cursor);
	}

	/**
	 * Fetch the groups that still need to be synced to the server
	 */
	public void fetchGroupsToBeSynced(){
		// groups that haven't been synced but need to be have the following
		// serverId == 0, -1, or null
		// keepLocal = 0
		// isUpdating = 0
		// last_update is old
		// isSynced = 0
		// markfor deletion = 0

		// GRAB current time
		String timeAgo = Utils.getTimeSecondsAgo((int) Utils.SECONDS_SINCE_UPDATE_RESET);

		// build the where clause
		String where = "(" + KEY_SERVER_ID + " =? OR " + KEY_SERVER_ID + " =? OR " + KEY_SERVER_ID + " IS NULL ) AND " +
		"(" + KEY_KEEP_LOCAL + " =? OR UPPER(" + KEY_KEEP_LOCAL + ") =?) AND " +
		"((" + KEY_IS_UPDATING + " =? OR UPPER(" + KEY_IS_UPDATING + ") =?) OR " + 
		"(Datetime(" + KEY_LAST_UPDATE_ATTEMPT_TIME + ") < Datetime('" + timeAgo + "'))) AND " +
		"(" + KEY_IS_SYNCED + " =? OR UPPER(" + KEY_IS_SYNCED + ") =?) AND " +
		"(" + KEY_MARK_FOR_DELETION + " =? OR UPPER(" + KEY_MARK_FOR_DELETION + ") =?)";

		// where args
		String[] whereArgs = {
				String.valueOf(-1), String.valueOf(0),
				String.valueOf(0), "FALSE",
				String.valueOf(0), "FALSE",
				String.valueOf(0), "FALSE",
				String.valueOf(0), "FALSE"};

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					where,
					whereArgs,
					null,
					null,
					SORT_ORDER,
					null);

		// set the cursor
		setCursor(cursor);
	}

	/**
	 * Fetch pictureIds for this given group on current thread. Will be slow as it fetches from server
	 * @param groupServerId The groupserverid, NOT THE ROW ID
	 * @return The result of the query
	 */
	public FetchPictureIdReturn fetchPictureIdsFromServer(long groupServerId){
		// bad serverId
		if (groupServerId == -1 || groupServerId == 0)
			return new FetchPictureIdReturn(new Exception("bad group access"));

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_GROUP_ID, groupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return new FetchPictureIdReturn(new Exception("bad json access"));
		}

		// post to server
		ServerReturn serverReturn = Utils.postToServer(ServerKeys.GetGroupImageIds.COMMAND, json, null, null);

		// the helper method
		return fetchPictureIdsFromServerHelper(groupServerId, serverReturn);
	}

	/**
	 * Fetch all the pictures from the server for a given groupID and then create holding pictures
	 * for any pictures not present in database. Done a a background thread
	 * @param <ACTIVITY_TYPE>
	 * @param act The activity that will be attached to callback on return.
	 * @param groupServerId the server id of the group in question
	 * @param indeterminateProgressBars indetermiante progress bars to show, null if none (the tags to find them)
	 * @param callback The callback to be called when we are done. Can be null.
	 * callback 3rd return will be how many new pictures there are
	 */
	public synchronized <ACTIVITY_TYPE extends CustomActivity>
	void fetchPictureIdsFromServerInBackground(
			ACTIVITY_TYPE act,
			final long groupServerId,
			ArrayList<String> indeterminateProgressBars,
			final ItemsFetchedCallback<ACTIVITY_TYPE> callback){
		//TODO: this shouldn't be synced. Will cause hanging, also doesn't even gaurantee thread safe, as multiple objects cann access this
		//TODO: make checking to see what new picture we have faster. Shouldn't check each picture individually, but do a batch callback and loop over pictures
		//TODO: remove pictures that are no longer in group as well

		// bad serverId
		if (groupServerId == -1 || groupServerId == 0)
			return;

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
			json.put(ServerKeys.GetGroupImageIds.POST_KEY_GROUP_ID, groupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return;
		}

		Utils.postToServer(
				ServerKeys.GetGroupImageIds.COMMAND,
				json.toString(),
				null,
				null,
				null,
				act,
				indeterminateProgressBars,
				new PostCallback<ACTIVITY_TYPE>() {

					@Override
					public void onPostFinished(
							ACTIVITY_TYPE act,
							ServerReturn result) {

						FetchPictureIdReturn processed = fetchPictureIdsFromServerHelper(groupServerId, result);

						// send callback back to activity
						//TODO: we should be passing how many deletions we had too, so we can delete the pics
						if (callback != null){
							if (processed.exception == null)
								callback.onItemsFetchedBackground(act, processed.nNewPictures, null);
							else
								callback.onItemsFetchedBackground(act, processed.nNewPictures, processed.exception.getMessage());
						}
					}

					@Override
					public void onPostFinishedUiThread(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						ShareBearServerReturn result2 = new ShareBearServerReturn(result);
						if (callback != null)
							if (result2.isSuccess())
								callback.onItemsFetchedUiThread(act, null);
							else
								callback.onItemsFetchedUiThread(act, result2.getErrorCode());
					}
				});
	}

	/**
	 * Helper class to hold return from fetchPictureIds
	 * @author kwatson
	 *
	 */
	public static class FetchPictureIdReturn{
		/**
		 * If an exception occured... can be null. if not null getMessge will always have data
		 */
		public Exception exception; 	
		/**
		 * The number of new pictures in this group
		 */
		public int nNewPictures = 0;
		/**
		 * The number of old pictures that were deleted.
		 */
		public int nDeletedPictures = 0;

		private FetchPictureIdReturn(Exception e){
			exception = e;
		}
		private FetchPictureIdReturn(int newPics, int deletedPics){
			nNewPictures = newPics;
			nDeletedPictures = deletedPics;
		}
	}

	/**
	 * Helper method to fetch pictureIds from the server. Will hang if called on UI thread
	 * @param groupServerId The group server id to query
	 * @param result The result from a serverreturn
	 * @return The result of the parsing.
	 */
	private FetchPictureIdReturn fetchPictureIdsFromServerHelper(long groupServerId, ServerReturn result){

		// convert to thumbnail data
		ShareBearServerReturn data = new ShareBearServerReturn(result);

		// not a good return
		if (!data.isSuccess()){
			Log.e(Utils.LOG_TAG, data.getDetailErrorMessage());
			return new FetchPictureIdReturn(new Exception(data.getDetailErrorMessage()));
		}

		// parse returned data and check if we have these pictures
		JSONArray array = data.getMessageArray();
		if (array == null)
			return new FetchPictureIdReturn(new Exception("Server error"));

		// loop checking the pictures and creating in database if we don't have
		PicturesAdapter pics = new PicturesAdapter(ctx);
		PicturesInGroupsAdapter comboAdapter = new PicturesInGroupsAdapter(ctx);
		GroupsAdapter adapter = new GroupsAdapter(ctx);
		Group group = adapter.getGroupByServerId(groupServerId);
		if(group == null || group.keepLocal)
			return new FetchPictureIdReturn(new Exception("no group or private"));
		int counter = 0;				
		HashSet<String> serverIds = new HashSet<String>();
		for (int i = 0; i < array.length(); i++){
			try {
				serverIds.add(String.valueOf(array.getLong(i)));
			} catch (JSONException e1) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
			}

			synchronized (GroupsAdapter.class) {
				try {

					// create a new picture in database
					if(!pics.isPicturePresent(array.getLong(i))){

						// determine the path to store files
						TwoStrings picNames = group.getNextPictureName(true);

						// create the picture in the database
						long rowId = pics.createPicture(
								ctx,
								picNames.mObject1,
								picNames.mObject2,
								"",
								-1l,
								null,
								null,
								false);

						pics.setIsSynced(rowId, true, array.getLong(i));
					}

					// find the picture by serverId
					pics.fetchPictureFromServerId(array.getLong(i));
					if (pics.getRowId() == 0 || pics.getRowId() == -1){
						pics.close();
						continue;
					}

					// add the picture to this group if not already present
					if(!pics.isPictureInGroup(pics.getRowId(), group.getRowId())){
						comboAdapter.addPictureToGroup(pics.getRowId(), group.getRowId());
						counter++;
					}
					pics.close();

				} catch (NumberFormatException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					continue;
				} catch (JSONException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					continue;
				} catch (IOException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					if (ctx != null){
						NotificationsAdapter notes = new NotificationsAdapter(ctx);
						notes.createNotification("Cannot download pictures.", NOTIFICATION_TYPES.DEVICE_ERROR, "");
					}
					continue;
				}
			}
			pics.close();
		}	

		// pictures that have been removed from this group
		TwoObjects<HashSet<String>, HashSet<String>> newAndOld = pics.getNewAndOldPicturesInGroup(group.getRowId(), serverIds);
		HashSet<String> beenRemoved = newAndOld.mObject2;
		Iterator<String> iterator = beenRemoved.iterator();
		// not yet updated are serverid <= 0
		int nNotYetUpdated = 0;
		while(iterator.hasNext()){
			long picServerId = Long.valueOf(iterator.next());
			if (picServerId <= 0){
				nNotYetUpdated ++;
				continue;
			}
			pics.fetchPictureFromServerId(picServerId);
			comboAdapter.removePictureFromGroup(pics.getRowId(), group.getRowId());
		}
		pics.close();

		// update number of pictures
		adapter.fetchGroupByServerId(groupServerId);
		adapter.setNPictures(array.length() + nNotYetUpdated);
		adapter.close();

		// notification for new pictures
		if (counter > 0){
			NotificationsAdapter notes = new NotificationsAdapter(ctx);
			String msg;
			if (counter == 1)
				msg = " new picture in ";
			else
				msg = " new pictures in ";
			notes.createNotification(
					"You have " + counter + msg + group.getName(),
					NotificationsAdapter.NOTIFICATION_TYPES.NEW_PICTURE_IN_GROUP,
					String.valueOf(group.getRowId()));
		}

		// return value
		if (data.isSuccess())
			return new FetchPictureIdReturn(counter, beenRemoved.size());
		else
			return new FetchPictureIdReturn(new Exception(data.getDetailErrorMessage()));

	}

	/**
	 * Fetch all the groups from the server for a given user and then create for any new groups. Also fetch the ids of the new pictures in groups
	 * Done a a background thread
	 * @param <ACTIVITY_TYPE>
	 * @param act The activity that will be attached to callback on return.
	 * @param groupServerId the server id of the group in question
	 * @param indeterminateProgressBars indetermiante progress bars to show, null if none (the tags to find them)
	 * @param callback The callback to be called when we are done. Can be null.
	 * callback 3rd return will be how many new groups there are
	 * @return did we make it to the post
	 */
	public <ACTIVITY_TYPE extends CustomActivity>
	boolean fetchAllGroupsFromServer(
			ACTIVITY_TYPE act,
			ArrayList<String> indeterminateProgressBars,
			final ItemsFetchedCallback<ACTIVITY_TYPE> callback){

		// we are already doing it, no need to do it twice
		if (isFetchingGroups == null)
			isFetchingGroups = new ExpiringValue<Boolean>(50f, false, false);
		if(isFetchingGroups.getValue())
			return false;
		isFetchingGroups.setValue(true);

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.GetGroups.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.GetGroups.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return false;
		}

		// post the command to the server
		Utils.postToServer(
				ServerKeys.GetGroups.COMMAND,
				json.toString(),
				null,
				null,
				null,
				act,
				indeterminateProgressBars,
				new PostCallback<ACTIVITY_TYPE>() {

					@Override
					public void onPostFinished(
							ACTIVITY_TYPE act,
							ServerReturn result) {

						try{
							// convert to custom return object
							GetGroupsServerReturn data = new GetGroupsServerReturn(result);

							// not a good return
							if (!data.isSuccess()){
								Log.e(Utils.LOG_TAG, data.getDetailErrorMessage());
								// send callback back to activity
								if (callback != null){
									callback.onItemsFetchedBackground(act, -1, data.getErrorCode());
									return;
								}else
									return;
							}

							// keep trak of how many pictures in each group
							HashMap<Long, Integer> nPicturesInGroupsFromServer = new HashMap<Long, Integer>();

							// grab all the group serverIds and also the number of pictures in each group
							GroupsAdapter adapter = new GroupsAdapter(ctx);
							int nGroups = data.getNGroups();
							HashSet<String> allServerids = new HashSet<String>(nGroups);
							for (int i = 0; i < nGroups; i++){
								long groupServerId = data.getGroupServerId(i);
								if (groupServerId != 0)
									allServerids.add(String.valueOf(groupServerId));
								nPicturesInGroupsFromServer.put(groupServerId, data.getNPictures(i));
							}

							// determine if there are new pictures in any of the groups.
							// how many pictures stored locally
							HashMap<Long, ThreeObjects<Long, Integer, String>> nPicturesInGroupsLocal = adapter.getNPicturesInGroupsByServerId();
							HashMap<Long, TwoObjects<Integer, String>> newPictures = new HashMap<Long, TwoObjects<Integer, String>>();

							// compare to server pictures
							for (Entry<Long, ThreeObjects<Long, Integer, String>> entry : nPicturesInGroupsLocal.entrySet()) {

								// grab the row id and pictures stored locally, and serverid
								ThreeObjects<Long, Integer, String> val = entry.getValue();
								long rowId = val.mObject1;
								int localNPics = val.mObject2;
								String name = val.mObject3;
								long serverId = entry.getKey();

								// compare to server
								Integer serverNPics = nPicturesInGroupsFromServer.get(serverId);
								if (serverNPics == null)
									continue;

								// store if new pictures
								if (serverNPics > localNPics){
									newPictures.put(rowId, new TwoObjects<Integer, String>(serverNPics-localNPics, name));

									// fetch new pictures
									adapter.fetchPictureIdsFromServer(serverId);
								}

								// save the new number
								if (serverNPics != localNPics){
									adapter.fetchGroup(rowId);
									adapter.setNPictures(serverNPics);
									adapter.close();
								}

							}

							// used for adding notifications
							NotificationsAdapter notes = new NotificationsAdapter(ctx);

							// determine which groups are new
							UsersAdapter users = new UsersAdapter(ctx);
							HashSet<String> newIds = adapter.getNewValues(TABLE_NAME, KEY_SERVER_ID, allServerids);
							int nNewGroups = newIds.size();

							// add new groups
							for (int i = 0; i < nGroups; i++){
								// if not new, then skip
								String groupServerId = String.valueOf(data.getGroupServerId(i));
								if (!newIds.contains(groupServerId))
									continue;

								// see if the user exists, if it doesn't then add it
								long userServerId = data.getUserServerIdWhoCreated(i);
								users.fetchUserByServerId(userServerId);
								long userRowId = users.getRowId();
								if(userRowId == 0 ||userRowId == -1){
									users.close();
									userRowId = users.makeNewUser(userServerId);
								}else
									users.close();

								// sync on this, so we don't duplicate groups
								long groupRowId = -1;
								synchronized(GroupsAdapter.class){
									// we must have updated on another thread, so don't do group again
									if (adapter.isGroupPresent(Long.valueOf(groupServerId)))
										continue;

									// create a new group
									//TODO: read allowOthersToAddMembers
									//TODO: read allow within distance
									//TODO: read lat and long
									//TODO: read picture id for group
									groupRowId = adapter.makeNewGroup(ctx,
											data.getName(i),
											null,
											data.getDateCreated(i),
											userRowId,
											true, 
											null,
											null,
											-1,
											false,
											data.getNPictures(i));

									if (groupRowId != -1){
										adapter.setIsSynced(groupRowId, true, Long.valueOf(groupServerId));
										notes.createNotification(
												"You've been added to " + data.getName(i),
												NotificationsAdapter.NOTIFICATION_TYPES.ADD_TO_NEW_GROUP, String.valueOf(groupRowId));

									}else
										nNewGroups--;
								}

								// fetch new pictures
								if (groupRowId != -1 && data.getNPictures(i) > 0)
									adapter.fetchPictureIdsFromServer(Long.valueOf(groupServerId));
							}

							// notification for new groups
							// only show if more than 1 group, as for a single group it's unecessary
							if (nNewGroups > 1){
								notes.createNotification(
										"You've been added to " + nNewGroups + " new groups.",
										NotificationsAdapter.NOTIFICATION_TYPES.ADD_TO_NEW_GROUP, null);
							}

							// send callback back to activity
							if (callback != null){
								if (data.isSuccess())
									callback.onItemsFetchedBackground(act, nNewGroups, null);
								else
									callback.onItemsFetchedBackground(act, nNewGroups, data.getErrorCode());
							}
						}finally{
							if (isFetchingGroups == null)
								isFetchingGroups = new ExpiringValue<Boolean>(50f, false, false);
							isFetchingGroups.setValue(false);
						}
					}

					@Override
					public void onPostFinishedUiThread(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						GetGroupsServerReturn result2 = new GetGroupsServerReturn(result);
						if (callback != null)
							if (result2.isSuccess())
								callback.onItemsFetchedUiThread(act, null);
							else
								callback.onItemsFetchedUiThread(act, result2.getErrorCode());
					}
				});

		return true;
	}

	/**
	 * Return a map with the number of pictures in each group as the values, and the keys are the SERVERIDS, NOT ROWIDs.
	 * Skips private groups
	 * @return The mapping, null if we couldn't read, the key is the serverId, and value is "RowId, nPictures, groupName"
	 */
	public HashMap<Long, ThreeObjects<Long, Integer, String>> getNPicturesInGroupsByServerId(){
		// grab all the groups
		GroupsAdapter allGroups = new GroupsAdapter(ctx);
		allGroups.fetchAllGroups();
		HashMap<Long, ThreeObjects<Long, Integer, String>> map = new HashMap<Long, ThreeObjects<Long, Integer, String>>(allGroups.size());

		// loop over groups counting how many pictures are in each group.
		while(allGroups.moveToNext()){

			if (allGroups.isKeepLocal())
				continue;

			/*
			// create the query where we match up all the pictures that are in the group
			String query = 
				"SELECT COUNT(*) FROM "
				+PicturesInGroupsAdapter.TABLE_NAME
				+" WHERE "
				+PicturesInGroupsAdapter.KEY_GROUP_ID
				+"=?";

			// do the query
			Cursor cursor = database.rawQuery(
					query,
					new String[]{String.valueOf(allGroups.getRowId())});
			if (cursor == null)
				continue;

			// grab the number of pictures
			int nPictures = 0;
			if (cursor.moveToFirst())
				nPictures = cursor.getInt(0);
			cursor.close();
			 */
			int nPictures = allGroups.getNPictures();

			// save in map
			long serverId = allGroups.getServerId();
			if (serverId != -1 && serverId != 0)
				map.put(serverId, new ThreeObjects<Long, Integer, String>(allGroups.getRowId(), nPictures, allGroups.getName()));		
		}

		allGroups.close();

		return map;
	}

	/**
	 * Fetch all the private groups in the database.
	 * @return An arraylist of the private groups. Will not be null, but can be empty
	 */
	public ArrayList<Group> fetchPrivateGroups(){

		// get the cursor
		Cursor cursor = 
			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_KEEP_LOCAL + " >'0'" + ADDITIONAL_QUERY,
					null,
					null,
					null,
					SORT_ORDER,
					null);

		// initalize output
		ArrayList<Group> output = new ArrayList<Group>();

		// check null
		if (cursor == null)
			return output;

		// loop over cursor
		while (cursor.moveToNext()){
			output.add(new Group(cursor));
		}

		cursor.close();

		//sort groups
		return output;
	}

	/**
	 * Get an array of all groups stored in database
	 * @return
	 */
	public ArrayList<Group> getAllGroups(){

		// grab the cursor over all groups
		Cursor cursor = fetchAllGroupsCursor();

		// initalize output
		ArrayList<Group> output = new ArrayList<Group>();

		// check null
		if (cursor == null)
			return output;

		// loop over cursor
		while (cursor.moveToNext()){
			output.add(new Group(cursor));
		}

		cursor.close();
		return output;
	}

	/**
	 * Return a group for the given rowId
	 * 
	 * @param rowId id of group to retrieve
	 * @return Group object, null if none found at rowId
	 */
	public Group getGroup(long rowId){

		// default
		Group output = null;

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_ROW_ID + "='" + rowId +"'" + ADDITIONAL_QUERY,
					null,
					null,
					null,
					SORT_ORDER,
					null);

		// check null
		if (cursor == null)
			return output;
		if(!cursor.moveToFirst()){
			cursor.close();
			return output;
		}

		// check if we are accessing more than one row, this shouuldn't happen
		if (cursor.getCount() > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to access more than one row. This should never happen");

		// make the group from cursor
		output = new Group(cursor);

		// close and return
		cursor.close();
		return output;
	}

	/**
	 * Return a group for the given serverId
	 * 
	 * @param serverId id of group to retrieve
	 * @return Group object, null if none found at serverId
	 */
	public Group getGroupByServerId(long serverId){

		// default
		Group output = null;

		// grab the cursor
		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_SERVER_ID + "=?" + ADDITIONAL_QUERY,
					new String[] {String.valueOf(serverId)},
					null,
					null,
					SORT_ORDER,
					null);

		// check null
		if (cursor == null)
			return output;
		if (!cursor.moveToFirst())
			return null;

		// check if we are accessing more than one row, this shouuldn't happen
		if (cursor.getCount() > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to access more than one row. This should never happen");

		// make the group from cursor
		output = new Group(cursor);

		// close and return
		cursor.close();
		return output;
	}

	/**
	 * Return the number of pictures that should be in the group
	 * @return
	 */
	public int getNPictures(){
		if (!checkCursor())
			return 0;
		else
			return getInt(KEY_NUMBER_OF_PICS);
	}

	/**
	 * Set the number of pictures the server says this group should have
	 * @param nPictures
	 */
	public void setNPictures(int nPictures){
		// grab which rowId we are working with
		if (!checkCursor())
			return;
		long rowId = getRowId();

		// the value to update
		ContentValues values = new ContentValues();
		values.put(KEY_NUMBER_OF_PICS, nPictures);	

		// update it
		if (database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) <= 0)
			Log.e(Utils.LOG_TAG, "database did not update the number of pictures for groupRowId" + rowId);
	}

	/**
	 * Get an array of all groups stored in database
	 * @return
	 */
	public ArrayList<Group> getGroupsMatchRowIds(ArrayList<Long> rowIds){

		// the selection
		TwoObjects<String, String[]> selection = createSelection(KEY_ROW_ID, rowIds);

		// initalize output
		ArrayList<Group> output = new ArrayList<Group>();

		if (selection.mObject1.length() == 0)
			return output;

		// get the cursor
		Cursor cursor = 
			database.query(
					TABLE_NAME,
					null,
					selection.mObject1 + ADDITIONAL_QUERY,
					selection.mObject2,
					null,
					null,
					SORT_ORDER);

		// check null
		if (cursor == null)
			return output;

		// loop over cursor
		while (cursor.moveToNext()){
			output.add(new Group(cursor));
		}

		cursor.close();
		return output;
	}

	/**
	 * Return the name of the group, or "" if not found.
	 * @return
	 */
	public String getName(){
		if (!checkCursor())
			return "";
		else{
			return getString(KEY_NAME);
		}
	}

	public long getRowId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_ROW_ID);
	}

	public long getServerId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_SERVER_ID);
	}

	public boolean isAccessibleGroup(){
		return (isAMember() && !isMarkedForDeletion());
	}

	public boolean isAMember(){
		if (!checkCursor())
			return false;
		else
			return getBoolean(KEY_AM_I_MEMBER);
	}

	public boolean isKeepLocal(){
		if (!checkCursor())
			return false;
		else{
			return getBoolean(KEY_KEEP_LOCAL);
		}
	}

	public boolean isMarkedForDeletion(){
		if (!checkCursor())
			return false;
		else
			return getBoolean(KEY_MARK_FOR_DELETION);
	}

	public boolean isSynced(){
		if (!checkCursor())
			return false;
		else{
			return getBoolean(KEY_IS_SYNCED);
		}
	}

	/**
	 * Make a private group with a default name.
	 * @param ctx The context required to make the group
	 * @return The rowId of the new group. -1 if fail
	 */
	public long makeDefaultPrivateGroup(Context ctx){
		return makeNewGroup(
				ctx,
				DEFAULT_PRIVATE_NAME,
				null,
				null,
				Prefs.getUserRowId(ctx),
				false,
				null,
				null,
				-1,
				true,
				0);
	}

	/**
	 * Insert a new group into the database. Return the new rowId or -1 if unsuccessful.
	 * This group will not have a serverId attached
	 * to it, and its isSynced column will be false. Use setIsSynced to set these.
	 * @param ctx Context used for various calls
	 * @param groupName The groupName, must not be null or empty.
	 * @param groupPictureId The picture id to the main picture for this group, -1 or null if not known
	 * @param dateCreated The date this group was created, use Utils.getNowTime() if now, or pass null and this will be called for you.
	 * @param userIdWhoCreated The userId who created this group, if null or -1 is passed, the phones userId will be used.
	 * @param allowOthersToAddMembers boolean to allow others to add members. 
	 * @param latitude The latitude of this group location pass null for no location
	 * @param longitude The longitude of this group location pass null for no location
	 * @param allowPublicWithinDistance allow anybody to join group if there are within this many miles of group (pass -1) to not allow
	 * @param keepLocal boolean to keep these pictures local
	 * @param nPictures the number of pictures in the group, 0 if new group, or a number read from server
	 * @return The rowId of the group, -1 if it failed.
	 */
	public long makeNewGroup(
			Context ctx,
			String groupName,
			Long groupPictureId,
			String dateCreated,
			Long userIdWhoCreated,
			boolean allowOthersToAddMembers,
			Double latitude,
			Double longitude,
			double allowPublicWithinDistance,
			boolean keepLocal,
			int nPictures){

		// override some values and/or check
		if (dateCreated == null || dateCreated.length() == 0)
			dateCreated = Utils.getNowTime();
		if (groupName == null || groupName.length() ==0)
			throw new IllegalArgumentException("groupName must not be null length > 0");
		if (userIdWhoCreated == null || userIdWhoCreated == -1)
			userIdWhoCreated = Prefs.getUserRowId(ctx);
		if ((latitude == null || longitude == null) &&
				allowPublicWithinDistance >= 0)
			throw new IllegalArgumentException("cannot allow public within a distance >= 0 if lat or long are null");

		// trim name
		groupName = groupName.trim();

		// find the allowable folder name and create it
		FoldersWritten folderNames;
		try {
			folderNames = makeRequiredFolders(ctx, groupName, dateCreated);
		} catch (IOException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return -1;
		}

		// create values
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, groupName);
		values.put(KEY_PICTURE_ID, groupPictureId);
		values.put(KEY_DATE_CREATED, dateCreated);
		values.put(KEY_USER_ID_CREATED, userIdWhoCreated);
		values.put(KEY_ALLOW_OTHERS_ADD_MEMBERS, allowOthersToAddMembers);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LATITUDE, longitude);
		values.put(KEY_ALLOW_PUBLIC_WITHIN_DISTANCE, allowPublicWithinDistance);
		values.put(KEY_KEEP_LOCAL, keepLocal);

		// where to store names
		if (folderNames.isStoreAlternate){
			values.put(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, folderNames.topFolder);
			values.put(KEY_PICTURE_PATH_ALTERNATE, folderNames.picFolder);
			values.put(KEY_THUMBNAIL_PATH_ALTERNATE, folderNames.thumbFolder);
			values.put(KEY_GROUP_TOP_LEVEL_PATH, "");
			values.put(KEY_PICTURE_PATH, "");
			values.put(KEY_THUMBNAIL_PATH, "");
		}else{
			values.put(KEY_GROUP_TOP_LEVEL_PATH, folderNames.topFolder);
			values.put(KEY_PICTURE_PATH, folderNames.picFolder);
			values.put(KEY_THUMBNAIL_PATH, folderNames.thumbFolder);
			values.put(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, "");
			values.put(KEY_PICTURE_PATH_ALTERNATE, "");
			values.put(KEY_THUMBNAIL_PATH_ALTERNATE, "");
		}

		values.put(KEY_NUMBER_OF_PICS, nPictures);

		// create new row
		long newRow = database.insert(
				TABLE_NAME,
				null,
				values);

		return newRow;
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
	 * @param rowId the rowId of the group to update.
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
	 * Set weither the given group should be kept local to phone from now on.
	 * @param act An activity that will be used to update on the server
	 * @param asyncId An Id indicating what async will be returned to activity when server update is complete
	 * @param rowId the rowId of the group to update.
	 * @param keepLocal boolean if we should keep local
	 * @return CreateGroupTask that can be used to update to server. Null if no need to update. <br>
	 * Simply do if task is not null:<br>
	 * this.asyncArray.add(task); <br>
	 * task.execute();
	 */
	public CreateGroupTask<CustomActivity> setKeepLocal(
			CustomActivity act,
			int asyncId,
			long rowId,
			boolean keepLocal){

		// get old value first
		Group group = getGroup(rowId);
		if (group.isKeepLocal() == keepLocal)
			return null;

		// values to store
		ContentValues values = new ContentValues();
		values.put(KEY_KEEP_LOCAL, keepLocal);	

		// update table
		boolean tableSuccess = database.update(TABLE_NAME,values,KEY_ROW_ID + "='" + rowId + "'", null) > 0;

		// no success
		if (!tableSuccess)
			return null;

		// update to server if we previouisly weren't local, but now are and no serverId
		if (group.isKeepLocal() && !keepLocal && (group.getServerId() == -1 || group.getServerId() == 0)){
			// launch the async task and add to array of tasks to be managed
			CreateGroupTask<CustomActivity> task =  new CreateGroupTask<CustomActivity>(
					act,
					asyncId,
					rowId);
			return task;
		}else
			return null;
	}

	/**
	 * Set the picture id for this group
	 * @param rowId the rowId of the group to update.
	 * @param pictureId the pictureId for this this group
	 * @return boolean if we updated successfully to sql table.
	 */
	public boolean setPictureId(long rowId, long pictureId){

		ContentValues values = new ContentValues();
		values.put(KEY_PICTURE_ID, pictureId);	

		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}

	/**
	 * Returns the name of the group with html formatting. <br>
	 * 1. Current formatting is to apply a light blue color to private groups. <br>
	 * 2. red color to non private non-synced groups
	 */
	@Override
	public String toString(){
		if (!checkCursor())
			return "";
		if (isKeepLocal())
			return "<font color='"+PRIVATE_GROUP_COLOR+"'>" + getName() + "</font>";
		else if (!isKeepLocal() && !isSynced())
			return "<font color='"+NON_SYNCED_PUBLIC_GROUPS_COLOR+"'>" + getName() + "</font>";
		else if (!didIMakeGroup(ctx))
			return "<font color='"+SOMEONE_ELSE_MADE_GROUPS_COLOR+"'>" + getName() + "</font>";
		else
			return getName();
	}

	/**
	 * Return the pictureId of this group. If none available, then grabs the most recent picture
	 * @param ctx The context is rewquired, if no picture is stored, and we are going to find the most recent. use null if we dont care about that.
	 * @return The picture rowId, -1 if none
	 */
	public long getPictureId() {
		if(!checkCursor())
			return -1;

		long pictureId = getLong(KEY_PICTURE_ID);

		// check if it is 0, if so, then just grab the most recent picture
		if (pictureId == 0)
			pictureId = -1;
		if (pictureId == -1){
			PicturesAdapter pics = new PicturesAdapter(ctx);
			pics.fetchPicturesInGroup(getRowId());
			pics.moveToFirst();
			pictureId = pics.getRowId();
			pics.close();
		}

		return pictureId;
	}

	// private helper classes
	/**
	 * Return a cursor over all groups and all columns of this database sorted by date created
	 * @return
	 */
	private Cursor fetAllGroupsCursorSimple(){

		return database.query(TABLE_NAME, null, ADDITIONAL_QUERY_NO_AND, null, null, null, SORT_ORDER);
	}

	/**
	 * Make the required folders for this group. The top level folder, and the pictures and thumbnails
	 * @param ctx Context required
	 * @param groupName The group name
	 * @param dateCreated The date this was created
	 * @return threeObjects<String, String, STring> with top level folder, pictures folder, and thumbnails folder
	 * @throws IOException 
	 */
	private FoldersWritten makeRequiredFolders(Context ctx, String groupName, String dateCreated)
	throws IOException{
		// check sd card mounted
		if (!com.tools.Tools.isStorageAvailable(true))
			throw new IOException("SD card not mounted and writable");

		// should we use the alternate storage location?
		boolean useAlternate = false;
		if (Prefs.isStoreExternal(ctx))
			useAlternate = true;

		// actually write the folders
		TwoObjects<String, TwoStrings> helper = null;
		String folderName = null;
		TwoStrings subFolders = null;
		try{
			helper = makeRequiredFoldersHelper(groupName, dateCreated, useAlternate);
			folderName = helper.mObject1;
			subFolders = helper.mObject2;
		}catch(IOException e){
			// try again with other
			useAlternate = !useAlternate;
			helper = makeRequiredFoldersHelper(groupName, dateCreated, useAlternate);
			folderName = helper.mObject1;
			subFolders = helper.mObject2;
		}

		// the output
		FoldersWritten output = new FoldersWritten(folderName, subFolders.mObject1, subFolders.mObject2, useAlternate);

		// return the three folders.
		return output;
	}

	/**
	 * Class used as output for when we write folders for this group
	 * @author Kyle
	 *
	 */
	private static class FoldersWritten{
		private String topFolder;
		private String picFolder;
		private String thumbFolder;
		private boolean isStoreAlternate = true;

		public FoldersWritten(String topFolder,
				String picFolder,
				String thumbFolder,
				boolean isStoreAlternate) {
			this.topFolder = topFolder;
			this.picFolder = picFolder;
			this.thumbFolder = thumbFolder;
			this.isStoreAlternate = isStoreAlternate;
		}

	}

	/**
	 * Make folders at the main or alternate location depending on input. Will make sure we are writting to a new folder
	 * (appending the date and 1,2,3... if necessary to avoid collisions)
	 * @param groupName The name of the group
	 * @param dateCreated The date group was created (for folder naming if required)
	 * @param useAlternate Should we write to main or alternate location
	 * @return The top folder, picture folder, and thumb folder
	 * @throws IOException if creation failed
	 */
	private TwoObjects<String, TwoStrings> makeRequiredFoldersHelper(
			String groupName, String dateCreated, boolean useAlternate)
			throws IOException{

		// the name
		String folderName = getAllowableFolderName(ctx, groupName, dateCreated, useAlternate);

		// now make the folder
		File dir = new File(folderName);
		if(!dir.mkdirs())
			throw new IOException("Cannot create folder " + folderName);

		// now make sub folders
		TwoStrings subFolders = generatePicturePath(folderName, groupName);
		File dir2 = new File(subFolders.mObject1);
		if(!dir2.mkdirs())
			throw new IOException("Cannot create folder " + subFolders.mObject1);
		File dir3 = new File(subFolders.mObject2);
		if(!dir3.mkdirs())
			throw new IOException("Cannot create folder " + subFolders.mObject2);

		// the no media file
		File nomedia = new File(dir3, ".nomedia");
		if (!nomedia.exists()){
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				throw new IOException("Cannot create file .nomedia");
			}
		}

		return new TwoObjects<String, TwoStrings>(folderName, subFolders);
	}

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {		
	}

	/**
	 * Set the last picture number saved in this group.
	 * @param rowId the rowId of the group to update
	 * @param number
	 */
	protected boolean setLastPictureNumberDD(long rowId, long number){

		ContentValues values = new ContentValues();
		values.put(KEY_LAST_PICTURE_NUMBER, number);	

		return database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	
	}

	/**
	 * Increment the last picture number by 1
	 * @param rowId The rowId to update
	 */
	protected void incrementLastPictureNumber(long rowId){
		synchronized (GroupsAdapter.class) {

			// update the value
			/*
			String command1 = "UPDATE " + TABLE_NAME + " SET " + 
			KEY_LAST_PICTURE_NUMBER + " = " + KEY_LAST_PICTURE_NUMBER + " + 1 WHERE " + 
			KEY_ROW_ID + " =?";
			String command2 = "UPDATE " + TABLE_NAME + " SET " + 
			KEY_NUMBER_OF_PICS + " = " + KEY_NUMBER_OF_PICS + " + 1 WHERE " + 
			KEY_ROW_ID + " =?";
			String command3 = "UPDATE " + TABLE_NAME + " SET " + 
			KEY_TIME_LAST_PICTURE_ADDED + " = '" + (new Date()).getTime() + "' WHERE " + 
			KEY_ROW_ID + " =?";
			 */
			String command = "UPDATE " + TABLE_NAME + " SET " + 
			KEY_LAST_PICTURE_NUMBER + " = " + KEY_LAST_PICTURE_NUMBER + " + 1, "+
			KEY_NUMBER_OF_PICS + " = " + KEY_NUMBER_OF_PICS + " + 1, "+
			KEY_TIME_LAST_PICTURE_ADDED + " = '" + (new Date()).getTime() + "' WHERE " + 
			KEY_ROW_ID + " =?";

			try{
				//database.execSQL(command1, new String[] {String.valueOf(rowId)});
				//database.execSQL(command2, new String[] {String.valueOf(rowId)});
				//database.execSQL(command3, new String[] {String.valueOf(rowId)});
				database.execSQL(command, new String[] {String.valueOf(rowId)});
			}catch(SQLException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}
	}

	/**
	 * Increment the last picture number by 1
	 * @param rowId The rowId to update
	 */
	protected void incrementLastPictureNumberForFilePath(long rowId){
		synchronized (GroupsAdapter.class) {

			// update the value
			String command = "UPDATE " + TABLE_NAME + " SET " + 
			KEY_LAST_PICTURE_NUMBER + " = " + KEY_LAST_PICTURE_NUMBER + " + 1 "+
			" WHERE " + KEY_ROW_ID + " =?";
			try{
				database.execSQL(command, new String[] {String.valueOf(rowId)});
			}catch(SQLException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}
	}

	/**
	 * Decrement the number of pictures of the given group by 1
	 * @param groupRowId The group id
	 */
	protected void decrementPictureNumber(long groupRowId){
		String command = "UPDATE " + TABLE_NAME + " SET " + 
		KEY_NUMBER_OF_PICS + " = " + KEY_NUMBER_OF_PICS + " - 1 "+
		" WHERE " + KEY_ROW_ID + " =?";

		try{
			//database.execSQL(command1, new String[] {String.valueOf(rowId)});
			//database.execSQL(command2, new String[] {String.valueOf(rowId)});
			//database.execSQL(command3, new String[] {String.valueOf(rowId)});
			database.execSQL(command, new String[] {String.valueOf(groupRowId)});
		}catch(SQLException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
	}

	/**
	 * Object that defines a group.
	 * @author Kyle
	 *
	 */
	public class Group{
		private long rowId = -1;
		private String name;
		private long serverId;
		private long pictureId;
		private String dateCreated;
		private long userIdWhoCreated;
		private boolean allowOthersToAddMembers;
		private double latitude;
		private double longitude;
		private double allowPublicWithinDistance;
		private boolean isUpdating;
		private boolean isSynced;
		private boolean isEmpty = true;
		private long lastPictureNumber;
		private String groupFolderName;
		private String picturePath;
		private String thumbnailPath;
		private String groupFolderNameAlternate;
		private String picturePathAlternate;
		private String thumbnailPathAlternate;
		private boolean keepLocal;
		private String lastUpdateAttemptTime;
		private String pictureThumbPath;
		private String pictureFullPath;

		/**
		 * Create a group from the database cursor at the current location.
		 * @param cursor
		 */
		private Group(Cursor cursor){

			// check null and size of cursor
			if (cursor == null)
				return;

			// fill fields
			setEmpty(false);
			setRowId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROW_ID)));
			setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
			setServerId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SERVER_ID)));
			setPictureId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PICTURE_ID)));
			setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE_CREATED)));
			setLastUpdateAttemptTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_UPDATE_ATTEMPT_TIME)));	
			setUserIdWhoCreated(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_USER_ID_CREATED)));
			setAllowOthersToAddMembers(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ALLOW_OTHERS_ADD_MEMBERS))>0);
			setAllowPublicWithinDistance(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_ALLOW_PUBLIC_WITHIN_DISTANCE)));
			setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE)));
			setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)));
			setKeepLocal(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_KEEP_LOCAL)) > 0);
			setLastPictureNumber(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LAST_PICTURE_NUMBER)));
			setGroupFolderName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_TOP_LEVEL_PATH)));
			setPicturePath(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PICTURE_PATH)));
			setThumbnailPath(cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMBNAIL_PATH)));
			setUpdating(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_UPDATING))>0);
			setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_SYNCED))>0);

			setGroupFolderNameAlternate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE)));
			setPicturePathAlternate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PICTURE_PATH_ALTERNATE)));
			setThumbnailPathAlternate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMBNAIL_PATH_ALTERNATE)));
		}

		public boolean canUserAddMembers(Context ctx){
			if (getUserIdWhoCreated() == Prefs.getUserRowId(ctx) ||
					isAllowOthersToAddMembers())
				return true;
			else
				return false;
		}

		public boolean canUserRemoveMembers(Context ctx){
			if (getUserIdWhoCreated() == Prefs.getUserRowId(ctx))
				return true;
			else
				return false;
		}

		/**
		 * These two objects are considered equal if all fields are equal
		 */
		@Override
		public boolean equals(Object o){
			// must be correct type
			if (!( o instanceof Group) || o==null)
				return false;

			// convert			
			Group input = ((Group) o);

			// if empty, they cannot be equal
			if (isEmpty() || input.isEmpty())
				return false;

			// now compare each field
			return equalsHelper(input);
		}

		public double getAllowPublicWithinDistance() {
			return allowPublicWithinDistance;
		}

		public String getDateCreated() {
			return dateCreated;
		}

		/**
		 * Get the next picture number we will use directly from the database and increment database value
		 * @return The next picture number for this group
		 */
		public long getNextPictureNumber() {

			// query directly from database in a synchronized fashion
			synchronized (GroupsAdapter.class) {

				// get the cursor
				Cursor cursor = database.query(
						TABLE_NAME,
						new String[] {KEY_LAST_PICTURE_NUMBER},
						KEY_ROW_ID + " = ?",
						new String[] {String.valueOf(rowId)},
						null,
						null,
						SORT_ORDER);

				// read value of cursor
				if (cursor == null)
					return -1;

				if (!cursor.moveToFirst())
					return -1;

				// grab the value
				long value = cursor.getLong(0);
				cursor.close();

				// update the value
				incrementLastPictureNumberForFilePath(rowId);

				// return the value
				return value;
			}
		}

		public String getLastUpdateAttemptTime() {
			return lastUpdateAttemptTime;
		}
		public double getLatitude() {
			return latitude;
		}
		public double getLongitude() {
			return longitude;
		}
		public String getName() {
			return name;
		}

		/**
		 * Get the next picture name that we can write to. Full path
		 * @param useDesiredLocation Do we want to force to the backup folder (false) or use normal location (true)
		 * @return The full path to the next available picture name and thumbnail name
		 *  in the given groupId.
		 * @throws IOException if we can't write folders and therefore there is no way to get a next picture
		 */
		public TwoStrings getNextPictureName(boolean useDesiredLocation)
		throws IOException{
			//TODO: this could be really slow as it iterates trying to find the best picture number.

			// get the current last number
			long newNumber = getNextPictureNumber();

			// now check the picture and thumbnail to make sure they are available, if not, go up one
			Group3Folders folders = null;
			if (useDesiredLocation)
				folders = writeFoldersIfNeeded();
			else
				folders = writeBackupFoldersIfNeeded();
			String pathPicture = folders.picFolder;
			String pathThumbnail = folders.thumbFolder;
			while ((new File(Utils.makeFullFileName(pathPicture, newNumber))).exists() ||
					(new File(Utils.makeFullFileName(pathThumbnail, newNumber))).exists())
				newNumber++;

			// the full path
			return new TwoStrings(
					Utils.makeFullFileName(pathPicture, newNumber),
					Utils.makeFullFileName(pathThumbnail, newNumber));
		}

		/**
		 * Return the picture path for the group. If none, then grabs the most recent picture
		 * @param ctx Context is required, if we have not previously called this method on this exact object (not just the group, but the Java object).
		 * @return The picture path
		 */
		public String getPictureFullPath(Context ctx){
			if (pictureFullPath == null || pictureFullPath.length()==0 && ctx != null){
				long picId = getPictureId(ctx);
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(picId);
				pictureFullPath = pics.getFullPicturePath();
				pics.close();
			}
			return pictureFullPath;
		}
		/**
		 * Return the pictureId of this group. If none available, then grabs the most recent picture
		 * @param ctx The context is rewquired, if no picture is stored, and we are going to find the most recent. use null if we dont care about that.
		 * @return The picture rowId, -1 if none
		 */
		public long getPictureId(Context ctx) {
			// check if it is 0, if so, then just grab the most recent picture
			if (pictureId == 0)
				pictureId = -1;
			if (pictureId == -1 && ctx != null){
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicturesInGroup(getRowId());
				pics.moveToFirst();
				pictureId = pics.getRowId();
				pics.close();
			}

			return pictureId;
		}

		/**
		 * Return the thumbnail for the group picture path. If none, thn grabs the most recent picture
		 * @param ctx Context is required, if we have not previously called this method on this exact object (not just the group, but the Java object).
		 * @return The thumbnail path
		 */
		public String getPictureThumbnailPath(Context ctx){
			if (pictureThumbPath == null || pictureThumbPath.length()==0 && ctx != null){
				long picId = getPictureId(ctx);
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(picId);
				pictureThumbPath = pics.getThumbnailPath();
				pics.close();
			}
			return pictureThumbPath;
		}
		public long getRowId() {
			return rowId;
		}
		public long getServerId() {
			return serverId;
		}

		public long getUserIdWhoCreated() {
			return userIdWhoCreated;
		}
		@Override
		public int hashCode(){
			if (isEmpty())
				return 0;
			else
				return ((Long)getRowId()).hashCode();
		}
		public boolean isAllowOthersToAddMembers() {
			return allowOthersToAddMembers;
		}
		public boolean isEmpty() {
			return isEmpty;
		}
		public boolean isKeepLocal() {
			return keepLocal;
		}
		public boolean isSynced() {
			return isSynced;
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
		/**
		 * Returns the name of the group with html formatting. <br>
		 * 1. Current formatting is to apply a light blue color to private groups. <br>
		 * 2. red color to non private non-synced groups
		 */
		@Override
		public String toString(){
			if (isKeepLocal())
				return "<font color='"+PRIVATE_GROUP_COLOR+"'>" + getName() + "</font>";
			else if (!isKeepLocal() && !isSynced())
				return "<font color='"+NON_SYNCED_PUBLIC_GROUPS_COLOR+"'>" + getName() + "</font>";
			else if (!didIMakeGroup(ctx))
				return "<font color='"+SOMEONE_ELSE_MADE_GROUPS_COLOR+"'>" + getName() + "</font>";
			else
				return getName();
		}

		/**
		 * Get the folders we want to write to read from sql database
		 * @return
		 */
		private Group3Folders getDesiredFoldersFromDatabase(){
			boolean useAlternate = false;
			if (Prefs.isStoreExternal(ctx))
				useAlternate = true;

			// create the folders object
			Group3Folders out = null;
			if (useAlternate)
				out = getAlternateFoldersFromDatabase();
			else
				out = getNormalFoldersFromDatabase();

			return out;
		}

		/**
		 * Get the folders we want to write to read from memory
		 * @return
		 */
		private Group3Folders getDesiredFoldersFromMemory(){
			boolean useAlternate = false;
			if (Prefs.isStoreExternal(ctx))
				useAlternate = true;

			// create the folders object
			Group3Folders out = null;
			if (useAlternate)
				out = new Group3Folders(
						groupFolderNameAlternate,
						picturePathAlternate,
						thumbnailPathAlternate);
			else
				out = new Group3Folders(
						groupFolderName,
						picturePath,
						thumbnailPath);

			return out;
		}

		/**
		 * Get the folders we dont' want to write to but will if we have to from sql database
		 * @return
		 */
		private Group3Folders getBackupFoldersFromDatabase(){
			boolean useAlternate = false;
			if (Prefs.isStoreExternal(ctx))
				useAlternate = true;
			useAlternate = !useAlternate;

			// create the folders object
			Group3Folders out = null;
			if (useAlternate)
				out = getAlternateFoldersFromDatabase();
			else
				out = getNormalFoldersFromDatabase();

			return out;
		}

		/**
		 * Get the folders we dont' want to write to but will if we have to from memory
		 * @return
		 */
		private Group3Folders getBackupFoldersFromMemory(){
			boolean useAlternate = false;
			if (Prefs.isStoreExternal(ctx))
				useAlternate = true;
			useAlternate = !useAlternate;

			// create the folders object
			Group3Folders out = null;
			if (useAlternate)
				out = new Group3Folders(
						groupFolderNameAlternate,
						picturePathAlternate,
						thumbnailPathAlternate);
			else
				out = new Group3Folders(
						groupFolderName,
						picturePath,
						thumbnailPath);

			return out;
		}

		/**
		 * Get the normal location folders directly from the database
		 * @return The desired folders
		 */
		private Group3Folders getNormalFoldersFromDatabase(){
			synchronized (getCreatingNewFolderLock(getRowId())) {
				Group group = getGroup(getRowId());
				return new Group3Folders(group.groupFolderName, group.picturePath, group.thumbnailPath);
			}
		}

		/**
		 * Get the altenrate location folders directly from the database
		 * @return The desired folders
		 */
		private Group3Folders getAlternateFoldersFromDatabase(){
			synchronized (getCreatingNewFolderLock(getRowId())) {
				Group group = getGroup(getRowId());
				return new Group3Folders(
						group.groupFolderNameAlternate,
						group.picturePathAlternate,
						group.thumbnailPathAlternate);
			}
		}

		/**
		 * Write the folders that need to be created before writing a picture to file. Will write to backup location if required.
		 * @return the top level path where folders were written
		 * @throws IOException if we can't create the folders
		 */
		public Group3Folders writeFoldersIfNeeded()
		throws IOException{

			// get the folders
			Group3Folders folders = getDesiredFoldersFromMemory();

			// check folders directly from database if empty
			if (folders.isGroupsEmpty())
				folders = getDesiredFoldersFromDatabase();

			// if the folders are still empty and we are waiting for them to be created, then wait
			while (folders.isGroupsEmpty() && isCreatingNewFolder(getRowId())){
				ExpiringValue<Boolean> lock = getCreatingNewFolderLock(getRowId());
				synchronized(lock){
					try {
						lock.wait((long)(GroupsAdapter.MAKE_NEW_FOLDER_TIMOUT*1000));
					}catch(InterruptedException e){
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					}
					folders = getDesiredFoldersFromMemory();
				}
			}

			// if we're still empty, then make the folders
			if (folders.isGroupsEmpty()){
				try{
					return makeRequiredFolderAtDesiredLocation();
				}catch(IOException exception){
					// if we errored, then write to backup folders
					return makeRequiredFolderAtBackupLocation();
				}
			}else{
				// write the folders if needed
				writeFoldersIfNeededHelper(folders.topFolder, folders.picFolder, folders.thumbFolder);
				return folders;
			}
		}


		/**
		 * Write the folders that need to be created before writing a picture to file at the backup location
		 * @return the top level path where folders were written
		 * @throws IOException if we can't create the folders
		 */
		public Group3Folders writeBackupFoldersIfNeeded()
		throws IOException{

			// get the folders
			Group3Folders folders = getBackupFoldersFromMemory();

			// check folders directly from database if empty
			if (folders.isGroupsEmpty())
				folders = getBackupFoldersFromDatabase();

			// if the folders are still empty and we are waiting for them to be created, then wait
			while (folders.isGroupsEmpty() && isCreatingNewFolder(getRowId())){
				ExpiringValue<Boolean> lock = getCreatingNewFolderLock(getRowId());
				synchronized(lock){
					try {
						lock.wait((long)(GroupsAdapter.MAKE_NEW_FOLDER_TIMOUT*1000));
					}catch(InterruptedException e){
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					}
					folders = getBackupFoldersFromDatabase();
				}
			}

			// if we're still empty, then make the folders
			if (folders.isGroupsEmpty())
					return makeRequiredFolderAtBackupLocation();
			else{
				// write the folders if needed
				writeFoldersIfNeededHelper(folders.topFolder, folders.picFolder, folders.thumbFolder);
				return folders;
			}
		}

		/**
		 * Helper that simply creates the required folders need to store pictures. Also creates the .nomedia file in thumbs
		 * @param topFolder The top level folder
		 * @param picFolder The picture folder
		 * @param thumbFolder The thumb folder
		 * @throws IOException if we cannot create any of the files
		 */
		private void writeFoldersIfNeededHelper(String topFolder, String picFolder, String thumbFolder)
		throws IOException{

			// check for existence and make if not exist
			File dir1 = new File(topFolder);
			if (!dir1.exists())
				if(!dir1.mkdirs())
					throw new IOException("Cannot create folder " + topFolder);

			File dir2 = new File(picFolder);
			if (!dir2.exists())
				if(!dir2.mkdirs())
					throw new IOException("Cannot create folder " + picFolder);

			File dir3 = new File(thumbFolder);
			if (!dir3.exists())
				if(!dir3.mkdirs())
					throw new IOException("Cannot create folder " + thumbFolder);

			// the no media file
			File nomedia = new File(dir3, ".nomedia");
			if (!nomedia.exists()){
				try {
					nomedia.createNewFile();
				} catch (IOException e) {
					throw new IOException("Cannot create file .nomeida");
				}
			}
		}

		/**
		 * Write the required folders and update database at the other location if needed. Not necessarilly the external storage, but just the non-user selected
		 * @return 
		 * @throws IOException If we failed
		 */
		private Group3Folders makeRequiredFolderAtBackupLocation()
		throws IOException{
			ExpiringValue<Boolean> lock = getCreatingNewFolderLock(getRowId());
			synchronized (lock){
				try{
					// keep track that we are writing
					setCreatingNewFolder(getRowId(), true);

					// get the group name
					String name = getName();
					if (name == null || name.length() == 0)
						throw new IOException("Group has no name to write as folder");

					// check sd card mounted
					if (!com.tools.Tools.isStorageAvailable(true))
						throw new IOException("SD card not mounted and writable");

					// should we use the alternate storage location?
					boolean useAlternate = false;
					if (Prefs.isStoreExternal(ctx))
						useAlternate = true;

					// now we want opposite, so switch it
					useAlternate = !useAlternate;

					// date created
					String dateCreated = getDateCreated();

					// make the folders
					TwoObjects<String, TwoStrings> helper = makeRequiredFoldersHelper(name, dateCreated, useAlternate);

					// what values to write to database
					ContentValues values = new ContentValues(3);
					if (useAlternate){
						values.put(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, helper.mObject1);
						values.put(KEY_PICTURE_PATH_ALTERNATE, helper.mObject2.mObject1);
						values.put(KEY_THUMBNAIL_PATH_ALTERNATE, helper.mObject2.mObject2);
					}else{
						values.put(KEY_GROUP_TOP_LEVEL_PATH, helper.mObject1);
						values.put(KEY_PICTURE_PATH, helper.mObject2.mObject1);
						values.put(KEY_THUMBNAIL_PATH, helper.mObject2.mObject2);
					}

					// where to write
					final String where = KEY_ROW_ID + " =?";			

					// write it
					int update = database.update(TABLE_NAME, values, where, new String[] {String.valueOf(getRowId())});

					// store in local object
					if (useAlternate){
						setGroupFolderNameAlternate(helper.mObject1);
						setPicturePathAlternate(helper.mObject2.mObject1);
						setThumbnailPathAlternate(helper.mObject2.mObject2);
					}else{
						setGroupFolderName(helper.mObject1);
						setPicturePath(helper.mObject2.mObject1);
						setThumbnailPath(helper.mObject2.mObject2);
					}

					// error if we couldn't update
					if (update < 1)
						throw new IOException("Could not update database");

					// keep track that we are writing
					setCreatingNewFolder(getRowId(), false);
					lock.notifyAll();

					return getBackupFoldersFromMemory();
				}catch(IOException e){
					lock.notifyAll();
					throw(e);
				}
			}
		}

		/**
		 * Write the required folders and update database at the desired location if needed. Not necessarilly the internal storage, but just the user selected
		 * @return the folders written
		 * @throws IOException If we failed
		 */
		private Group3Folders makeRequiredFolderAtDesiredLocation()
		throws IOException{
			ExpiringValue<Boolean> lock = getCreatingNewFolderLock(getRowId());
			synchronized (lock){
				try{
					// keep track that we are writing
					setCreatingNewFolder(getRowId(), true);

					// get the group name
					String name = getName();
					if (name == null || name.length() == 0)
						throw new IOException("Group has no name to write as folder");

					// check sd card mounted
					if (!com.tools.Tools.isStorageAvailable(true))
						throw new IOException("SD card not mounted and writable");

					// should we use the alternate storage location?
					boolean useAlternate = false;
					if (Prefs.isStoreExternal(ctx))
						useAlternate = true;

					// date created
					String dateCreated = getDateCreated();

					// make the folders
					TwoObjects<String, TwoStrings> helper = makeRequiredFoldersHelper(name, dateCreated, useAlternate);

					// what values to write to database
					ContentValues values = new ContentValues(3);
					if (useAlternate){
						values.put(KEY_GROUP_TOP_LEVEL_PATH_ALTERNATE, helper.mObject1);
						values.put(KEY_PICTURE_PATH_ALTERNATE, helper.mObject2.mObject1);
						values.put(KEY_THUMBNAIL_PATH_ALTERNATE, helper.mObject2.mObject2);
					}else{
						values.put(KEY_GROUP_TOP_LEVEL_PATH, helper.mObject1);
						values.put(KEY_PICTURE_PATH, helper.mObject2.mObject1);
						values.put(KEY_THUMBNAIL_PATH, helper.mObject2.mObject2);
					}

					// where to write
					final String where = KEY_ROW_ID + " =?";			

					// write it
					int update = database.update(TABLE_NAME, values, where, new String[] {String.valueOf(getRowId())});

					// store in local object
					if (useAlternate){
						setGroupFolderNameAlternate(helper.mObject1);
						setPicturePathAlternate(helper.mObject2.mObject1);
						setThumbnailPathAlternate(helper.mObject2.mObject2);
					}else{
						setGroupFolderName(helper.mObject1);
						setPicturePath(helper.mObject2.mObject1);
						setThumbnailPath(helper.mObject2.mObject2);
					}

					// error if we couldn't update
					if (update < 1)
						throw new IOException("Could not update database");

					// keep track that we are writing
					setCreatingNewFolder(getRowId(), false);

					lock.notifyAll();
					return getDesiredFoldersFromMemory();
				}catch(IOException e){
					lock.notifyAll();
					throw(e);
				}
			}
		}

		/**
		 * compare each field in this class to be equal
		 * @param group
		 * @return true if all fields equal, false otherwise.
		 */
		private boolean equalsHelper(Group group){
			Class<? extends Object> thisClass = this.getClass();
			Field[] fieldsArray = thisClass.getDeclaredFields();
			for (Field field : fieldsArray){
				if (field.getName().equalsIgnoreCase("this$0"))
					continue;
				try {
					Object tmp = field.get(this);
					if (tmp == null || !this.equals(field.get(group)))
						return false;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		private void setAllowOthersToAddMembers(boolean allowOthersToAddMembers) {
			this.allowOthersToAddMembers = allowOthersToAddMembers;
		}

		private void setAllowPublicWithinDistance(double allowPublicWithinDistance) {
			this.allowPublicWithinDistance = allowPublicWithinDistance;
		}

		private void setDateCreated(String dateCreated) {
			this.dateCreated = dateCreated;
		}

		private void setEmpty(boolean isEmpty) {
			this.isEmpty = isEmpty;
		}

		private void setGroupFolderName(String groupFolderName) {
			this.groupFolderName = groupFolderName;
		}

		private void setGroupFolderNameAlternate(String groupFolderName) {
			this.groupFolderNameAlternate = groupFolderName;
		}

		private void setKeepLocal(boolean keepLocal) {
			this.keepLocal = keepLocal;
		}

		private void setLastPictureNumber(long lastPictureNumber) {
			this.lastPictureNumber = lastPictureNumber;
		}

		private void setLastUpdateAttemptTime(String lastUpdateAttemptTime) {
			this.lastUpdateAttemptTime = lastUpdateAttemptTime;
		}

		private void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		private void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		private void setName(String name) {
			this.name = name;
		}

		private void setPictureId(long pictureId) {
			this.pictureId = pictureId;
		}

		private void setPicturePath(String path){
			picturePath = path;
		}

		private void setPicturePathAlternate(String path){
			picturePathAlternate = path;
		}

		public boolean didIMakeGroup(Context ctx){
			return (getUserIdWhoCreated() == Prefs.getUserRowId(ctx));
		}

		/**
		 * Set the number of pictures in this group. Must be called after setRowId has been called.
		 */
		/*
		private void setNPictures(){
			long id = getRowId();
			if (id == -1)
				throw new IllegalArgumentException("attempting to set nPictures before rowId has been set");

			Pic
		}
		 */
		private void setRowId(long rowId) {
			this.rowId = rowId;
		}

		private void setServerId(long serverId) {
			this.serverId = serverId;
		}

		private void setSynced(boolean isSynced) {
			this.isSynced = isSynced;
		}

		private void setThumbnailPath(String thumbnailPath) {
			this.thumbnailPath = thumbnailPath;
		}

		private void setThumbnailPathAlternate(String thumbnailPath) {
			this.thumbnailPathAlternate = thumbnailPath;
		}

		private void setUpdating(boolean isUpdating) {
			this.isUpdating = isUpdating;
		}

		private void setUserIdWhoCreated(long userIdWhoCreated) {
			this.userIdWhoCreated = userIdWhoCreated;
		}
	}

	/**
	 * Class used to determine exactly which folder we will attempt to write pictures to.
	 * @author Kyle
	 *
	 */
	public static class Group3Folders{
		public String topFolder;
		public String picFolder;
		public String thumbFolder;

		private Group3Folders(String topFolder, String picFolder, String thumbFolder) {
			this.topFolder = topFolder;
			this.picFolder = picFolder;
			this.thumbFolder = thumbFolder;
		}

		/**
		 * Are the group contained in this object empty?
		 * @return
		 */
		public boolean isGroupsEmpty(){
			if (topFolder != null && topFolder.length() != 0
					&& picFolder != null && picFolder.length() != 0
					&& thumbFolder != null && thumbFolder.length() != 0)
				return false;
			else
				return true;
		}
	}

	public interface ItemsFetchedCallback <ACTIVITY_TYPE>{
		/**
		 * This is called when the items are done being fetched and placeholders have been put into database for new items.
		 * This is called before onItemsFetchedUiThread
		 * @param act The current activity that is active
		 * @param nNewItems The number of new items fetched
		 * @param errorCode, null if no error, otherwise the error message
		 */
		public void onItemsFetchedBackground(ACTIVITY_TYPE act, int nNewItems, String errorMessage);
		/**
		 * This is called on the UI thread when we are done getting the items. and after onItemsFetchedBackground
		 * @param act The current activity, not guaranteed to be the same activity as in onItemsFetchedBackground
		 * @param errorCode null if no error, otherwise the error code
		 */
		public void onItemsFetchedUiThread(ACTIVITY_TYPE act, String errorCode);
	}

	/**
	 * Any groups that have the given picture as their group picture will be set back to defaul
	 * @param rowId The rowId of the picture to remove
	 */
	protected void removePictureAsId(long pictureRowId){

		// update with default
		ContentValues values = new ContentValues(1);
		values.put(KEY_PICTURE_ID, -1);

		// do teh update
		database.update(TABLE_NAME, values, KEY_PICTURE_ID + " = ?", new String[] {String.valueOf(pictureRowId)});
	}
}


