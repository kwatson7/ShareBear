package com.instantPhotoShare.Activities;

import java.util.ArrayList;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.tools.MultipleCheckPopUp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

public class CreateCustomCamShortcut
extends Activity{

	private Activity act = this;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		chooseShortcutHelper();
	}

	private boolean chooseShortcutHelper(){

		// determine if we are supposed to just create a home screen icon
		final Intent intent0 = getIntent();
		final String action = intent0.getAction();

		if (!Intent.ACTION_CREATE_SHORTCUT.equals(action))
			return false;

		// grab all groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(this);
		ArrayList<Group> groups = groupsAdapter.getAllGroups();

		// create dialog for selecting and naming group name
		final MultipleCheckPopUp<Group> chooseGroups = new MultipleCheckPopUp<Group>(
				this, 
				groups,
				"Choose Group for Camera Shortcut.",
				false,
				null,
				null);

		// just addign buttons here
		chooseGroups.setButton(AlertDialog.BUTTON_POSITIVE, "Choose", new OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {}});

		// show it
		chooseGroups.show();

		// set the listener for button
		chooseGroups.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ArrayList<Group> selectedGroupsLocal = chooseGroups.getCheckedItemsGeneric();

				// save if successful
				Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
				shortcutIntent.setClassName(act, TakePicture.class.getName());

				// the shortcut name
				String shortcutName = "Bear Cam";
				if (selectedGroupsLocal.size() == 1)
					shortcutName = selectedGroupsLocal.get(0).getName() + " Cam";
				else if (selectedGroupsLocal.size() > 1)
					shortcutName = "Custom Bear Cam";

				// create group IDs
				String groupIds = "";
				for (Group item : selectedGroupsLocal)
					groupIds += item.getRowId() + Utils.DELIM;
				if (groupIds.length() > 0)
					groupIds = groupIds.substring(0, groupIds.length()-1);

				// Then, set up the container intent (the response to the caller)
				Intent intent = new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
				Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
						act,  R.drawable.shutter_icon_inverted);
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
				shortcutIntent.putExtra(TakePicture.GROUP_IDS, groupIds);
				shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				// Now, return the result to the launcher
				setResult(RESULT_OK, intent);
				finish();		
				chooseGroups.dismiss();
			}
		});

		return true;
	}
}
