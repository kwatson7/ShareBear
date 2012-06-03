package com.instantPhotoShare.Adapters;

import java.text.ParseException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.FullSizeServerReturn;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ThumbnailServerReturn;
import com.instantPhotoShare.Utils;
import com.tools.SuccessReason;
import com.tools.TwoObjects;
import com.tools.images.ImageLoader.LoadImage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.ImageView;

public class PicturesAdapter 
extends TableAdapter<PicturesAdapter>{

	// other contants
	private static final float PICTURE_OVERSIZE = 2f;
	private static final int THUMBNAILS_TO_GRAB_AT_ONCE = 10;
	private static final int TIMEOUT_ON_THUMBNAIL = 60;

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
	private static final String KEY_IS_FULLSIZE_DOWNLOADING = "KEY_IS_FULLSIZE_DOWNLOADING"; 
	private static final String KEY_LAST_FULLSIZE_DOWNLOAD_TIME = "KEY_LAST_FULLSIZE_DOWNLOAD_TIME";
	private static final String KEY_IS_THUMBNAIL_DOWNLOADING = "KEY_IS_THUMBNAIL_DOWNLOADING"; 
	private static final String KEY_LAST_THUMBNAIL_DOWNLOAD_TIME = "KEY_LAST_THUMBNAIL_DOWNLOAD_TIME";
	private static final String KEY_LAST_UPDATE_ATTEMPT_TIME = "KEY_LAST_UPDATE_ATTEMPT_TIME";
	private static final String KEY_HAS_THUMBNAIL_DATA = "HAS_THUMBNAIL_DATA";

	// types for some of the keys
	private static final String LAST_UPDATE_ATTEMPT_TIME_TYPE = " text not null DEFAULT '1900-01-01 01:00:00'";
	private static final String IS_FULLSIZE_DOWNLOADING_TYPE = " text not null DEFAULT 'FALSE'";
	private static final String LAST_FULLSIZE_DOWNLOAD_ATTEMPT_TIME_TYPE = " text not null DEFAULT '1900-01-01 01:00:00'";
	private static final String IS_THUMBNAIL_DOWNLOADING_TYPE = " text not null DEFAULT 'FALSE'";
	private static final String LAST_THUMBNAIL_DOWNLOAD_ATTEMPT_TIME_TYPE = " text not null DEFAULT '1900-01-01 01:00:00'";
	private static final String HAS_THUMBNAIL_DATA_TYPE = " BOOLEAN not null DEFAULT 'FALSE'";

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
					+KEY_IS_FULLSIZE_DOWNLOADING + IS_FULLSIZE_DOWNLOADING_TYPE + ", "
					+KEY_LAST_FULLSIZE_DOWNLOAD_TIME + LAST_FULLSIZE_DOWNLOAD_ATTEMPT_TIME_TYPE + ", "
					+KEY_IS_THUMBNAIL_DOWNLOADING + IS_THUMBNAIL_DOWNLOADING_TYPE + ", "
					+KEY_LAST_THUMBNAIL_DOWNLOAD_TIME + LAST_THUMBNAIL_DOWNLOAD_ATTEMPT_TIME_TYPE + ", "
					+KEY_HAS_THUMBNAIL_DATA + HAS_THUMBNAIL_DATA_TYPE + ", "
					+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
					+"foreign key(" +KEY_USER_ID_TOOK +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + ")"
					+");";

	/**
	 * Initialize a pictures adapter that is needed for most operations.
	 * @param context
	 */
	public PicturesAdapter(Context context) {
		super(context);
	}

	/**
	 * Grab the bitmap from the server for a given picture. This method is synchronized because we will fetch more than 
	 * one picture at a time. When we go into this method, we will check if the picture is available locally first.
	 * Because while we were waiting to sync, it may have been downloaded. <br>
	 * *** Slow make sure to call on background thread ***
	 * @param ctx Context required to search
	 * @param pictureRowId The picture id we want to fetch
	 * @param groupRowId The groupRowId we want to limit ourselves to when fetching other pictures. Enter -1 if you only want to grab this individual picture
	 * @return The bitmap of the given picture
	 */
	public static Bitmap getThumbnailFromServer(Context ctx, long pictureRowId, long groupRowId){

		// pictureRowId -1 or 0 break
		if (pictureRowId == 0 || pictureRowId == -1){
			Log.e(Utils.LOG_TAG, "entered " + pictureRowId + " which is unallowed into getThumbnailFromServer");
			return null;
		}

		// we need a context
		if (ctx == null){
			Log.e(Utils.LOG_TAG, "entered a null context into getThumbnailFromServer");
			return null;
		}

		// grab the app context
		Context appCtx = ctx.getApplicationContext();
		ctx = null;

		// grab which picture we are talking about
		PicturesAdapter pics = new PicturesAdapter(appCtx);
		pics.fetchPicture(pictureRowId);
		if (!pics.checkCursor()){
			Log.e(Utils.LOG_TAG, "tried to download picture with a bad rowId");
			return null;
		}

		// if we are updating, then wait
		Bitmap bmp = null;
		while (pics.isDownloadingThumbnail() && bmp == null){
			synchronized (PicturesAdapter.class) {
				try {
					PicturesAdapter.class.wait(TIMEOUT_ON_THUMBNAIL*1000);
				} catch (InterruptedException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
				bmp = pics.getThumbnail();
			}
		}

		// check if we had an update while we were waiting
		if (bmp == null)
			bmp = pics.getThumbnail();
		if (bmp != null){
			pics.close();
			synchronized (PicturesAdapter.class) {
				pics.setFinishedDownloadingThumbnail(pictureRowId, true);
			}
			return bmp;
		}

		// grab the serverId
		long serverId = pics.getServerId();
		pics.close();

		// we haven't found it locally, so grab from server, but also grab 9 more pics	

		// the array to grab the thumbnails
		JSONArray array = new JSONArray();
		ArrayList <Long> rowIds = new ArrayList<Long>(10);
		if (!(serverId == -1 || serverId == 0)){
			array.put(serverId);
			rowIds.add(pictureRowId);
			pics.setIsDownloadingThumbnail(pictureRowId);
		}

		// grab the 9 other pictures
		if (groupRowId != -1){
			pics.fetchThumbnailsNeedDownloading(groupRowId, THUMBNAILS_TO_GRAB_AT_ONCE-1);

			// fill the json array
			while(pics.moveToNext()){
				long server = pics.getServerId();
				if (!(server == -1 || server == 0)){
					// check that we actually don't have data
					Bitmap tmp = pics.getThumbnail();
					if (tmp != null){
						synchronized (PicturesAdapter.class) {
							pics.setFinishedDownloadingThumbnail(pics.getRowId(), true);
						}
						continue;
					}
					
					// fill the array
					array.put(server);
					rowIds.add(pics.getRowId());
					synchronized (PicturesAdapter.class) {
						pics.setIsDownloadingThumbnail(pics.getRowId());
					}
				}
			}
			pics.close();
		}

		// empty array break
		if (array.length() == 0)
			return null;

		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put("user_id", Prefs.getUserServerId(appCtx));
			json.put("secret_code", Prefs.getSecretCode(appCtx));
			json.put("thumbnail_ids", array);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}

		// post to the server
		ThumbnailServerReturn result = new ThumbnailServerReturn(Utils.postToServer("get_thumbnails", json, null, null));

		// check success
		if (!result.isSuccess()){
			Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
			return null;
		}

		// loop over return grabbing thumbnail data
		for (int i = 0; i < array.length(); i++){
			// grab the thumbnail data
			byte[] data;
			try {
				data = result.getThumbnailBytes(array.getLong(i));
			} catch (JSONException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				synchronized (PicturesAdapter.class) {
					pics.setFinishedDownloadingThumbnail(rowIds.get(i), false);
				}
				continue;
			}

			// store the important one
			try {
				if (data != null && data.length != 0 && array.getLong(i) == serverId)
					bmp = com.tools.images.ImageLoader.getThumbnail(data, 0);
			} catch (JSONException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				continue;
			}

			// store to file
			pics.fetchPicture(rowIds.get(i));

			// determine wehre to save it
			String thumbPath = pics.getThumbnailPath();

			// write to file
			if (thumbPath != null && thumbPath.length() != 0){
				SuccessReason result2 = 
						com.tools.Tools.saveByteDataToFile(
								appCtx,
								data,
								"",
								false,
								thumbPath,
								ExifInterface.ORIENTATION_NORMAL,
								false);
				if (result2.getSuccess())
					synchronized (PicturesAdapter.class) {
						pics.setFinishedDownloadingThumbnail(rowIds.get(i), true);
					}
				else
					synchronized (PicturesAdapter.class) {
						pics.setFinishedDownloadingThumbnail(rowIds.get(i), false);
					}
			}else
				synchronized (PicturesAdapter.class) {
					pics.setFinishedDownloadingThumbnail(rowIds.get(i), false);
				}
			pics.close();

			// notify anyone waiting that we are done saveing a file
			synchronized (PicturesAdapter.class) {
				PicturesAdapter.class.notifyAll();
			}
		}

		// return the data
		return bmp;
	}

	/**
	 * Image loader for this picture class
	 * @param activityContext The context required to load images
	 * @return The callback for use in ImageLoader
	 */
	public static LoadImage<TwoObjects<Long, Long>, TwoObjects<Long, Long>> imageLoaderCallback(Context activityContext){
		final Context ctx = activityContext.getApplicationContext();

		return new LoadImage<TwoObjects<Long, Long>, TwoObjects<Long, Long>>() {

			@Override
			public void createThumbnailFromFull(
					TwoObjects<Long, Long> thumbnailData, TwoObjects<Long, Long> fullSizeData) {

				// grab which picture
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(thumbnailData.mObject1);

				// resize
				if(com.tools.images.ImageLoader.createThumbnailFromFull(
						pics.getThumbnailPath(),
						pics.getFullPicturePath(),
						Utils.MAX_THUMBNAIL_DIMENSION,
						Utils.FORCE_BASE2_THUMBNAIL_RESIZE,
						Utils.IMAGE_QUALITY))
					pics.setFinishedDownloadingThumbnail(thumbnailData.mObject1, true);

				// close it
				pics.close();
			}

			@Override
			public Bitmap onFullSizeLocal(
					TwoObjects<Long, Long> fullSizeData,
					int desiredWidth,
					int desiredHeight) {

				// no data
				if (fullSizeData == null)
					return null;

				// figure out the correct path
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(fullSizeData.mObject1);
				String path = pics.getFullPicturePath();
				pics.close();

				// read the data from the path
				return com.tools.images.ImageLoader.getFullImage(path,
						(int)(desiredWidth*PICTURE_OVERSIZE),
						(int)(desiredHeight*PICTURE_OVERSIZE));
			}

			@Override
			public Bitmap onFullSizeWeb(
					TwoObjects<Long, Long> fullSizeData,
					int desiredWidth,
					int desiredHeight) {

				// grab the picture from the server
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(fullSizeData.mObject1);
				if (pics.getServerId() == 0 || pics.getServerId() == -1){
					pics.close();
					return null;
				}
				byte[] data = pics.getFullImageServer();
				pics.close();

				// resize the data and rotate
				return com.tools.images.ImageLoader.getFullImage(
						data,
						0,
						(int)(desiredWidth*PICTURE_OVERSIZE),
						(int)(desiredHeight*PICTURE_OVERSIZE));
			}

			@Override
			public Bitmap onThumbnailLocal(TwoObjects<Long, Long> thumbnailData) {
				if (thumbnailData == null)
					return null;
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(thumbnailData.mObject1);
				Bitmap bmp = pics.getThumbnail();
				pics.close();
				return bmp;
			}

			@Override
			public Bitmap onThumbnailWeb(TwoObjects<Long, Long> thumbnailData) {
				return PicturesAdapter.getThumbnailFromServer(ctx, thumbnailData.mObject1, thumbnailData.mObject2);
			}
		};
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
		if (oldVersion < 5 && newVersion >= 5){
			String upgradeQuery = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_IS_FULLSIZE_DOWNLOADING + " "+
							IS_FULLSIZE_DOWNLOADING_TYPE;
			out.add(upgradeQuery);
			String upgradeQuery2 = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_LAST_FULLSIZE_DOWNLOAD_TIME + " "+
							LAST_FULLSIZE_DOWNLOAD_ATTEMPT_TIME_TYPE;
			out.add(upgradeQuery2);
		}
		if (oldVersion < 6 && newVersion >= 6){
			String upgradeQuery = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_IS_THUMBNAIL_DOWNLOADING + " "+
							IS_THUMBNAIL_DOWNLOADING_TYPE;
			out.add(upgradeQuery);
			String upgradeQuery2 = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_LAST_THUMBNAIL_DOWNLOAD_TIME + " "+
							LAST_THUMBNAIL_DOWNLOAD_ATTEMPT_TIME_TYPE;
			out.add(upgradeQuery2);
			String upgradeQuery3 = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_HAS_THUMBNAIL_DATA + " "+
							HAS_THUMBNAIL_DATA_TYPE;
			out.add(upgradeQuery3);
		}

		return out;
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

	/**
	 * Is the given picture based on the serverId present in the database
	 * @param serverId is the picture present
	 * @return
	 */
	public boolean isPicturePresent(long serverId){
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
	
	/**
	 * Set that we are done downloading thumbnail data for this picture
	 * @param rowId the rowId of the picture
	 * @param didWeReceiveData If we successfully received data.
	 */
	public void setFinishedDownloadingThumbnail(long rowId, boolean didWeReceiveData){

		// the values
		ContentValues values = new ContentValues();
		values.put(KEY_IS_THUMBNAIL_DOWNLOADING, false);	
		if (didWeReceiveData)
			values.put(KEY_HAS_THUMBNAIL_DATA, true);

		// update the values to the table
		if (database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) <=0)
			Log.e(Utils.LOG_TAG, "setFinishedDownloadingThumbnail did not update properly");
	}

	/**
	 * If we are downlaiding thumbnail from the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * If isDownlading is true, then we know we are not synced, so we will set sync to false as well.
	 * @param rowId the rowId of the picture to update.
	 */
	public void setIsDownloadingThumbnail(long rowId){

		// the values
		ContentValues values = new ContentValues();
		values.put(KEY_IS_THUMBNAIL_DOWNLOADING, true);	
		values.put(KEY_HAS_THUMBNAIL_DATA, false);
		values.put(KEY_LAST_THUMBNAIL_DOWNLOAD_TIME, Utils.getNowTime());

		// update the values to the table
		if (database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) <=0)
			Log.e(Utils.LOG_TAG, "setDownloadingThumbnail did not update properly");
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
	 * @param hasThumbnailData if there is thumbnail data available locally
	 * @return rowId or -1 if failed
	 */
	public long createPicture(
			Context ctx,
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			Long idWhoTook, 
			Double latitude,
			Double longitude,
			boolean hasThumbnailData){	

		// override some values and/or check
		if (dateTaken == null || dateTaken.length() == 0)
			dateTaken = Utils.getNowTime();
		if (idWhoTook == null)
			idWhoTook = Prefs.getUserRowId(ctx);

		// all the values
		ContentValues values = new ContentValues();
		values.put(KEY_PATH, path);
		values.put(KEY_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_DATE_TAKEN, dateTaken);
		values.put(KEY_USER_ID_TOOK, idWhoTook);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LONGITUE, longitude);
		values.put(KEY_HAS_THUMBNAIL_DATA, hasThumbnailData);

		// insert the values
		return database.insert(TABLE_NAME, null, values);
	}

	/**
	 * Fetch the picture with the given rowId. <br>
	 * @param rowId The rowId of the picture
	 */
	public void fetchPicture(long rowId){
		setCursor(fetchPicturePrivate(rowId));
		moveToFirst();
	}	

	/**
	 * Fetch the picture with the given serverId. <br>
	 * *** Make sure to close cursor when finished with closeCursor ***
	 * @param serverId The serverId of the picture
	 */
	public void fetchPictureFromServerId(long serverId){
		Cursor cursor =

				database.query(
						true,
						TABLE_NAME,
						null,
						KEY_SERVER_ID + "=?",
						new String[] {String.valueOf(serverId)},
						null,
						null,
						SORT_ORDER,
						null);

		setCursor(cursor);
		moveToFirst();
	}

	/**
	 * Load into the cursor 
	 * @param groupId The rowId of the groupd we search on
	 */
	public void fetchPicturesInGroup(long groupId){
		setCursor(fetchPicturesInGroupPrivate(groupId));
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
	 * Fetch the pictures that need thumbnails downloaded and aren't currently downloading from the given group
	 * @param groupId The group to focus on when grabbing pictures
	 * @param howManyPics how many pictures to grab, use -1 to grab all
	 */
	public void fetchThumbnailsNeedDownloading(long groupId, int howManyPics){

		// GRAB current time
		String timeAgo = Utils.getTimeSecondsAgo(TIMEOUT_ON_THUMBNAIL);

		// build the where clause
		String where = 
				"(UPPER(pics." + KEY_HAS_THUMBNAIL_DATA + ") = 'FALSE' OR pics." + KEY_HAS_THUMBNAIL_DATA + " = '0')" 
						+ " AND "
						+ "((UPPER(pics." + KEY_IS_THUMBNAIL_DOWNLOADING + ") = 'FALSE' OR pics." + KEY_IS_THUMBNAIL_DOWNLOADING + " = '0')"	
						+ " OR "
						+ "(Datetime(pics." + KEY_LAST_THUMBNAIL_DOWNLOAD_TIME + ") < Datetime('" + timeAgo + "')))"; 

		// create the query where we match up all the pictures that are in the group
		String query = 
				"SELECT pics.* FROM "
						+PicturesAdapter.TABLE_NAME + " pics "
						+" INNER JOIN "
						+PicturesInGroupsAdapter.TABLE_NAME + " joinner "
						+" ON "
						+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
						+"joinner." + PicturesInGroupsAdapter.KEY_PICTURE_ID
						+" WHERE "
						+"joinner." + PicturesInGroupsAdapter.KEY_GROUP_ID
						+"=?"
						+" AND "
						+where
						+" ORDER BY " + SORT_ORDER;

		// the number to grab
		String limit = null;
		if (howManyPics == -1)
			limit = null;
		else
			limit = String.valueOf(howManyPics);
		if (limit != null)
			query += " LIMIT " + limit;

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// set cursor
		setCursor(cursor);
	}

	/**
	 * Return the date taken as a string, or "" if not accessible.
	 * @return
	 */
	public String getDateTaken(){
		if (!checkCursor())
			return "";
		else{
			return getString(KEY_DATE_TAKEN);
		}
	}

	/**
	 * Grab the full image data from the server and save it to file. <br>
	 * *** This is slow, so perform on background thread ***
	 * @return The image data
	 */
	public byte[] getFullImageServer(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getFullImageServer on a bad cursor");
			return null;
		}

		// grab the server Id
		long serverId = getServerId();
		if (serverId == -1){
			Log.e(Utils.LOG_TAG, "tried to get image from server for picture with no serverId");
			return null;
		}

		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put("user_id", Prefs.getUserServerId(ctx));
			json.put("secret_code", Prefs.getSecretCode(ctx));
			json.put("image_id", serverId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}

		// post to the server
		FullSizeServerReturn result = new FullSizeServerReturn(Utils.postToServer("get_fullsize", json, null, null));

		// check success
		if (!result.isSuccess()){
			Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
			return null;
		}

		// grab the image data
		byte[] data = result.getImageBytes();

		// no data, return
		if (data == null || data.length == 0)
			return null;

		// determine where to save it
		String fullPath = getFullPicturePath();

		// write to file
		com.tools.Tools.saveByteDataToFile(
				ctx,
				data,
				"",
				false,
				fullPath,
				ExifInterface.ORIENTATION_NORMAL,
				false);

		// return the data
		return data;
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
			return getString(KEY_PATH);
		}
	}
	
	/**
	 * Get formatted last update time. or "1900-01-01 01:00:00" if not accessible
	 * @return
	 */
	public String getLastUpdateAttemptTime(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getLastUpdateAttemptTime on a bad cursor");
			return "1900-01-01 01:00:00";
		}
		return getString(KEY_LAST_UPDATE_ATTEMPT_TIME);
	}
	
	/**
	 * Return the rowId of the current row, or return -1 if not accessible.
	 * @return
	 */
	public long getRowId(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getRowId on a bad cursor");
			return -1;
		}else
			return getLong(KEY_ROW_ID);
	}
	
	/**
	 * Get the server Id of this picture, -1 if none.
	 * @return
	 */
	public long getServerId(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getServerId on a bad cursor");
			return -1;
		}else
			return getLong(KEY_SERVER_ID);
	}
	
	/**
	 * Get the bitmap for the thumbnnail. null if there is none.
	 * @return
	 */
	public Bitmap getThumbnail(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getThumbnail on a bad cursor");
			return null;
		}

		// read the path
		Bitmap out = null;
		String path = getThumbnailPath();
		if (path.length() == 0)
			return out;

		// decode the file
		return BitmapFactory.decodeFile(path);
	}

	/**
	 * Return the path to the picture thumbnail, "" if there is none or cursor cannot be read. <br>
	 * @see getThumbnail
	 * @see setThumbnail
	 * @return
	 */
	public String getThumbnailPath(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called getThumbnailPath on a bad cursor");
			return "";
		}else{
			return getString(KEY_THUMBNAIL_PATH);
		}
	}	

	/**
	 * If we are downloading the thumbnail and we haven't timedout, then return true, else false
	 * @return
	 */
	public boolean isDownloadingThumbnail(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called isDownloadingThumbnail on a bad cursor");
			return false;
		}
		
		boolean isDownloading = getBoolean(KEY_IS_THUMBNAIL_DOWNLOADING);
		boolean isTimeout;
		try {
			isTimeout = Utils.parseMilliseconds(Utils.getNowTime()) - 
					Utils.parseMilliseconds(getString(KEY_LAST_THUMBNAIL_DOWNLOAD_TIME))
					> 1000*TIMEOUT_ON_THUMBNAIL;
		}catch (ParseException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return false;
		}

		// we must be downloading and not timeout
		return (isDownloading && !isTimeout);
	}

	

	/**
	 * Determine if we are currently updating by looking at database. If last update time was too long ago, then we are no longer updating.
	 * @return
	 */
	public boolean isUpdating() {
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called isUpdating on a bad cursor");
			return false;
		}
		
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
			return getBoolean(KEY_IS_UPDATING);
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
	 * Return a Cursor positioned at the picture that matches the given rowId
	 * @param rowId id of picture to retrieve
	 * @return Cursor positioned to matching picture, if found
	 */
	private Cursor fetchPicturePrivate(long rowId){

		Cursor cursor =

				database.query(
						true,
						TABLE_NAME,
						null,
						KEY_ROW_ID + "=?",
						new String[] {String.valueOf(rowId)},
						null,
						null,
						SORT_ORDER,
						null);

		return cursor;
	}

	/**
	 * Get a cursor for all the pictures that are linked to the group groupId
	 * in PicturesInGroup table
	 * @param groupId The rowId of the group in question.
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
	 * Delete the picture with the given rowId from the database
	 * 
	 * @param rowId id of picture to delete
	 * @return true if deleted, false otherwise
	 */
	private boolean removePictureFromDatabase(long rowId) {

		return database.delete(
				TABLE_NAME,
				KEY_ROW_ID + "=?",
				new String[] {String.valueOf(rowId)}) > 0;
	}
}
