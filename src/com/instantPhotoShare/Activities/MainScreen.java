package com.instantPhotoShare.Activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.ServerKeys;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter.NumberNotifications;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.TableAdapter;
import com.instantPhotoShare.Tasks.CreatePrivateGroup;
import com.instantPhotoShare.Tasks.SyncGroupsThatNeedIt;
import com.instantPhotoShare.Tasks.SyncUsersInGroupThatNeedIt;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ServerPost.PostCallback;
import com.tools.ServerPost.ServerReturn;
import com.tools.TwoObjects;
import com.tools.images.MemoryCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.StaleDataException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Kyle
 * Main screen user sees when launching app
 */
public class MainScreen
extends CustomActivity{

	// constants
	private static final String FETCHING_DATA_TAG = "fetchingData";
	private static final int N_RANDOM_PICTURES = 150; 	// number of pictures to fetch on bottom gallery
	private static final long TIME_TO_WAIT_ON_EMAIL_ERROR = 3500;
	private static final boolean IS_GET_RANDOM_PICS = false;

	// pointers to graphics objects
	private ImageView createNewGroup;
	private Gallery gallery; 					// the gallery to show pictures
	private PicturesGridAdapter adapter; 		// the adapter to show pictures
	private PicturesAdapter picturesAdapater;	// An array of all the pictures
	private TextView nNotificationsText;
	private TextView validateReminder;
	private Button youShouldMakeAGroup;

	// misc private variables
	private CustomActivity act = this;
	private AlertDialog noEmailDialog = null;
	private int nNewGroups = 0;
	private boolean isEmailValidated = false;
	private boolean isGroupsFetched = false;

	// enums for menu items
	private enum MENU_ITEMS { 										
		EDIT_PREFERENCES, SHOW_HELP;
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}

	// enums for tasks
	private enum ASYNC_TASKS { 										
		MAKE_PRIVATE_GROUP;
		@SuppressWarnings("unused")
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}
	
	// enums for dialogs
	private enum DialogId {
		SHOW_HELP;
		private static DialogId convert (int value){
			return DialogId.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
	
		initializeLayout();	
		getPictures();

		// increment that we've used this app
		Prefs.incrementNumberTimesUsed(ctx);

		//	DebugUtils.deleteAllNonServerUsersAndGroupLinks(this);

		// move to correct position
		if (picturesAdapater.size() > 2){
			if (!IS_GET_RANDOM_PICS)
				gallery.setSelection(0, false);
			else
				gallery.setSelection(picturesAdapater.size()/2, false);
		}

		// check for private group and make it if need be
		CreatePrivateGroup<MainScreen> task = new CreatePrivateGroup<MainScreen>(this, ASYNC_TASKS.MAKE_PRIVATE_GROUP.ordinal());
		task.execute();

		// check for any groups that need to be synced
		SyncGroupsThatNeedIt<MainScreen> task2 = new SyncGroupsThatNeedIt<MainScreen>(this);
		task2.execute();

		// sync any users that need to be synced
		(new SyncUsersInGroupThatNeedIt<CustomActivity>(act)).execute();	

		// upgrade database 
		(new TableAdapter(ctx)).customUpgrade(this);
		
		// ask the user to rate the app
		com.tools.AppRater.app_launched(ctx, getResources().getString(R.string.app_name), getPackageName());
	}

	private void backupDb() throws IOException {
	    File sd = Environment.getExternalStorageDirectory();
	    File data = Environment.getDataDirectory();

	    if (sd.canWrite()) {

	        String currentDBPath = "/data/com.instantPhotoShare/databases/data.db";
	        String backupDBPath = "/yourapp_logs/data.db";

	        File currentDB = new File(data, currentDBPath);
	        File backupDB = new File(sd, backupDBPath);

	        if (backupDB.exists())
	            backupDB.delete();

	        if (currentDB.exists()) {
	            makeLogsFolder();

	            copy(currentDB, backupDB);
	       }

	     //   dbFilePath = backupDB.getAbsolutePath();
	   }
	}
	private void copy(File from, File to) throws FileNotFoundException, IOException {
	    FileChannel src = null;
	    FileChannel dst = null;
	    try {
	        src = new FileInputStream(from).getChannel();
	        dst = new FileOutputStream(to).getChannel();
	        dst.transferFrom(src, 0, src.size());
	    }
	    finally {
	        if (src != null)
	            src.close();
	        if (dst != null)
	            dst.close();
	    }
	}
	private void makeLogsFolder() {
	    try {
	        File sdFolder = new File(Environment.getExternalStorageDirectory(), "/yourapp_logs/");
	        sdFolder.mkdirs();
	    }
	    catch (Exception e) {}
	  }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Add the menu items
		menu.add(0, MENU_ITEMS.EDIT_PREFERENCES.ordinal(), 0, "Edit Prefernces");
		menu.add(0, MENU_ITEMS.SHOW_HELP.ordinal(), 0, "Help");

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS id = MENU_ITEMS.convert(item.getItemId());

		// decide on what each button should do
		switch(id) {
		case EDIT_PREFERENCES:
			Intent intent = new Intent(ctx, Preferences.class);
			startActivity(intent);
			return true;
		case SHOW_HELP:
			showDialog(DialogId.SHOW_HELP.ordinal());
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	/**
	 * Show the help for the app
	 */
	private Dialog getHelpDialog(){
		// the string to show
		String message = "Simply take a picture and all your friends in the group will automatically have the picture on their phone."
				+ System.getProperty ("line.separator")
				+ System.getProperty ("line.separator")
				+ "1. Create a group"
				+ System.getProperty ("line.separator")
				+ "2. Invite your friends to the group"
				+ System.getProperty ("line.separator")
				+ "3. Take pictures"
				+ System.getProperty ("line.separator")
				+ "4. Everyone in the group automatically gets the pictures on their phone!";
		
		// build the dialog
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(act);
		dlgAlert.setMessage(message);
		dlgAlert.setCancelable(true);
		dlgAlert.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//dismiss the dialog  
			}
		});
		return dlgAlert.create();
	}
	
	@Override
	public Dialog onCreateDialog(int id, Bundle args){
		// convert to enum
		DialogId dialogId = DialogId.convert(id);
		
		switch(dialogId){
		case SHOW_HELP:
			return getHelpDialog();
		}
		Log.e(Utils.LOG_TAG, "no dialog with id: " + id);
		return null;
	}

	/**
	 * Fetch any new groups that the user has been added to
	 */
	private void fetchNewGroups(){

		// the groups adapter needed to fetch groups
		GroupsAdapter groups = new GroupsAdapter(ctx);

		// the notification spinning progress bars to update while downloading
		ArrayList<String> bars = new ArrayList<String>(1);
		bars.add(FETCHING_DATA_TAG);

		// fetch the groups
		groups.fetchAllGroupsFromServer(this, bars, new FetchGroupsCallback());
	}

	private static class FetchGroupsCallback 
	implements GroupsAdapter.ItemsFetchedCallback<MainScreen> {

		@Override
		public void onItemsFetchedBackground(
				MainScreen act,
				int nNewItems,
				String errorCode) {

			// save the number of groups
			if (act != null && errorCode == null){
				act.nNewGroups = nNewItems;
				act.isGroupsFetched = true;
				//TODO: should these calls just be insided the calling function instead of out here.
			}
		}

		@Override
		public void onItemsFetchedUiThread(
				MainScreen act,
				String errorCode) {

			// update adatper if there are new pictures
			if (act!= null && act.nNewGroups > 0 && errorCode == null){
				act.setNotificationsNumber();
			}
			if (act != null && errorCode == null)
				act.showMakeGroupBannerIfRequired();

			// if there was an error
			if (errorCode != null){
				Log.e(Utils.LOG_TAG, "Error code when fetching groups " + errorCode);
			}	
		}
	};

	/**
	 * This thread will wait 3.5 seconds and then kill the activity
	 */
	private Thread killActivtyThread = new Thread(){
		@Override
		public void run() {
			try {
				Thread.sleep(TIME_TO_WAIT_ON_EMAIL_ERROR);
				try{
					if (noEmailDialog != null)
						noEmailDialog.dismiss();
				}catch(Exception e){
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
				MainScreen.this.finish();
			} catch (Exception e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}  
	};

	/**
	 * Find the cursor required for pictures
	 */
	private void getPicturesOld(){
		picturesAdapater = new PicturesAdapter(this);
		picturesAdapater.fetchRandomPicture(N_RANDOM_PICTURES);
		picturesAdapater.startManagingCursor(this);

		// set adapter
		adapter = new PicturesGridAdapter(this, picturesAdapater);
		gallery.setAdapter(adapter);
	}

	/**
	 * Find the cursor required for searching Contacts and load into gridView
	 */
	private void getPictures(){

		// create the new searcher if needed
		if (picturesAdapater == null)
			picturesAdapater = new PicturesAdapter(this);
		else
			picturesAdapater.stopManagingCursor(this);

		// do the search and manage
		if (!IS_GET_RANDOM_PICS)
			picturesAdapater.fetchNewestPictures(N_RANDOM_PICTURES);
		else
			picturesAdapater.fetchRandomPicture(N_RANDOM_PICTURES);
		picturesAdapater.startManagingCursor(this);	

		// set adapter
		if (adapter == null){
			adapter = new PicturesGridAdapter(this, picturesAdapater);
			gallery.setAdapter(adapter);
		}

		adapter.notifyDataSetChanged();
	}

	@Override
	public void onPause(){
		
		//overridePendingTransition(0, R.anim.picture_scale_down_animation);
		if (adapter != null)
			adapter.imageLoader.stopThreads();

		super.onPause();
	}

	@Override
	public void onResume(){
		super.onResume();
		if (adapter != null)
			adapter.imageLoader.restartThreads();
		adapter.notifyDataSetChanged();
		isEmailValidated = false;
		isGroupsFetched = false;
		fetchNewGroups();
		fetchIsEmailValidated();
		setNotificationsNumber();
		
		// show help to the user if they haven't seen it yet
		if (picturesAdapater.size() == 0 && Prefs.getNumberTimesUsed(ctx) <= 3){
			showDialog(DialogId.SHOW_HELP.ordinal());
		}
	}
	
	/**
	 * Fetch if the email is validated
	 */
	private void fetchIsEmailValidated(){
		
		// it was already validated, no need to check with server
		if (Prefs.isEmailValidated(act)){
			validateReminder.setVisibility(View.GONE);
			isEmailValidated = true;
			showMakeGroupBannerIfRequired();
			return;
		}
		
		// make json data to post
		JSONObject json = new JSONObject();
		try{
			json.put(ServerKeys.HasValidatedEmail.POST_KEY_USER_ID, Prefs.getUserServerId(ctx));
			json.put(ServerKeys.HasValidatedEmail.POST_KEY_SECRET_CODE, Prefs.getSecretCode(ctx));
		}catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return;
		}
		
		// post to server and get return
		Utils.postToServer(ServerKeys.HasValidatedEmail.COMMAND, json.toString(), null, null, null, MainScreen.this, null, new PostCallback<MainScreen>() {

			@Override
			public void onPostFinished(MainScreen act, ServerReturn result) {

			}

			@Override
			public void onPostFinishedUiThread(
					MainScreen act,
					ServerReturn result) {
				
				// activity is gone
				if (act == null)
					return;
				
				// convert to sharebear
				ShareBearServerReturn data = new ShareBearServerReturn(result);
				
				// show banner based on success or not
				if (data.isSuccess()){
					Prefs.setIsEmailValidated(act, true);
					act.validateReminder.setVisibility(View.GONE);
					isEmailValidated = true;
				}else if (!data.isSuccess() && "NO_VALID_EMAIL".compareToIgnoreCase(data.getErrorCode()) == 0){
					act.validateReminder.setVisibility(View.VISIBLE);
					isEmailValidated = false;
				}else{
					Log.e(Utils.LOG_TAG, data.getDetailErrorMessage());
					act.validateReminder.setVisibility(View.GONE);
				}	
				
				// show banner if we need to
				act.showMakeGroupBannerIfRequired();
			}
		});
	}
	
	/**
	 * Show the make a group banner if we've 1. validated email, 2. fetched all groups 
	 */
	private void showMakeGroupBannerIfRequired(){

		// show banner if only 1 group (private group) and we've already fetched groups and checked email
		if (isEmailValidated && isGroupsFetched){
			new CustomAsyncTask<MainScreen, Void, Integer>(MainScreen.this, -1, true, true, null){

				@Override
				protected void onPreExecute() {
				}

				@Override
				protected Integer doInBackground(Void... params) {
					GroupsAdapter groups = new GroupsAdapter(applicationCtx);
					return groups.getNGroups();
				}

				@Override
				protected void onProgressUpdate(Void... progress) {
				}

				@Override
				protected void onPostExectueOverride(Integer result) {
					if (result <= 1 && callingActivity != null){
						callingActivity.youShouldMakeAGroup.setVisibility(View.VISIBLE);
					}else
						callingActivity.youShouldMakeAGroup.setVisibility(View.GONE);
				}

				@Override
				protected void setupDialog() {

				}

			}.execute();	
		}
	}
	
	/**
	 * When the validate reminder is clicked, attempt to launch gmail.
	 * @param v
	 */
	public void onValidateReminderClickedDOESNTWORK(View v){
		try {
			com.tools.Tools.launchGmailSearchDOESNTWORK(this, Utils.GMAIL_SEARCH_TERM);
		} catch (Exception e) {
			Utils.showCustomToast(this, "Gmail client could not be launched", true, 1);
		}
	}

	/**
	 * initialize the layout and grab pointers for widgets
	 */
	@Override
	protected void initializeLayout() {
		// set the content view
		setContentView(R.layout.main_screen);

		// grab pointers for graphics objects
		createNewGroup = (ImageView) findViewById(R.id.createNewGroupButton);
		gallery = (Gallery) findViewById(R.id.galleryView);
		nNotificationsText = (TextView) findViewById(R.id.nNotificationsText);
		validateReminder = (TextView) findViewById(R.id.validateReminder);
		youShouldMakeAGroup = (Button) findViewById(R.id.youShouldMakeAGroup);

		// add click listener
		gallery.setOnItemClickListener(pictureClick);
	}

	/**
	 * Currently empty, but set up to allow action on picture click
	 */
	private OnItemClickListener pictureClick =  new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> parent,
				View view,
				int position,
				long id) {

			// move to correct position, if it doesn't exist, do nothing
			if (position == -1 || !picturesAdapater.moveToPosition(position))
				return;

			// find what group this picture is in
			long picId = picturesAdapater.getRowId();
			GroupsAdapter groups = new GroupsAdapter(act);
			groups.fetchGroupsContainPicture(picId);
			long groupId = -1;
			while (groupId == -1 && groups.moveToNext()){
				if(groups.isAccessibleGroup())
					groupId = groups.getRowId();
			}			
			groups.close();

			// no group, just go to top level of gallery
			if (groupId == -1){
				groups.close();
				Intent intent = new Intent(act, GroupGallery.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return;
			}

			// find position in cursor
			PicturesAdapter helper = new PicturesAdapter(act);
			helper.fetchPicturesInGroup(groupId);
			int positionForNewAdapter = -1;
			while (helper.moveToNext()){
				if (helper.getRowId() == picId){
					positionForNewAdapter = helper.getPosition();
					break;
				}
			}
			helper.close();

			// couldn't find picture, just go to group
			if (positionForNewAdapter == -1){
				Intent intent = new Intent(act, InsideGroupGallery.class);
				intent.putExtra(
						InsideGroupGallery.GROUP_ID,
						groupId);

				// load the activity
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return;
			}

			// load the intent for this groups gallery
			Intent intent = new Intent(act, SinglePictureGallery.class);
			intent.putExtra(
					SinglePictureGallery.GROUP_ID,
					groupId);
			intent.putExtra(
					SinglePictureGallery.PICTURE_POSITION,
					positionForNewAdapter);

			// load the activity
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);		
		}
	};

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {

	}

	@Override
	protected void additionalConfigurationStoring() {

	}

	@Override
	protected void onDestroyOverride() {
		gallery.setAdapter(null);	
	}

	public void createNewGroupClicked(View v){
		Intent intent = new Intent(this, CreateGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void takePictureClicked(View v){
		Intent intent = new Intent(this, TakePicture.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void viewPicturesClicked(View v){
		Intent intent = new Intent(this, GroupGallery.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void notificationsClicked(View v){
		Intent intent = new Intent(this, NotificationsScreen.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void manageGroupsClicked(View v){
		Intent intent = new Intent(this, ManageGroups.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void contactDeveloperClicked(View v){
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
		emailIntent.setType("text/plain");

		String uriText;

		uriText = "mailto:" + Utils.DEVELOPER_EMAIL + 
		"?subject=A question about ShareBear" + 
		"&body=";
		uriText = uriText.replace(" ", "%20");
		Uri uri = Uri.parse(uriText);

		emailIntent.setData(uri);
		startActivity(Intent.createChooser(emailIntent, "Email The Developer"));
	}

	/**
	 * Set the number of new notifications
	 */
	private void setNotificationsNumber(){
		// get the number of unread notificiations
		NotificationsAdapter notes = new NotificationsAdapter(ctx);
		notes.getNumberNewNotifications(this, new GetNumberNewNotificationsCallback());
	}

	/**
	 * Helper class used to retrieve and set the number of new notifications
	 */
	private static class GetNumberNewNotificationsCallback
	implements CustomAsyncTask.FinishedCallback<MainScreen, NumberNotifications> {

		@Override
		public void onFinish(MainScreen activity, NumberNotifications result) {
			if (result == null || activity == null)
				return;

			int unreadNotifications = result.nNewNotifications;
			// set the number, but clip at 99
			if (unreadNotifications == 0){
				activity.nNotificationsText.setVisibility(View.INVISIBLE);
			}else if (unreadNotifications <= 99){
				activity.nNotificationsText.setText(String.valueOf(unreadNotifications));
				activity.nNotificationsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
				activity.nNotificationsText.setVisibility(View.VISIBLE);
				activity.getPictures();
			}else{
				activity.nNotificationsText.setText("99+");
				activity.nNotificationsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
				activity.nNotificationsText.setVisibility(View.VISIBLE);
				activity.getPictures();
			}
		}
	};

	private class PicturesGridAdapter
	extends BaseAdapter {

		private PicturesAdapter data;
		private LayoutInflater inflater = null;
		public com.tools.images.ImageLoader<Long, TwoObjects<Long, Long>, TwoObjects<Long, Long>> imageLoader; 

		public PicturesGridAdapter(Activity a, PicturesAdapter pictures) {
			data = pictures;
			inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			imageLoader = new com.tools.images.ImageLoader<Long, TwoObjects<Long, Long>, TwoObjects<Long, Long>>(
					R.drawable.stub,
					0,
					0,
					false,
					PicturesAdapter.imageLoaderCallback(ctx, null));
		}

		public int getCount() {
			return data.size();
		}

		public Object getItem(int position) {
			try{
				if(data.moveToPosition(position))
					return data;
				else
					return null;
			}catch(StaleDataException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return null;
			}
		}

		public long getItemId(int position) {
			try{
				if(data.moveToPosition(position))
					return data.getRowId();
				else
					return 0;
			}catch(StaleDataException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				return 0;
			}
		}

		/**
		 * Return the memory cache.<br>
		 * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
		 * for example on orientation changes *****
		 * @return
		 */
		public MemoryCache getMemoryCache(){
			return imageLoader.getMemoryCache();
		}


		/**
		 * Clear cache as we need to reload picture
		 */
		public void clearCache(){
			imageLoader.clearCache();
		}

		/**
		 * Set the memory cache to this new value, clearing old one.
		 * @see getMemoryCache.
		 * @param mem
		 */
		public void restoreMemoryCache(MemoryCache mem){
			imageLoader.restoreMemoryCache(mem);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			// attempt to use recycled view
			ImageView imageView = null;
			if(convertView==null){
				int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
				imageView = (ImageView) inflater.inflate(R.layout.main_screen_photo_item, null);
				imageView.setLayoutParams(new Gallery.LayoutParams(
						height,
						height));
			}else
				imageView = (ImageView) convertView;

			// move to correct location and fill views
			if (data.moveToPosition(position)){
				try{
					TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(data.getRowId(), (long) -1);
					imageLoader.DisplayImage(data.getRowId(), loaderData, loaderData, imageView, null);
				}catch(StaleDataException e){
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
			}

			return imageView;
		}
	}
}
