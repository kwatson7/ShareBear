package com.instantPhotoShare.Tasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.ContactCheckedArray.ContactCheckedItem;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

public class LoadGroupUsers <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Integer, ContactCheckedArray>{

	// member variables
	private long groupId; 					// the group id we are loading
	private boolean cancelTask = false;
	
	public LoadGroupUsers(
			ACTIVITY_TYPE act,
			int requestId,
			long groupId) {
		super(
				act,
				requestId,
				false,
				true,
				null);
		this.groupId = groupId;
	}

	@Override
	protected void onPreExecute() {		
	}

	@Override
	protected ContactCheckedArray doInBackground(Void... params) {
		
		if (callingActivity == null)
			return null;
		
		// search for all users in this group
		UsersAdapter users = new UsersAdapter(this.callingActivity);
		users.fetchUsersInGroup(groupId);
		
		// initialize the contact checked array
		ContactCheckedArray	contactChecked = new ContactCheckedArray();
		
		// set the max of the dialog
		dialog.setMax(users.size());
		
		// fill the array with all the users
		while (users.moveToNext()){
			if (callingActivity == null)
				return null;
			contactChecked.setItem(new ContactCheckedItem(
					users.getContactDatabaseRowId(),
					true,
					users.getName(),
					users.getDefaultContactMethod(),
					users.getLookupKey(),
					users.getRowId()));
			this.publishProgress(users.getPosition()+1);
			
			if(cancelTask){
				return null;
			}
		}
		
		// close the group
		users.close();
		
		return contactChecked;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExectueOverride(ContactCheckedArray result) {
		if (callingActivity != null){
			sendObjectToActivityFromPostExecute(result);
		}
		
	}

	@Override
	protected void setupDialog() {
		// show dialog for this long process
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setMessage("Please wait...");
			dialog.setTitle("Loading Users");
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					cancelTask = true;
				}
			});
		}			
	}
}
