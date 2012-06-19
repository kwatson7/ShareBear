package com.instantPhotoShare.Adapters;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.GetGroupsServerReturn;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Activities.GroupGallery;
import com.instantPhotoShare.Tasks.CreateGroupTask;
import com.tools.CustomActivity;
import com.tools.ServerPost.PostCallback;
import com.tools.ServerPost.ServerReturn;
import com.tools.ThreeObjects;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.util.Log;

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

	// types
	private static final String MARK_FOR_DELETION_TYPE = " BOOLEAN DEFAULT 'FALSE'";
	private static final String AM_I_MEMBER_TYPE = " BOOLEAN DEFAULT 'TRUE'";

	// private constants
	private static final String DEFAULT_PRIVATE_NAME = "Private"; 			// default group name for the auto-generated private group
	private static final String PRIVATE_GROUP_COLOR = "#ADD8E6"; 			// color for private groups when toString is called
	private static final String NON_SYNCED_PUBLIC_GROUPS_COLOR = "red"; 	// color for non-synced groups when toString is called
	private static final String SORT_ORDER = KEY_DATE_CREATED + " DESC"; 			// sort the picture by most recent
	private static final String ADDITIONAL_QUERY_NO_AND = 
			"(" + KEY_AM_I_MEMBER + " ='TRUE' OR " + KEY_AM_I_MEMBER + " = '1') AND (" 
					+ KEY_MARK_FOR_DELETION + " = 'FALSE' OR " + KEY_MARK_FOR_DELETION + " = '0')";
	private static final String ADDITIONAL_QUERY = " AND " + ADDITIONAL_QUERY_NO_AND;

	// error codes
	public static final String GROUP_ACCESS_ERROR = "GROUP_ACCESS_ERROR";

	// static variables
	private static boolean isFetchingGroups = false;


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
					+KEY_MARK_FOR_DELETION + MARK_FOR_DELETION_TYPE + ", "
					+KEY_AM_I_MEMBER + AM_I_MEMBER_TYPE +", "
					+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
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

		return out;
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
	 * @return
	 */
	private static String getAllowableFolderName(
			Context ctx,
			String groupName,
			String dateCreated){

		// read in some properites to make it faster
		final char pathsep = Utils.pathsep;

		// remove illegal characters
		groupName = Utils.getAllowableFileName(groupName);
		dateCreated = Utils.getAllowableFileName(dateCreated);

		// the top storage place
		String top = Utils.getExternalStorageTopPath();

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
				while (!(new File(path + newNumber + pathsep)).exists())
					newNumber++;
				path = path + newNumber + pathsep;
			}
		}

		// the paht
		return path;
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
		boolean value = cursor.moveToFirst();
		cursor.close();
		return value;
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
		String topPath = group.getGroupFolderName();

		// add marke for deletion to row
		boolean updateVal =  database.update(
				TABLE_NAME,
				values,
				where, selectionArgs) > 0;

				// delete folders if they are empty
				if (com.tools.Tools.isFolderEmpty(topPath))
					com.tools.Tools.deleteEmptyFolder(new File(topPath));

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
		String topPath = group.getGroupFolderName();

		// delete the row
		int effected = database.delete(
				TABLE_NAME,
				where,
				selectionArgs);

		// throw error if we somehow linked to more than one group
		if (effected > 1 && !Prefs.debug.allowMultipleUpdates)
			throw new IllegalArgumentException("attempting to delete more than one row in groups. This should never happen");

		// delete folders if they are empty
		if (com.tools.Tools.isFolderEmpty(topPath))
			com.tools.Tools.deleteEmptyFolder(new File(topPath));

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
	public void fetchAllGroups(){

		// grab the cursor over all groups
		Cursor cursor = getAllGroupsCursor();

		setCursor(cursor);
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
	void fetchPictureIdsFromServer(
			ACTIVITY_TYPE act,
			final long groupServerId,
			ArrayList<String> indeterminateProgressBars,
			final ItemsFetchedCallback<ACTIVITY_TYPE> callback){
		//TODO: this shouldn't be synced. Will cause hanging, also doesn't even gaurantee thread safe, as multiple objects cann access this

		// bad serverId
		if (groupServerId == -1 || groupServerId == 0)
			return;

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put("user_id", Prefs.getUserServerId(ctx));
			json.put("secret_code", Prefs.getSecretCode(ctx));
			json.put("group_id", groupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return;
		}

		Utils.postToServer(
				"get_image_ids",
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

						// convert to thumbnail data
						ShareBearServerReturn data = new ShareBearServerReturn(result);

						// not a good return
						if (!data.isSuccess()){
							Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
							return;
						}

						// parse returned data and check if we have these pictures
						JSONArray array = data.getMessageArray();
						if (array == null)
							return;

						// loop checking the pictures and creating in database if we don't have
						PicturesAdapter pics = new PicturesAdapter(ctx);
						PicturesInGroupsAdapter comboAdapter = new PicturesInGroupsAdapter(ctx);
						GroupsAdapter adapter = new GroupsAdapter(ctx);
						Group group = adapter.getGroupByServerId(groupServerId);
						if(group == null || group.keepLocal)
							return;
						int counter = 0;
						for (int i = 0; i < array.length(); i++){

							synchronized (GroupsAdapter.class) {
								// determine the path to store files
								TwoStrings picNames = group.getNextPictureName();
								try {

									// create a new picture in database
									if(!pics.isPicturePresent(array.getLong(i))){
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
									if (pics.getRowId() == 0 || pics.getRowId() == -1)
										continue;

									// add the picture to this group if not already present
									if(!pics.isPictureInGroup(pics.getRowId(), group.getRowId())){
										comboAdapter.addPictureToGroup(ctx, pics.getRowId(), group.getRowId());
										counter++;
									}

								} catch (NumberFormatException e) {
									Log.e("TAG", Log.getStackTraceString(e));
									continue;
								} catch (JSONException e) {
									Log.e("TAG", Log.getStackTraceString(e));
									continue;
								}
							}
						}	

						// notification for new pictures
						if (counter > 0){
							NotificationsAdapter notes = new NotificationsAdapter(ctx);
							notes.createNotification(
									counter + " new pictures for " + group.getName(),
									NotificationsAdapter.NOTIFICATION_TYPES.NEW_PICTURE_IN_GROUP, String.valueOf(group.getRowId()));
						}

						// send callback back to activity
						if (callback != null){
							if (data.isSuccess())
								callback.onItemsFetchedBackground(act, counter, null);
							else
								callback.onItemsFetchedBackground(act, counter, result.getErrorCode());
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
	 * Fetch all the groups from the server for a given user and then create for any new groups. 
	 * Done a a background thread
	 * @param <ACTIVITY_TYPE>
	 * @param act The activity that will be attached to callback on return.
	 * @param groupServerId the server id of the group in question
	 * @param indeterminateProgressBars indetermiante progress bars to show, null if none (the tags to find them)
	 * @param callback The callback to be called when we are done. Can be null.
	 * callback 3rd return will be how many new groups there are
	 */
	public <ACTIVITY_TYPE extends CustomActivity>
	void fetchAllGroupsFromServer(
			ACTIVITY_TYPE act,
			ArrayList<String> indeterminateProgressBars,
			final ItemsFetchedCallback<ACTIVITY_TYPE> callback){

		// we are already doing it, no need to do it twice
		if(isFetchingGroups)
			return;
		isFetchingGroups = true;

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put("user_id", Prefs.getUserServerId(ctx));
			json.put("secret_code", Prefs.getSecretCode(ctx));
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return;
		}

		// post the command to the server
		Utils.postToServer(
				"get_groups",
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
								Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
								return;
							}

							// grab all the group serverIds and also the number of pictures in each group
							GroupsAdapter adapter = new GroupsAdapter(ctx);
							Iterator<?> iterator = data.getMessageObject().keys();
							HashSet<String> allServerids = new HashSet<String>(data.getMessageObject().length());
							HashMap<Long, Integer> nPicturesInGroupsFromServer = new HashMap<Long, Integer>();
							while(iterator.hasNext()){
								String val = (String) iterator.next();
								long groupServerId = Long.valueOf(val);
								if (groupServerId != 0)
									allServerids.add(val);
								nPicturesInGroupsFromServer.put(groupServerId, data.getNPictures(groupServerId));
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
								if (serverNPics > localNPics)
									newPictures.put(rowId, new TwoObjects<Integer, String>(serverNPics-localNPics, name));
							}
							
							// notifications for new pictures in groups
							NotificationsAdapter notes = new NotificationsAdapter(ctx);
							for (Entry<Long, TwoObjects<Integer, String>> entry : newPictures.entrySet()) {
								long rowId = entry.getKey();
								TwoObjects<Integer, String> val = entry.getValue();
								int nNewPictures = val.mObject1;
								String name = val.mObject2;
								notes.createNotification(
										"You have " + nNewPictures + " new pictures in " + name,
										NotificationsAdapter.NOTIFICATION_TYPES.NEW_PICTURE_IN_GROUP,
										String.valueOf(rowId));								
							}
							//TODO: do something with these updates.
							//TODO: we will keep getting the same update over and over until we actually go into the group.
							

							// determine which groups are new
							UsersAdapter users = new UsersAdapter(ctx);
							HashSet<String> newIds = adapter.getNewValues(TABLE_NAME, KEY_SERVER_ID, allServerids);
							int nNewGroups = newIds.size();

							// add new groups
							for (String item : newIds){
								long groupServerId = Long.valueOf(item);

								// see if the user exists, if it doesn't then add it
								long userServerId = data.getUserServerIdWhoCreated(groupServerId);
								users.fetchUserByServerId(userServerId);
								long userRowId = users.getRowId();
								if(userRowId == 0 ||userRowId == -1){
									users.close();
									userRowId = users.makeNewUser(userServerId);
								}else
									users.close();

								// create a new group
								//TODO: read allowOthersToAddMembers
								//TODO: read allow within distance
								//TODO: read lat and long
								//TODO: read picture id for group
								long groupRowId = adapter.makeNewGroup(ctx,
										data.getName(groupServerId),
										null,
										data.getDateCreated(groupServerId),
										userRowId,
										true, 
										null,
										null,
										-1,
										false);

								adapter.setIsSynced(groupRowId, true, groupServerId);
							}

							// notification for new groups
							if (nNewGroups > 0){
								notes.createNotification(
										nNewGroups + " new groups you've been added to.",
										NotificationsAdapter.NOTIFICATION_TYPES.ADD_TO_NEW_GROUP, null);
							}

							// send callback back to activity
							if (callback != null){
								if (data.isSuccess())
									callback.onItemsFetchedBackground(act, nNewGroups, null);
								else
									callback.onItemsFetchedBackground(act, nNewGroups, result.getErrorCode());
							}
						}finally{
							isFetchingGroups = false;
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
	}

	/**
	 * Return a map with the number of pictures in each group as the values, and the keys are the SERVERIDS, NOT ROWIDs
	 * @return The mapping, null if we couldn't read, the key is the serverId, and value is <RowId, nPictures, groupName>
	 */
	public HashMap<Long, ThreeObjects<Long, Integer, String>> getNPicturesInGroupsByServerId(){
		// grab all the groups
		GroupsAdapter allGroups = new GroupsAdapter(ctx);
		allGroups.fetchAllGroups();
		HashMap<Long, ThreeObjects<Long, Integer, String>> map = new HashMap<Long, ThreeObjects<Long, Integer, String>>(allGroups.size());
		
		// loop over groups counting how many pictures are in each group.
		while(allGroups.moveToNext()){
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
			
			// save in map
			long serverId = allGroups.getServerId();
			if (serverId != -1 && serverId != 0)
				map.put(serverId, new ThreeObjects<Long, Integer, String>(allGroups.getRowId(), nPictures, allGroups.getName()));		
		}
		
		return map;
	}

	/**
	 * Fetch all the groups from the server for a given user and then create for any new groups. 
	 * Done a a background thread
	 * @param <ACTIVITY_TYPE>
	 * @param act The activity that will be attached to callback on return.
	 * @param groupServerId the server id of the group in question
	 * @param indeterminateProgressBars tags to progress bars to show while posting, null if none
	 * @param callback The callback to be called when we are done. Can be null.
	 * callback 3rd return will be how many new groups there are
	 */
	public synchronized <ACTIVITY_TYPE extends CustomActivity>
	void fetchAllGroupsFromServerOld(
			ACTIVITY_TYPE act,
			ArrayList<String> indeterminateProgressBars,
			final ItemsFetchedCallback<ACTIVITY_TYPE> callback){

		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put("user_id", Prefs.getUserServerId(ctx));
			json.put("secret_code", Prefs.getSecretCode(ctx));
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return;
		}

		Utils.postToServer(
				"get_groups",
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

						// convert to custom return object
						GetGroupsServerReturn data = new GetGroupsServerReturn(result);

						// not a good return
						if (!data.isSuccess()){
							Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
							return;
						}

						// loop checking the pictures and creating in database if we don't have
						GroupsAdapter adapter = new GroupsAdapter(ctx);
						UsersAdapter users = new UsersAdapter(ctx);
						int counter = 0;
						Iterator<String> iterator = data.getMessageObject().keys();
						while (iterator.hasNext()){
							synchronized (GroupsAdapter.class) {
								long groupServerId = Long.valueOf(iterator.next());
								if (groupServerId == 0)
									continue;

								// check if group is present
								if (!adapter.isGroupPresent(groupServerId)){
									// see if the user exists, if it doesn't then add it
									long userServerId = data.getUserServerIdWhoCreated(groupServerId);
									users.fetchUserByServerId(userServerId);
									long userRowId = users.getRowId();
									if(userRowId == 0 ||userRowId == -1){
										users.close();
										userRowId = users.makeNewUser(userServerId);
									}else
										users.close();

									// create a new group
									//TODO: read allowOthersToAddMembers
									//TODO: read allow within distance
									//TODO: read lat and long
									//TODO: read picture id for group
									long groupRowId = adapter.makeNewGroup(ctx,
											data.getName(groupServerId),
											-1l,
											data.getDateCreated(groupServerId),
											userRowId,
											true, 
											null,
											null,
											-1,
											false);

									adapter.setIsSynced(groupRowId, true, groupServerId);

									counter++;
								}
							}
						}

						// notification for new groups
						if (counter > 0){
							NotificationsAdapter notes = new NotificationsAdapter(ctx);
							notes.createNotification(
									counter + " new groups you've been added to.",
									NotificationsAdapter.NOTIFICATION_TYPES.ADD_TO_NEW_GROUP, null);
						}

						// send callback back to activity
						if (callback != null){
							if (data.isSuccess())
								callback.onItemsFetchedBackground(act, counter, null);
							else
								callback.onItemsFetchedBackground(act, counter, result.getErrorCode());
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
		Cursor cursor = getAllGroupsCursor();

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
		if (cursor == null || !cursor.moveToFirst())
			return output;

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
		if (cursor == null || !cursor.moveToFirst())
			return output;

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
				null,
				false,
				null,
				null,
				-1,
				true);
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
			boolean keepLocal){

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

		// find the allowable folder name and create it
		ThreeObjects<String, String, String> folderNames = makeRequiredFolders(ctx, groupName, dateCreated);

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
		values.put(KEY_GROUP_TOP_LEVEL_PATH, folderNames.mObject1);
		values.put(KEY_PICTURE_PATH, folderNames.mObject2);
		values.put(KEY_THUMBNAIL_PATH, folderNames.mObject3);

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
		else
			return getName();
	}

	// private helper classes
	/**
	 * Return a cursor over all groups and all columns of this database
	 * @return
	 */
	private Cursor getAllGroupsCursor(){

		return database.query(TABLE_NAME, null, ADDITIONAL_QUERY_NO_AND, null, null, null, SORT_ORDER);
	}

	/**
	 * Make the required folders for this group. The top level folder, and the pictures and thumbnails
	 * @param ctx Context required
	 * @param groupName The group name
	 * @param dateCreated The date this was created
	 * @return threeObjects<String, String, STring> with top level folder, pictures folder, and thumbnails folder
	 */
	private ThreeObjects<String, String, String> makeRequiredFolders(Context ctx, String groupName, String dateCreated){

		// the name
		String folderName = getAllowableFolderName(ctx, groupName, dateCreated);

		// now make the folder
		File dir = new File(folderName);
		if(!dir.mkdirs())
			throw new IllegalAccessError("Cannot create folder " + folderName);

		// now make sub folders
		TwoStrings subFolders = generatePicturePath(folderName, groupName);
		File dir2 = new File(subFolders.mObject1);
		if(!dir2.mkdirs())
			throw new IllegalAccessError("Cannot create folder " + subFolders.mObject1);
		File dir3 = new File(subFolders.mObject2);
		if(!dir3.mkdirs())
			throw new IllegalAccessError("Cannot create folder " + subFolders.mObject2);
		// the no media file
		File nomedia = new File(dir3, ".nomedia");
		if (!nomedia.exists()){
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				throw new IllegalAccessError("Cannot create file .nomeida");
			}
		}

		ThreeObjects<String, String, String> output =
				new ThreeObjects<String, String, String>(folderName, subFolders.mObject1, subFolders.mObject2);

		// return the three folders.
		return output;
	}

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {		
	}

	/**
	 * Set the last picture number saved in this group.
	 * @param rowId the rowId of the group to update
	 * @param number
	 */
	protected boolean setLastPictureNumberD(long rowId, long number){

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
			String command = "UPDATE " + TABLE_NAME + " SET " + 
					KEY_LAST_PICTURE_NUMBER + " = " + KEY_LAST_PICTURE_NUMBER + " + 1 WHERE " + 
					KEY_ROW_ID + " =?";

			try{
				database.execSQL(command, new String[] {String.valueOf(rowId)});
			}catch(SQLException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
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
		 * The full path to the top level folder name of this group.
		 * @return
		 */
		public String getGroupFolderName() {
			return groupFolderName;
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
				incrementLastPictureNumber(rowId);

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
		 * @param groupId The groupId where we want the next picture number
		 * @return The full path to the next available picture name and thumbnail name
		 *  in the given groupId.
		 */
		public TwoStrings getNextPictureName(){
			//TODO: this could be really slow as it iterates trying to find the best picture number.

			// get the current last number
			long newNumber = getNextPictureNumber();

			// now check the picture and thumbnail to make sure they are available, if not, go up one
			String pathPicture = getPicturePath();
			String pathThumbnail = getThumbnailPath();
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
		 * Get the top path for all pictures. ie /sdcard/shareBear/folder1/pictures/
		 * @return The full path to the pictures folder
		 */
		public String getPicturePath(){
			return picturePath;
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
		/**
		 * Get the top path for all thumbnail. ie /sdcard/shareBear/folder1/thumbnails/
		 * @return The full path to the pictures folder
		 */
		public String getThumbnailPath() {
			return thumbnailPath;
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
			else
				return getName();
		}
		/**
		 * Write the folders that need to be created before writing a picture to file.
		 * @throws IOException if we can't create the folders
		 */
		public void writeFoldersIfNeeded()
				throws IOException{

			// get the folders
			String topFolder = getGroupFolderName();
			String picFolder = getPicturePath();
			String thumbFolder = getThumbnailPath();

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

		private void setUpdating(boolean isUpdating) {
			this.isUpdating = isUpdating;
		}

		private void setUserIdWhoCreated(long userIdWhoCreated) {
			this.userIdWhoCreated = userIdWhoCreated;
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
		public void onItemsFetchedBackground(ACTIVITY_TYPE act, int nNewItems, String errorCode);
		/**
		 * This is called on the UI thread when we are done getting the items. and after onItemsFetchedBackground
		 * @param act The current activity, not guaranteed to be the same activity as in onItemsFetchedBackground
		 * @param errorCode null if no error, otherwise the error message
		 */
		public void onItemsFetchedUiThread(ACTIVITY_TYPE act, String errorCode);
	}
}


