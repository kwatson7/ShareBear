package com.instantPhotoShare.Tasks;

import android.app.ProgressDialog;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.ContactCursorWrapper;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

public class MakeGroupTask 
extends CustomAsyncTask<Void, Integer, Long>{

	// private variables
	private ContactCheckedArray mContactChecked;
	private ContactCursorWrapper mNamesCursor;
	private String groupName;
	private int progressMax = 1;
	private boolean allowOthersToAddMembers;
	
	/**
	 * Make a group and put it into the groups database and then add all the users to it.
	 * @param act The calling activity
	 * @param requestId An id code signifying which task was called
	 * @param progressBars A string array of progress bars
	 * @param mContactChecked
	 * @param mNamesCursor
	 * @param groupName
	 */
	public MakeGroupTask(
			CustomActivity act,
			int requestId,
			ContactCheckedArray mContactChecked,
			ContactCursorWrapper mNamesCursor,
			String groupName,
			boolean allowOthersToAddMembers) {
		super(act,
				requestId,
				true,
				false,
				null);
		
		// store inputs
		this.groupName = groupName;
		this.mContactChecked = mContactChecked;
		this.mNamesCursor = mNamesCursor;
		progressMax = Math.max(mContactChecked.getNChecked(), 1);
		dialog.setMax(progressMax); // this needs to be done here, because setupDialog is called before we've stored mContactChecked
		this.allowOthersToAddMembers = allowOthersToAddMembers;
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected Long doInBackground(Void... params) {
		// make the new group
		GroupsAdapter groups = new GroupsAdapter(applicationCtx);
		long rowId = groups.makeNewGroup(
				applicationCtx,
				groupName,
				-1,
				Utils.getNowTime(),
				Prefs.getUserId(applicationCtx),
				allowOthersToAddMembers,
				null,
				null,
				-1);
		
		// if we were not successful
		if (rowId == - 1)
			return rowId;
		
		// now add memebrs to group
		

		return rowId;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
		
	}

	@Override
	protected void onPostExectueOverride(Long result) {
		// TODO Auto-generated method stub
		
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
			dialog.setCancelable(false);	
			dialog.setProgress(0);
			if (mContactChecked != null){
				dialog.setMax(progressMax);
			}
		}		
	}

}
