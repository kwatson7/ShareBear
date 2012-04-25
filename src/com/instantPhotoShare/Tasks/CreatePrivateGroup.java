package com.instantPhotoShare.Tasks;

import java.util.ArrayList;

import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NOTIFICATION_TYPES;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

public class CreatePrivateGroup
extends CustomAsyncTask<Void, Void, String>{

	private static boolean isRunning = false;
	/**
	 * Check if we have any private groups. If not, then create one.
	 * @param act The calling custom activity
	 * @param requestId The request Id
	 */
	public CreatePrivateGroup(
			CustomActivity act,
			int requestId) {
		super(
				act,
				requestId,
				true,
				false,
				null);
		
		// if already running, then cancel it
		if (isRunning)
			cancel(true);
		else
			isRunning = true;
	}
	
	@Override
	protected void onCancelled(){
		// we keep track if we've cancelled this task
		super.onCancelled();
		isRunning = false;
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected String doInBackground(Void... params) {
		// check for private groups, and then create if empty
		GroupsAdapter groupsAdapter = new GroupsAdapter(applicationCtx);
		ArrayList<Group> groups = groupsAdapter.fetchPrivateGroups();
		long id = -1;
		if (groups.size() == 0){
			id = groupsAdapter.makeDefaultPrivateGroup(applicationCtx);
			if (id == -1){
				NotificationsAdapter notes = new NotificationsAdapter(applicationCtx);
				String message = "Cannot create default private group for unknown reason.";
				notes.createNotification(message, NOTIFICATION_TYPES.DEVICE_ERROR);
				return message;
			}
		}
		
		// set Preferences
		if (applicationCtx != null && id != -1){
			ArrayList<Long> groupIds = Prefs.getGroupIds(applicationCtx);
			if (groupIds == null || groupIds.size() == 0)
				Prefs.setGroupIds(applicationCtx, id);
		}
		
		// done
		return "";
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
		
	}

	@Override
	protected void onPostExectueOverride(String result) {
		isRunning = false;
		if (result.length() != 0 && applicationCtx != null)
			Toast.makeText(
					applicationCtx,
					result,
					Toast.LENGTH_LONG).show();		
	}

	@Override
	protected void setupDialog() {
		
	}
}
