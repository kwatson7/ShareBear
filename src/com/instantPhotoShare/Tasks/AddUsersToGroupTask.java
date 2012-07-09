package com.instantPhotoShare.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersInGroupsAdapter;
import com.instantPhotoShare.Tasks.AddUsersToGroupTask.ReturnFromAddUsersToGroupTask.ResponseCode;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.ServerReturn;

public class AddUsersToGroupTask <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Integer, AddUsersToGroupTask.ReturnFromAddUsersToGroupTask>{

	// constants
	private static final String LOCAL_TITLE = "Saving Users to Group";
	private static final String SERVER_TITLE = "Uploading to Server";

	// private variables
	private ContactCheckedArray mContactChecked;
	private int progressMax = 1;
	private long groudRowId = -1;
	private long groupServerId = -1;
	private int usersAdded = 0;
	private int usersRemoved = 0;
	private String dialogTitle = LOCAL_TITLE;

	// codes to be sent to server
	private static final String ACTION = "add_user_to_group";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String KEY_PERSON_F_NAME = "person_fname";
	private static final String KEY_PERSON_L_NAME = "person_lname";
	private static final String KEY_PERSON_EMAIL = "person_email";
	private static final String KEY_PHONE_NUMBER = "phone_number";
	private static final String KEY_GROUP_ID = "group_id";
	private static final String KEY_CONTACT_METHOD = "contact_method";


	// server errors

	// local error
	private static final String LOCAL_CREATION_ERROR = "USER_COULD_NOT_BE_ADDED_FOR_UNKNOWN_REASON";
	private static final String ERROR_MESSAGE = "User could not be added for unknown reason on device";
	private static final String GROUP_EXIST_SERVER_ERROR = "GROUP_EXIST_SERVER_ERROR";
	private static final String GROUP_EXIST_SERVER_ERROR_MESSAGE = "group is not synced with server. Cannot add members";

	/**
	 * Make a group and put it into the groups database and then add all the users to it.
	 * @param act The calling activity
	 * @param requestId An id code signifying which task was called
	 * @param mContactChecked Object holding data about users that have been selected
	 * @param groudRowId the group id from the device sql database to add users to
	 */
	public AddUsersToGroupTask(
			ACTIVITY_TYPE act,
			int requestId,
			ContactCheckedArray mContactChecked,
			long groudRowId) {
		super(act,
				requestId,
				true,
				false,
				null);

		// store inputs
		this.groudRowId = groudRowId;
		this.mContactChecked = mContactChecked;
		progressMax = mContactChecked.getNChecked();
		dialog.setMax(progressMax); // this needs to be done here, because setupDialog is called before we've stored mContactChecked
		
		//TODO: not allowing deletion locally or on server
	}

	private int getTotalEdits(){
		return usersAdded + usersRemoved;
	}
	@Override
	protected void onPreExecute() {

	}

	private static class WhichUsersCreated{
		/**
		 * The list of new users to add to group
		 */
		ArrayList<Long> newUsers = new ArrayList<Long>();
		/**
		 * The list of users to delete from group
		 */
		ArrayList<Long> deletions = new ArrayList<Long>();
		/**
		 * The list of ids that link new users and group(s)
		 */
		ArrayList<Long> linkIdsNewUsers = new ArrayList<Long>();
		/**
		 * Did we succeeed in updating database locally
		 */
		boolean isSuccess = false;
	}

	@Override
	protected AddUsersToGroupTask.ReturnFromAddUsersToGroupTask doInBackground(Void... params) {

		// add the people to the group locally
		WhichUsersCreated localCreation = createPeople(groudRowId);

		// if we errored, then stop
		if (!localCreation.isSuccess){
			ReturnFromAddUsersToGroupTask serverResponse = 
				new ReturnFromAddUsersToGroupTask(LOCAL_CREATION_ERROR, ERROR_MESSAGE);
			return serverResponse;
		}

		// make sure group exists on server
		GroupsAdapter groups = new GroupsAdapter(applicationCtx);
		Group group = groups.getGroup(groudRowId);
		if (group == null || group.getRowId() == 0 || group.getRowId() == -1
				|| group.getServerId() == 0 || group.getServerId() == -1){
			ReturnFromAddUsersToGroupTask serverResponse = 
				new ReturnFromAddUsersToGroupTask(GROUP_EXIST_SERVER_ERROR, GROUP_EXIST_SERVER_ERROR_MESSAGE);
			return serverResponse;
		}
		groupServerId = group.getServerId();

		// no one to edit, so return
		if (getTotalEdits() == 0)
			return new ReturnFromAddUsersToGroupTask();

		// now add users to server
		UsersInGroupsAdapter links = new UsersInGroupsAdapter(applicationCtx);
		links.setIsUpdating(localCreation.linkIdsNewUsers, true);
		ReturnFromAddUsersToGroupTask serverResponse = new ReturnFromAddUsersToGroupTask();
		try {
			serverResponse = new ReturnFromAddUsersToGroupTask(
					Utils.postToServer(
							ACTION, getDataToPost(
									localCreation.newUsers), null, null));
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse.setError(e);
		}

		// check that the return is the correct size
		if (serverResponse.getMessageArray().length() != localCreation.newUsers.size() ||
				localCreation.linkIdsNewUsers.size() != localCreation.newUsers.size()){
			Log.e(Utils.LOG_TAG, "sizes returned from server are not compatible");
			serverResponse.setError(new Exception("incompattible sizes returned from server"));
			return serverResponse;
		}

		// loop across returned users to check if we added successfully
		UsersAdapter users = new UsersAdapter(applicationCtx);
		if (serverResponse.isSuccess()){
			
			for (int i = 0; i < serverResponse.getMessageArray().length(); i++){
				
				// grab the server ids and row ids and link ids
				long userRowId = localCreation.newUsers.get(i);
				long linkId = localCreation.linkIdsNewUsers.get(i);
				long userServerId = serverResponse.getUserServerId(i);

				// loop across all switch possibilities
				ResponseCode response = serverResponse.getUserCode(i);			
				switch(response){
				case Server_Error:
					links.removeUserFromGroup(userRowId, groudRowId);
					users.setIsUpdating(userRowId, false);
					Log.e(Utils.LOG_TAG, "user did not update for unknown reason: " + serverResponse.getErrorCode());
					break;
				case Temp_User_Created_Invite_Sent:
					users.setIsSynced(userRowId, true, userServerId, false);
					links.setIsSynced(linkId, true);
					break;
				case Temp_User_Exists_Invite_Resent:
					users.setIsSynced(userRowId, true, userServerId, false);
					links.setIsSynced(linkId, true);
					break;
				case User_Exists_Added_To_Group:
					users.setIsSynced(userRowId, true, userServerId, true);
					links.setIsSynced(linkId, true);
					break;
				case User_Exists_Already_In_Group:
					users.setIsSynced(userRowId, true, userServerId, true);
					links.setIsSynced(linkId, true);
					break;
				default:
					links.removeUserFromGroup(userRowId, groudRowId);
					users.setIsUpdating(userRowId, false);
					Log.e(Utils.LOG_TAG, "bad response from server for addUserToGroup" + response.getName());
					serverResponse.setError(new Exception("Unexpected result from server: " + response.getName()));
				}
			}
		}else{

			// remove users from group link
			for (long item : localCreation.newUsers){
				links.removeUserFromGroup(item, groudRowId);
				users.setIsUpdating(item, false);
			}
		}

		// return the value
		return serverResponse;
	}

	/**
	 * Return a list of parameter value pairs required to create group on server
	 * @return a JSOBObject
	 * @throws JSONException 
	 */
	private JSONArray getDataToPost(ArrayList<Long> newAdditions)
	throws JSONException{	

		// initialize the array
		JSONArray jsonArray = new JSONArray();

		// add top level values
		JSONObject top = new JSONObject();
		top.put(KEY_USER_ID, Prefs.getUserServerId(applicationCtx));
		top.put(KEY_SECRET_CODE, Prefs.getSecretCode(applicationCtx));
		jsonArray.put(top);

		// initalize the users
		UsersAdapter users = new UsersAdapter(applicationCtx);

		// loop adding new users to json array
		for (Long id : newAdditions){

			// grab the user info
			users.fetchUser(id);
			JSONObject json = new JSONObject();
			json.put(KEY_PERSON_F_NAME, users.getFirstName());
			json.put(KEY_PERSON_L_NAME, users.getLastName());
			json.put(KEY_PHONE_NUMBER, users.getPhones());
			json.put(KEY_PERSON_EMAIL, users.getEmails());
			json.put(KEY_CONTACT_METHOD, users.getDefaultContactMethod());
			json.put(KEY_GROUP_ID, groupServerId);

			// add to array
			jsonArray.put(json);
		}
		users.close();

		return jsonArray;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
		if (progress[0] == progressMax){
			dialogTitle = SERVER_TITLE;
			dialog.setTitle(dialogTitle);
		}
	}

	@Override
	protected void onPostExectueOverride(AddUsersToGroupTask.ReturnFromAddUsersToGroupTask result) {

		if (applicationCtx == null)
			return;

		// no edits
		if (getTotalEdits() == 0 && result.getErrorCode() == ReturnFromAddUsersToGroupTask.UNKNOWN_ERROR_CODE){
			Toast.makeText(applicationCtx, "No changes", Toast.LENGTH_SHORT).show();
			sendObjectToActivityFromPostExecute(result);
			return;
		}

		// show toast if successful and send result to activity
		GroupsAdapter groupsAdapter = new GroupsAdapter(applicationCtx);
		Group group = groupsAdapter.getGroup(groudRowId);
		if (result.isSuccess())	{
			Toast.makeText(
					applicationCtx,
					getTotalEdits() + " users updated to " + group.getName(),
					Toast.LENGTH_SHORT)
					.show();
			sendObjectToActivityFromPostExecute(result);
			return;
		}

		// if not successful, then see how
		// local error
		if (result.isLocalError()){
			Toast.makeText(applicationCtx, result.getDetailErrorMessage(), Toast.LENGTH_LONG).show();
			return;

		}else{
			// the an error message
			com.tools.Tools.showAlert(null, applicationCtx, getTotalEdits() + " users updated to " + group.getName()
					+ " on device, but not added to server because:\n" + result.getDetailErrorMessage());

			// store in log
			Log.e(Utils.LOG_TAG, "Group with name '" + group.getName() + "' and rowId " + groudRowId +
					" users not updated successfully on server because:\n"
					+ result.getDetailErrorMessage() + ".\nUsers are still created on device, but not on server!");

			// send the result back to calling activity
			sendObjectToActivityFromPostExecute(result);
		}
	}

	@Override
	protected void setupDialog() {
		// show dialog for this long process
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			if (dialogTitle == null)
				dialogTitle = LOCAL_TITLE;
			dialog.setTitle(dialogTitle);
			dialog.setMessage("Please wait...");
			dialog.setIndeterminate(false);
			dialog.setCancelable(false);
			if (mContactChecked != null){
				dialog.setMax(progressMax);
			}
		}		
	}

	/**
	 * Create/update all the people who are in this group and add to group.
	 * @param groupId the group to add the people to
	 * @return The results of local updates
	 */
	private WhichUsersCreated createPeople(long groupId){

		// the users adapter
		UsersAdapter users = new UsersAdapter(applicationCtx);

		// the user/group adapter
		UsersInGroupsAdapter usersInGroupsAdapter = new UsersInGroupsAdapter(applicationCtx);

		// keep track of new additions and deletions
		HashSet<Long> newAdditions = new HashSet<Long>();
		HashSet<Long> deletions = new HashSet<Long>();
		HashSet<Long> newIds = new HashSet<Long>();

		// get list of contacts already in the group
		users.fetchUsersInGroup(groupId);
		HashSet<Long> oldIds = new HashSet<Long>(users.size());
		while (users.moveToNext())
			oldIds.add(users.getRowId());
		users.close();

		// get list of new contacts to add to group
		Set<Long> keys = mContactChecked.getCheckedKeys();
		Iterator<Long> iterator = keys.iterator();
		while (iterator.hasNext()){
			Long id = mContactChecked.getRowId(iterator.next());
			newIds.add(id);
			if (!oldIds.contains(id))
				newAdditions.add(id);
		}

		// get list of people to remove from group
		Iterator<Long> iterator2 = oldIds.iterator();
		while (iterator2.hasNext()){
			Long id = iterator2.next();
			if (!newIds.contains(id))
				deletions.add(id);
		}

		// change progress
		progressMax = newAdditions.size() + deletions.size();
		dialog.setMax(progressMax);
		usersAdded = newAdditions.size();
		usersRemoved = deletions.size();

		// initialize ouptut
		WhichUsersCreated output = new WhichUsersCreated();
		output.newUsers = new ArrayList<Long>(newAdditions);
		output.deletions = new ArrayList<Long>(deletions);

		// loop over all contacts and adding them to database and to group
		int i = 1;
		for (Long id : output.newUsers) {

			// check we have add successfully
			if (id < 0){
				output.isSuccess = false;
				return output;
			}

			// add to group
			long linkId = usersInGroupsAdapter.addUserToGroup(id, groupId);

			// check we have add successfully
			if (linkId < 0){
				output.isSuccess = false;
				return output;
			}else
				output.linkIdsNewUsers.add(linkId);

			// publish progress
			publishProgress(i);
			i++;
		}

		output.isSuccess = true;
		return output;
	}

	public static class ReturnFromAddUsersToGroupTask
	extends ShareBearServerReturn{

		// responase codes
		public enum ResponseCode{
			Temp_User_Created_Invite_Sent,
			Temp_User_Exists_Invite_Resent,
			User_Exists_Already_In_Group,
			User_Exists_Added_To_Group,
			Server_Error;

			public String getName(){
				return name();
			}
		}

		// response keys
		private static final String KEY_CODE = "user_message_code";
		private static final String USER_ID = "user_id";

		// member fields
		SparseArray<JSONObject> userObjects = new SparseArray<JSONObject>();

		// error codes
		public static final String CONVERSION_CODE = "CONVERSION_CODE";
		private static final String CONVERSION_STRING = "Bad server return";

		private ReturnFromAddUsersToGroupTask(ServerReturn toCopy) {
			super(toCopy);ResponseCode.Temp_User_Created_Invite_Sent.name();
		}

		private ReturnFromAddUsersToGroupTask() {
			super();
		}

		private ReturnFromAddUsersToGroupTask(String errorCode, String detailErrorMessage){
			super();
			setError(errorCode, detailErrorMessage);
		}

		/**
		 * If we had a local error (we couldn't add users locally) then return true, else false
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
			if (isSuccess() || !isLocalError())
				return true;
			else
				return false;
		}

		/**
		 * Return the code returned for this user.
		 * @param index The index of which item was return to grab
		 * @return The code returned by the server. Will not be null
		 */
		public ResponseCode getUserCode(int index){		

			// default
			ResponseCode value = ResponseCode.Server_Error;

			// return null if unsuccessful
			if (!isSuccess())
				return value;

			// grab the code
			JSONObject json = getItemObject(index);
			if(json == null)
				return value;
			String string = json.optString(KEY_CODE);

			// convert to responseCode
			try{
				value = ResponseCode.valueOf(string);
			}catch(IllegalArgumentException e){
				Log.e(Utils.LOG_TAG, "unknown return from server for user: "+ string);
			}catch(NullPointerException e){
				Log.e(Utils.LOG_TAG, "null return from server for user");
			}
			return value;
		}

		/**
		 * Return the server id of the user at the given index
		 * @param index The index of which item was return to grab
		 * @return The serverId of the user, -1 if not found
		 */
		public long getUserServerId(int index){		

			// return null if unsuccessful
			if (!isSuccess())
				return -1;

			// grab the user serverId
			JSONObject json = getItemObject(index);
			if(json == null)
				return -1;
			return json.optLong(USER_ID, -1);
		}

		/**
		 * Get the json object for this given index, null if unsuccessful
		 * @param index The index of which item to grab info from
		 * @return The object for this thumbnail, or null
		 */
		private JSONObject getItemObject(int index){
			// return null if unsuccessful
			if (!isSuccess())
				return null;

			// see if we've retreived it already
			JSONObject item = userObjects.get(index);
			if (item != null)
				return item;

			// grab the item at this index
			JSONArray array = getMessageArray();
			if (array == null)
				return null;
			item = array.optJSONObject(index);
			userObjects.put(index, item);
			return item;
		} 	
	}

	public static class ReturnFromAddUsersToGroupTaskOld
	extends ShareBearServerReturn{

		// responase codes
		public enum ResponseCode{
			Temp_User_Created_Invite_Sent,
			Temp_User_Exists_Invite_Resent,
			User_Exists_Already_In_Group,
			User_Exists_Added_To_Group,
			Server_Error;

			public String getName(){
				return name();
			}
		}

		// response keys
		private static final String KEY_CODE = "user_message_code";

		// member fields
		HashMap<Long, JSONObject> userObjects = new HashMap<Long, JSONObject>(10);

		// error codes
		public static final String CONVERSION_CODE = "CONVERSION_CODE";
		private static final String CONVERSION_STRING = "Bad server return";

		private ReturnFromAddUsersToGroupTaskOld(ServerReturn toCopy) {
			super(toCopy);ResponseCode.Temp_User_Created_Invite_Sent.name();
		}

		private ReturnFromAddUsersToGroupTaskOld() {
			super();
		}

		private ReturnFromAddUsersToGroupTaskOld(String errorCode, String detailErrorMessage){
			super();
			setError(errorCode, detailErrorMessage);
		}

		/**
		 * If we had a local error (we couldn't add users locally) then return true, else false
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
			if (isSuccess() || !isLocalError())
				return true;
			else
				return false;
		}

		/**
		 * Return the code returned for this user.
		 * @param serverId The serverId of the thumbnail data
		 * @return The code returned by the server. Will not be null
		 */
		public ResponseCode getUserCode(long serverId){		

			// default
			ResponseCode value = ResponseCode.Server_Error;

			// return null if unsuccessful
			if (!isSuccess())
				return value;

			// grab the code
			JSONObject json = getItemObject(serverId);
			if(json == null)
				return value;
			String string = json.optString(KEY_CODE);

			// convert to responseCode
			try{
				value = ResponseCode.valueOf(string);
			}catch(IllegalArgumentException e){
				Log.e(Utils.LOG_TAG, "unknown return from server for user: " +serverId + ", "+ string);
			}catch(NullPointerException e){
				Log.e(Utils.LOG_TAG, "null return from server for user: " +serverId);
			}
			return value;
		}

		/**
		 * Get all the serverIds that were return, will be null if unsuccessful.
		 * @return The iterator, or null if we had an error
		 */
		public Iterator<String> getUserServerIds(){
			// return null if unsuccessful
			if (!isSuccess())
				return null;

			// grab the message
			JSONObject json = getMessageObject();
			if (json == null)
				return null;

			// the keys
			@SuppressWarnings("unchecked") // legacy api
			Iterator<String> iterator = json.keys();

			return iterator;
		}

		/**
		 * Get the json object for this given serverId, null if unsuccessful
		 * @param serverId The serverId of the object to get
		 * @return The object for this thumbnail, or null
		 */
		private JSONObject getItemObject(long serverId){
			// return null if unsuccessful
			if (!isSuccess())
				return null;

			// see if we've retreived it already
			JSONObject message = userObjects.get(serverId);
			if (message != null)
				return message;

			// grab the item at this index
			message = getMessageObject();
			if (message == null)
				return null;
			JSONObject item = message.optJSONObject(String.valueOf(serverId));
			userObjects.put(serverId, item);
			return item;
		} 	
	}
}
