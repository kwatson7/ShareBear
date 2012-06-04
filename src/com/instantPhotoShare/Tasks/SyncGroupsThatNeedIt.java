package com.instantPhotoShare.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Tasks.CreateGroupTask.ReturnFromCreateGroupTask;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

public class SyncGroupsThatNeedIt <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Void, Void>{

	// codes to be sent to server
	private static final String CREATE_GROUP = "create_group";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String KEY_GROUP_NAME = "group_name";

	public SyncGroupsThatNeedIt(
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
		synchronized (SyncGroupsThatNeedIt.class) {

			GroupsAdapter adapter = new GroupsAdapter(applicationCtx);

			// find the groups that need updating
			adapter.fetchGroupsToBeSynced();

			// loop over the groups updating them
			while (adapter.moveToNext()){

				// now add group to server
				adapter.setIsUpdating(adapter.getRowId(), true);
				ReturnFromCreateGroupTask serverResponse = new ReturnFromCreateGroupTask();
				try {
					serverResponse = new ReturnFromCreateGroupTask(Utils.postToServer(CREATE_GROUP, getDataToPost(adapter.getName()), null, null));
				} catch (JSONException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					serverResponse.setError(e);
				}

				// set sync and update status
				if (serverResponse.isSuccess())
					adapter.setIsSynced(adapter.getRowId(), true, serverResponse.getGroupId());
				else
					Log.e(Utils.LOG_TAG, serverResponse.getDetailErrorMessage());
				adapter.setIsUpdating(adapter.getRowId(), false);
			}

			return null;
		}
	}

	@Override
	protected void onProgressUpdate(Void... progress) {

	}

	@Override
	protected void onPostExectueOverride(Void result) {
		// now that we've synced groups, sync pictures
		if (callingActivity != null){
			UploadPicturesThatNeedIt<ACTIVITY_TYPE> task = new UploadPicturesThatNeedIt<ACTIVITY_TYPE>(callingActivity);
			task.execute();
		}
	}

	@Override
	protected void setupDialog() {

	}

	/**
	 * Return a list of parameter value pairs required to create group on server
	 * @param groupName, the groupName to post
	 * @return a JSOBObject
	 * @throws JSONException 
	 */
	private JSONObject getDataToPost(String groupName)
			throws JSONException{	

		// add values to json object
		JSONObject json = new JSONObject();
		json.put(KEY_GROUP_NAME, groupName);
		json.put(KEY_USER_ID, Prefs.getUserServerId(applicationCtx));
		json.put(KEY_SECRET_CODE, Prefs.getSecretCode(applicationCtx));
		return json;
	}
}
