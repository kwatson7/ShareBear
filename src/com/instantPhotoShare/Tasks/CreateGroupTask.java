package com.instantPhotoShare.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.ServerReturn;

public class CreateGroupTask <ACTIVITY_TYPE extends CustomActivity>  
extends CustomAsyncTask<ACTIVITY_TYPE, Integer, CreateGroupTask<ACTIVITY_TYPE>.ReturnFromCreateGroupTask>{

	// private variables
	private String groupName;
	private boolean allowOthersToAddMembers;
	private boolean isLocal;
	private CreateGroupTask<ACTIVITY_TYPE> task = this;
	private long rowId = -1;
	private boolean isCreateGroupLocally = true;

	// codes to be sent to server
	private static final String CREATE_GROUP = "create_group";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String KEY_GROUP_NAME = "group_name";

	// server errors
	private static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
	private static final String USER_NOT_VALID_ERROR = "USER_NOT_VALID_ERROR";
	private static final String DUPLICATE_GROUP_ERROR = "DUPLICATE_GROUP_ERROR";
	private static final String CREATE_GROUP_ERROR = "CREATE_GROUP_ERROR";
	private static final String GROUP_ID_ERROR = "GROUP_ID_ERROR";

	// local error
	private static final String LOCAL_CREATION_ERROR = "LOCAL_CREATION_ERROR";
	private static final String ERROR_MESSAGE = "Group could not be created for unknown reason";
	private static final String LOCAL_ONLY = "LOCAL_ONLY";

	/**
	 * Make a group and put it into the groups database and then add all the users to it.
	 * @param act The calling activity
	 * @param requestId An id code signifying which task was called
	 * @param groupName The name of the group to create
	 * @param allowOthersToAddMembers boolean to allow others to add members to group besides creator
	 * @param isLocal boolean if this is a local to device group
	 */
	public CreateGroupTask(
			ACTIVITY_TYPE act,
			int requestId,
			String groupName,
			boolean allowOthersToAddMembers,
			boolean isLocal) {
		super(act,
				requestId,
				true,
				false,
				null);

		// store inputs
		this.groupName = groupName;
		this.allowOthersToAddMembers = allowOthersToAddMembers;
		this.isLocal = isLocal;
		this.isCreateGroupLocally = true;
	}
	
	/**
	 * Take an already created group and put it on the server. It will check that it is KeepLocal on.
	 * @param act The calling activity
	 * @param requestId An id code signifying which task was called
	 * @param rowId The rowId of an already created group
	 */
	public CreateGroupTask(
			ACTIVITY_TYPE act,
			int requestId,
			long rowId) {
		super(act,
				requestId,
				true,
				false,
				null);

		// store inputs
		this.rowId = rowId;
		this.isCreateGroupLocally = false;
	}

	@Override
	protected void onPreExecute() {

	}

	@Override
	protected ReturnFromCreateGroupTask doInBackground(Void... params) {

		GroupsAdapter groups = new GroupsAdapter(applicationCtx);
		
		// make the new group
		if (isCreateGroupLocally){
			rowId = groups.makeNewGroup(
					applicationCtx,
					groupName,
					null,
					Utils.getNowTime(),
					Prefs.getUserRowId(applicationCtx),
					allowOthersToAddMembers,
					null,
					null,
					-1,
					isLocal);

			// if we were not successful
			if (rowId == - 1){
				ReturnFromCreateGroupTask result = new ReturnFromCreateGroupTask();
				result.setError(LOCAL_CREATION_ERROR, ERROR_MESSAGE);
				result.setRowId(rowId);
				return result;
			}

			// no need to add to server if local
			if (isLocal){
				ReturnFromCreateGroupTask result = new ReturnFromCreateGroupTask();
				result.setError(LOCAL_ONLY, LOCAL_ONLY);
				result.setRowId(rowId);
				return result;
			}
		}
		
		// check local value for already created group
		if (!isCreateGroupLocally){
			GroupsAdapter.Group tmpGroup = groups.getGroup(rowId);
			if(tmpGroup.isKeepLocal()){
				ReturnFromCreateGroupTask result = new ReturnFromCreateGroupTask();
				return result;
			}
			
			// set the group name
			groupName = tmpGroup.getName();
		}
		
		// now add group to server
		groups.setIsUpdating(rowId, true);
		ReturnFromCreateGroupTask serverResponse = new ReturnFromCreateGroupTask();
		try {
			serverResponse = new ReturnFromCreateGroupTask(Utils.postToServer(CREATE_GROUP, getDataToPost(groupName), null));
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse.setError(e);
		}
		serverResponse.setRowId(rowId);

		// set sync and update status
		if (serverResponse.isSuccess())
			groups.setIsSynced(rowId, true, serverResponse.getGroupId());
		groups.setIsUpdating(rowId, false);

		// return the value
		return serverResponse;
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

	@Override
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExectueOverride(final ReturnFromCreateGroupTask result) {

		if (applicationCtx == null)
			return;

		// show toast if successful and send result to activity
		if (result.isSuccess())	{
			Toast.makeText(applicationCtx, groupName + " group created.", Toast.LENGTH_SHORT).show();
			task.sendObjectToActivityFromPostExecute(result);
			// change default group
			Prefs.setGroupIds(applicationCtx, rowId);
			return;
		}
		
		// show toast if we only tried local
		if (result.isOnlyTryLocalSuccess())	{
			Toast.makeText(applicationCtx, groupName + " group created.", Toast.LENGTH_SHORT).show();
			task.sendObjectToActivityFromPostExecute(result);
			// change default group
			Prefs.setGroupIds(applicationCtx, rowId);
			return;
		}
		
		// if not successful, then see how
		// local error
		if (result.isLocalError()){
			Toast.makeText(applicationCtx, result.getDetailErrorMessage(), Toast.LENGTH_LONG).show();
			return;

			// server error
		}else{
			// the 3 values we must set
			String toastMessage = "";
			String notesMessage = "";
			NOTIFICATION_TYPES notesType;
			
			// default values
			toastMessage = "Group not created on server because:\n" + result.getDetailErrorMessage() +
				".\nGroup is still created locally, but is not shared!";
			notesMessage = "Group with name '" + groupName + "' and rowId " + rowId + " not created on server because:\n"
				+ result.getDetailErrorMessage() + ".\nGroup is still created locally, but is not shared!";
			notesType = NOTIFICATION_TYPES.SERVER_ERROR;
			
			// custom actions for different group errors
			String errorCode = result.getErrorCode();
			
			// the user hasn't authenticated their email address
			if (errorCode.equalsIgnoreCase(USER_NOT_VALID_ERROR)){		
				toastMessage = "Email address not authenticated. Authenticate before groups can be created.";
				notesMessage = "Email address not authenticated. Group "+
					groupName + " not created.";
				notesType = NOTIFICATION_TYPES.MISC;
				
				// delete the group
				GroupsAdapter groupsAdapter = new GroupsAdapter(applicationCtx);
				groupsAdapter.deleteGroup(rowId);
				result.setRowId(-1);
				
			}else if (errorCode.equalsIgnoreCase(DUPLICATE_GROUP_ERROR)){
				toastMessage = "Cannot create duplicate group names";
				notesMessage = "Cannot create because of duplicate group name. Group "+
					groupName + " not created.";
				notesType = NOTIFICATION_TYPES.WARNING;
				
				// delete the group
				GroupsAdapter groupsAdapter = new GroupsAdapter(applicationCtx);
				groupsAdapter.deleteGroup(rowId);
				result.setRowId(-1);
			}

			// show the toast
			Toast.makeText(applicationCtx,
					toastMessage,
					Toast.LENGTH_LONG).show();

			// store in notifications
			NotificationsAdapter notes = new NotificationsAdapter(applicationCtx);
			notes.createNotification(notesMessage, notesType);

			// change default group
			if (result.isLocalSuccess())
				Prefs.setGroupIds(applicationCtx, rowId);
			
			// send the result back to calling activity
			task.sendObjectToActivityFromPostExecute(result);		
		}
	}

	@Override
	protected void setupDialog() {
		// show dialog for this process
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Creating Group");
			dialog.setMessage("Please wait...");
			dialog.setIndeterminate(true);
		}
	}

	public class ReturnFromCreateGroupTask
	extends ShareBearServerReturn{

		// other private variables
		private long rowId = -1;

		// KEYS in JSON
		private static final String KEY_GROUP_ID = "group_id";;

		/**
		 * Intiailize a ReturnFromCreateGroupTask object from a ServerJSON object.
		 * @param toCopy
		 */
		protected ReturnFromCreateGroupTask(ServerReturn toCopy) {
			super(toCopy);
		}
		
		protected ReturnFromCreateGroupTask(){
			super();
		}

		/**
		 * The json must have either <br>
		 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
		 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE <br>
		 * Also if successfull must have KEY_GROUP_ID
		 */
		protected void checkAcceptableSub(){

			// now check that we have userId and secretCode
			if (isSuccess()){
				if (getGroupId() == -1)
					throw new IllegalArgumentException(
							"ReturnFromCreateGroupTask " + 
							KEY_GROUP_ID + " key required.");

			}		
		}

		/**
		 * Gets the group ID stored in this object returned from the server. Returns -1 if there isn't one. <br>
		 * That should never happen, because an illegal argument exceptions should have been
		 * thrown if this were the case, when object was created.
		 * @return
		 */
		public long getGroupId() {
			try {
				return getMessageObject().getLong(KEY_GROUP_ID);
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
			if (isSuccess() || !isLocalError() && getRowId() != -1)
				return true;
			else
				return false;
		}

		private void setRowId(long rowId) {
			this.rowId = rowId;
		}

		public long getRowId() {
			return rowId;
		}
		
		public boolean isOnlyTryLocalSuccess(){
			return getErrorCode().equalsIgnoreCase(LOCAL_ONLY);
		}
	}
}
