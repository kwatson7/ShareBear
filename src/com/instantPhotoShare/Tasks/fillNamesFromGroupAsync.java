package com.instantPhotoShare.Tasks;

import java.util.ArrayList;
import java.util.Collections;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.instantPhotoShare.ContactCheckedArray;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.TwoObjects;
import com.tools.TwoStrings;

public class fillNamesFromGroupAsync <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Integer, ArrayList <TwoObjects<String, Long>>> {

	// private variables
	private ArrayList<TwoStrings> mGroupList; 			// The list of groups
	private ContactCheckedArray mContactChecked; 		// The array of people that are already checked
	private int position; 								// the position in the group that was selected
	
	/**
	 * Constructor for filling names into CreateGroup.java from contacts database.
	 * @param act
	 * @param requestId
	 * @param isAttachToApplication
	 * @param isCancelOnActivityDestroy
	 * @param progressBars
	 * @param mGroupList
	 * @param position the position int he groupslist that was selected
	 */
	public fillNamesFromGroupAsync(
			ACTIVITY_TYPE act,
			int requestId,
			ArrayList<TwoStrings> mGroupList,
			ContactCheckedArray mContactChecked,
			int position) {
		
		// super call
		super(act, requestId, true, true, null);
		
		// store variables
		this.mGroupList = mGroupList;
		this.mContactChecked = mContactChecked;
		this.position = position;
	}

	@Override
	protected ArrayList <TwoObjects<String, Long>> doInBackground(Void... params) {
		
		// check we dont' have a null activity.
		if (applicationCtx == null){
			this.cancel(true);
			return null;
		}
		
		// grab which group was selected
		String groupId = mGroupList.get(position).mObject2;
		if (groupId == null)
			return null;

		// keep track of which names we are trying to add
		final ArrayList <TwoObjects<String, Long>> namesToAdd = 
			new ArrayList<TwoObjects<String, Long>>();

		// grab ids from database of names taht are int he group
		if (applicationCtx == null){
			this.cancel(true);
			return null;
		}
		Cursor cursor = com.tools.CustomCursors.getContactInfoInGroup
		(applicationCtx.getContentResolver(), groupId, true, new String[] {
			ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID, 
			ContactsContract.CommonDataKinds.GroupMembership.DISPLAY_NAME});

		// leave if no cursor
		if (cursor==null)
			return null;

		if (!cursor.moveToFirst())
			return null;

		// fill checkboxes
		int colDisplayName = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		int colRawId = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID);
		long rawId;

		Boolean isChecked;
		do{
			// grab contact ids and display names
			rawId = cursor.getLong(colRawId);
			String displayName = cursor.getString(colDisplayName);
			
			isChecked = mContactChecked.isChecked((long)colRawId);
			if (isChecked==null || !isChecked){

				// keep track of names that we are adding
				namesToAdd.add(new TwoObjects<String, Long>(displayName, rawId));
			}

		}while(cursor.moveToNext());

		//sort namesToAdd alphabetically
		Collections.sort(namesToAdd);
	
		return namesToAdd;
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		
	}

	@Override
	protected void onPostExectueOverride(
			final ArrayList<TwoObjects<String, Long>> namesToAdd) {
		
		// check we dont' have a null activity.
		if (callingActivity == null){
			this.cancel(true);
			return;
		}
		this.sendObjectToActivityFromPostExecute(namesToAdd);		
	}

	@Override
	protected void setupDialog() {
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Retrieving contacts");
			dialog.setMessage( "Please wait...");
			dialog.setIndeterminate(true);
		}
	}	
}