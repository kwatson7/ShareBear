package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.HashSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.GroupsAdapter.ItemsFetchedCallback;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Tasks.DownloadPicturesTask;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.TwoObjects;
import com.tools.images.MemoryCache;

public class InsideGroupGallery 
extends CustomActivity{

	// constants
	private static final String FETCHING_DATA_TAG = "fetchingData";
	private static final int DEFAULT_PIC_WIDTH_DP = 96;

	// graphics
	private GridView gridView; 					// the gridview to show pictures
	private ImageView screen; 					// The background screen image
	private ImageView takePictureButton; 		// the pointer to the take picture button
	private TextView groupNameText; 			// Pointer to textView showing the group name
	private RelativeLayout multiSelectBar; 		// the bar at the bottom when we are selecting items
	private TextView nItemsSelected; 			// the text of how many items selected

	// misc private variables
	private PicturesGridAdapter adapter; 		// the adapter to show pictures
	private PicturesAdapter picturesAdapater;	// An array of all the pictures
	private long groupId; 						// the groupId that we are currently showing
	private CustomActivity act = this; 			// This activity
	private Drawable backgroundDrawable = null;
	private String groupName; 					// The name of the group we are in
	private String unformattedGroupName;
	private int nNewPictures = 0;
	private com.tools.images.MemoryCache<Long> oldCache = null; 		// the old imageloader cache. use this to handle screen rotations.
	private boolean isMultiSelectMode = false; 	// are we in multi select mode?
	private HashSet<Long> picturesSelected = new HashSet<Long>(10);	// list of pictures selected
	private int actualPicWidth = DEFAULT_PIC_WIDTH_DP; // the width of the pic item. Will be set later.
	//private float lastXPosition = 0.5f;
	//private float lastYPosition = 0.5f;


	// variables to indicate what can be passed in through intents
	public static final String GROUP_ID = "GROUP_ID";

	// enums for menu items
	private enum MENU_ITEMS { 										
		DELETE_GROUP, MANAGE_GROUP, SELECT_PICTURES;
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}

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
				oldCache = data.cache;
				isMultiSelectMode = data.isMultiSelectMode;
				picturesSelected = data.picturesSelected;
			}
		}

		// grab the group name
		GroupsAdapter groups = new GroupsAdapter(this);
		try{
			groupName = groups.getGroup(groupId).toString();
			unformattedGroupName = groups.getGroup(groupId).getName();
		}catch(Exception e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			finish();
			return;
		}

		// initialize layout
		initializeLayout();	

		// grab cursor for all the groups
		getPictures();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Add the menu items
		menu.add(0, MENU_ITEMS.DELETE_GROUP.ordinal(), 0, "Leave Group");
		menu.add(0, MENU_ITEMS.MANAGE_GROUP.ordinal(), 0, "Manage Group");
		MenuItem share = menu.add(0, MENU_ITEMS.SELECT_PICTURES.ordinal(), 0, "Share Pictures");

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS id = MENU_ITEMS.convert(item.getItemId());

		// decide on what each button should do
		switch(id) {
		case DELETE_GROUP:
			deleteGroupClicked();
			return true;
		case MANAGE_GROUP:
			Intent intent = new Intent(this, ManageGroups.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(ManageGroups.GROUP_ID, groupId);
			startActivity(intent);
			return true;
		case SELECT_PICTURES:
			showMultiSelectBar(true);
		}

		return super.onMenuItemSelected(featureId, item);
	}

	public void deleteGroupClicked(){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					GroupsAdapter groups = new GroupsAdapter(ctx);
					groups.deleteGroup(groupId);
					groupId = -1;
					finish();
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
	 * Find the cursor required for searching Contacts and load into gridView
	 */
	private void getPictures(){
		// save index and top position
		int index = GridView.INVALID_POSITION;

		// create the new searcher if needed
		if (picturesAdapater == null){
			picturesAdapater = new PicturesAdapter(this);
		}else{
			index = gridView.getFirstVisiblePosition();
			picturesAdapater.stopManagingCursor(this);
		}

		// do the search and manage
		picturesAdapater.fetchPicturesInGroup(groupId);
		picturesAdapater.startManagingCursor(this);	

		// set adapter
		if (adapter == null){
			adapter = new PicturesGridAdapter(this, picturesAdapater);
			gridView.setAdapter(adapter);
		}		

		// restore cache
		if (oldCache != null){
			adapter.restoreMemoryCache(oldCache);
			oldCache = null;
		}

		// restore position
		if (index != GridView.INVALID_POSITION)
			gridView.smoothScrollToPosition(index);	

		// show the selection bar if needed
		showMultiSelectBar(isMultiSelectMode);
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

		// change size of images in grid view to have no space between tiles
		actualPicWidth = com.tools.Tools.setGridViewColsBasedOnScreen(act, DEFAULT_PIC_WIDTH_DP, gridView, 0, true);

		// add click listener
		gridView.setOnItemClickListener(gridViewClick);
		gridView.setOnItemLongClickListener(gridViewLongClick);

		// pointers to graphics
		takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
		screen = (ImageView)findViewById(R.id.screen);
		groupNameText = (TextView) findViewById(R.id.groupName);
		multiSelectBar = (RelativeLayout) findViewById(R.id.multiSelectBar);
		nItemsSelected = (TextView) findViewById(R.id.nItemsSelected);

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
			groups.fetchPictureIdsFromServerInBackground(this, serverId, bars, new FetchPictureIdsCallback());
		}
	}

	/**
	 * The callback to run when we are done grabbing the pictures from the server
	 */
	private static class FetchPictureIdsCallback 
	implements ItemsFetchedCallback<InsideGroupGallery>{

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
				Utils.showCustomToast(act, act.nNewPictures + " new pictures!", true, 1);
				act.getPictures();
			}

			// if there was an error
			if (errorCode != null){
				if (act!= null && errorCode.compareToIgnoreCase(GroupsAdapter.GROUP_ACCESS_ERROR) == 0){
					Utils.showCustomToast(act, "Not in group", true, 1);
				}else{
					String group = "unknown";
					if (act != null)
						group = ""+act.groupId;
					Log.e(Utils.LOG_TAG, "Error code when fetching pictures in this group: ( " + group + ") " + errorCode);
				}
			}
		}
	};

	@Override
	public void onPause(){
		if (adapter != null)
			adapter.imageLoader.stopThreads();

		// recyle the old bitmap if one exists
		Drawable drawable = screen.getDrawable();
		Bitmap bitmapOld = null;
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			bitmapOld = bitmapDrawable.getBitmap();
		}
		screen.setImageResource(android.R.color.background_dark);
		if (bitmapOld != null)
			bitmapOld.recycle();
		screen.invalidate();

		super.onPause();
	}

	@Override
	public void onResume(){
		super.onResume();
		if (adapter != null){
			adapter.imageLoader.restartThreads();
			adapter.notifyDataSetChanged();
		}
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
		}
		int width = act.getWindowManager().getDefaultDisplay().getWidth(); 
		int height = act.getWindowManager().getDefaultDisplay().getHeight();

		lastXPosition = ((float) x)/width;
		lastYPosition = ((float) y)/height;

		return false;
	}
	 */

	/**
	 * when an item is long clicked, we will initiate multi-select
	 */
	private OnItemLongClickListener gridViewLongClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(
				AdapterView<?> parent,
				View view,
				int position,
				long id) {

			// add the picture to the list of pictures selected or remove
			picturesAdapater.moveToPosition(position);
			long rowId = picturesAdapater.getRowId();
			if (picturesSelected.contains(rowId))
				picturesSelected.remove(rowId);
			else
				picturesSelected.add(rowId);

			// update the number, notify list to change views, and show the bar
			updateNPicturesSelected();
			adapter.notifyDataSetChanged();
			showMultiSelectBar(true);

			return true;
		}
	};

	/**
	 * When we click an item, then perform action. If not multi select mode, then open large picture.
	 * If in multi select mode, then simply store this picture as selected
	 */
	private OnItemClickListener gridViewClick =  new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> parent,
				View view,
				int position,
				long id) {

			// standard mode
			if (!isMultiSelectMode){
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
			}else{
				// multi select mode

				// add the picture to the list of pictures selected or remove
				picturesAdapater.moveToPosition(position);
				long rowId = picturesAdapater.getRowId();
				if (picturesSelected.contains(rowId))
					picturesSelected.remove(rowId);
				else
					picturesSelected.add(rowId);
				updateNPicturesSelected();
				adapter.notifyDataSetChanged();
			}
		}
	};

	/**
	 * Update the number of pictures selected in view
	 */
	private void updateNPicturesSelected(){
		// get the total selected
		int nSelected = picturesSelected.size();

		// update the string
		nItemsSelected.setText(nSelected + " Selected");
	}

	/**
	 * Show or hide the multi-select bar and other appropriate actions to happen with this showing/hiding
	 * @param toShow toshow the bar or hide the bar
	 */
	private void showMultiSelectBar(boolean toShow){
		if (toShow){
			multiSelectBar.setVisibility(View.VISIBLE);
			updateNPicturesSelected();
			isMultiSelectMode = true;
		}else{
			multiSelectBar.setVisibility(View.GONE);
			isMultiSelectMode = false;

			// remove all selected
			picturesSelected.clear();
		}

		// invalidate it
		adapter.notifyDataSetChanged();
	}

	/**
	 * Multi select cancel clicked. just hide the bar
	 * @param v
	 */
	public void cancelClicked(View v){
		showMultiSelectBar(false);
	}

	/**
	 * The share button clicked. Open share menu and share multiple pictures
	 * @param v
	 */
	public void shareClicked(View v){
		Utils.showCustomToast(ctx, "Sharing pictures...", true, 0.5f);
		
		// download task, as we may need to download some files
		DownloadPicturesTask<InsideGroupGallery> task = new DownloadPicturesTask<InsideGroupGallery>(
				this,
				0,
				true,
				null,
				picturesSelected,
				groupId);
		
		// when finished hide bar
		task.setFinishedCallback(downloadFinishedCallback);
		
		task.execute();
	}
	
	/**
	 * Callback when we are done downloading pictures.
	 */
	private static FinishedCallback<InsideGroupGallery, DownloadPicturesTask.DownloadOutput> 
		downloadFinishedCallback =
		new FinishedCallback<InsideGroupGallery, DownloadPicturesTask.DownloadOutput>() {

			@Override
			public void onFinish(InsideGroupGallery activity, DownloadPicturesTask.DownloadOutput result) {
				if (activity == null)
					return;
				
				if (result.cancelled == true){
					Utils.showCustomToast(activity, "Cancelled", true, 1);
					activity.showMultiSelectBar(false);
					return;
				}
				
				// if no items, then quit
				if (result.successfulPictures == 0){
					Utils.showCustomToast(activity, "No pictures available. Try again later", true, 1);
					activity.showMultiSelectBar(false);
					return;
				}

				// create subject, body, and prompt
				String shareBody = "Take a look at my pictures from Share Bear. You can share instantly by downloading Share Bear at " + Utils.APP_URL;
				String shareSubject = "Some pictures from my Share Bear group " + activity.unformattedGroupName;
				String prompt = "Share pictures to..";

				// launch the intent
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
				shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, result.imageUris);
				shareIntent.setType("image/*");
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject);
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
				shareIntent.putExtra(Intent.EXTRA_TITLE, shareBody);
				activity.startActivity(Intent.createChooser(shareIntent, prompt));


				/*
				// grab the picture file
				picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
				String fileName = picturesAdapater.getFullPicturePath();
				if (fileName == null || fileName.length() == 0 || !(new File(fileName)).exists()){
					Utils.showCustomToast(this, "No full picture. Thumbnail used", true, 1);
					fileName = picturesAdapater.getThumbnailPath();
				}

				// send the intent
				if(!com.tools.Tools.sharePicture(this, shareSubject, shareBody, fileName, prompt))
					Utils.showCustomToast(this, "Picture could not be sent", true, 1);
				 */
	
				Utils.showCustomToast(activity,
						result.successfulPictures + " of " + result.attemptedPictures + " pictures ready to share", true, 1);
				activity.showMultiSelectBar(false);
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
		if (adapter != null)
			data.cache = adapter.getMemoryCache();
		data.picturesSelected = picturesSelected;
		data.isMultiSelectMode = isMultiSelectMode;
		configurationProperties.customData = data;
	}

	private static class ConfigurationData{
		public Drawable backgroundDrawable;
		public MemoryCache<Long> cache = null;
		public HashSet<Long> picturesSelected;
		public boolean isMultiSelectMode;
		
	}

	@Override
	protected void onDestroyOverride() {

		// null out adapter
		if (gridView != null)
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
					PicturesAdapter.imageLoaderCallback(ctx, null));
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
			ImageView selector = (ImageView)vi.findViewById(R.id.selector);

			// recycle bitmaps
			/*
			if (convertView != null){
				Drawable toRecycle= image.getDrawable();
				if (toRecycle != null) {
				    ((BitmapDrawable)image.getDrawable()).getBitmap().recycle();
				}
			}
			 */

			// change view size
			image.getLayoutParams().height = actualPicWidth;
			image.getLayoutParams().width = actualPicWidth;

			// fill the views
			text.setText("");
			if (data.moveToPosition(position)){
				TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(data.getRowId(), groupId);
				imageLoader.DisplayImage(data.getRowId(), loaderData, loaderData, image, null);

				// set the border
				if(picturesSelected.contains(data.getRowId())){
					// change size of selector and make visible
					selector.getLayoutParams().height = actualPicWidth;
					selector.getLayoutParams().width = actualPicWidth;
					selector.setVisibility(View.VISIBLE);
				}else
					selector.setVisibility(View.GONE);
			}

			return vi;
		}

		/**
		 * Set the memory cache to this new value, clearing old one.
		 * @see getMemoryCache.
		 * @param mem
		 */
		public void restoreMemoryCache(com.tools.images.MemoryCache<Long> mem){
			imageLoader.restoreMemoryCache(mem);
		}
	}
}
