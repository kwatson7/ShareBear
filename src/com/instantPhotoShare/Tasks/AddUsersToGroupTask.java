package com.instantPhotoShare.Tasks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.widget.Toast;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersInGroupsAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.ServerReturn;
import com.tools.SuccessReason;
import com.tools.ThreeObjects;

public class AddUsersToGroupTask <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Integer, AddUsersToGroupTask.ReturnFromAddUsersToGroupTask>{

	// constants
	private static final String LOCAL_TITLE = "Saving Users to Group";
	private static final String SERVER_TITLE = "Uploading to Server";
		
	// private variables
	private ContactCheckedArray mContactChecked;
	private int progressMax = 1;
	private boolean cancelTask = false;
	private long groudRowId = -1;
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
		
		
		// make sure group exists
		GroupsAdapter groups = new GroupsAdapter(applicationCtx);
		Group group = groups.getGroup(groudRowId);
		if (group == null)
			throw new IllegalArgumentException("groupRowId input into AddUsersToGroupTask does not exist");
	}

	private int getTotalEdits(){
		return usersAdded + usersRemoved;
	}
	@Override
	protected void onPreExecute() {

	}

	@Override
	protected AddUsersToGroupTask.ReturnFromAddUsersToGroupTask doInBackground(Void... params) {
		
		// make the new group
		ThreeObjects<SuccessReason, HashSet<Long>, HashSet<Long>> out = 
				createPeople(groudRowId);
		SuccessReason localResult = out.mObject1;
		HashSet<Long> newUsers = out.mObject2;
		HashSet<Long> deletions = out.mObject3;
		
		// if we errored, then stop
		if (!localResult.getSuccess()){
			ReturnFromAddUsersToGroupTask serverResponse = 
					new ReturnFromAddUsersToGroupTask(LOCAL_CREATION_ERROR, ERROR_MESSAGE);
			return serverResponse;
		}

		// no one to edit, so return
		if (getTotalEdits() == 0)
			return new ReturnFromAddUsersToGroupTask();
				
		// now add users to server
		//TODO: set the links and users to be updating
		//TODO: convert non json server return to proper error
		ReturnFromAddUsersToGroupTask serverResponse = new ReturnFromAddUsersToGroupTask();
		try {
			serverResponse = new ReturnFromAddUsersToGroupTask(Utils.postToServer(ACTION, getDataToPost(newUsers, deletions), null, null));
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse.setError(e);
		}
		
		// set serverids of the users
		//TODO: set sync and update status of links
		//TODO: be able to delte users, locally and on server
		if (serverResponse.isSuccess()){
			UsersAdapter users = new UsersAdapter(applicationCtx);
			JSONArray array = serverResponse.getMessageArray();
			Iterator<Long> iterator = newUsers.iterator();
			for (int i = 0; i < array.length(); i++){
				long id = array.optLong(i, 0);
				if (id != 0){
					users.setIsSynced(iterator.next(), true, id);
				}else{
					users.setIsUpdating(iterator.next(), false);
					Log.e(Utils.LOG_TAG, "user did not update for unknown reason: " + serverResponse.getErrorCode());
				}
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
	private JSONArray getDataToPost(HashSet<Long> newAdditions, HashSet<Long> deletions)
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
		Iterator<Long> iterator = newAdditions.iterator();
		while(iterator.hasNext()){
			
			// grab the user info
			Long id = iterator.next();
			users.fetchUser(id);
			JSONObject json = new JSONObject();
			json.put(KEY_PERSON_F_NAME, users.getFirstName());
			json.put(KEY_PERSON_L_NAME, users.getLastName());
			json.put(KEY_PHONE_NUMBER, users.getPhones());
			json.put(KEY_PERSON_EMAIL, users.getEmails());
			json.put(KEY_CONTACT_METHOD, users.getDefaultContactMethod());
			json.put(KEY_GROUP_ID, groudRowId);
			
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
		if (getTotalEdits() == 0){
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

			// server error
		}else{
			// making a dialog has rotation problems, so just allow local creation with server error

			// show the toast
			Toast.makeText(applicationCtx,
					getTotalEdits() + " users updated to " + group.getName()
					+ " on device, but not added to server because:\n" + result.getDetailErrorMessage(),
					Toast.LENGTH_LONG).show();

			// store in notifications
			NotificationsAdapter notes = new NotificationsAdapter(applicationCtx);
			notes.createNotification("Group with name '" + group.getName() + "' and rowId " + groudRowId +
					" users not updated successfully on server because:\n"
					+ result.getDetailErrorMessage() + ".\nUsers are still created on device, but not on server!", 
					NOTIFICATION_TYPES.SERVER_ERROR);

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
			dialog.setCancelable(true);
			if (mContactChecked != null){
				dialog.setMax(progressMax);
			}
			dialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					cancelTask = true;

				}
			});
		}		
	}

	/**
	 * Create/update all the people who are in this group and add to group.
	 */
	private ThreeObjects<SuccessReason, HashSet<Long>, HashSet<Long>> createPeople(long groupId){

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
		ThreeObjects<SuccessReason, HashSet<Long>, HashSet<Long>> output = 
				new ThreeObjects<SuccessReason, HashSet<Long>, HashSet<Long>>(null, newAdditions, deletions);

		// loop over all contacts and adding them to database and to group
		int i = 1;
		for (Long id : newAdditions) {

			// check we have add successfully
			if (id < 0){
				output.mObject1 = new SuccessReason(false, "User could not be added.");
				return output;
			}

			// add to group
			long linkId = usersInGroupsAdapter.addUserToGroup(applicationCtx, id, groupId);

			// check we have add successfully
			if (linkId < 0){
				output.mObject1 = new SuccessReason(false, "Link between users and groups could not be added");
				return output;
			}

			// publish progress
			publishProgress(i);
			i++;

			if(cancelTask){
				GroupsAdapter groups = new GroupsAdapter(applicationCtx);
				groups.deleteGroup(groupId);
				output.mObject1 = new SuccessReason(false, "User cancelled action.");
				return output;
			}
		}

		output.mObject1 = new SuccessReason(true);
		return output;
	}

	public static class ReturnFromAddUsersToGroupTask
	extends ShareBearServerReturn{

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

		@Override
		protected boolean isSuccessCustom2() {
			JSONArray array = getMessageArray();
			if (array == null)
				return false;
			else
				return true;
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
		
	}
}
