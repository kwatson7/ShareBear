package com.instantPhotoShare.Tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Tasks.SaveTakenPictureTask.ReturnFromPostPicture;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.TwoObjects;

public class UploadPicturesThatNeedIt <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Void, Void>{

	// codes to be sent to server
	private static final String ACTION = "upload_image";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String KEY_GROUP_ID = "group_id";

	public UploadPicturesThatNeedIt(
			ACTIVITY_TYPE act) {
		super(act,
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
		synchronized (UploadPicturesThatNeedIt.class) {

			// this is used to find all the groups a picture is in
			GroupsAdapter groupsHelper = new GroupsAdapter(applicationCtx);

			// loop over all the groups
			GroupsAdapter groups = new GroupsAdapter(applicationCtx);
			groups.fetchAllGroups();
			PicturesAdapter pics = new PicturesAdapter(applicationCtx);
			while(groups.moveToNext()){

				// loop over all the pictures that need uploading in this group
				pics.fetchPicturesNeedUploading(groups.getRowId());
				while (pics.moveToNext()){

					// find all the groups this picture is in
					groupsHelper.fetchGroupsContainPicture(pics.getRowId());
					ArrayList<Long> groupServerIds = new ArrayList<Long>(groupsHelper.size());
					while(groupsHelper.moveToNext()){
						if (groupsHelper.isSynced()
								&& !groupsHelper.isKeepLocal())
							groupServerIds.add(groupsHelper.getServerId());
					}
					groupsHelper.close();
					String ids = groupServerIds.toString();
					
					// grab the thumbnail data
					//TODO: re-encoding file which will lead to artifacts
					//TODO: we're not re-encoding anymore, but make sure this gives good thumbnail data
					String thumbPath = pics.getThumbnailPath();
					//byte[] thumb = com.tools.images.ImageLoader.getThumbnailAsByteArray(thumbPath, Utils.IMAGE_QUALITY);
					byte[] thumb;
					try {
						thumb = com.tools.Tools.readFile(thumbPath);
					}catch (FileNotFoundException e2){
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e2));
						if (com.tools.Tools.isStorageAvailable(false))
							pics.removePictureFromDatabase(pics.getRowId());
						continue;
					} catch (IOException e1) {
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
						continue;
					}
					String fullPath = pics.getFullPicturePath();
					if (thumb == null || thumb.length == 0 || fullPath == null || fullPath.length() == 0){
						Log.e(Utils.LOG_TAG, "in UploadPicturesThatNeedIt, we somehow bad picture paths");
						continue;
					}
					
					// the json data to post
					JSONObject json = new JSONObject();
					try {
						json.put(KEY_USER_ID, Prefs.getUserServerId(applicationCtx));
						json.put(KEY_SECRET_CODE, Prefs.getSecretCode(applicationCtx));
						json.put(KEY_GROUP_ID, ids);
					} catch (JSONException e) {
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
						continue;
					}
					
					// tell the database that we are updating
					pics.setIsUpdating(pics.getRowId(), true);
					
					// post picture to server
					ReturnFromPostPicture result = new ReturnFromPostPicture(Utils.postToServer(
							ACTION,
							json,
							fullPath,
							thumb));

					// set sync and update status
					if (result.isSuccess())
						pics.setIsSynced(pics.getRowId(), true, result.getPictureServerId());
				}
				
				pics.close();
			}

			groups.close();
			
			return null;
		}
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
	}

	@Override
	protected void onPostExectueOverride(Void result) {

	}

	@Override
	protected void setupDialog() {
	}

}
