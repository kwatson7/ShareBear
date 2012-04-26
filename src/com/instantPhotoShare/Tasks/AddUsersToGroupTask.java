package com.instantPhotoShare.Tasks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.widget.Toast;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ServerJSON;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersInGroupsAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.SuccessReason;

public class AddUsersToGroupTask 
extends CustomAsyncTask<Void, Integer, AddUsersToGroupTask.ReturnFromAddUsersToGroupTask>{

	// private variables
	private ContactCheckedArray mContactChecked;
	private int progressMax = 1;
	private boolean cancelTask = false;
	private AddUsersToGroupTask task = this;
	private long groudRowId = -1;
	private int usersAdded = 0;
	private int usersRemoved = 0;

	// codes to be sent to server
	private static final String ACTION = "add_user_to_group";
	private static final String KEY_USER_ID = "user_id";
	private static final String KEY_SECRET_CODE = "secret_code";
	private static final String PERSON_F_NAME = "person_fname";
	private static final String PERSON_L_NAME = "person_lname";
	private static final String PERSON_EMAIL = "person_email";
	private static final String PHONE_NUMBER = "phone_number";
	private static final String GROUP_ID = "group_id";

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
			CustomActivity act,
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

		//TODO: add the functionality to remove people from groups
		//TODO: update / add users on server
		//TODO: add logic of who can remove users
		
		// make the new group
		SuccessReason localResult = createPeople(groudRowId);

		// if we errored, then stop
		if (!localResult.getSuccess()){
			ReturnFromAddUsersToGroupTask serverResponse = 
				new ReturnFromAddUsersToGroupTask(ServerJSON.getDefaultFailure());
			serverResponse.setErrorMessage(ERROR_MESSAGE, LOCAL_CREATION_ERROR);
			return serverResponse;
		}

		// now add users to server
		ReturnFromAddUsersToGroupTask serverResponse = 
			new ReturnFromAddUsersToGroupTask(ServerJSON.getDefaultFailure());

		// return the value
		return serverResponse;
	}

	/**
	 * Return a list of parameter value pairs required to create group on server
	 * @return a JSOBObject
	 * @throws JSONException 
	 */
	private JSONObject getDataToPost()
	throws JSONException{	

		[
{
"person_fname":"Brennan",
"person_lname":"Heyde",
"person_email":"test@12ds3.com,bheyde1@gsdsdmail.com",
"phone_number": "555-554-5555,750-809-4756,1-800-123-456",
"group_id":"29"
},
{
"person_fname":"Kyle",
"person_lname":"Watson",
"person_email":"kwatson7@gmail.com",
"phone_number": "123-345-5555",
"group_id":"7"
}
]

		// add values to json object
		JSONObject json = new JSONObject();
		json.put(KEY_USER_ID, Prefs.getUserServerId(applicationCtx));
		json.put(KEY_SECRET_CODE, Prefs.getSecretCode(applicationCtx));
		return json;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExectueOverride(AddUsersToGroupTask.ReturnFromAddUsersToGroupTask result) {

		if (applicationCtx == null)
			return;

		// no edits
		if (getTotalEdits() == 0){
			Toast.makeText(applicationCtx, "No changes", Toast.LENGTH_SHORT).show();
			task.sendObjectToActivityFromPostExecute(result);
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
			task.sendObjectToActivityFromPostExecute(result);
			return;
		}

		// if not successful, then see how
		// local error
		if (result.isLocalError()){
			Toast.makeText(applicationCtx, result.getMessage(), Toast.LENGTH_LONG).show();
			return;

			// server error
		}else{
			// making a dialog has rotation problems, so just allow local creation with server error

			// show the toast
			Toast.makeText(applicationCtx,
					getTotalEdits() + " users updated to " + group.getName()
					+ " on device, but not added to server because:\n" + result.getMessage(),
					Toast.LENGTH_LONG).show();

			// store in notifications
			NotificationsAdapter notes = new NotificationsAdapter(applicationCtx);
			notes.createNotification("Group with name '" + group.getName() + "' and rowId " + groudRowId +
					" users not updated successfully on server because:\n"
					+ result.getMessage() + ".\nUsers are still created on device, but not on server!", 
					NOTIFICATION_TYPES.SERVER_ERROR);

			// send the result back to calling activity
			task.sendObjectToActivityFromPostExecute(result);
		}
	}

	@Override
	protected void setupDialog() {
		// show dialog for this long process
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setMessage("Saving users to group. Please wait...");
			dialog.setTitle("Creating Group");
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
	private SuccessReason createPeople(long groupId){

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

		// loop over all contacts and adding them to database and to group
		int i = 1;
		for (Long id : newAdditions) {

			// create new user or update old one
			long userId = users.makeNewUser(
					applicationCtx,
					id,
					mContactChecked.getLookupKey(id),
					mContactChecked.getDefaultContactMethod(id),
					-1);

			// check we have add successfully
			if (userId < 0)		
				return new SuccessReason(false, "User could not be added.");

			// add to group
			long linkId = usersInGroupsAdapter.addUserToGroup(applicationCtx, userId, groupId);

			// check we have add successfully
			if (linkId < 0)
				return new SuccessReason(false, "Link between users and groups could not be added");

			// publish progress
			publishProgress(i);
			i++;

			if(cancelTask){
				GroupsAdapter groups = new GroupsAdapter(applicationCtx);
				groups.deleteGroup(groupId);
				return new SuccessReason(false, "User cancelled action.");
			}
		}

		return new SuccessReason(true);
	}

	public class ReturnFromAddUsersToGroupTask
	extends ServerJSON{

		// KEYS in JSON
		private static final String KEY_GROUP_ID = "group_id";;

		/**
		 * Intiailize a ReturnFromAddUsersToGroupTask object from a ServerJSON object.
		 * @param toCopy
		 */
		protected ReturnFromAddUsersToGroupTask(ServerJSON toCopy) {
			super(toCopy);
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
				return jsonObject.getLong(KEY_GROUP_ID);
			} catch (JSONException e) {
				return -1;
			}
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
