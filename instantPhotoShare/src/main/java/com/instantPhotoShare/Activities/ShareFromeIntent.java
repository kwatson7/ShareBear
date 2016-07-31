package com.instantPhotoShare.Activities;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Tasks.SaveTakenPictureTask;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.MultipleCheckPopUp;

public class ShareFromeIntent
extends CustomActivity{

	// member variables
	private ArrayList<String> picturePathsToShare;		// the list of passed pictures
	private com.tools.MultipleCheckPopUp<Group> dialog;
	ArrayList<Group> groups = null;

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// check that we have logged in
		if(!InitialLaunch.isUserAccountInfoAvailable(ctx)){
			Utils.showCustomToast(this, "Login first", true, 1);
			return;
		}

		getPassedPictures(); 		

		if (picturePathsToShare == null || picturePathsToShare.size() == 0){
			Utils.showCustomToast(ctx, "Picture(s) could not be shared", true, 1);
			finish();
			return;
		}

		// get the groups
		setupAndShowGetGroupsDialog();
	}

	@Override
	protected void initializeLayout() {
	}

	@Override
	protected void additionalConfigurationStoring() {
	}

	@Override
	protected void onDestroyOverride() {
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
	}

	/**
	 * Get the passed pictures and assign them to class data
	 */
	private void getPassedPictures(){
		// initialize
		picturePathsToShare = new ArrayList<String>();

		// grab the intent data
		Intent intent = getIntent();
		if (intent == null)
			return;
		String action = intent.getAction();
		if (action == null)
			return;

		// read the intent data only if it matches correct action
		if (Intent.ACTION_SEND_MULTIPLE.equals(action)
				&& intent.hasExtra(Intent.EXTRA_STREAM)) { 

			ArrayList<Parcelable> list = 
				intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM); 
			for (Parcelable p : list) { 
				Uri uri = (Uri) p; 
				try{
					uri = Uri.parse((URLDecoder.decode(uri.toString())));
				}catch(Exception e){
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					continue;
				}
				// now try to read from URI
				if (uri != null){
					String[] filePathColumn = {MediaStore.Images.Media.DATA};

					Cursor cursor = this.getContentResolver().query(uri, filePathColumn, null, null, null);
					if (cursor != null && cursor.moveToFirst()){
						int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
						String path = cursor.getString(columnIndex);
						if (path != null && path.length() > 0)
							picturePathsToShare.add(path);
						cursor.close();
					}else{
						if (cursor != null)
							cursor.close();	
						
						// try reading as file
						String path = uri.getPath();
						if (path != null && path.length() > 0){
							File file = new File(path);
							if (file.exists())
								picturePathsToShare.add(path);
						}
					}
				}
			} 
		}else if (Intent.ACTION_SEND.equals(action)
				&& intent.hasExtra(Intent.EXTRA_STREAM)){

			Uri uri;
			try{
				uri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
				uri = Uri.parse((URLDecoder.decode(uri.toString())));
			}catch(Exception e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return;
			}
			String[] filePathColumn = {MediaStore.Images.Media.DATA};

			Cursor cursor = this.getContentResolver().query(uri, filePathColumn, null, null, null);
			if (cursor != null && cursor.moveToFirst()){
				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String path = cursor.getString(columnIndex);
				if (path != null && path.length() > 0)
					picturePathsToShare.add(path);
				cursor.close();
			}else{
				if (cursor != null)
					cursor.close();	
				
				// try reading as file
				String path = uri.getPath();
				if (path != null && path.length() > 0){
					File file = new File(path);
					if (file.exists())
						picturePathsToShare.add(path);
				}
			}
				
		}
	}

	/**
	 * Setup and show the get group dialog
	 */
	private void setupAndShowGetGroupsDialog(){
		// grab all the groups
		GroupsAdapter adapter = new GroupsAdapter(this);
		ArrayList<Group> allGroups = adapter.getAllGroups();

		// create the dialog
		dialog = new MultipleCheckPopUp<Group>(
				ctx,
				allGroups,
				"Choose group(s) to add pictures",
				false,
				null,
				null);

		// just addign buttons here
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Share to Groups", new OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {}});

		// keep track if this dialog is showing
		dialog.setOnDismissListener(new OnDismissListener() {	
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});

		// on cancel, show toast and leave
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Utils.showCustomToast(ShareFromeIntent.this, "No group selected. Finishing...", true, 1);
				finish();
			}
		});

		// show it
		dialog.show();

		// set the listener for button
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				groups = dialog.getCheckedItemsGeneric();

				// if we are empty, don't allow
				if (groups.size() == 0){
					Utils.showCustomToast(ShareFromeIntent.this, "No groups selected. Leaving", true, 1);
					finish();
					return;
				}

				// save if successful
				savePictures();

				// dismiss the dialog
				dialog.dismiss();
			}
		});
	}

	/**
	 * Save the pictures in a background task
	 */
	private void savePictures(){
		//TODO: save caption

		// if no data, then just quit
		if (picturePathsToShare == null || picturePathsToShare.size() == 0 ||
				groups == null || groups.size() == 0){
			Utils.showCustomToast(this, "No shares", true, 1);
			return;
		}

		SavePicturesTask task = new SavePicturesTask(this, picturePathsToShare, groups);
		task.execute();
	}

	private static class SavePicturesTask
	extends CustomAsyncTask<ShareFromeIntent, Void, Void>{

		// member variables
		ArrayList<String> picturePathsToShare;
		ArrayList<Group> groups;

		public SavePicturesTask(
				ShareFromeIntent act,
				ArrayList<String> picturePathsToShare,
				ArrayList<Group> groups) {
			super(act, -1, true, false,
					null);
			this.picturePathsToShare = picturePathsToShare;
			this.groups = groups;
		}

		@Override
		protected void onPreExecute() {
			if (applicationCtx != null)
				Utils.showCustomToast(applicationCtx, "Saving...", true, 1);
		}

		@Override
		protected Void doInBackground(Void... params) {
			// loop over all pictures
			for (String file : picturePathsToShare){
				// open the task
				//TODO: save caption
				SaveTakenPictureTask<ShareFromeIntent> task = new SaveTakenPictureTask<ShareFromeIntent>(
						applicationCtx, -1, file, groups, "");
				task.showPostExecutionAlerts(false);
				task.execute();

				// run it
				try {
					task.get();
				} catch (InterruptedException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				} catch (ExecutionException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}

			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Void... progress) {
		}

		@Override
		protected void onPostExectueOverride(Void result) {
			if (picturePathsToShare == null || picturePathsToShare.size() == 0)
				return;
			if (picturePathsToShare.size() == 1)
				Utils.showCustomToast(applicationCtx, "Picture uploaded", true, 1);
			if (picturePathsToShare.size() >= 1)
				Utils.showCustomToast(applicationCtx, picturePathsToShare.size() + " pictures uploaded", true, 1);
		}

		@Override
		protected void setupDialog() {

		}

	}
}
