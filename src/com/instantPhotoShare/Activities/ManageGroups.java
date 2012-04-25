package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
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
				CreateGroupTask.ReturnFromCreateGroupTask value = (CreateGroupTask.ReturnFromCreateGroupTask) data;
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
	
	private class ConfigurationData{
		public long groupdId = groupId;
		public Drawable backgroundDrawable;
	}

	@Override
	protected void onDestroyOverride() {
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
		AlertDialog dialog = builder.setMessage("Are you sure you want to delete this group? There are " +numPics + " pictures in this group.").setPositiveButton("Yes", dialogClickListener)
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
		CreateGroupTask task = groups.setKeepLocal(act, ASYNC_CALLS.MAKE_GROUP_ON_SERVER.ordinal(), groupId, keepLocal);

		//  check what it should be
		Group group = groups.getGroup(groupId);
		view.setChecked(group.isKeepLocal());

		// update on server
		if (task != null){
			addTask(task); 
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
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		if (spinner.isShown())
			return;

		// grab all groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(act);
		final ArrayList<Group> groups = groupsAdapter.getAllGroups();

		// make array adapter to hold group names
		GroupSpinnerAdpater spinnerArrayAdapter = new GroupSpinnerAdpater(
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
					return;
				}
				isChangeGroupShowing = !isChangeGroupShowing;
					
				groupId = groups.get(position).getRowId();
				fillViewsWithGroupValues();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});

		// set adapter and launch it
		spinner.setAdapter(spinnerArrayAdapter);
		
		// find which group is currenlty selected
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
				Toast.makeText(this, "No groups to edit. Create one First", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			groupId = groups.get(0).getRowId();
		}

		// now start filling values
		GroupsAdapter adapter = new GroupsAdapter(this);
		Group group = adapter.getGroup(groupId);
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

	/**
	 * Adapter for showing html formatted strings. We expect toString() of objects to be html format
	 * @author Kyle
	 *
	 */
	public class GroupSpinnerAdpater
	extends ArrayAdapter<Group>
	implements SpinnerAdapter{

		private List<Group> data;
		private LayoutInflater inflater = null;
		private int layoutId;
		private int mDropDownResource;

		public GroupSpinnerAdpater(Context context, int textViewResourceId,
				List<Group> objects) {
			super(context, textViewResourceId, objects);		 
			this.data = objects;
			inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layoutId = textViewResourceId;
		}

		/**
		 * <p>Sets the layout resource to create the drop down views.</p>
		 *
		 * @param resource the layout resource defining the drop down views
		 * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
		 */
		public void setDropDownViewResource(int resource) {
			this.mDropDownResource = resource;
		}

		/**
		 * Returns the Size of the ArrayList
		 */
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return android.R.layout.simple_spinner_dropdown_item;
		}

		/**
		 * Returns the View that is shown when a element was
		 * selected.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			// grab data
			Group group = data.get(position);

			// inflate new view if we have to
			View vi = convertView;
			if(convertView==null)
				vi = inflater.inflate(layoutId, null);

			// set text
			TextView text = (TextView) vi;
			text.setText(Html.fromHtml(group.toString()));

			return vi; 
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub

		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub

		}

		/**
		 * The Views which are shown in when the arrow is clicked
		 * (In this case, I used the same as for the "getView"-method.
		 */
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			// grab data
			Group group = data.get(position);

			// inflate new view if we have to
			View vi = convertView;
			if(convertView==null)
				vi = inflater.inflate(mDropDownResource, null);

			// set text
			TextView text = (TextView) vi;
			text.setText(Html.fromHtml(group.toString()));

			return vi; 
		}
	}
}
