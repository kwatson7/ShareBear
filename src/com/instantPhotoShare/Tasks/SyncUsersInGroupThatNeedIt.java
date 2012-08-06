package com.instantPhotoShare.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ServerKeys;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersInGroupsAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.ServerReturn;

public class SyncUsersInGroupThatNeedIt <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Integer, Void>{

	//TODO: massive overlap in code between this and AddUsersToGroupTask - consolidate
		
	// codes to be sent to server
	private static final String ACTION = ServerKeys.AddUsersToGroup.COMMAND;
	private static final String KEY_USER_ID = ServerKeys.AddUsersToGroup.POST_KEY_USER_ID;
	private static final String KEY_SECRET_CODE = ServerKeys.AddUsersToGroup.POST_KEY_SECRET_CODE;
	private static final String KEY_PERSON_F_NAME = ServerKeys.AddUsersToGroup.POST_KEY_PERSON_F_NAME;
	private static final String KEY_PERSON_L_NAME = ServerKeys.AddUsersToGroup.POST_KEY_PERSON_L_NAME;
	private static final String KEY_PERSON_EMAIL = ServerKeys.AddUsersToGroup.POST_KEY_PERSON_EMAIL;
	private static final String KEY_PHONE_NUMBER = ServerKeys.AddUsersToGroup.POST_KEY_PHONE_NUMBER;
	private static final String KEY_GROUP_ID = ServerKeys.AddUsersToGroup.POST_KEY_GROUP_ID;
	private static final String KEY_CONTACT_METHOD = "contact_method";
	

	/**
	 * Sync all users that have been added to groups, but not on the server
	 * @param act The calling activity
	 * @param requestId An id code signifying which task was called
	 * @param mContactChecked Object holding data about users that have been selected
	 * @param groudRowId the group id from the device sql database to add users to
	 */
	public SyncUsersInGroupThatNeedIt(
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

		// the required adapters
		UsersInGroupsAdapter usersInGroups = new UsersInGroupsAdapter(applicationCtx);
		UsersAdapter users = new UsersAdapter(applicationCtx);

		// find all the groups and then loop over them
		GroupsAdapter groups = new GroupsAdapter(applicationCtx);
		groups.fetchAllGroups();
		while(groups.moveToNext()){

			// if the group is private or doesn't have an id, then skip
			if (groups.getServerId() == 0 || groups.getServerId() == -1 || groups.isKeepLocal())
				continue;

			// fetch the links that need to be updated
			usersInGroups.fetchLinksNeedUploading(groups.getRowId());

			// create an arraylist of which users to add
			ArrayList<Long> newUsers = new ArrayList<Long>(usersInGroups.size());

			// keep track of which links we are updating
			ArrayList<Long> links = new ArrayList<Long>(usersInGroups.size());

			// do the iterating
			while(usersInGroups.moveToNext()){
				long id = usersInGroups.getUserRowId();
				if (id != -1){
					newUsers.add(id);
					links.add(usersInGroups.getRowId());
				}
			}
			usersInGroups.close();

			// no new links
			if (newUsers.size() == 0)
				continue;

			// upload to server
			usersInGroups.setIsUpdating(links, true);
			ReturnFromAddUsersToGroupTask serverResponse = new ReturnFromAddUsersToGroupTask();
			try {
				serverResponse = new ReturnFromAddUsersToGroupTask(
						Utils.postToServer(
								ACTION, getDataToPost(
										newUsers, groups.getServerId(), applicationCtx), null, null));
			} catch (JSONException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				continue;
			}

			// loop across returned users to check if we added successfully
			if (serverResponse.isSuccess()){

				for (int i = 0; i < serverResponse.getMessageArray().length(); i++){

					// grab the server ids and row ids and link ids
					long userRowId = newUsers.get(i);
					long linkId = links.get(i);
					long userServerId = serverResponse.getUserServerId(i);

					// loop across all switch possibilities
					com.instantPhotoShare.Tasks.SyncUsersInGroupThatNeedIt.ReturnFromAddUsersToGroupTask.ResponseCode response =
							serverResponse.getUserCode(i);			
					switch(response){
					case Server_Error:
						users.setIsUpdating(userRowId, false);
						Log.e(Utils.LOG_TAG, "user did not update for unknown reason: " + serverResponse.getErrorCode());
						break;
					case No_User_Account_Invites_Sent:
						users.setIsSynced(userRowId, true, -1, false);
						usersInGroups.setIsSynced(linkId, true);
						break;
					case User_Added_To_Group:
						users.setIsSynced(userRowId, true, userServerId, true);
						usersInGroups.setIsSynced(linkId, true);
						break;
					case User_Already_In_Group:
						users.setIsSynced(userRowId, true, userServerId, true);
						usersInGroups.setIsSynced(linkId, true);
						break;
					default:
						users.setIsUpdating(userRowId, false);
						Log.e(Utils.LOG_TAG, "bad response from server for addUserToGroup" + response.getName());
					}
				}
			}else{
				Log.e(Utils.LOG_TAG, "Bad return from server while adding users to groups");
			}
		}

		// close the group cursor
		groups.close();

		return null;

	}

	/**
	 * Return a list of parameter value pairs required to create group on server
	 * @param newAdditions Which userRowIds are we goign to add to the group
	 * @param groupServerId The group serverId to which we are adding
	 * @return The json data to post to server
	 * @throws JSONException
	 */
	private static JSONArray getDataToPost(ArrayList<Long> newAdditions, long groupServerId, Context ctx)
			throws JSONException{	

		// initialize the array
		JSONArray jsonArray = new JSONArray();

		// add top level values
		JSONObject top = new JSONObject();
		top.put(KEY_USER_ID, Prefs.getUserServerId(ctx));
		top.put(KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
		jsonArray.put(top);

		// initalize the users
		UsersAdapter users = new UsersAdapter(ctx);

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
	}

	@Override
	protected void onPostExectueOverride(Void result) {

	}

	@Override
	protected void setupDialog() {

	}

	private static class ReturnFromAddUsersToGroupTask
	extends ShareBearServerReturn{

		// responase codes
		public enum ResponseCode{
			No_User_Account_Invites_Sent,
			User_Already_In_Group,
			User_Added_To_Group,
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

		private ReturnFromAddUsersToGroupTask(ServerReturn toCopy) {
			super(toCopy);
		}

		private ReturnFromAddUsersToGroupTask() {
			super();
		}

		private ReturnFromAddUsersToGroupTask(String errorCode, String detailErrorMessage){
			super();
			setError(errorCode, detailErrorMessage);
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
}
