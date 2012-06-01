/**
 * 
 */
package com.instantPhotoShare.Activities;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Tasks.CreatePrivateGroup;
import com.tools.CustomActivity;
import com.tools.TwoObjects;
import com.tools.images.ImageLoader.LoadImage;
import com.tools.images.MemoryCache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Kyle
 * Main screen user sees when launching app
 */
public class MainScreen
extends CustomActivity{
	
	// pointers to graphics objects
	private ImageView createNewGroup;
	private Gallery gallery; 				// the gallery to show pictures
	private PicturesGridAdapter adapter; 		// the adapter to show pictures
	private PicturesAdapter picturesAdapater;	// An array of all the pictures
	
	// misc private variables
	private CustomActivity act = this;
	
	// constants
	private static final int N_RANDOM_PICTURES = 500; 	// number of pictures to fetch on bottom gallery
	
	// enums for menu items
	private enum MENU_ITEMS { 										
		CLEAR_APP_DATA;
		@SuppressWarnings("unused")
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
		fillPictures();
		
	//	DebugUtils.deleteAllNonServerUsersAndGroupLinks(this);
		
		// move to correct position
		if (picturesAdapater.size() > 2)
			gallery.setSelection(picturesAdapater.size()/2, false);
		
		// check for private group and make it if need be
		CreatePrivateGroup<MainScreen> task = new CreatePrivateGroup<MainScreen>(this, ASYNC_TASKS.MAKE_PRIVATE_GROUP.ordinal());
		task.execute();
	}
	
	/**
	 * Find the cursor required for pictures
	 */
	private void getPictures(){
		picturesAdapater = new PicturesAdapter(this);
		picturesAdapater.fetchRandomPicture(this, N_RANDOM_PICTURES);
		picturesAdapater.startManagingCursor(this);
	}
	
	/**
	 * Fill the list of pictures
	 */
	private void fillPictures() {
		// set adapter
		adapter = new PicturesGridAdapter(this, picturesAdapater);
		gallery.setAdapter(adapter);
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
		
	}

	@Override
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub
		
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
	
	public void manageGroupsClicked(View v){
		Intent intent = new Intent(this, ManageGroups.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
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
            	TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(data.getRowId(), (long) -1);
            	imageLoader.DisplayImage(data.getRowId(), loaderData, loaderData, imageView);
            }

			return imageView;
		}
	}
}
