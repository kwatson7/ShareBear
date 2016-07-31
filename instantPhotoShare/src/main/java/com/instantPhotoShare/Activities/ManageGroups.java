package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.GroupSpinnerAdapter;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Tasks.CreateGroupTask;
import com.tools.*;

public class ManageGroups
extends CustomActivity{

	// private variables
	private Context ctx = this;							// The context
	private CustomActivity act = this;					// This activity
	private long groupId = -1; 							// The groupId we are editing.
	private boolean isChangeGroupShowing = false;		// boolean to keep track if group selector is showing
	private Drawable backgroundDrawable = null;

	// pointers to graphics
	private TextView groupNameEdit; 					// Pointer to group name edit box
	private CheckBox keepPrivate; 						// Pointer to keep private checkbox
	private CheckBox allowOthersToAddMembers; 			// allow others to add members
	private Button doneButton; 							// done with activity
	private LinearLayout dummyView; 					// dummy used to hide keyboard
	private Button addUsersToGroupButton; 				// add users button
	private CheckBox publicGroup; 						// make group public or not
	private ImageView screen;
	
	// public constants for passing itnent data
	public static final String GROUP_ID = "GROUP_ID";

	//enums for async calls
	private enum ASYNC_CALLS {
		MAKE_GROUP_ON_SERVER;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		
		// load in passed info
		Bundle extras = getIntent().getExtras(); 
		if (extras != null)
			groupId = extras.getLong(GROUP_ID, -1);
		
		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationData data = (ConfigurationData) config.customData;
			if (data != null){
				groupId = data.groupdId;
				backgroundDrawable = data.backgroundDrawable;	
			}
		}
		initializeLayout();
	}

	@Override
	protected void initializeLayout(){

		// set the layout
		setContentView(R.layout.manage_groups);

		// find pointers to objects
		doneButton = (Button) findViewById(R.id.done);
		dummyView = (LinearLayout) findViewById(R.id.dummyView);
		groupNameEdit = (TextView) findViewById(R.id.groupName);
		addUsersToGroupButton = (Button) findViewById(R.id.addUsersToGroup);
		keepPrivate = (CheckBox) findViewById(R.id.keepPrivate);
		publicGroup = (CheckBox) findViewById(R.id.publicGroup);
		allowOthersToAddMembers = (CheckBox) findViewById(R.id.allowOtherMembersToAddMembers);
		screen = (ImageView) findViewById(R.id.screen);

		// hide the keyboard
		final View dummy = findViewById(R.id.dummyView);
		(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });

		if (groupId == -1)
			changeGroupClicked(groupNameEdit);
		else
			// fill views with correct values
			fillViewsWithGroupValues();
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		// convert request code to an ASYNC_CALLS
		ASYNC_CALLS request = ASYNC_CALLS.convert(requestCode);

		// switch over all types of async
		switch (request){
		case MAKE_GROUP_ON_SERVER:
			switch (asyncTypeCall){
			case POST:
				CreateGroupTask.ReturnFromCreateGroupTask value = 
					(CreateGroupTask.ReturnFromCreateGroupTask) data;
				fillViewsWithGroupValues();
				break;
			}
			break;
		}
	}

	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationData data = new ConfigurationData();
		data.groupdId = groupId;
		data.backgroundDrawable = screen.getDrawable();
		configurationProperties.customData = data;
	}
	
	private static class ConfigurationData{
		public long groupdId = -1;
		public Drawable backgroundDrawable;
	}

	@Override
	protected void onDestroyOverride() {
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);
		LinearLayout enclosingFrame = (LinearLayout) findViewById(R.id.enclosingFrame);
		enclosingFrame.removeView(spinner);
	//	if (isChangeGroupShowing){
	//		spinner.setAdapter(null);
	//		spinner.performClick();
	//	}
	}

	/**
	 * Add users to the selected group
	 * @param v
	 */
	public void addUsersClicked(View v){
		Intent intent = new Intent(this, AddUsersToGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(AddUsersToGroup.GROUP_ID, groupId);
		startActivity(intent);
	}

	public void deleteGroupClicked(View v){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            GroupsAdapter groups = new GroupsAdapter(ctx);
		            groups.deleteGroup(groupId);
		            groupId = -1;
		            fillViewsWithGroupValues();
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		// find number of pictures in group
		PicturesAdapter pics = new PicturesAdapter(this);
		pics.fetchPicturesInGroup(groupId);
		int numPics = pics.size();
		pics.close();
		
		// show the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog = builder.setMessage("Are you sure you want be removed from this group? There are " +numPics + " pictures in this group.").setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener).create();
		addDialog(dialog);
		dialog.show();
	}
	
	/**
	 * Set the privacy value for this group
	 * @param v
	 */
	public void keepPrivateClicked(View v){
		// convert to checkbox
		CheckBox view = (CheckBox) v;

		// what are we currently doing
		boolean keepLocal = view.isChecked();
		
		// update locally
		GroupsAdapter groups = new GroupsAdapter(this);
		CreateGroupTask<CustomActivity> task = groups.setKeepLocal(act, ASYNC_CALLS.MAKE_GROUP_ON_SERVER.ordinal(), groupId, keepLocal);

		//  check what it should be
		Group group = groups.getGroup(groupId);
		view.setChecked(group.isKeepLocal());

		// update on server
		if (task != null){
			task.execute();
		}
		
		fillViewsWithGroupValues();
	}

	/**
	 * Just finish the activity
	 * @param v
	 */
	public void doneClicked(View v){
		finish();
	}

	public void changeGroupClicked(View v){

		// grab the spinner
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);
		if (spinner.isShown())
			return;

		// grab all groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(act);
		final ArrayList<Group> groups = groupsAdapter.getAllGroups();
		
		// make sure there are groups
		if (groups == null || groups.size() == 0){
			Utils.showCustomToast(this, "No groups to edit. Create one First", true, 1);
			finish();
			return;
		}

		// make array adapter to hold group names
		GroupSpinnerAdapter spinnerArrayAdapter = new GroupSpinnerAdapter(
				this, android.R.layout.simple_spinner_item, groups);

		// grab standard android spinner
		spinnerArrayAdapter.setDropDownViewResource( R.layout.spinner_layout );

		// make listener when spinner is clicked
		spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, 
					View selectedItemView, int position, long id) {

				if (!isChangeGroupShowing){
					isChangeGroupShowing = !isChangeGroupShowing;
					if (groupId != -1)
						return;
				}else
					isChangeGroupShowing = !isChangeGroupShowing;
					
				groupId = groups.get(position).getRowId();
				fillViewsWithGroupValues();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				Utils.showCustomToast(ctx, "No Group Selected. Finishing...", true, 1);
				finish();
			}
		});

		// set adapter and launch it
		spinner.setAdapter(spinnerArrayAdapter);
		
		// find which group is currenlty selected
		spinner.setSelection(0);
		for (int i = 0; i < groups.size(); i++){
			if (groups.get(i).getRowId() == groupId){
				spinner.setSelection(i);
				break;
			}
		}
		isChangeGroupShowing = false;
		spinner.performClick();
	}
	
	/**
	 * Fill all the screens views with the values from the current group
	 */
	private void fillViewsWithGroupValues(){
		
		// first check if we have a groupId, if not, then get one
		if (groupId == -1){
			GroupsAdapter adapter = new GroupsAdapter(this);
			ArrayList<Group> groups = adapter.getAllGroups();
			if (groups == null || groups.size() == 0){
				Utils.showCustomToast(this, "No groups to edit. Create one First", true, 1);
				finish();
				return;
			}
			groupId = groups.get(0).getRowId();
		}

		// now start filling values
		GroupsAdapter adapter = new GroupsAdapter(this);
		Group group = adapter.getGroup(groupId);
		if (group == null){
			groupId = -1;
			return;
		}
		groupNameEdit.setText(Html.fromHtml(group.toString()));
		keepPrivate.setChecked(group.isKeepLocal());
		allowOthersToAddMembers.setChecked(group.isAllowOthersToAddMembers());		
		addUsersToGroupButton.setEnabled(group.canUserAddMembers(ctx));		
		if (backgroundDrawable != null){
			screen.setImageDrawable(backgroundDrawable);
			backgroundDrawable = null;
		}else
			Utils.setBackground(this, group.getPictureThumbnailPath(ctx), screen, 1);	
	}
}
