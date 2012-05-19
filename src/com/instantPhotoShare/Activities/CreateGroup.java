package com.instantPhotoShare.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Tasks.CreateGroupTask;
import com.tools.*;

public class CreateGroup
extends CustomActivity{

	// private variables
	private Context ctx = this;							// The context
	private CustomActivity act = this;					// This activity
	
	// pointers to graphics
	private Button goButton; 							// The create group button
	private EditText groupNameEdit; 					// Pointer to group name edit box
	private CheckBox keepPrivate; 						// Pointer to keep private checkbox
	private CheckBox allowOthersToAddMembers; 			// allow others to add members

	//enums for async calls
	private enum ASYNC_CALLS {
		MAKE_GROUP_TASK;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		initializeLayout();
	}

	@Override
	protected void initializeLayout(){

		// set the layout
		setContentView(R.layout.create_group);

		// find pointers to objects
		goButton = (Button) findViewById(R.id.goButton1);
		groupNameEdit = (EditText) findViewById(R.id.groupName);
		keepPrivate = (CheckBox) findViewById(R.id.keepPrivate);
		allowOthersToAddMembers = (CheckBox) findViewById(R.id.allowOtherMembersToAddMembers);

		// hide the keyboard
		final View dummy = findViewById(R.id.dummyView);
		(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
	}

	public void goClicked(View view){

		// make sure we have a group name
		if (groupNameEdit.getText() == null || groupNameEdit.getText().toString().length() == 0){
			Toast.makeText(ctx, "Must have a group name", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// grab values
		String groupName = groupNameEdit.getText().toString();
		boolean isPrivate = keepPrivate.isChecked();
		boolean allowOthersAdd = allowOthersToAddMembers.isChecked();
		
		// launch the async task and add to array of tasks to be managed
		CreateGroupTask<CreateGroup> task =  new CreateGroupTask<CreateGroup>(
				this,
				ASYNC_CALLS.MAKE_GROUP_TASK.ordinal(),
				groupName,
				allowOthersAdd,
				isPrivate);
		task.execute();
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {

		// convert request code to an ASYNC_CALLS
		ASYNC_CALLS request = ASYNC_CALLS.convert(requestCode);

		// switch over all types of async
		switch (request){
		case MAKE_GROUP_TASK:
			switch (asyncTypeCall){
			case POST:
				CreateGroupTask<CreateGroup>.ReturnFromCreateGroupTask value = 
					(CreateGroupTask<CreateGroup>.ReturnFromCreateGroupTask) data;

				// if we successfully made the group, now add member to it
				if (value.isLocalSuccess()){
					Intent intent = new Intent(ctx, AddUsersToGroup.class);
					intent.putExtra(AddUsersToGroup.GROUP_ID, value.getRowId());
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				}
					
				break;
			}
			break;
		}
	}

	@Override
	protected void additionalConfigurationStoring() {
	}

	@Override
	protected void onDestroyOverride() {

	}
}
