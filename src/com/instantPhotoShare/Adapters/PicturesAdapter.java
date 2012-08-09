package com.instantPhotoShare.Adapters;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ServerKeys;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.ThumbnailServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.tools.CustomActivity;
import com.tools.ServerPost.ServerReturn;
import com.tools.SuccessReason;
import com.tools.TwoObjects;
import com.tools.images.ImageLoader.LoadImage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class PicturesAdapter 
extends TableAdapter<PicturesAdapter>{

	// other contants
	private static final float PICTURE_OVERSIZE = 2f;
	private static final int THUMBNAILS_TO_GRAB_AT_ONCE = 1;
	private static final int TIMEOUT_ON_THUMBNAIL = 10;
	private static final int TIMEOUT_ON_FULLSIZE = 90;

	/** Table name */
	public static final String TABLE_NAME = "pictureInfo";

	// picture table keys
	public static final String KEY_ROW_ID = "_id";
	private static final String KEY_SERVER_ID = "serverId";
	private static final String KEY_PATH = "picturePath";
	private static final String KEY_THUMBNAIL_PATH = "pictureThumbnailPath";	
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
	private static String SORT_ORDER = 
		KEY_SERVER_ID + " DESC, "
		+ KEY_ROW_ID + " DESC"; 			// sort the picture by most recent

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
	 * Grab the full image data from the server and save it to file. <br>
	 * *** This is slow, so perform on background thread ***
	 * @context Context required to do the searching
	 * @pictureRowId the row id we want to get from the server
	 * @desiredWidth desiredWidth the desired width of the output image
	 * @desiredHeight The desired height of the output image
	 * @groupRowId the group row Id this picture is in
	 * @progressBar the progress bar we want to update as we download, null if none
	 * @return The image data
	 */
	public static Bitmap getFullImageServer(Context context, long pictureRowId, int desiredWidth, int desiredHeight, long groupRowId, ProgressBar progressBar){
		//TODO: this and getThumbnailImageServer are both really bad - need better sync logic. Don't know how to do it at the moment.

		WeakReference<ProgressBar> weakProgess = new WeakReference<ProgressBar>(progressBar);
		progressBar = null;

		// grab the app context and make a picture object
		Context appCtx = context.getApplicationContext();
		context = null;
		PicturesAdapter pic = new PicturesAdapter(appCtx);
		pic.fetchPicture(pictureRowId);

		// make sure we have a good cursor
		if (!pic.checkCursor()){
			Log.e(Utils.LOG_TAG, "called getFullImageServer on a bad cursor");
			return null;
		}		

		// check that we're not currently downloading - wait if so
		Bitmap bmp = null;
		while (pic.isDownloadingFullsize() && bmp == null){
			synchronized (PicturesAdapter.class) {
				try {
					PicturesAdapter.class.wait(TIMEOUT_ON_FULLSIZE*1000);
				} catch (InterruptedException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
				bmp = pic.getFullImage(desiredWidth, desiredHeight);
			}
			if (bmp == null)
				pic.fetchPicture(pictureRowId);
		}

		// check if we had an update while we were waiting
		if (bmp == null)
			bmp = pic.getFullImage(desiredWidth, desiredHeight);
		if (bmp != null){
			pic.close();
			pic.setFinishedDownloadingFullsize(pictureRowId);
			return bmp;
		}

		// grab the server Id and path
		long serverId = pic.getServerId();
		String path = pic.getFullPicturePath();
		pic.close();
		long groupServerId = (new GroupsAdapter(appCtx)).getGroup(groupRowId).getServerId();
		if (serverId == -1 || groupRowId == -1 || groupServerId == -1 || serverId == 0 || groupRowId == 0 || groupServerId == 0){
			Log.e(Utils.LOG_TAG, "tried to get image url from server for picture with no serverId and/or group with no");
			return null;
		}

		synchronized (PicturesAdapter.class) {
			pic.setIsDownloadingFullsize(pictureRowId);
		}

		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.GetFullSize.POST_KEY_USER_ID, Prefs.getUserServerId(appCtx));
			json.put(ServerKeys.GetFullSize.POST_KEY_SECRET_CODE, Prefs.getSecretCode(appCtx));
			json.put(ServerKeys.GetFullSize.POST_KEY_IMAGE_ID, serverId);
			json.put(ServerKeys.GetFullSize.POST_KEY_GROUP_ID, groupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			pic.setFinishedDownloadingFullsize(pictureRowId);
			return null;
		}

		// write the required folders
		Group group = (new GroupsAdapter(appCtx)).getGroup(groupRowId);
		try {
			group.writeFoldersIfNeeded();
		} catch (IOException e1) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
			pic.setFinishedDownloadingFullsize(pictureRowId);
			return null;
		}

		// post to the server
		ShareBearServerReturn result = Utils.postToServerToGetFile(ServerKeys.GetFullSize.COMMAND, json.toString(), path, weakProgess.get());
		if (!result.isSuccess()){
			Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
			pic.setFinishedDownloadingFullsize(pictureRowId);
			return null;
		}else{
			pic.setFinishedDownloadingFullsize(pictureRowId);
		}

		// read the file
		pic.fetchPicture(pictureRowId);
		bmp =  pic.getFullImage(desiredWidth, desiredHeight);
		pic.close();
		return bmp;
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
			pics.close();
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
			pics.setFinishedDownloadingThumbnail(pictureRowId, true);
			return bmp;
		}

		// grab the serverId
		long serverIdToFetch = pics.getServerId();
		pics.close();

		// we haven't found it locally, so grab from server

		// the array to grab the thumbnails
		JSONArray array = new JSONArray();
		ArrayList <Long> rowIds = new ArrayList<Long>(10);
		if (!(serverIdToFetch == -1 || serverIdToFetch == 0)){
			array.put(serverIdToFetch);
			rowIds.add(pictureRowId);
			pics.setIsDownloadingThumbnail(pictureRowId);
		}

		// empty array break
		if (array.length() == 0){
			Log.e(Utils.LOG_TAG, "no values in array, this shouldn't happen for pic " + pictureRowId);
			pics.setFinishedDownloadingThumbnail(pictureRowId, false);
			return null;
		}

		// grab the group serverId
		long groupServerId = -1;
		if (groupRowId != -1){
			GroupsAdapter groups = new GroupsAdapter(appCtx);
			Group group = groups.getGroup(groupRowId);
			groupServerId = group.getServerId();
		}else{
			GroupsAdapter groups = new GroupsAdapter(appCtx);
			groups.fetchGroupsContainPicture(pictureRowId);
			while(groupServerId == -1 && groups.moveToNext()){
				groupServerId = groups.getServerId();
				groupRowId = groups.getRowId();
			}
			groups.close();
		}

		// no group, can't get picture
		if (groupServerId == -1){
			Log.e(Utils.LOG_TAG, "bad group serverId, so we can't grab the thumbnail");
			pics.setFinishedDownloadingThumbnail(pictureRowId, false);
			return null;
		}

		// write the required folders
		Group group = (new GroupsAdapter(appCtx)).getGroup(groupRowId);
		try {
			group.writeFoldersIfNeeded();
		} catch (IOException e1) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
			pics.setFinishedDownloadingThumbnail(pictureRowId, false);
			return null;
		}

		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.GetThumbnails.POST_KEY_USER_ID, Prefs.getUserServerId(appCtx));
			json.put(ServerKeys.GetThumbnails.POST_KEY_SECRET_CODE, Prefs.getSecretCode(appCtx));
			json.put(ServerKeys.GetThumbnails.POST_KEY_THUMBNAIL_IDS, array);
			json.put(ServerKeys.GetThumbnails.POST_KEY_GROUP_ID, groupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			pics.setFinishedDownloadingThumbnail(pictureRowId, false);
			return null;
		}

		// post to the server
		ThumbnailServerReturn result = new ThumbnailServerReturn(Utils.postToServer(
				ServerKeys.GetThumbnails.COMMAND, json, null, null));

		// check success
		if (!result.isSuccess()){
			Log.e(Utils.LOG_TAG, result.getDetailErrorMessage());
			pics.setFinishedDownloadingThumbnail(pictureRowId, false);
			return null;
		}

		// loop over return grabbing thumbnail data
		for (int iii = 0; iii < result.getNItems(); iii++){

			// grab the thumbnail data
			byte[] data = result.getThumbnailBytes(iii);
			
			// grab the serverid of the picture
			long pictureServerId = result.getPictureServerId(iii);

			// store the important one
			//TODO: if the picture data contains exif info, this process will not work correctly.
			if (data != null && data.length != 0 && pictureServerId == serverIdToFetch)
				bmp = com.tools.ImageProcessing.getThumbnail(data, 0);

			// move the adapter to the correct row
			pics.fetchPictureFromServerId(pictureServerId);
			
			// determine where to save it
			long currentPictureRowId = pics.getRowId();
			if(currentPictureRowId <= 0){
				Log.e(Utils.LOG_TAG, "picture with serverId: "+pictureServerId + " is not available");
				for (int ii = 0; ii < rowIds.size(); ii++)
					pics.setFinishedDownloadingThumbnail(rowIds.get(ii), false);
				pics.close();
				continue;
			}
			String thumbPath = pics.getThumbnailPath();
			pics.close();

			// write to file
			//TODO: if the byte data has exif data, this will not work correctly.
			if (thumbPath != null && thumbPath.length() != 0){
				SuccessReason result2 = 
					com.tools.ImageProcessing.saveByteDataToFile(
							appCtx,
							data,
							false,
							thumbPath,
							ExifInterface.ORIENTATION_NORMAL,
							false);

				// find the user who took this picture
				UsersAdapter users = new UsersAdapter(appCtx);
				users.fetchUserByServerId(result.getUserServerIdWhoCreated(iii));
				long userRowId = users.getRowId();

				// unknown user, so create him.
				if (userRowId <= 0){
					users.close();
					userRowId = users.makeNewUser(result.getUserServerIdWhoCreated(iii));
				}

				// update the picture in the database
				pics.updatePicture(
						currentPictureRowId,
						userRowId,
						result.getDateCreated(iii));

				if (result2.getSuccess())
					pics.setFinishedDownloadingThumbnail(currentPictureRowId, true);
				else
					pics.setFinishedDownloadingThumbnail(currentPictureRowId, false);
			}else
				pics.setFinishedDownloadingThumbnail(currentPictureRowId, false);
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
	 * Move a picture from one group to another both on the server and locally
	 * @param <ACTIVITY_TYPE>
	 * @param indeterminateProgressBarTags progress bar tags to update while posting. can be null
	 * @param act The activity required to call this post. can be null
	 * @param sourceGroupRowId the rowId of the group we are starting from
	 * @param destinationGroupRowId the rowId of the group we are copying to
	 * @param callback teh callback when finished. can be null
	 * @throws Exception if we could not upload request to server
	 */
	public <ACTIVITY_TYPE extends CustomActivity> void addPictureToGroup(
			ArrayList<String> indeterminateProgressBarTags,
			ACTIVITY_TYPE act,
			final long sourceGroupRowId,
			final long destinationGroupRowId,
			final ItemsFinished<ACTIVITY_TYPE> callback) throws Exception{
		
		// show toast
		if (act != null)
			Toast.makeText(act, "Copying picture...", Toast.LENGTH_SHORT).show();
		
		// get some data 
		final long rowId = getRowId();
		long serverId = getServerId();
		GroupsAdapter groups = new GroupsAdapter(ctx);
		groups.fetchGroup(sourceGroupRowId);
		long sourceGroupServerId = groups.getServerId();
		boolean isSourceLocal = groups.isKeepLocal();
		groups.fetchGroup(destinationGroupRowId);
		long destinationGroupServerId = groups.getServerId();
		boolean isDestinationLocal = groups.isKeepLocal();
		groups.close();
		
		// check good data
		if (rowId <= 0 || sourceGroupRowId <= 0 || destinationGroupRowId <= 0)
			throw new Exception("acces to group or picture that don't exist");
		
		// check if both local
		if (isSourceLocal && isDestinationLocal){
			addPictureToGroupLocally(sourceGroupRowId, destinationGroupRowId);
			if (callback != null)
				callback.onItemsFinishedUI(act, null);
			return;
		}
		
		// source local, destination public
		if (isSourceLocal && !isDestinationLocal){
			//TODO: make it possible to coppy private picture to public groups
			throw new Exception("cannot currently copy pictures from private group to public group");
		}
		
		// source public, destination private
		if (!isSourceLocal && isDestinationLocal){
			addPictureToGroupLocally(sourceGroupRowId, destinationGroupRowId);
			if (callback != null)
				callback.onItemsFinishedUI(act,null);
			return;
		}
		
		// bad row access
		if (serverId <= 0 || sourceGroupServerId <= 0 || destinationGroupServerId <= 0)
			throw new Exception("Pictures or groups are not on server yet");
		
		// first add on server
		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.CopyImage.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.CopyImage.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
			json.put(ServerKeys.CopyImage.POST_KEY_IMAGE_ID, serverId);
			json.put(ServerKeys.CopyImage.POST_KEY_SOURCE_GROUP_ID, sourceGroupServerId);
			json.put(ServerKeys.CopyImage.POST_KEY_NEW_GROUP_ID, destinationGroupServerId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			throw new Exception("bad json data");
		}
		
		// post to server
		Utils.postToServer(
				ServerKeys.CopyImage.COMMAND,
				json.toString(),
				null,
				null,
				null,
				act,
				indeterminateProgressBarTags,
				new com.tools.ServerPost.PostCallback<ACTIVITY_TYPE>(){

					@Override
					public void onPostFinished(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						
						// convert return
						ShareBearServerReturn data = new ShareBearServerReturn(result);
						
						// check if error
						if (!data.isSuccess()){
							if (callback != null)
								callback.onItemsFinishedBackground(act, new Exception(data.getDetailErrorMessage()));
							return;
						}
						
						// success - copy on local database
						PicturesAdapter pics = new PicturesAdapter(ctx);
						pics.fetchPicture(rowId);
						try {
							pics.addPictureToGroupLocally(sourceGroupRowId, destinationGroupRowId);
						} catch (Exception e) {
							Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
							if (callback != null)
								callback.onItemsFinishedBackground(act, e);
							pics.close();
							return;
						}
						pics.close();
						
						// user supplied callback
						if (callback != null)
							callback.onItemsFinishedBackground(act, null);
					}

					@Override
					public void onPostFinishedUiThread(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						
						// convert return
						ShareBearServerReturn data = new ShareBearServerReturn(result);
						
						// user supplied callback
						// check if error
						if (!data.isSuccess()){
							if (callback != null)
								callback.onItemsFinishedUI(act, new Exception(data.getDetailErrorMessage()));
						}else if (callback != null)
							callback.onItemsFinishedUI(act, null);
					}
					
				});
	}
	
	/**
	 * Add the current picture to the new group locally
	 * @param sourceGroupId The original group, the picture must be in
	 * @param destinationGroupId The final group the picture must be in
	 * @throws Exception if we could not add the picture to the new group
	 */
	private void addPictureToGroupLocally(long sourceGroupId, long destinationGroupId) throws Exception{
		// grab picture
		long picRowId = getRowId();
		if (picRowId <= 0)
			throw new Exception("picture doesn't exist");
		
		// make sure picture is in group
		if(!isPictureInGroup(picRowId, sourceGroupId))
			throw new Exception("Picture is not in this group");
		
		// make sure not already in new group
		if (isPictureInGroup(picRowId, destinationGroupId))
			throw new Exception("Picture already in group");
		
		// add the picture
		PicturesInGroupsAdapter inGroups = new PicturesInGroupsAdapter(ctx);
		long linkRow = inGroups.addPictureToGroup(picRowId, destinationGroupId);
		if (linkRow == -1){
			String msg = "link between picture and group could be made for unknown reason";
			Log.e(Utils.LOG_TAG, msg);
			throw new Exception("Picture could not be added to group for unknown reason");
		}
	}

	public interface ThumbnailListener{
		/**
		 * When the thumbnail is done being downloaded
		 * @param pictureId the pictureId that was downloaded
		 */
		public void onThumbNailDownloaded(long pictureId);
	}
	/**
	 * Image loader for this picture class
	 * @param activityContext The context required to load images
	 * @param thumbailCallback call this when the thumbnail is done downloading. Can be null
	 * @return The callback for use in ImageLoader
	 */
	public static LoadImage<TwoObjects<Long, Long>, TwoObjects<Long, Long>> imageLoaderCallback(
			Context activityContext,
			final ThumbnailListener thumbailCallback){
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

				// read the data from the path
				Bitmap bmp = pics.getFullImage(desiredWidth, desiredHeight);
				pics.close();
				return bmp;
			}

			@Override
			public Bitmap onFullSizeWeb(
					TwoObjects<Long, Long> fullSizeData,
					int desiredWidth,
					int desiredHeight,
					WeakReference<ProgressBar> weakProgress) {

				// grab the picture from the server
				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(fullSizeData.mObject1);
				if (pics.getServerId() == 0 || pics.getServerId() == -1){
					pics.close();
					return null;
				}
				Bitmap bmp = getFullImageServer(ctx, fullSizeData.mObject1, desiredWidth, desiredHeight, fullSizeData.mObject2, weakProgress.get());
				pics.close();
				return bmp;
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
				Bitmap bmp = PicturesAdapter.getThumbnailFromServer(ctx, thumbnailData.mObject1, thumbnailData.mObject2);
				if (thumbailCallback != null)
					thumbailCallback.onThumbNailDownloaded(thumbnailData.mObject1);
				return bmp;
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
	 * Fetch the pictures with the given user id took. <br>
	 * @param rowId The rowId of the user
	 */
	public void fetchPicturesByUser(long userRowId){
		
		final String where = KEY_USER_ID_TOOK + " = ?";
		Cursor cursor = database.query(
				TABLE_NAME,
				null,
				where,
				new String[] {String.valueOf(userRowId)},
				null, null, SORT_ORDER);

		setCursor(cursor);
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
	 * Fetch the pictures that still need to be uploaded to the server for a given group.
	 * @param groupRowId
	 */
	public void fetchPicturesNeedUploading(long groupRowId){
		// pictures that haven't been synced but need to be have the following
		// serverId == 0, -1, or null
		// part of a group that has keepLocal = 0
		// isUpdating = 0
		// last_update is old
		// isSynced = 0
		// KEY_HAS_THUMBNAIL_DATA is true

		// first find the group and make sure it isn't keep local
		GroupsAdapter groups = new GroupsAdapter(ctx);
		groups.fetchGroup(groupRowId);
		if (groups.getRowId() == -1 || groups.getRowId() == 0 || groups.isKeepLocal()){
			groups.close();
			return;
		}

		// now we query the pictures in this group that match our criteria

		// GRAB current time
		String timeAgo = Utils.getTimeSecondsAgo((int) Utils.SECONDS_SINCE_UPDATE_RESET);

		// build the where clause
		String where = "(pics." + KEY_SERVER_ID + " =? OR pics." + KEY_SERVER_ID + " =? OR pics." + KEY_SERVER_ID + " IS NULL ) AND " +
		"((pics." + KEY_IS_UPDATING + " =? OR UPPER(pics." + KEY_IS_UPDATING + ") =?) OR " + 
		"(Datetime(" + KEY_LAST_UPDATE_ATTEMPT_TIME + ") < Datetime('" + timeAgo + "'))) AND " +
		"(pics." + KEY_IS_SYNCED + " =? OR UPPER(pics." + KEY_IS_SYNCED + ") =?)";// AND " +
		//"(" + KEY_HAS_THUMBNAIL_DATA + " =? OR UPPER(" + KEY_HAS_THUMBNAIL_DATA + ") =?)";

		//TODO: we don't require that we have thumbnail data, for backwards compatability, but this should be turned back on soon

		// sort backwards order
		final String sortOrder = 
			KEY_SERVER_ID + " ASC, "
			+ KEY_ROW_ID + " ASC";

		// where args
		String[] whereArgs = {String.valueOf(groupRowId),
				String.valueOf(-1), String.valueOf(0),
				String.valueOf(0), "FALSE",
				String.valueOf(0), "FALSE"};//,
		//String.valueOf(1), "TRUE"};

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
			+" ORDER BY " + sortOrder;

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				whereArgs);

		// set cursor
		setCursor(cursor);
	}

	/**
	 * Fetch random pictures. <br>
	 * If nPictures <= 1, then we start of positioned at the first (and only)
	 * item. If nPictures > 1, then we are positioned before the first item, and moveToFirst()
	 * or moveToNext() must be called.
	 * @param nPictures Number of pictures to fetch.
	 */
	public void fetchRandomPicture(int nPictures){
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
	}
	
	/**
	 * Fetch random pictures. <br>
	 * If nPictures <= 1, then we start of positioned at the first (and only)
	 * item. If nPictures > 1, then we are positioned before the first item, and moveToFirst()
	 * or moveToNext() must be called.
	 * @param nPictures Number of pictures to fetch.
	 */
	public void fetchNewestPictures(int nPictures){
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
		if (selection.mObject1.length() == 0){
			setCursor(null);
			return;
		}
			
		/*
		String query = 
			"SELECT DISTINCT pics.* FROM "
			+PicturesAdapter.TABLE_NAME + " pics "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
			+"groups." + PicturesInGroupsAdapter.KEY_PICTURE_ID
			+" WHERE " +selection.mObject1
			+" ORDER BY " + SORT_ORDER + " LIMIT '" + nPictures + "'";
			*/
		String query = 
			"SELECT DISTINCT pics.* "
			+", CASE WHEN (pics." + KEY_SERVER_ID + " <= '0' AND "
			+" pics." + KEY_IS_UPDATING + " = '1') THEN 0 ELSE 1 END As ORDER_COL1 "
			+" FROM "
			+PicturesAdapter.TABLE_NAME + " pics "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
			+"groups." + PicturesInGroupsAdapter.KEY_PICTURE_ID
			+" WHERE " +selection.mObject1
			+" ORDER BY " + " ORDER_COL1 ASC, " + SORT_ORDER + " LIMIT '" + nPictures + "'";
		
		// do the query
		Cursor cursor = database.rawQuery(
				query,
				selection.mObject2);

		// return the cursor
		setCursor(cursor);

		// move to correct location
		if (nPictures == 1)
			moveToFirst();
	}
	
	/**
	 * Deelete the picture both locally and on server
	 * @param <ACTIVITY_TYPE>
	 * @param indeterminateProgressBarTags progress bar tags to update while posting. can be null
	 * @param act The activity required to call this post. can be null
	 * @param callback teh callback when finished. can be null
	 * @throws Exception if we could not upload request to server
	 */
	public <ACTIVITY_TYPE extends CustomActivity> void deletePicture(
			ArrayList<String> indeterminateProgressBarTags,
			ACTIVITY_TYPE act,
			final ItemsFinished<ACTIVITY_TYPE> callback) throws Exception{
		
		// show toast
		if (act != null)
			Toast.makeText(act, "Deleting...", Toast.LENGTH_SHORT).show();
		
		// get some data 
		final long rowId = getRowId();
		long serverId = getServerId();
		
		// check if local
		if (serverId <= 0 && rowId >= 1){
			// success - delete from local databases
			PicturesAdapter pics = new PicturesAdapter(ctx);
			pics.fetchPicture(rowId);
			pics.deleteImageLocally();
			pics.close();
			if (callback != null)
				callback.onItemsFinishedUI(act,null);
			return;
		}
		
		// bad row access
		if (rowId == -1 || serverId == -1)
			throw new Exception("bad row access");
		
		// first delete from server
		// create the data required to post to server
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.DeleteImage.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.DeleteImage.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
			json.put(ServerKeys.DeleteImage.POST_KEY_IMAGE_ID, serverId);
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			throw new Exception("bad json data");
		}
		
		// post to server
		Utils.postToServer(
				ServerKeys.DeleteImage.COMMAND,
				json.toString(),
				null,
				null,
				null,
				act,
				indeterminateProgressBarTags,
				new com.tools.ServerPost.PostCallback<ACTIVITY_TYPE>(){

					@Override
					public void onPostFinished(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						
						// convert return
						ShareBearServerReturn data = new ShareBearServerReturn(result);
						
						// check if error
						if (!data.isSuccess() && !data.getErrorCode().equalsIgnoreCase("IMAGE_DOES_NOT_EXIST")){
							if (callback != null)
								callback.onItemsFinishedBackground(act, new Exception(data.getDetailErrorMessage()));
							return;
						}
						
						// success - delete from local databases
						PicturesAdapter pics = new PicturesAdapter(ctx);
						pics.fetchPicture(rowId);
						pics.deleteImageLocally();
						pics.close();
						
						// user supplied callback
						if (callback != null)
							callback.onItemsFinishedBackground(act, null);
					}

					@Override
					public void onPostFinishedUiThread(
							ACTIVITY_TYPE act,
							ServerReturn result) {
						
						// convert return
						ShareBearServerReturn data = new ShareBearServerReturn(result);
						
						// user supplied callback
						// check if error
						if (!data.isSuccess()){
							if (callback != null)
								callback.onItemsFinishedUI(act, new Exception(data.getDetailErrorMessage()));
						}else if (callback != null)
							callback.onItemsFinishedUI(act, null);
					}
					
				});
	}
	
	public interface ItemsFinished<ACTIVITY_TYPE extends CustomActivity>{
		/**
		 * Will be run after items have been finshed processing on the ui thread
		 * @param act the calling activity can be null
		 * @param e If an exception occured, or null
		 */
		public void onItemsFinishedUI(ACTIVITY_TYPE act, Exception e);
		
		/**
		 * Will be run after items have been finished processing on the background thread
		 * @param the calling activity, can be null
		 * @param e If an exception occured, or null
		 */
		public void onItemsFinishedBackground(ACTIVITY_TYPE act, Exception e);
	}
	
	/**
	 * Delete the given picture locally
	 */
	public void deleteImageLocally(){
		//TODO: if we add pictures in different areas in database, they need to be delted here
		long rowId = getRowId();
		String fullPath = getFullPicturePath();
		String thumbPath = getThumbnailPath();
		
		// delete from this database
		if(database.delete(TABLE_NAME, KEY_ROW_ID + " = ?", new String[] {String.valueOf(rowId)}) != 1)
			Log.e(Utils.LOG_TAG, "picture with rowId " + rowId + " was deleted in != 1 places");
		
		// delete actualy files
		if(!(new File(fullPath)).delete())
			Log.e(Utils.LOG_TAG, "picture with rowId " + rowId + " was not deleted actual file");
		if(!(new File(thumbPath)).delete())
			Log.e(Utils.LOG_TAG, "picture with rowId " + rowId + " was not deleted actual thumbnail");
		
		// remove all pictureids of group
		GroupsAdapter groups = new GroupsAdapter(ctx);
		groups.removePictureAsId(rowId);
		
		// remove this picture from any group
		PicturesInGroupsAdapter inGroups = new PicturesInGroupsAdapter(ctx);
		inGroups.removePictureFromAllGroups(rowId);
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
	 * Return new pictures in this group when compared to the pictures returned from server. And also old pictures that should be deleted
	 * @param groupRowId the group we are searching on
	 * @param pictureServerIds the list of serverIds we are comparing to
	 * @return The new values as mObject1, and the old values as mObject2 *** The ids returned are serverIds, NOT row ***
	 */
	protected TwoObjects<HashSet<String>, HashSet<String>> getNewAndOldPicturesInGroup(long groupRowId, HashSet<String> pictureServerIds){
		
		// fetch all pictures in group
		PicturesAdapter pics = new PicturesAdapter(ctx);
		pics.fetchPicturesInGroup(groupRowId);

		// loop over cursor filling values
		HashSet<String> databaseServerIds = new HashSet<String>(pics.size());
		while(pics.moveToNext()){
			databaseServerIds.add(String.valueOf(pics.getServerId()));
		}
		pics.close();

		// new values
		HashSet<String> newValues = new HashSet<String>(pictureServerIds);
		newValues.removeAll(databaseServerIds);
		databaseServerIds.removeAll(pictureServerIds);

		return new TwoObjects<HashSet<String>, HashSet<String>>(newValues, databaseServerIds);
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
		TwoObjects<String, String[]> selection = TableAdapter.createSelection(KEY_USER_ID_TOOK, otherUserRowIds);

		// the new values
		ContentValues values = new ContentValues(1);
		values.put(KEY_USER_ID_TOOK, userRowIdToKeep);

		// do the update
		int nRowsUpdated = -1;
		synchronized (database) {
			// update the rows
			nRowsUpdated = database.update(TABLE_NAME, values, selection.mObject1, selection.mObject2);
		}
		
		if (nRowsUpdated > 0)
			Log.w(Utils.LOG_TAG, "updated " + nRowsUpdated + " rows for user id " + userRowIdToKeep + " in PicturesAdapter");
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
	 * Return the full size image, or null if there is none locally. It will be resize to the desired size, with an oversize factor (helps for zooming)
	 * @param desiredWidth The desired width
	 * @param desiredHeight The desired height
	 * @return The bitmap, or null if none
	 */
	public Bitmap getFullImage(int desiredWidth, int desiredHeight){
		return com.tools.images.ImageLoader.getFullImage(getFullPicturePath(), (int)(desiredWidth*PICTURE_OVERSIZE), (int)(desiredHeight*PICTURE_OVERSIZE));
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
			//	Log.e(Utils.LOG_TAG, "called getRowId on a bad cursor");
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
		
		// read the file
		return com.tools.Tools.getThumbnail(path);
	}

	/**
	 * Return the path to the picture thumbnail, "" if there is none or cursor cannot be read. <br>
	 * @see getThumbnail
	 * @see setThumbnail
	 * @return
	 */
	public String getThumbnailPath(){
		if (!checkCursor()){
			//	Log.e(Utils.LOG_TAG, "called getThumbnailPath on a bad cursor");
			return "";
		}else{
			return getString(KEY_THUMBNAIL_PATH);
		}
	}

	/**
	 * Get the row Id of the user who took this picture. -1 if unknown
	 * @return
	 */
	public long getUserIdWhoTook(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_USER_ID_TOOK);
	}

	/**
	 * If we are downloading the fullsize picture and we haven't timed out, then return true, else false
	 * @return
	 */
	public boolean isDownloadingFullsize(){
		if (!checkCursor()){
			Log.e(Utils.LOG_TAG, "called idDownloadingFullsize on a bad cursor");
			return false;
		}

		boolean isDownloading = getBoolean(KEY_IS_FULLSIZE_DOWNLOADING);
		boolean isTimeout;
		try {
			isTimeout = Utils.parseMilliseconds(Utils.getNowTime()) - 
			Utils.parseMilliseconds(getString(KEY_LAST_FULLSIZE_DOWNLOAD_TIME))
			> 1000*TIMEOUT_ON_FULLSIZE;

		}catch (ParseException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return false;
		}

		// we must be downloading and not timeout
		return (isDownloading && !isTimeout);
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
	 * Set that we are done downloading the fullsize data for this picture
	 * @param rowId the rowId of the picture
	 */
	public void setFinishedDownloadingFullsize(long rowId){

		// the values
		ContentValues values = new ContentValues();
		values.put(KEY_IS_FULLSIZE_DOWNLOADING, false);	

		// update the values to the table
		synchronized (PicturesAdapter.class) {

			if (database.update(
					TABLE_NAME,
					values,
					KEY_ROW_ID + "='" + rowId + "'", null) <=0)
				Log.e(Utils.LOG_TAG, "setFinishedDownloadingFullsize did not update properly");

			PicturesAdapter.class.notifyAll();
		}
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
		synchronized (PicturesAdapter.class) {
			if (database.update(
					TABLE_NAME,
					values,
					KEY_ROW_ID + "='" + rowId + "'", null) <=0)
				Log.e(Utils.LOG_TAG, "setFinishedDownloadingThumbnail did not update properly");

			PicturesAdapter.class.notifyAll();
		}
	}

	/**
	 * If we are downlaiding the fullsize picture from the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * @param rowId the rowId of the picture to update.
	 */
	public void setIsDownloadingFullsize(long rowId){

		// the values
		ContentValues values = new ContentValues();
		values.put(KEY_IS_FULLSIZE_DOWNLOADING, true);	
		values.put(KEY_LAST_FULLSIZE_DOWNLOAD_TIME, Utils.getNowTime());

		// update the values to the table
		if (database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) <=0)
			Log.e(Utils.LOG_TAG, "setIsDownloadingFullsize did not update properly");
	}	

	/**
	 * If we are downlaiding thumbnail from the server, then set this field to true.
	 * When we are done updating, make sure to set to false. <br>
	 * @param rowId the rowId of the picture to update.
	 */
	public void setIsDownloadingThumbnail(long rowId){

		// the values
		ContentValues values = new ContentValues();
		values.put(KEY_IS_THUMBNAIL_DOWNLOADING, true);	
		values.put(KEY_HAS_THUMBNAIL_DATA, false);
		values.put(KEY_LAST_THUMBNAIL_DOWNLOAD_TIME, Utils.getNowTime());

		// update the values to the table
		synchronized (PicturesAdapter.class) {
			if (database.update(
					TABLE_NAME,
					values,
					KEY_ROW_ID + " =?", new String[] {String.valueOf(rowId)}) <=0)
				Log.e(Utils.LOG_TAG, "setDownloadingThumbnail did not update properly");
		}
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
				KEY_ROW_ID + " =?", new String[]{String.valueOf(rowId)}) > 0;	
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
	 * @param userIdWhoCreated the rowId of the user who created this picture
	 * @param dateTaken The date this picture was take
	 * @return true if the picture was successfully updated, false otherwise
	 */
	public void updatePicture(
			long rowId,
			long userIdWhoCreated,
			String dateTaken){		

		ContentValues values = new ContentValues();
		values.put(KEY_DATE_TAKEN, dateTaken);
		values.put(KEY_USER_ID_TOOK, userIdWhoCreated);

		boolean success =  database.update(
				TABLE_NAME,
				values,
				KEY_ROW_ID + "='" + rowId + "'", null) > 0;	

				if (!success)
					Log.e(Utils.LOG_TAG, "picture with rowId " + rowId + " could not be updated for unknown reason: " + values.toString());
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
		/*
		String query0 = 
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
			*/
		
		// create the query where we match up all the pictures that are in the group
		String query = 
			"SELECT pics.* "
			+", CASE WHEN (pics." + KEY_SERVER_ID + " <= '0' AND "
			+" pics." + KEY_IS_UPDATING + " = '1') THEN 0 ELSE 1 END As ORDER_COL1 "
			+" FROM "
			+PicturesAdapter.TABLE_NAME + " pics "
			+" INNER JOIN "
			+PicturesInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"pics." + PicturesAdapter.KEY_ROW_ID + " = "
			+"groups." + PicturesInGroupsAdapter.KEY_PICTURE_ID
			+" WHERE "
			+"groups." + PicturesInGroupsAdapter.KEY_GROUP_ID
			+"=?"
			+" ORDER BY " + " ORDER_COL1 ASC, " + SORT_ORDER;
		
		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// return the cursor
		return cursor;
	}
}
