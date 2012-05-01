package com.instantPhotoShare.Activities;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.ContactCheckedArray.ContactCheckedItem;
import com.instantPhotoShare.ContactCursorWrapper;
import com.instantPhotoShare.R;
import com.instantPhotoShare.R.drawable;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Tasks.AddUsersToGroupTask;
import com.instantPhotoShare.Tasks.AddUsersToGroupTask.ReturnFromAddUsersToGroupTask;
import com.instantPhotoShare.Tasks.LoadGroupUsers;
import com.instantPhotoShare.Tasks.fillNamesFromGroupAsync;
import com.instantPhotoShare.Tasks.insertContactsIntoGroupAsync;
import com.tools.*;

public class AddUsersToGroup2
extends CustomActivity{

	// misc private variables
	private Context mCtx = this;						// The context
	private CustomActivity mAct = this;					// This activity
	private ContactCheckedArray mContactChecked = null; // Object keeping track of which contacts are checked
	private boolean canIDeleteMembers = false; 			// if the current user can delte members from this group		
	//TODO: implement cadIDeleteMembers
	
	// for adding people from google groups
	private ArrayList<TwoStrings> mGroupList; 			// This list of group names and column ids, but stored as strings
	private ArrayList<TwoObjects<String, Long>> addFromGroupData;	// data for adding from group. Needs storing for orientation switching
	private long groupId; 								// The group id we are editing
	private String groupName; 							// The group name

	// pointers to graphics objects
	private MultipleCheckPopUp<TwoObjects <String, Long>>  popUp; // The popUp after you've selected a group of who to add
	private AutoCompleteTextView mSearchNameObj; 		// Searching for users by name
	private TextView mNContactsSelectedObj; 			// number of contacts selceted text
	private Button mGoButton; 							// The go button when finished
	private TextView groupNameText; 					// the group name
	private ListView mListView; 						// The list view showing people in group

	// variables for menu items
	private final int SELECT_ALL = Menu.FIRST+0;
	private final int UNSELECT_ALL = Menu.FIRST+1; 
	private final int MAKE_NEW_GROUP = Menu.FIRST+2; 
	private final int CHOOSE_GROUP = Menu.FIRST+3; 
	private final int HELP = Menu.FIRST+4;

	private final int CHANGE_DEFAULT = Menu.FIRST+5;

	// private static constants
	private static final int MAX_CONTACTS_BECAUSE_GOOGLE_SERVICES = 40; 		// max people to make new group, because of google services crashing
	private static final float N_CONTACTS_ANIMATRION_FACTOR = (float) 1.1;		// blowup factor when selecting a new contact
	private static final long MILLI_SECONDS_ANIMATE_N_CONTACTS_SELECTED = 500; 	// animate the textview and take this long to do it
	private static final String GOOGLE_TITLE = "Google"; 						// String for showing that the new group will be a gogole group
	private static final String PHONE_TITLE = "Phone"; 							// string for showing that the new group will be on the phone
	private static final boolean onlyShowContactsFromVisibleGroup = true; 		// only show people who are inside visible groups

	// public strings for passing info
	public static String GROUP_ID = "GROUP_ID";

	//enums for async calls
	private enum ASYNC_CALLS {
		ADD_FROM_GROUP, INSERT_CONTACTS_INTO_GROUP, ADD_USERS_TO_PHOTOSHARE_GROUP, LOAD_FROM_GROUP;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// load in passed info
		Bundle extras = getIntent().getExtras(); 
		if (extras == null)
			throw new IllegalArgumentException("AddUsersToGroup requires passed data");

		// the group id passed
		groupId = extras.getLong(GROUP_ID, -1);
		if (groupId == -1)
			throw new IllegalArgumentException("AddUsersToGroup cannot be called without a groupId passed in");

		// check if group is private
		GroupsAdapter groupsAdapter = new GroupsAdapter(this);
		Group group = groupsAdapter.getGroup(groupId);
		if (group == null){
			Toast.makeText(this, "Group does not exist", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if(group.isKeepLocal()){
			Toast.makeText(this, "Group is private to phone", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		// store group name
		groupName = group.getName();

		initializeLayout();

		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationPropertiesCustom data = (ConfigurationPropertiesCustom) config.customData;
			// if this is a new setup, then restart hashtable, otherwise use old one
			if (data != null){
				mContactChecked = data.mContactChecked;
				if (data.isAddFromGroupDialogShowing  && data.addFromGroupData != null){
					showDialogAfterAddGroup(data.addFromGroupData);
					if (popUp != null)
						popUp.setSelection(data.addFromGroupDialogPosition);
				}
			}
		}

		// load the users into the list
		loadCurrentUsers();

		// update the list view
		updateNContacts();
		updateVisibleView();

		// setup context menu
		registerForContextMenu(mListView);	
		
		// can i delete members
		GroupsAdapter groups = new GroupsAdapter(mCtx);
		groups.fetchGroup(groupId);
		canIDeleteMembers = groups.canIRemoveMembers(mCtx);
		groups.close();
	}

	@Override
	protected void initializeLayout(){

		// initialize the array, because this is needed to initialize layout later
		if (mContactChecked == null)
			mContactChecked = new ContactCheckedArray();

		// grab any text that is already there
		String searchName = null;
		if (mSearchNameObj != null){
			searchName = mSearchNameObj.getText().toString();
		}

		// set the layout
		setContentView(R.layout.add_users_to_group);

		// find pointers to objects
		mSearchNameObj = (AutoCompleteTextView) this.findViewById(R.id.searchName);	
		mNContactsSelectedObj = (TextView) findViewById(R.id.totalContactsNumber);
		mGoButton = (Button) findViewById(R.id.goButton1);
		mListView = (ListView) findViewById(R.id.list);
		groupNameText = (TextView) findViewById(R.id.whoText);

		// set adapter
		LazyAdapter adapter = new LazyAdapter(mCtx, R.layout.person_row);
		mSearchNameObj.setAdapter(adapter);
		mSearchNameObj.setOnItemClickListener(adapter);

		// assign string of old layout
		if (searchName != null)
			mSearchNameObj.setText(searchName);

		// set search name action when enter is clicked
		mSearchNameObj.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				// just hide the keyboard
				if (arg1 == EditorInfo.IME_ACTION_SEARCH){
					com.tools.Tools.hideKeyboard(mCtx, arg0);
					return true;
				}else
					return false;
			}
		});

		// change text view at top
		groupNameText.setText(Html.fromHtml("Edit people in " + "<b>"+groupName+"</b>"));

		// hide the keyboard
		final View dummy = findViewById(R.id.dummyView);
		(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
	}

	@Override 
	public void onCreateContextMenu(ContextMenu menu, View view, 
			ContextMenuInfo menuInfo) { 
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, CHANGE_DEFAULT, 0, "Change Default");	    
	} 

	@Override
	public boolean onSearchRequested() {

		// put focus on searchbox
		mSearchNameObj.requestFocus();

		//show keyboard
		com.tools.Tools.showKeyboard(this, mSearchNameObj);

		return true;//false;  // don't go ahead and show the search box
	}

	/**
	 * Load the current users into the list in an asynctask
	 */
	private void loadCurrentUsers(){

		if (mContactChecked == null || mContactChecked.getNChecked() == 0){
			LoadGroupUsers task = new LoadGroupUsers(mAct, ASYNC_CALLS.LOAD_FROM_GROUP.ordinal(), groupId);
			task.execute();
			addTask(task);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		// which long click did we do
		switch(item.getItemId()) {

		// in this case we are trying to change the default contact method
		case CHANGE_DEFAULT:

			// get the position in the list view
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			int position = info.position;

			// change the default contact method
			changeDefault(position);
			return true;
		}

		// the super method otherwise
		return super.onContextItemSelected(item);
	}

	/** update string in UI for how many contacts have been selected*/
	private void updateNContacts(){

		//grab current string, and if the same as the new one, then just exit
		String currentText = mNContactsSelectedObj.getText().toString();
		String newText = mContactChecked.getNChecked() + " selected.";
		if (currentText.equals(newText))
			return;

		// change the numbers selected
		mNContactsSelectedObj.setText(newText);

		// change the font type
		Typeface tf = mNContactsSelectedObj.getTypeface();
		if (mContactChecked.getNChecked()==0 && (tf==null ||tf.isBold()))
			mNContactsSelectedObj.setTypeface(Typeface.DEFAULT);
		if (mContactChecked.getNChecked() != 0 && (tf==null || !tf.isBold()))
			mNContactsSelectedObj.setTypeface(Typeface.DEFAULT_BOLD);

		// animate the numbers that are selected
		float width = mNContactsSelectedObj.getWidth();
		float height = mNContactsSelectedObj.getHeight();
		float increase = N_CONTACTS_ANIMATRION_FACTOR;
		ScaleAnimation up = new ScaleAnimation((float)1, increase, (float)1, increase, width/2, height/2);
		ScaleAnimation down = new ScaleAnimation(increase, (float)1, increase, (float)1, width/2, height/2);
		up.setDuration(MILLI_SECONDS_ANIMATE_N_CONTACTS_SELECTED);
		down.setDuration(MILLI_SECONDS_ANIMATE_N_CONTACTS_SELECTED);
		mNContactsSelectedObj.startAnimation(up);
		mNContactsSelectedObj.startAnimation(down);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Add the menu items
		MenuItem selectAll = menu.add(0,SELECT_ALL, 0, "Select All");
		MenuItem unselectAll = menu.add(0, UNSELECT_ALL, 0, "Unselect All");
		MenuItem chooseGroup = menu.add(0, CHOOSE_GROUP, 0, "Select Group");
		MenuItem newGroup = menu.add(0, MAKE_NEW_GROUP, 0, "Make New Group");
		MenuItem help = menu.add(0, HELP, 0, "See Help");

		// add icons
		selectAll.setIcon(drawable.emo_im_laughing);
		unselectAll.setIcon(drawable.emo_im_lips_are_sealed);
		chooseGroup.setIcon(drawable.emo_im_happy);
		help.setIcon(drawable.emo_im_wtf);
		newGroup.setIcon(drawable.emo_im_winking);

		// make some inoperative
		selectAll.setEnabled(false);
		chooseGroup.setEnabled(false);
		newGroup.setEnabled(false);
		help.setEnabled(false);

		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {

		// hide keyboard
		com.tools.Tools.hideKeyboard(this, mSearchNameObj);

		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		// decide on what each button should do
		switch(item.getItemId()) {
		case UNSELECT_ALL:
			unselectAll();
			return true;
		case MAKE_NEW_GROUP:
			makeNewGroupClicked();
			return true;
		case CHOOSE_GROUP:
			chooseGroup();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	public void goClicked(View view){

		// check we have a groupId and there are users
		if (groupId == -1){
			Toast.makeText(this, "No people selected to share with", Toast.LENGTH_LONG).show();
			return;
		}

		// Add members to group		
		AddUsersToGroupTask task = new AddUsersToGroupTask(
				mAct,
				ASYNC_CALLS.ADD_USERS_TO_PHOTOSHARE_GROUP.ordinal(),
				mContactChecked,
				groupId);
		addTask(task);
		task.execute();
	}

	/**
	 * We have clicked the make new group button, so launch the dialog to do so
	 */
	private void makeNewGroupClicked(){

		// cannot make a new group with too many contacts because of google services error
		if (mContactChecked.getNChecked() > MAX_CONTACTS_BECAUSE_GOOGLE_SERVICES){
			Toast.makeText(mCtx, 
					"Cannot add more than " + MAX_CONTACTS_BECAUSE_GOOGLE_SERVICES + 
					" contacts to a group at once, because of a google services sync error. Sorry, blame google.",
					Toast.LENGTH_LONG).show();
			return;
		}

		// if nothing selected, then no group
		if (mContactChecked.getNChecked() == 0){
			Toast.makeText(mCtx, 
					"You haven't selected anyone to add to a new group! Select someone first.", 
					Toast.LENGTH_LONG).show();
			return;
		}

		// get google accounts
		ArrayList<String> googleAccounts = com.tools.CustomCursors.getGoogleAccountNames(this);

		// put the accounts into a list with Google as primary string and account name as secondary
		final ArrayList<TwoStrings> accountList = new ArrayList<TwoStrings>(googleAccounts.size()+1);
		for (int i = 0; i < googleAccounts.size(); i++){
			accountList.add(new TwoStrings(GOOGLE_TITLE, googleAccounts.get(i)));
		}

		// add phone to bottom of list
		accountList.add(new TwoStrings(PHONE_TITLE, null));

		// create dialog for selecting and naming group name
		final SelectOptionAndInputPopUp newGroupDialog = new SelectOptionAndInputPopUp(
				mCtx, 
				accountList,
				"Add "+mContactChecked.getNChecked() + " contacts to new group",
				0,
				"Input group name",
				null,
				true);

		// OK clicked, create the new Group
		newGroupDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				// grab the new name, account type, and name
				String groupName = newGroupDialog.getInput();

				//empty name, don't allow
				if (groupName.length() == 0){
					Toast.makeText(mCtx, "Cannot have an empty group name! No group made", Toast.LENGTH_SHORT).show();
					return;
				}
				int accountId = newGroupDialog.getChecked();	
				String type = accountList.get(accountId).mObject1;
				String name = accountList.get(accountId).mObject2;

				// change the google account type
				if (type.equals(GOOGLE_TITLE))
					type = com.tools.CustomCursors.GOOGLE_ACCOUNT_TYPE;

				// change the phone account type
				if (type.equals(PHONE_TITLE)){
					type = null;
					name = null;
				}

				// create the new group with the added contacts
				makeNewGroup(type,
						name,
						groupName);
			}
		});

		// make empty callback for cancel, so the button will show
		newGroupDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {}});

		// show the dialog
		newGroupDialog.show();
	}

	/**
	 * Actually create a new group, launch the asynctask
	 * @param accountType 	The account type to create (ie com.google for google group, or null for on teh phone)
	 * @param accountName 	The account name
	 * @param groupName 	The group name
	 */
	private void makeNewGroup(String accountType,
			String accountName,
			String groupName){

		// launch the async task and add to array of tasks to be managed
		insertContactsIntoGroupAsync task =  new insertContactsIntoGroupAsync(
				mAct,
				ASYNC_CALLS.INSERT_CONTACTS_INTO_GROUP.ordinal(),
				mContactChecked,
				accountType,
				accountName,
				groupName);
		addTask(task);
		task.execute();
	}

	/**
	 * Choose a group to add, by showing a popup dialog
	 */
	private void chooseGroup(){

		// grab list of groups, if we get a null, try again 5 times, and then quit
		mGroupList = com.tools.CustomCursors.getGroupNamesAndIdsList(this);
		if (mGroupList==null){
			for (int i = 0; i<5; i++){
				if (mGroupList != null)
					continue;
				mGroupList = com.tools.CustomCursors.getGroupNamesAndIdsList(this);
			}
		}
		if (mGroupList ==null)
			return;

		// make array adapter to hold group names
		ArrayAdapter<TwoStrings> spinnerArrayAdapter = new ArrayAdapter<TwoStrings>(
				this, android.R.layout.simple_spinner_item, mGroupList);

		// grab standard android spinner
		spinnerArrayAdapter.setDropDownViewResource( R.layout.spinner_layout );

		// create a spinner
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);

		// make listener when spinner is clicked
		spinner.setOnItemSelectedListener(spinnerListener);

		// set adapter and launch it
		spinner.setAdapter(spinnerArrayAdapter);
		spinner.performClick();
	}

	/**
	 * The spinner listener for adding people from a group
	 */
	private Spinner.OnItemSelectedListener spinnerListener = new Spinner.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parentView, 
				View selectedItemView, int position, long id) {

			// just add the people to the group
			fillNamesFromGroup(position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// do nothing, just leave
		}
	};

	/**
	 * Launch the async task for adding people from a group
	 * @param position
	 */
	private void fillNamesFromGroup(int position){

		// launch the async task and add to array of tasks to be managed
		fillNamesFromGroupAsync task =  new fillNamesFromGroupAsync(
				mAct,
				ASYNC_CALLS.ADD_FROM_GROUP.ordinal(),
				mGroupList,
				mContactChecked);
		addTask(task);
		task.execute(position);
	}

	/**
	 * Update the visible view if things have changed.
	 */
	private void updateVisibleView(){		

		// save index and top position
		int index = mListView.getFirstVisiblePosition();
		View v = mListView.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();

		// set adapter
		ListAdapter adapter = new ListAdapter(mAct, mContactChecked);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(adapter);

		// restore
		mListView.setSelectionFromTop(index, top);
	}

	/**
	 * Unselect all contacts
	 */
	private  void unselectAll(){

		//clear hashtable
		mContactChecked.clearAll();

		// update visible items and total items selected
		updateVisibleView();
		updateNContacts();
	}

	private String selectDefaultContactMethod(ContactCursorWrapper contactCursorWrapper, long contactId){

		// get teh default from hash first
		String contactMethod = mContactChecked.getDefaultContactMethod(contactId);
		if (contactMethod != null && contactMethod.length() != 0)
			return contactMethod;

		// get the default contact
		contactMethod = contactCursorWrapper.getDefaultContact(mCtx);

		// return with method if we have a good answer
		if (contactMethod != null && contactMethod.length() > 0)
			return contactMethod;

		// if no default method then search the contacts database and show a dialog
		if (contactMethod == null || contactMethod.length() == 0){
			return chooseMethodFromPopup(contactCursorWrapper, contactId);
		}

		return null;
	}

	/**
	 * Choose the users contact method from popup dialog. If only one is immediatley available, then return it, 
	 * otherwise return null, and then in the onClick listener, the user will be updated.
	 * @param contactId The contactId of the user to affect
	 * @return The string if only one is available, or null if not.
	 */
	private String chooseMethodFromPopupMultiple(final ContactCursorWrapper contactCursorWrapper, final long contactId){

		TwoObjects<HashSet<TwoStrings>, HashSet<TwoStrings>> possibleContactMethods = 
			com.tools.CustomCursors.getContactPhoneAndEmailArray(mCtx, contactId);

		// must have a way to contact, or break out
		if (possibleContactMethods == null ||
				((possibleContactMethods.mObject1 == null || 
						possibleContactMethods.mObject1.size() == 0) &&
						(possibleContactMethods.mObject2 == null || 
								possibleContactMethods.mObject2.size() == 0))){
			Toast.makeText(mCtx,
					"Fail!! No method to contact user. Must have an email or phone",
					Toast.LENGTH_LONG).show();
			return null;
		}

		// create a list of phones and emails
		ArrayList<TwoStrings> contactMethodsArray = new ArrayList<TwoStrings>(possibleContactMethods.mObject1);
		ArrayList<TwoStrings> tmp = new ArrayList<TwoStrings>(possibleContactMethods.mObject2);
		contactMethodsArray.addAll(tmp);

		// if length is one, then just return, no need to select
		if (contactMethodsArray.size() == 1)
			return contactMethodsArray.get(0).mObject1;

		// find the current default (if any)
		String def = contactCursorWrapper.getDefaultContact(mCtx);
		String[] defArray = null;
		if (def != null)
			defArray = def.split(",");
		ArrayList<Long> overrideDefault = new ArrayList<Long>();
		if (defArray != null && contactMethodsArray != null && contactMethodsArray.size() != 0)
			for (int i = 0; i < defArray.length; i++){
				int ii = contactMethodsArray.indexOf(new TwoStrings(defArray[i].trim(), ""));
				if (ii != -1)
					overrideDefault.add((long) ii);
			}
		long[] override = new long[overrideDefault.size()];
		int i = 0;
		for (Long item : overrideDefault)
			override[i++] = item;

		// store the name
		final String displayName = contactCursorWrapper.getName();

		// create dialog for selecting and naming group name
		final MultipleCheckPopUp<TwoStrings> chooseContactMethodDialog = new MultipleCheckPopUp<TwoStrings>(
				mCtx, 
				null,
				"Choose default contact method for " + contactCursorWrapper.getName(),
				false,
				override,
				contactMethodsArray);

		// add functionality to ok button to add selected names
		chooseContactMethodDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Confirm", new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				// loop over items that where checked
				ArrayList<TwoStrings> selectedMethods = chooseContactMethodDialog.getCheckedItemsTwoStrings();

				// empty then just return
				if (selectedMethods == null || selectedMethods.size() == 0)
					return;

				// store as default contact, comma separated
				String contactMethod = "";
				for (TwoStrings item : selectedMethods){
					contactMethod += item.mObject1 + ", ";
				}
				contactMethod = contactMethod.substring(0, Math.max(contactMethod.length()-2, 0));

				if (contactMethod.length() == 0)
					return;

				// store in hash set
				UsersAdapter users = new UsersAdapter(mCtx);
				users.fetchUserByContactsId(contactId);
				
				// check if we need to create a new user
				if (users.getRowId() == -1){
					users.fetchUser(users.makeNewUser(
							mCtx,
							contactId,
							contactCursorWrapper.getLookupKey(),
							contactMethod,
							-1));
				}else
					users.setDefaultContactInfo(users.getRowId(), contactMethod);
				
				// now store
				mContactChecked.setItem(new ContactCheckedItem(
						contactId,
						true,
						displayName,
						contactMethod,
						contactCursorWrapper.getLookupKey(),
						users.getRowId()));
				
				users.close();
				
				updateNContacts();
				updateVisibleView();
			}
		});

		// show it
		chooseContactMethodDialog.show();

		return null;
	}
	
	/**
	 * Choose the users contact method from popup dialog. If only one is immediatley available, then return it, 
	 * otherwise return null, and then in the onClick listener, the user will be updated.
	 * @param contactId The contactId of the user to affect
	 * @return The string if only one is available, or null if not.
	 */
	private String chooseMethodFromPopup(final ContactCursorWrapper contactCursorWrapper, final long contactId){

		TwoObjects<HashSet<TwoStrings>, HashSet<TwoStrings>> possibleContactMethods = 
			com.tools.CustomCursors.getContactPhoneAndEmailArray(mCtx, contactId);

		// must have a way to contact, or break out
		if (possibleContactMethods == null ||
				((possibleContactMethods.mObject1 == null || 
						possibleContactMethods.mObject1.size() == 0) &&
						(possibleContactMethods.mObject2 == null || 
								possibleContactMethods.mObject2.size() == 0))){
			Toast.makeText(mCtx,
					"Fail!! No method to contact user. Must have an email or phone",
					Toast.LENGTH_LONG).show();
			return null;
		}

		// create a list of phones and emails
		final ArrayList<TwoStrings> contactMethodsArray = new ArrayList<TwoStrings>(possibleContactMethods.mObject1);
		ArrayList<TwoStrings> tmp = new ArrayList<TwoStrings>(possibleContactMethods.mObject2);
		contactMethodsArray.addAll(tmp);

		// if length is one, then just return, no need to select
		if (contactMethodsArray.size() == 1)
			return contactMethodsArray.get(0).mObject1;

		// find the current default (if any)
		String def = contactCursorWrapper.getDefaultContact(mCtx);
		String[] defArray = null;
		if (def != null)
			defArray = def.split(",");
		ArrayList<Long> overrideDefault = new ArrayList<Long>();
		if (defArray != null && contactMethodsArray != null && contactMethodsArray.size() != 0)
			for (int i = 0; i < defArray.length; i++){
				int ii = contactMethodsArray.indexOf(new TwoStrings(defArray[i].trim(), ""));
				if (ii != -1)
					overrideDefault.add((long) ii);
			}
		long[] override = new long[Math.min(overrideDefault.size(), 1)];
		if (override.length == 1)
			override[0] = overrideDefault.get(0);

		// store the name
		final String displayName = contactCursorWrapper.getName();

		// make array adapter to hold group names
		ArrayAdapter<TwoStrings> spinnerArrayAdapter = new ArrayAdapter<TwoStrings>(
				this, android.R.layout.simple_spinner_item, contactMethodsArray);

		// grab standard android spinner
		spinnerArrayAdapter.setDropDownViewResource( R.layout.spinner_layout );

		// create a spinner
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.chooseDefaultSpinner);
		spinner.setPrompt(getResources().getString(R.string.chooseDefaultSpinner) + " for " + contactCursorWrapper.getName());

		if (override.length == 1)
			spinner.setSelection((int) override[0]);
		
		// make listener when spinner is clicked
		spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, 
					View selectedItemView, int position, long id) {

				ArrayList<TwoStrings> selectedMethods = new ArrayList<TwoStrings>();
				selectedMethods.add(contactMethodsArray.get(position));
				
				// empty then just return
				if (selectedMethods == null || selectedMethods.size() == 0)
					return;
				
				// store as default contact, comma separated
				String contactMethod = "";
				for (TwoStrings item : selectedMethods){
					contactMethod += item.mObject1 + ", ";
				}
				contactMethod = contactMethod.substring(0, Math.max(contactMethod.length()-2, 0));

				if (contactMethod.length() == 0)
					return;

				// store in hash set
				UsersAdapter users = new UsersAdapter(mCtx);
				users.fetchUserByContactsId(contactId);
				
				// check if we need to create a new user
				if (users.getRowId() == -1){
					users.fetchUser(users.makeNewUser(
							mCtx,
							contactId,
							contactCursorWrapper.getLookupKey(),
							contactMethod,
							-1));
				}else
					users.setDefaultContactInfo(users.getRowId(), contactMethod);
				
				// now store
				mContactChecked.setItem(new ContactCheckedItem(
						contactId,
						true,
						displayName,
						contactMethod,
						contactCursorWrapper.getLookupKey(),
						users.getRowId()));
				
				users.close();
				
				updateNContacts();
				updateVisibleView();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing, just leave
			}
		});
		

		// set adapter and launch it
		spinner.setAdapter(spinnerArrayAdapter);
		spinner.performClick();			

		return null;
	}

	/**
	 * Allow user to change the default contact method
	 * @param position
	 */
	private void changeDefault(final int position){
		//TODO: make this work for correct list
		/*
		mNamesCursor.moveToPosition(position);
		chooseMethodFromPopup(mNamesCursor.getId());
		 */

	}

	@Override
	public void onAsyncExecute(
			int requestCode,
			AsyncTypeCall asyncTypeCall,
			Object data) {

		// convert request code to an ASYNC_CALLS
		ASYNC_CALLS request = ASYNC_CALLS.convert(requestCode);

		// switch over all types of async
		switch (request){
		case ADD_FROM_GROUP:
			switch (asyncTypeCall){
			case POST:
				final ArrayList<TwoObjects<String, Long>> namesToAdd = (ArrayList<TwoObjects<String,Long>>) data;
				showDialogAfterAddGroup(namesToAdd);
				break;
			}
			break;
		case ADD_USERS_TO_PHOTOSHARE_GROUP:
			switch (asyncTypeCall){
			case POST:
				ReturnFromAddUsersToGroupTask value = (ReturnFromAddUsersToGroupTask) data;

				// if we are successful, just quit
				if (value.isLocalSuccess())
					finish();
				break;
			}
			break;
		case LOAD_FROM_GROUP:
			switch (asyncTypeCall){
			case POST:
				mContactChecked = (ContactCheckedArray) data;
				if (mContactChecked == null){
					Toast.makeText(mCtx, "Cancelled group loading", Toast.LENGTH_SHORT).show();
					finish();
				}else{
					updateNContacts();
					updateVisibleView();
				}
				break;
			}
			break;
		}
	}

	/**
	 * Show a dialog after we have added from a group to show people who will be added and allow to deselect if desired.
	 * @param namesToAdd
	 */
	private void showDialogAfterAddGroup(final ArrayList<TwoObjects<String, Long>> namesToAdd){

		// store this info for orientation changes
		addFromGroupData = namesToAdd;

		// if no names to add, then just break out
		if (namesToAdd == null || namesToAdd.size() == 0)
			return;

		//showing popup here
		popUp =
			new MultipleCheckPopUp<TwoObjects <String, Long>>(mCtx,
					namesToAdd,
					"Add these " + namesToAdd.size() + " contacts?",
					true,
					null,
					null);

		// add functionality to ok button to add selected names
		popUp.setButton(AlertDialog.BUTTON_POSITIVE, "Confirm", new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				// grab the names selected and store them
				long [] itemsChecked = popUp.getChecked();
				int namesAdded = 0;
				for (int i = 0; i<itemsChecked.length; i++){
					mContactChecked.setIsChecked(namesToAdd.get((int) itemsChecked[i]).mObject2, true);
					namesAdded++;
				}

				// update visible contacts
				String name = " names added.";
				if (namesAdded == 1)
					name = " name added.";
				Toast.makeText(mCtx, namesAdded+name, Toast.LENGTH_SHORT).show();
				updateNContacts();
				updateVisibleView();
			}
		});

		// show it
		popUp.show();
	}

	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationPropertiesCustom data = new ConfigurationPropertiesCustom();
		data.mContactChecked = mContactChecked;
		data.addFromGroupData = addFromGroupData;
		if (popUp != null){
			data.isAddFromGroupDialogShowing = popUp.isShowing();
			data.addFromGroupDialogPosition = popUp.getFirstVisiblePosition();
		}
		configurationProperties.customData = data;
	}

	@Override
	protected void onDestroyOverride() {

	}

	/**
	 * Additional configuration properties
	 *
	 */
	private class ConfigurationPropertiesCustom{
		ContactCheckedArray mContactChecked = null;
		ArrayList<TwoObjects<String, Long>> addFromGroupData;
		boolean isAddFromGroupDialogShowing = false;
		int addFromGroupDialogPosition = -1;
	}

	/**
	 * The custom class for dealing with the listView and how to show views
	 * @author Kyle
	 *
	 */
	public class LazyAdapter
	extends SimpleCursorAdapter 
	implements android.widget.AdapterView.OnItemClickListener{ 

		private final Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.icon3);
		private ContactCursorWrapper contactCursorWrapper;
		private ContactCursorWrapper contactCursorWrapperTmp;

		/**
		 * This field should be made private, so it is hidden from the SDK.
		 * {@hide}
		 */
		protected Cursor mCursor;

		public LazyAdapter(
				Context context,
				int layout) {
			super(context, layout, null, new String[] {ContactsContract.Contacts._ID}, new int[] {R.id.name});
		}

		@Override
		public Object getItem(int position) {
			contactCursorWrapper.moveToPosition(position);
			return contactCursorWrapper.getCursor();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor){

			// name text
			TextView name = (TextView)view.findViewById(R.id.name);
			String displayName = contactCursorWrapper.getName();
			name.setText(displayName);

			// checkbox look at hashtable
			long contactId = contactCursorWrapper.getId();
			if (contactId == -1){
				Log.e(Utils.LOG_TAG, "id = -1 for " +displayName);
				return;
			}
			//TODO: background query is giving the above, fix

			boolean isChecked = mContactChecked.isChecked(contactId);
			CheckBox check = (CheckBox)view.findViewById(R.id.contactSelectedCheckBox);
			if (!isChecked){
				check.setChecked(false);
				view.setBackgroundColor(Color.WHITE);
			}else{
				check.setChecked(true);
				view.setBackgroundColor(Color.LTGRAY);
			}

			// contact method text
			if (isChecked){
				TextView contactMethod = (TextView)view.findViewById(R.id.contactMethod);
				String method = mContactChecked.getDefaultContactMethod(contactCursorWrapper.getId());
				contactMethod.setText(method);
				contactMethod.setVisibility(View.VISIBLE);
			}else{
				TextView contactMethod = (TextView)view.findViewById(R.id.contactMethod);
				contactMethod.setVisibility(View.GONE);
			}

			// the contacts picture
			ImageView image = (ImageView)view.findViewById(R.id.contactImage);

			// try to set the image to the contact image, and if unsuccessful, then set to default.
			if (!contactCursorWrapper.setImageToContactBitmap(mCtx, image) && image != null)
				image.setImageBitmap(defaultPhoto);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint){

			if (constraint == null)
				return null;

			// grab cursor from search result grabbing names of interest
			ContactCursorWrapper cursor = new ContactCursorWrapper();
			cursor.searchDatabaseForName(
					mAct,
					constraint.toString(),
					onlyShowContactsFromVisibleGroup);

			contactCursorWrapperTmp = cursor;
			return cursor.getCursor();
		}
		
		@Override
		public void changeCursor(Cursor c){
			super.changeCursor(c);
			this.contactCursorWrapper = this.contactCursorWrapperTmp;
		}


		/**
		 * Called by the AutoCompleteTextView field when a choice has been made
		 * by the user.
		 *
		 * @param listView
		 *            The ListView containing the choices that were displayed to
		 *            the user.
		 * @param view
		 *            The field representing the selected choice
		 * @param position
		 *            The position of the choice within the list (0-based)
		 * @param id
		 *            The id of the row that was chosen (as provided by the _id
		 *            column in the cursor.)
		 */
		@Override
		public void onItemClick(AdapterView<?> listView, View view, int position, long id) {

			// move cursor to the correct position
			contactCursorWrapper.moveToPosition((int)listView.getItemIdAtPosition(position));

			// grab the id of the user clicked
			long contactId = contactCursorWrapper.getId();

			// find the checkbox of interest
			CheckBox check = (CheckBox) view.findViewById(R.id.contactSelectedCheckBox);

			// see if contact is checked
			Boolean isChecked = mContactChecked.isChecked(contactId);
			UsersAdapter users = new UsersAdapter(mCtx);
			if (!isChecked){
				String defaultMethod = selectDefaultContactMethod(contactCursorWrapper, contactId);
				if (defaultMethod != null && defaultMethod.length() > 0){
					// check the box and save in hashtable
					check.setChecked(true);
					users.fetchUserByContactsId(contactId);
					
					// if user doesn't exist, make a new one
					if (users.getRowId() == -1){
						users.fetchUser(users.makeNewUser(
								mCtx,
								contactId,
								contactCursorWrapper.getLookupKey(), 
								defaultMethod,
								-1));
					}
					mContactChecked.setItem(new ContactCheckedItem(
							contactId,
							true,
							contactCursorWrapper.getName(),
							defaultMethod,
							contactCursorWrapper.getLookupKey(),
							users.getRowId()));
					users.close();
				}
			}else{
				// make sure we can even delelte this members
				GroupsAdapter groups = new GroupsAdapter(mCtx);
				groups.fetchGroup(groupId);
				boolean canRemove = groups.canIRemoveThisMember(mCtx, mContactChecked.getRowId(contactId));
				if (!canRemove){
					Toast.makeText(mCtx, "You do not have permission to remove this member", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// uncheck and save in hashtable
				check.setChecked(false);
				mContactChecked.setIsChecked(contactId, false);
			}

			// update the adapters
			updateVisibleView();
			updateNContacts();

			// hide they keyboard
			com.tools.Tools.hideKeyboard(mCtx, mSearchNameObj);
			final View dummy = findViewById(R.id.dummyView);
			(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
		}

		@Override
		public CharSequence convertToString (Cursor cursor){
			return "";
		}
	}

	/**
	 * The custom class for dealing with the listView and how to show views
	 * @author Kyle
	 *
	 */
	public class ListAdapter
	extends BaseAdapter
	implements android.widget.AdapterView.OnItemClickListener{ 

		private final Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.icon3);
		private ContactCheckedArray data;
		private LayoutInflater inflater = null;
		private ArrayList<Long> keySet;

		public ListAdapter(Activity a, ContactCheckedArray contactChecked) {
			data = contactChecked;
			inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			keySet = new ArrayList<Long>(data.getCheckedKeys());	
		}

		@Override
		public int getCount() {
			return keySet.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent){

			// attempt to use recycled view
			View view = convertView;
			if(convertView==null)
				view = inflater.inflate(R.layout.person_row, null);

			// move to correct location
			long id = keySet.get(position);

			// name text
			TextView name = (TextView)view.findViewById(R.id.name);
			String displayName = data.getDisplayName(id);
			name.setText(displayName);

			// checkbox look at hashtable
			boolean isChecked = data.isChecked(id);
			CheckBox check = (CheckBox)view.findViewById(R.id.contactSelectedCheckBox);
			if (!isChecked){
				check.setChecked(false);
				view.setBackgroundColor(Color.WHITE);
			}else{
				check.setChecked(true);
				view.setBackgroundColor(Color.LTGRAY);
			}

			// contact method text
			if (isChecked){
				TextView contactMethod = (TextView)view.findViewById(R.id.contactMethod);
				String method = data.getDefaultContactMethod(id);
				contactMethod.setText(method);
				contactMethod.setVisibility(View.VISIBLE);
			}else{
				TextView contactMethod = (TextView)view.findViewById(R.id.contactMethod);
				contactMethod.setVisibility(View.GONE);
			}

			// the contacts picture
			ImageView image = (ImageView)view.findViewById(R.id.contactImage);

			// try to set the image to the contact image, and if unsuccessful, then set to default.
			Uri uri = data.getUri(getContentResolver(), id);
			InputStream input = null;
			try{
				input = ContactsContract.Contacts.
				openContactPhotoInputStream(mCtx.getContentResolver(), uri);
			}catch(Exception e){
				e.printStackTrace();
			}
			if (input != null)
				image.setImageBitmap(BitmapFactory.decodeStream(input));
			else
				image.setImageBitmap(defaultPhoto);

			return view;
		}

		/**
		 * Called by the AutoCompleteTextView field when a choice has been made
		 * by the user.
		 *
		 * @param listView
		 *            The ListView containing the choices that were displayed to
		 *            the user.
		 * @param view
		 *            The field representing the selected choice
		 * @param position
		 *            The position of the choice within the list (0-based)
		 * @param id
		 *            The id of the row that was chosen (as provided by the _id
		 *            column in the cursor.)
		 */
		@Override
		public void onItemClick(AdapterView<?> listView, View view, int position, long id) {

			// move to correct location
			long contactId = keySet.get(position);

			// find the checkbox of interest
			CheckBox check = (CheckBox) view.findViewById(R.id.contactSelectedCheckBox);

			// see if contact is checked
			Boolean isChecked = mContactChecked.isChecked(contactId);
			if (!isChecked){
				check.setChecked(true);
				mContactChecked.setIsChecked(contactId, true);
			}else{
				// make sure we can even delelte this members
				GroupsAdapter groups = new GroupsAdapter(mCtx);
				groups.fetchGroup(groupId);
				boolean canRemove = groups.canIRemoveThisMember(mCtx, data.getRowId(contactId));
				if (!canRemove){
					Toast.makeText(mCtx, "You do not have permission to remove this member", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// uncheck and save in hashtable
				check.setChecked(false);
				mContactChecked.setIsChecked(contactId, false);
			}

			// update the adapters
			updateNContacts();

			// hide they keyboard
			com.tools.Tools.hideKeyboard(mCtx, mSearchNameObj);
			final View dummy = findViewById(R.id.dummyView);
			(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
		}
	}

	//TODO: put this somewhere else, and optimize
	public static Bitmap loadContactPhoto(ContentResolver cr, long  id,long photo_id) 
	{

		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
		if (input != null) 
		{
			return BitmapFactory.decodeStream(input);
		}
		else
		{
			Log.d("PHOTO","first try failed to load photo");

		}

		byte[] photoBytes = null;

		Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photo_id);

		Cursor c = cr.query(photoUri, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);

		try 
		{
			if (c.moveToFirst()) 
				photoBytes = c.getBlob(0);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		} finally {

			c.close();
		}           

		if (photoBytes != null)
			return BitmapFactory.decodeByteArray(photoBytes,0,photoBytes.length);
		else
			Log.d("PHOTO","second try also failed");
		return null;
	}
}
