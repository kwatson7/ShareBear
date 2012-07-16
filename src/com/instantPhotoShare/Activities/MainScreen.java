/**
 * 
 */
package com.instantPhotoShare.Activities;

import java.util.ArrayList;

import com.instantPhotoShare.Alarm;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.TableAdapter;
import com.instantPhotoShare.Tasks.CreatePrivateGroup;
import com.instantPhotoShare.Tasks.SyncGroupsThatNeedIt;
import com.instantPhotoShare.Tasks.SyncUsersInGroupThatNeedIt;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.TwoObjects;
import com.tools.images.MemoryCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.StaleDataException;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

	// misc private variables
	private CustomActivity act = this;
	private AlertDialog noEmailDialog = null;
	private int nNewGroups = 0;

	// enums for menu items
	private enum MENU_ITEMS { 										
		CLEAR_APP_DATA, EDIT_PREFERENCES;
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

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		initializeLayout();	

		getPictures();

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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Add the menu items
		menu.add(0, MENU_ITEMS.EDIT_PREFERENCES.ordinal(), 0, "Edit Prefernces");

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
		}

		return super.onMenuItemSelected(featureId, item);
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

				//TODO: should these calls just be insided the calling function instead of out here.
			}
		}

		@Override
		public void onItemsFetchedUiThread(
				MainScreen act,
				String errorCode) {

			// update adatper if there are new pictures
			if (act!= null && act.nNewGroups > 0 && errorCode == null){
				Toast.makeText(act, "You've been added to " + act.nNewGroups + " new groups!", Toast.LENGTH_SHORT).show();
				act.setNotificationsNumber();
			}

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
		if (adapter != null)
			adapter.imageLoader.restartThreads();
		super.onResume();
		adapter.notifyDataSetChanged();
		fetchNewGroups();
		setNotificationsNumber();
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
		gallery = (Gallery)findViewById(R.id.galleryView);
		nNotificationsText = (TextView) findViewById(R.id.nNotificationsText);

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
	implements CustomAsyncTask.FinishedCallback<MainScreen, Integer> {

		@Override
		public void onFinish(MainScreen activity, Integer result) {
			if (result == null || activity == null)
				return;

			int unreadNotifications = (java.lang.Integer) result;
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
					PicturesAdapter.imageLoaderCallback(ctx));
		}

		public int getCount() {
			return data.size();
		}

		public Object getItem(int position) {
			if(data.moveToPosition(position))
				return data;
			else
				return null;
		}

		public long getItemId(int position) {
			if(data.moveToPosition(position))
				return data.getRowId();
			else
				return 0;
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
