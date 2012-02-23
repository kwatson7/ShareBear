package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import javax.net.ssl.HandshakeCompletedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.instantPhotoShare.ContactCheckedArray;
import com.instantPhotoShare.ContactCursorWrapper;
import com.instantPhotoShare.R;
import com.instantPhotoShare.R.drawable;
import com.instantPhotoShare.Tasks.MakeGroupTask;
import com.instantPhotoShare.Tasks.fillNamesFromGroupAsync;
import com.instantPhotoShare.Tasks.insertContactsIntoGroupAsync;
import com.tools.*;

public class CreateGroup
extends CustomActivity{

	// misc private variables
	private ContactCursorWrapper mNamesCursor = null; 	// cursor to keep track of contacts
	private Context mCtx = this;						// The context
	private CustomActivity mAct = this;					// This activity
	private ListView mListView; 						// the main listView
	private ContactCheckedArray mContactChecked = null; // Object keeping track of which contacts are checked
	private boolean mClearText = false; 				// keep track if we should clear text when touching search box
	private boolean onlyShowContactsFromVisibleGroup = true; 		// only show people who are inside visible groups
	private ArrayList<TwoStrings> mGroupList; 			// This list of group names and column ids, but stored as strings
	private ArrayList<TwoObjects<String, Long>> addFromGroupData;	// data for adding from group. Needs storing for orientation switching

	// pointers to graphics objects
	private com.tools.NoDefaultSpinner spinner; 		// the spinner for choosing a group to add
	private ProgressDialog mProgressDialog; 			// The progress dialog for creating a new group
	private MultipleCheckPopUp<TwoObjects <String, Long>>  popUp; // The popUp after you've selected a group of who to add
	private com.tools.ClearableEditText mSearchNameObj; // Searching for users by name
	private TextView mNContactsSelectedObj; 			// number of contacts selceted text
	private Button mGoButton; 							// The go button when finished
	private EditText mGroupNameEdit; 					// Pointer to group name edit box
	
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
	private static final boolean ALLOW_OTEHRS_TO_ADD_MEMBERS_DEFAULT = true; 	// default to allow others in group to add members to group
	
	// other values
	private boolean allowOthersToAddMembers = ALLOW_OTEHRS_TO_ADD_MEMBERS_DEFAULT; // actual value for this group
	
	//enums for async calls
	private enum ASYNC_CALLS {
		ADD_FROM_GROUP, INSERT_CONTACTS_INTO_GROUP, MAKE_GROUP_TASK;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		initializeLayout();

		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationPropertiesCustom data = (ConfigurationPropertiesCustom) config.customData;
			// if this is a new setup, then restart hashtable, otherwise use old one
			if (data != null){
				mClearText = data.mClearText;
				mContactChecked = data.mContactChecked;
				if (data.isAddFromGroupDialogShowing  && data.addFromGroupData != null){
					showDialogAfterAddGroup(data.addFromGroupData);
					if (popUp != null)
						popUp.setSelection(data.addFromGroupDialogPosition);
				}
					
			}
		}
		
		// fill out names
		findNameCursor();
		fillNames();
		updateNContacts();

		// setup context menu
		registerForContextMenu(getListView());	
	}
	
	/**
	 * Return the list view in this layout
	 * @return
	 */
	private ListView getListView(){
		return mListView;
	}
	
	/**
	 * Get the adpater for the list
	 * @return
	 */
	private ListAdapter getListAdapter(){
		ListView list = getListView();
		return list.getAdapter();
	}
	
	/**
	 * Set the adapter for the list
	 * @param adapter
	 */
	private void setListAdapter(LazyAdapter adapter){
		ListView list =  getListView();
		list.setAdapter(adapter);
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
		setContentView(R.layout.create_group);
		
		// find pointers to objects
		mSearchNameObj = (com.tools.ClearableEditText) this.findViewById(R.id.searchName);	
		mNContactsSelectedObj = (TextView) findViewById(R.id.totalContactsNumber);
		mGoButton = (Button) findViewById(R.id.goButton1);
		mListView = (ListView) findViewById(R.id.list);
		mGroupNameEdit = (EditText) findViewById(R.id.groupName);
		
		// listener for list
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				onListItemClick((ListView)arg0, arg1, arg2, arg3);	
				
			}
		});
		
		// assign string of old layout
		if (searchName != null)
			mSearchNameObj.setText(searchName);
		
		// create a spinner
		spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);

		// make listener when spinner is clicked
		spinner.setOnItemSelectedListener(spinnerListener);

		// make search box searchable
		mSearchNameObj.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {

				// find cursor to contacts and fill in list
				findNameCursor();
				fillNames();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

			}
		});

		// clear the search box if we have already selected someone, so we want to type new stuff
		mSearchNameObj.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// just clear the text if we are supposed to
				if (mClearText){
					mSearchNameObj.setText("");
					mClearText = false;
				}
				return false;
			}
		});
		
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
		
		// update the list view
		updateNContacts();
		updateVisibleView();
		
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

		return false;  // don't go ahead and show the search box
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
		case SELECT_ALL:
			selectAll();
			return true;
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
		
		// make sure we have a group name
		if (mGroupNameEdit.getText() == null || mGroupNameEdit.getText().toString().length() == 0){
			Toast.makeText(mCtx, "Must have a group name", Toast.LENGTH_SHORT).show();
			return;
		}
		String groupName = mGroupNameEdit.getText().toString();
		
		// make the group and add users to group
		// launch the async task and add to array of tasks to be managed
		MakeGroupTask task =  new MakeGroupTask(
				mAct,
				ASYNC_CALLS.MAKE_GROUP_TASK.ordinal(),
				mContactChecked,
				mNamesCursor,
				groupName,
				allowOthersToAddMembers);
		this.asyncArray.add(task);
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
		this.asyncArray.add(task);
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
		this.asyncArray.add(task);
		task.execute(position);
	}

	/**
	 * Add all contacts to selection
	 */
	private  void selectAll(){

		// id for which name so we can set checked in hashtable, this will be changed over and over
		long contactId;

		// if no names then quite
		if (!mNamesCursor.moveToFirst())
			return;

		// step through cursor selecting all
		do{
			contactId = mNamesCursor.getId();
			mContactChecked.setIsChecked(contactId, true);
		}while(mNamesCursor.moveToNext());

		// update view and number of total contacts selected
		updateVisibleView();
		updateNContacts();
	}

	/**
	 * Update the visible view if things have changed.
	 */
	private void updateVisibleView(){
		ListAdapter adapter = getListAdapter();
		if (adapter != null)
			((SimpleCursorAdapter) adapter).notifyDataSetChanged();
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
	
	// fill list of possible name
	private void fillNames() {

		// Create an array to specify the fields we want to display in the list
		String[] from = mNamesCursor.getColumnsArray();

		// and an array of the fields we want to bind those fields to (in this case just text1)
		int[] to = new int[]{R.id.name, R.id.contactSelectedCheckBox, R.id.contactImage};

		// make the adapter and set it to the listview
		LazyAdapter contacts = 
			new LazyAdapter(mCtx, R.layout.person_row, mNamesCursor, from, to, mAct);
		setListAdapter(contacts);
	}
	
	/**
	 * Find the cursor required for searching Contacts
	 */
	private void findNameCursor(){
		String searchName = mSearchNameObj.getText().toString();
		
		// grab cursor from search result grabbing names of interest
		mNamesCursor = new ContactCursorWrapper();
		mNamesCursor.searchDatabaseForName(
				mAct,
				searchName,
				onlyShowContactsFromVisibleGroup);
	}

	/**
	 * When and item is clicked in the listView. Simply check the box and store this value as being checked
	 * @param l
	 * @param v
	 * @param position
	 * @param id
	 */
	private void onListItemClick(ListView l, View v, int position, long id) {

		// hide they keyboard
		com.tools.Tools.hideKeyboard(this, mSearchNameObj);

		// indicate that we should clear the search box on next click
		mClearText = true;

		// move cursor to the correct position
		mNamesCursor.moveToPosition(position);

		// grab the id of the user clicked
		long contactId = mNamesCursor.getId();

		// find the checkbox of interest
		CheckBox check = (CheckBox) findViewById(R.id.contactSelectedCheckBox);

		// see if contact is checked
		Boolean isChecked = mContactChecked.isChecked(contactId);
		if (!isChecked){
			String defaultMethod = selectDefaultContactMethod(contactId);
			if (defaultMethod != null && defaultMethod.length() > 0){
				// check the box and save in hashtable
				check.setChecked(true);
				mContactChecked.setIsChecked(contactId, true);
				mContactChecked.setDefaultContact(contactId, defaultMethod);
			}
		}else{
			// uncheck and save in hashtable
			check.setChecked(false);
			mContactChecked.setIsChecked(contactId, false);
		}

		updateNContacts();
		//updateVisibleView();
	}

	private String selectDefaultContactMethod(long contactId){
		
		// get teh default from hash first
		String contactMethod = mContactChecked.getDefaultContactMethod(contactId);
		if (contactMethod != null && contactMethod.length() != 0)
			return contactMethod;
		
		// get the default contact
		contactMethod = mNamesCursor.getDefaultContact(mCtx);
		
		// return with method if we have a good answer
		if (contactMethod != null && contactMethod.length() > 0)
			return contactMethod;
		
		// if no default method then search the contacts database and show a dialog
		if (contactMethod == null || contactMethod.length() == 0){
			return chooseMethodFromPopup(contactId);
		}
		
		return null;
	}
	
	/**
	 * Choose the users contact method from popup dialog. If only one is immediatley available, then return it, 
	 * otherwise return null, and then in the onClick listener, the user will be updated.
	 * @param contactId The contactId of the user to affect
	 * @return The string if only one is available, or null if not.
	 */
	private String chooseMethodFromPopup(final long contactId){
		
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
		String def = mNamesCursor.getDefaultContact(mCtx);
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

		// create dialog for selecting and naming group name
		final MultipleCheckPopUp<TwoStrings> chooseContactMethodDialog = new MultipleCheckPopUp<TwoStrings>(
				mCtx, 
				null,
				"Choose default contact method.",
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
				mContactChecked.setIsChecked(contactId, true);
				mContactChecked.setDefaultContact(contactId, contactMethod);
				mNamesCursor.setDefaultContact(mCtx, (int)contactId, contactMethod);
				
				updateNContacts();
				updateVisibleView();
			}
		});

		// show it
		chooseContactMethodDialog.show();
		
		return null;
	}
	
	/**
	 * Allow user to change the default contact method
	 * @param position
	 */
	private void changeDefault(final int position){
		mNamesCursor.moveToPosition(position);
		chooseMethodFromPopup(mNamesCursor.getId());
		
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
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
		data.mClearText = mClearText;
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
		boolean mClearText = false;
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
	extends SimpleCursorAdapter { 

		private Activity activity;
	    public com.instantPhotoShare.ImageLoader imageLoader; 
	    private final Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
	    private ContactCursorWrapper contactCursorWrapper;
	    
	    public LazyAdapter(
	    		Context context,
	    		int layout,
	    		ContactCursorWrapper contactCursorWrapper,
				String[] from,
				int[] to,
				Activity act) {
			super(context, layout, contactCursorWrapper.getCursor(), from, to);
			this.contactCursorWrapper = contactCursorWrapper;
			activity = act;
	        imageLoader=new com.instantPhotoShare.ImageLoader(activity.getApplicationContext());
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
	    public void bindView(View view, Context context, Cursor cursor){

	    	// name text
	    	TextView name = (TextView)view.findViewById(R.id.name);
	    	String displayName = contactCursorWrapper.getName();
	    	name.setText(displayName);

	    	// checkbox look at hashtable
	    	long contactId = contactCursorWrapper.getId();
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
	}
}
