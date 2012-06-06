package com.instantPhotoShare.Tasks;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.PicturesInGroupsAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.ServerReturn;
import com.tools.SuccessReason;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

public class SaveTakenPictureTask <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Void, SaveTakenPictureTask.ReturnFromPostPicture>{

	// private task variables
	private byte[] camData; 			// The byte data for the picture 
	private int camRotation; 			// The rotation value, so we save the picture correctly.
	private String caption; 			// caption to save with image
	private ArrayList<Group> groups;	// arraylist of groups to add this picture to.

	// other inputs
	private Double longitude = null;
	private Double latitude = null;
	//TODO: actually pass in longitude and latitude
	//TODO: actually pass a caption to savetakenpicturetask from takepicture.java

	// codes to be sent to server
	private static final String ACTION = "upload_image";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String KEY_GROUP_ID = "group_id";

	// error codes
	private static final String IMAGE_UPLOAD_ERROR = "IMAGE_UPLOAD_ERROR";
	private static final String IMAGE_EXISTS_ERROR = "IMAGE_EXISTS_ERROR";
	private static final String MOVE_UPLOAD_ERROR = "MOVE_UPLOAD_ERROR";
	private static final String INVALID_IMAGE_TYPE_ERROR = "INVALID_IMAGE_TYPE_ERROR";
	private static final String IMAGE_ID_ERROR = "IMAGE_ID_ERROR";

	// local error
	private static final String LOCAL_CREATION_ERROR = "LOCAL_CREATION_ERROR";
	private static final String ERROR_MESSAGE = "picture could not be created for unknown reason";


	/**
	 * Make an activity to save a picture to file and store it in the database
	 * @param act The calling activity
	 * @param requestId The requestId, so if/when data is sent back to activity, we know by which task
	 * @param camData The byte array of camera data
	 * @param camRotation Rotation value specifying what rotation angle the picture was taken at.
	 */ 
	public SaveTakenPictureTask(
			ACTIVITY_TYPE act,
			int requestId,
			byte[] camData,
			int camRotation,
			ArrayList<Group> groups,
			String caption) {

		super(
				act,
				requestId,
				true,
				false,
				null);

		// save data
		this.camData = camData;
		this.camRotation = camRotation;
		this.caption = caption;
		this.groups = groups;

		// check empty group
		if (groups == null || groups.size() == 0)
			throw new IllegalArgumentException("groups cannot be null or empty, at least one group must be passed in");
	}

	@Override
	protected void onPreExecute() {

	}

	@Override
	protected ReturnFromPostPicture doInBackground(Void... params) {

		// determine rotation
		Integer rotation = null;
		switch (camRotation){
		case 0:
			rotation = ExifInterface.ORIENTATION_NORMAL;
			break;
		case 90:
			rotation = ExifInterface.ORIENTATION_ROTATE_90;
			break;
		case 180:
			rotation = ExifInterface.ORIENTATION_ROTATE_180;
			break;
		case 270:
			rotation = ExifInterface.ORIENTATION_ROTATE_270;
		}

		// get the folder in which we are going to save
		TwoStrings picName = groups.get(0).getNextPictureName();	

		// write the folders if we can
		try {
			groups.get(0).writeFoldersIfNeeded();
		} catch (IOException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			NotificationsAdapter not = new NotificationsAdapter(applicationCtx);
			not.createNotification("could not create proper folders for group " + groups.get(0).getName(), NOTIFICATION_TYPES.DEVICE_ERROR);
			ReturnFromPostPicture result = new ReturnFromPostPicture();
			result.setError(LOCAL_CREATION_ERROR, "could not crate proper folders for group " + groups.get(0).getName());
			result.setPictureRowId(-1);
			return result;
		}	

		// save the data
		SuccessReason pictureSave = 
				com.tools.Tools.saveByteDataToFile(
						applicationCtx,
						camData,
						caption,
						false,
						picName.mObject1,
						rotation,
						true);

		// break out if we didn't save successfully
		if (!pictureSave.getSuccess()){
			NotificationsAdapter not = new NotificationsAdapter(applicationCtx);
			not.createNotification(pictureSave.getReason(), NOTIFICATION_TYPES.DEVICE_ERROR);
			ReturnFromPostPicture result = new ReturnFromPostPicture();
			result.setError(LOCAL_CREATION_ERROR, pictureSave.getReason());
			result.setPictureRowId(-1);
			return result;
		}

		// create the thumbnail
		byte[] thumbnail = com.tools.Tools.makeThumbnail(
				camData,
				rotation,
				Utils.MAX_THUMBNAIL_DIMENSION,
				Utils.FORCE_BASE2_THUMBNAIL_RESIZE,
				Utils.IMAGE_QUALITY);

		// save the thumbnail
		SuccessReason thumbnailSave = 
				com.tools.Tools.saveByteDataToFile(
						applicationCtx,
						thumbnail,
						caption,
						false,
						picName.mObject2,
						ExifInterface.ORIENTATION_NORMAL,
						false);

		// break out if we didn't save successfully
		if (!thumbnailSave.getSuccess()){
			NotificationsAdapter not = new NotificationsAdapter(applicationCtx);
			not.createNotification(thumbnailSave.getReason(), NOTIFICATION_TYPES.DEVICE_ERROR);
			ReturnFromPostPicture result = new ReturnFromPostPicture();
			result.setError(LOCAL_CREATION_ERROR, thumbnailSave.getReason());
			result.setPictureRowId(-1);
			return result;
		}

		// put the data into pictures database
		long picId = -1;
		PicturesAdapter pic = new PicturesAdapter(applicationCtx);
		picId = pic.createPicture(
				applicationCtx,
				pictureSave.getReason(),
				thumbnailSave.getReason(),
				null,
				null,
				latitude,
				longitude,
				true);

		// we had an error, so break out
		if (picId == -1){
			String msg = "Saving picture into database could not be completed for unknown reason";
			NotificationsAdapter not = new NotificationsAdapter(applicationCtx);
			not.createNotification(msg, NOTIFICATION_TYPES.DEVICE_ERROR);
			ReturnFromPostPicture result = new ReturnFromPostPicture();
			result.setError(LOCAL_CREATION_ERROR, msg);
			result.setPictureRowId(-1);
			return result;
		}

		// link the picture to all the groups
		PicturesInGroupsAdapter picturesInGroups = new PicturesInGroupsAdapter(applicationCtx);
		for (Group item : groups){
			long linkRow = picturesInGroups.addPictureToGroup(applicationCtx, picId, item.getRowId());
			if (linkRow == -1){
				String msg = "link between picture and group could be made for unknown reason";
				NotificationsAdapter not = new NotificationsAdapter(applicationCtx);
				not.createNotification(msg, NOTIFICATION_TYPES.DEVICE_ERROR);
				ReturnFromPostPicture result = new ReturnFromPostPicture();
				result.setError(LOCAL_CREATION_ERROR, msg);
				result.setPictureRowId(-1);
				return result;
			}
		}
		Prefs.setLastPictureTaken(applicationCtx, picId);
		Prefs.setLastGroupOfLastPicture(applicationCtx, groups.get(0).getRowId());
		publishProgress();

		// post to server
		return postDataToServer(picId, pictureSave.getReason(), thumbnail);
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
		sendObjectToActivityFromProgress(true);
	}

	@Override
	protected void onPostExectueOverride(ReturnFromPostPicture result) {

		if (applicationCtx == null)
			return;

		// null result means we were successful local, and there was no need for server to receive anything
		if (result == null){
			Toast.makeText(applicationCtx, "Image Saved on Device only.", Toast.LENGTH_SHORT).show();
			return;
		}

		// show toast if successful
		if (result.isSuccess())	{
			Toast.makeText(applicationCtx, "Picture uploaed to the interwebs", Toast.LENGTH_SHORT).show();
			return;
		}

		// if not successful, then see how
		// local error
		if (result.isLocalError()){
			//Toast.makeText(applicationCtx, result.getMessage(), Toast.LENGTH_LONG).show();
			showAlert(result.getDetailErrorMessage());
			return;

			// server error
		}else{
			// the 3 values we must set
			String toastMessage = "";
			String notesMessage = "";
			NOTIFICATION_TYPES notesType;

			// default values
			toastMessage = "Picture not saved on server because:\n" + result.getDetailErrorMessage() +
					".\nPictre is still saved on device, but is not shared!";
			notesMessage = "Picture with rowId " + result.getPictureRowId() + " not created on server because:\n"
					+ result.getDetailErrorMessage() + ".\nPicture is still saved on device, but is not shared!";
			notesType = NOTIFICATION_TYPES.SERVER_ERROR;

			// show the toast
			//Toast.makeText(applicationCtx,
			//		toastMessage,
			//		Toast.LENGTH_LONG).show();
			showAlert(toastMessage);

			// store in notifications
			NotificationsAdapter notes = new NotificationsAdapter(applicationCtx);
			notes.createNotification(notesMessage, notesType);		
		}
	}

	/**
	 * Show an alert window if we can, else just show a toast
	 * @param message
	 */
	private void showAlert(String message){
		try{
			if (!callingActivity.isFinishing()){
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(callingActivity);

				dlgAlert.setMessage(message);
				dlgAlert.setTitle("Message");
				dlgAlert.setCancelable(true);
				dlgAlert.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//dismiss the dialog  
					}
				});
				dlgAlert.create().show();
			}else{
				Intent intent = new Intent(applicationCtx, com.tools.MessageDialog.class);
				intent.putExtra(com.tools.MessageDialog.TITLE_BUNDLE, "Message");
				intent.putExtra(com.tools.MessageDialog.DEFAULT_TEXT, message);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				applicationCtx.startActivity(intent);

				//Toast.makeText(applicationCtx, message, Toast.LENGTH_LONG);
			}
		}catch(Exception e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
	}

	private ReturnFromPostPicture postDataToServer(long pictureRowId, String fullPictureFile, byte[] thumbnail){

		//TODO: can we do multiple group ids on upload
		//TODO: is brennan making sure to check if we can actually post to a group
		//TODO: before doign any updates, we should be checking if there is already an update goign for all update possibilities
		//TODO: deal with updating and syncing of picture / group pairs

		ReturnFromPostPicture serverResponse = null;

		// determine the data we need to post to the server
		TwoObjects<JSONObject, ArrayList<Long>> serverData;
		try {
			serverData = getDataToPost();
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse = new ReturnFromPostPicture();
			serverResponse.setError(e);
			return serverResponse;
		}
		if (serverData == null)
			return null;

		// tell the database that we are updating
		PicturesAdapter picturesAdapter = new PicturesAdapter(applicationCtx);
		//picturesAdapter.fetchPicture(pictureRowId);
		picturesAdapter.setIsUpdating(pictureRowId, true);

		// post picture to server
		serverResponse = new ReturnFromPostPicture(
				Utils.postToServer(
						ACTION,
						serverData.mObject1,
						fullPictureFile,
						thumbnail));
		serverResponse.setPictureRowId(pictureRowId);

		// set sync and update status
		if (serverResponse.isSuccess())
			picturesAdapter.setIsSynced(pictureRowId, true, serverResponse.getPictureServerId());

		// return the value
		return serverResponse;	
	}

	/**
	 * Return a list of parameter value pairs required to upload picture to server. If there are
	 * no public and synced groups, then null is returned.
	 * Also in the second object of the return, an arrayList of the rowIds of the groups that we
	 * will attempt to sync to on the server
	 * @return a JSONObject and an ArrayList of group rowIds
	 * @throws JSONException 
	 */
	private TwoObjects<JSONObject, ArrayList<Long>> getDataToPost()
			throws JSONException{	

		// build a list of group ids
		String ids = "";
		ArrayList<Long> longIds = new ArrayList<Long>(groups.size());
		for (Group item : groups){
			if (item.isSynced() && !item.isKeepLocal()){
				ids += item.getServerId() + Utils.DELIM;
				longIds.add(item.getRowId());
			}
		}
		if (ids.length() > 1)
			ids = ids.substring(0, ids.length()-1);
		if (ids.length() == 0)
			return null;

		//ids = longIds.toString(); // wrong ids, we are mixing server and row ids.

		// add values to json object
		JSONObject json = new JSONObject();
		json.put(KEY_USER_ID, Prefs.getUserServerId(applicationCtx));
		json.put(KEY_SECRET_CODE, Prefs.getSecretCode(applicationCtx));
		json.put(KEY_GROUP_ID, ids);
		return new TwoObjects<JSONObject, ArrayList<Long>>(json, longIds);
	}

	@Override
	protected void setupDialog() {
		// do nothing, we don't want a dialog
	}

	static class ReturnFromPostPicture
	extends ShareBearServerReturn{

		// other private variables
		private long pictureRowId = -1;

		// KEYS in JSON
		private static final String KEY_IMAGE_ID = "image_id";;

		/**
		 * Intiailize a ReturnFromCreateGroupTask object from a ServerJSON object.
		 * @param toCopy
		 */
		protected ReturnFromPostPicture(ServerReturn toCopy) {
			super(toCopy);
		}

		protected ReturnFromPostPicture(){
			super();
		}

		/**
		 * The json must have either <br>
		 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
		 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE <br>
		 * Also if successfull must have KEY_GROUP_ID
		 */
		@Override
		protected boolean isSuccessCustom2(){

			// now check that we have userId and secretCode
			if (getPictureServerId() == -1){
				Log.e(Utils.LOG_TAG, "Incorrect SaveTakenPictureServerReturn");
				return false;
			}

			return true;
		}

		/**
		 * Gets the picture server ID stored in this object returned from the server. Returns -1 if there isn't one. <br>
		 * That should never happen, because an illegal argument exceptions should have been
		 * thrown if this were the case, when object was created.
		 * @return
		 */
		public long getPictureServerId() {
			try {
				return getMessageObject().getLong(KEY_IMAGE_ID);
			} catch (JSONException e) {
				return -1;
			}
		}

		/**
		 * If we had a local error (we couldn't create group locally) then return true, else false
		 * @return
		 */
		public boolean isLocalError(){
			if (isSuccess())
				return false;
			else
				return getErrorCode().equalsIgnoreCase(LOCAL_CREATION_ERROR);
		}

		/**
		 * Boolean if we had a local group creation success.
		 * @return
		 */
		public boolean isLocalSuccess(){
			if (isSuccess() || !isLocalError() && getPictureRowId() != -1)
				return true;
			else
				return false;
		}

		private void setPictureRowId(long rowId) {
			this.pictureRowId = rowId;
		}

		public long getPictureRowId() {
			return pictureRowId;
		}
	}
}