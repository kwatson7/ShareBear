package com.instantPhotoShare.Activities;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.GroupsAdapter.ItemsFetchedCallback;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.tools.CustomActivity;
import com.tools.TwoObjects;
import com.tools.images.MemoryCache;

public class InsideGroupGallery 
extends CustomActivity{

	// constants
	private static final String FETCHING_DATA_TAG = "fetchingData";
	
	// graphics
	private GridView gridView; 					// the gridview to show pictures
	private ImageView screen; 					// The background screen image
	private ImageView takePictureButton; 		// the pointer to the take picture button
	private TextView groupNameText; 			// Pointer to textView showing the group name

	// misc private variables
	private PicturesGridAdapter adapter; 		// the adapter to show pictures
	private PicturesAdapter picturesAdapater;	// An array of all the pictures
	private long groupId; 						// the groupId that we are currently showing
	private CustomActivity act = this; 			// This activity
	private Drawable backgroundDrawable = null;
	private String groupName; 					// The name of the group we are in
	private int nNewPictures = 0;

	// variables to indicate what can be passed in through intents
	public static final String GROUP_ID = "GROUP_ID";

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// load in passed info
		Bundle extras = getIntent().getExtras(); 
		if (extras == null)
			throw new IllegalArgumentException("InsideGroupGallery cannot be called without a groupId passed in");

		// the group id passed
		groupId = extras.getLong(GROUP_ID, -1);
		if (groupId == -1)
			throw new IllegalArgumentException("InsideGroupGallery cannot be called without a groupId passed in");

		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationData data = (ConfigurationData) config.customData;
			if (data != null){
				backgroundDrawable = data.backgroundDrawable;	
			}
		}

		// grab the group name
		GroupsAdapter groups = new GroupsAdapter(this);
		try{
			groupName = groups.getGroup(groupId).toString();
		}catch(Exception e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			finish();
			return;
		}

		// initialize layout
		initializeLayout();	

		// grab cursor for all the groups
		getPictures();
		fillPictures();
	}

	// fill list with the pictures
	private void fillPictures() {
		// stop old thread if we need to
		//if (adapter != null)
		//	adapter.imageLoader.stopThreads();

		// set the adapter
		//  adapter = new PicturesGridAdapter(this, picturesAdapater);
		//  gridView.setAdapter(adapter);



		// save index and top position
		int index = gridView.getFirstVisiblePosition();

		// set adapter
		MemoryCache<Long> cache = null;
		if (adapter != null){
			adapter.imageLoader.stopThreads();
			cache = adapter.imageLoader.getMemoryCache();
		}
		adapter = new PicturesGridAdapter(this, picturesAdapater);
		if (cache != null)
			adapter.imageLoader.restoreMemoryCache(cache);
		gridView.setAdapter(adapter);

		// restore

		if (index != GridView.INVALID_POSITION)
			gridView.smoothScrollToPosition(index);	
	}

	/**
	 * Find the cursor required for searching Contacts
	 */
	private void getPictures(){
		picturesAdapater = new PicturesAdapter(this);
		picturesAdapater.fetchPicturesInGroup(groupId);
		picturesAdapater.startManagingCursor(this);
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {

	}

	@Override
	protected void initializeLayout() {

		// make full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// set main view
		setContentView(R.layout.inside_group_viewer);

		// grab pointers to objects
		gridView = (GridView)findViewById(R.id.photosView);

		// add click listener
		gridView.setOnItemClickListener(gridViewClick);

		// pointers to graphics
		takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
		screen = (ImageView)findViewById(R.id.screen);
		groupNameText = (TextView) findViewById(R.id.groupName);

		// set alpha of take button picture
		takePictureButton.setAlpha(Utils.PICTURE_ALPHA);

		// set the name
		groupNameText.setText(Html.fromHtml(groupName));

		// set the background picture
		GroupsAdapter groups = new GroupsAdapter(this);
		Group group = groups.getGroup(groupId);
		if (backgroundDrawable != null){
			screen.setImageDrawable(backgroundDrawable);
			backgroundDrawable = null;
		}else
			Utils.setBackground(this, group.getPictureThumbnailPath(this), screen, 0.5f);

		// fetch the list of pictures from server
		long serverId = group.getServerId();
		if (serverId != -1){
			ArrayList<String> bars = new ArrayList<String>(1);
			bars.add(FETCHING_DATA_TAG);
			groups.fetchPictureIdsFromServer(this, serverId, bars, fetchPictureIdsCallback);
		}
	}

	/**
	 * The callback to run when we are done grabbing the pictures from the server
	 */
	private static ItemsFetchedCallback<InsideGroupGallery> fetchPictureIdsCallback = 
			new ItemsFetchedCallback<InsideGroupGallery>() {

		@Override
		public void onItemsFetchedBackground(
				InsideGroupGallery act,
				int nNewItems,
				String errorCode) {
			// store the pictures
			if (act != null)
				act.nNewPictures = nNewItems;
		}

		@Override
		public void onItemsFetchedUiThread(InsideGroupGallery act, String errorCode) {
			// update adatper if there are new pictures
			if (act != null && act.nNewPictures > 0){
				Toast.makeText(act, act.nNewPictures + " new pictures!", Toast.LENGTH_SHORT).show();
				act.getPictures();
				act.fillPictures();
			}

			// if there was an error
			if (errorCode != null){
				if (act!= null && errorCode.compareToIgnoreCase(GroupsAdapter.GROUP_ACCESS_ERROR) != 0){
					Toast.makeText(act, "Not in group", Toast.LENGTH_LONG);
				}else{
					String group = "unknown";
					if (act != null)
						group = ""+act.groupId;
					Log.e(Utils.LOG_TAG, "Error code when fetching pictures in this group (: " + group + ") " + errorCode);
				}
			}
		}
	};

	@Override
	public void onPause(){
		//	overridePendingTransition(0, R.anim.picture_scale_down_animation);
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		super.onPause();
	}

	@Override
	public void onResume(){
		if (adapter != null){
			adapter.imageLoader.restartThreads();
			adapter.notifyDataSetChanged();
		}
		super.onResume();
	}

	private OnItemClickListener gridViewClick =  new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> parent,
				View view,
				int position,
				long id) {

			// load the intent for this groups gallery
			SinglePictureGallery.passedCache = adapter.getMemoryCache();
			Intent intent = new Intent(act, SinglePictureGallery.class);
			intent.putExtra(
					SinglePictureGallery.GROUP_ID,
					groupId);
			picturesAdapater.moveToPosition(position);
			intent.putExtra(
					SinglePictureGallery.PICTURE_POSITION,
					position);

			// load the activity
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, 0);								
		}
	};

	/**
	 * When this is clicked, set default group to this one, and open cameara
	 * @param v
	 */
	public void takePictureClicked(View v){
		Prefs.setGroupIds(this, groupId);
		Intent intent = new Intent(this, TakePicture.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationData data = new ConfigurationData();
		data.backgroundDrawable = screen.getDrawable();
		configurationProperties.customData = data;
	}

	private static class ConfigurationData{
		public Drawable backgroundDrawable;
	}

	@Override
	protected void onDestroyOverride() {

		// null out adapter
		adapter.imageLoader.clearCache();
		gridView.setAdapter(null);	
	}

	private class PicturesGridAdapter
	extends BaseAdapter {

		private PicturesAdapter data;
		private LayoutInflater inflater = null;
		private com.tools.images.ImageLoader<Long, TwoObjects<Long, Long>, TwoObjects<Long, Long>> imageLoader; 

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
		public com.tools.images.MemoryCache<Long> getMemoryCache(){
			return imageLoader.getMemoryCache();
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			// attempt to use recycled view
			View vi=convertView;
			if(convertView==null)
				vi = inflater.inflate(R.layout.photo_item, null);

			// grab pointers
			TextView text=(TextView)vi.findViewById(R.id.photoCaption);
			ImageView image=(ImageView)vi.findViewById(R.id.photoImage);
			
			// recycle bitmaps
			/*
			if (convertView != null){
				Drawable toRecycle= image.getDrawable();
				if (toRecycle != null) {
				    ((BitmapDrawable)image.getDrawable()).getBitmap().recycle();
				}
			}
			*/

			// fill the views
			text.setText("");
			if (data.moveToPosition(position)){
				TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(data.getRowId(), groupId);
				imageLoader.DisplayImage(data.getRowId(), loaderData, loaderData, image, null);
			}

			return vi;
		}
	}
}
