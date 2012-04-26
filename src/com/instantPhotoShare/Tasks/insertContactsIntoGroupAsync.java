package com.instantPhotoShare.Tasks;

import java.util.Hashtable;

import com.instantPhotoShare.ContactCheckedArray;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.widget.Toast;

/**
 * AsyncTask for inserting contacts into new group
 */
public class insertContactsIntoGroupAsync
extends CustomAsyncTask<Void, Integer, Integer> {

	// private variables
	private ContactCheckedArray mContactChecked = null;		// The array of contacts that are checked
	private String accountType; 							// The account type				
	private String accountName; 							// The account name
	private String groupName;								// The group name
	
	/**
	 * The constructor to create a new group and insert their names into the group in google contacts database.
	 * @param act
	 * @param requestId
	 * @param mContactChecked
	 */
	public insertContactsIntoGroupAsync(
			CustomActivity act,
			int requestId,
			ContactCheckedArray mContactChecked,
			String accountType,
			String accountName,
			String groupName) {
		super(
				act,
				requestId,
				true,
				false,
				null);
		
		// save variables
		this.accountName = accountName;
		this.accountType = accountType;
		this.groupName = groupName;
		this.mContactChecked = mContactChecked;
	}

	@Override
	protected Integer doInBackground(Void... objects) {

		// check null app
		if (applicationCtx == null)
			return 0;
		
		// make the new group
		String groupID = com.tools.Tools.makeNewGroup(
				applicationCtx,
				accountType,
				accountName,
				groupName);

		// hash table to keep track of raw contact id, so we dont' duplicate
		Hashtable<Long, Boolean> rawHash = new Hashtable<Long, Boolean>();

		// fill with the selected contacts		
		ContentValues[] allValues = new ContentValues[(int) mContactChecked.getN()];
		int i = 0;
		for (Long value : mContactChecked.getCheckedKeys()) {

			// add all important values to the row
			rawHash.put(value, true);
			ContentValues values = new ContentValues();
			values.put(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
					ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);
			values.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, Long.parseLong(groupID));
			values.put(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID, value);
			allValues[i] = values;
			i++;
		}

		// insert new contacts into group
		if (applicationCtx != null){
			ContentValues[] allValues2 = new ContentValues[i];
			System.arraycopy(allValues, 0, allValues2, 0, i);
			applicationCtx.getContentResolver().bulkInsert(ContactsContract.Data.CONTENT_URI, allValues2);
			applicationCtx.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, allValues2[0]);

			// sync accounts
			com.tools.Tools.requestSync(applicationCtx.getApplicationContext());

			return i;
		}else
			return 0;
	}

	@Override
	protected void onPreExecute() {	}

	@Override
	protected void onProgressUpdate(Integer... progress) {	}

	@Override
	protected void onPostExectueOverride(Integer result) {
		// close dialog
		if (dialog != null)
			dialog.dismiss();
		dialog = null;
		if (applicationCtx != null)
			Toast.makeText(applicationCtx, result + " contacts added to new group", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void setupDialog() {

		// show dialog for this long process
		if (dialog == null && applicationCtx != null)
			dialog = ProgressDialog.show(applicationCtx, "", 
                "Saving Contacts. Please wait...", true);
		
	}
}
