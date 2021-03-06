package com.instantPhotoShare.Activities;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.GroupSpinnerAdapter;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter.ThumbnailListener;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.TwoObjects;
import com.tools.ViewLoader;
import com.tools.ViewLoader.LoadData;
import com.tools.images.CustomGallery;
import com.tools.images.ImageViewTouch;

public class SinglePictureGallery 
extends CustomActivity{

	// private variables
	private PicturesGridAdapter adapter; 		// the adapter to show pictures
	private PicturesAdapter picturesAdapater;	// An array of all the pictures
	private long groupId; 						// the groupId that we are currently showing
	private int initialPicturePosition; 		// The first picture to show
	private String formattedGroupName; 			// The group name we are currently showing with formatting
	private String unformatedGroupName; 		// the unformatted group name
	private int pictureWindowWidth; 			// the width of the area the picture fits inside
	private int pictureWindowHeight; 			// the height of the area the picture fits inside
	private com.tools.images.MemoryCache<Long> oldCache = null; 		// the old imageloader cache. use this to handle screen rotations.
	private boolean isChangeGroupShowing = false;		// boolean to keep track if group selector is showing

	// graphics
	private CustomGallery gallery; 				// the gallery to show pictures
	private ImageView takePictureButton; 		// the pointer to the take picture button
	private TextView groupNameText; 			// Pointer to textView showing the group name

	// public static variables for passing values, be very careful with these
	/** Static variable for passing in MemoryCache <br>
	 * only should be used from InsidePictureGallery, right before calling startActivity.
	 */
	public static com.tools.images.MemoryCache<Long> passedCache = null;

	// variables to indicate what can be passed in through intents
	public static final String GROUP_ID = "GROUP_ID";
	public static final String PICTURE_POSITION = "PICTURE_POSITION";
	private static final String FETCHING_DATA_TAG = "fetchingData";

	//enums for async calls
	private enum MENU_ITEMS {
		SET_AS_DEFAULT_PICTURE, SHARE_PICTURE, ADD_TO_ANOTHER_GROUP, DELETE_PICTURE, ROTATE_CW, ROTATE_CCW, ;
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
			throw new IllegalArgumentException("SinglePictureGallery cannot be called without a groupId passed in");

		// the group id passed
		groupId = extras.getLong(GROUP_ID, -1);
		if (groupId == -1)
			throw new IllegalArgumentException("SinglePictureGallery cannot be called without a groupId passed in");

		// the initial pictureID
		initialPicturePosition = extras.getInt(PICTURE_POSITION, -1);
		if (initialPicturePosition == -1)
			throw new IllegalArgumentException("SinglePictureGallery cannot be called without a PICTURE_POSITION passed in");

		// grab the group name
		GroupsAdapter groups = new GroupsAdapter(this);
		try{
			formattedGroupName = groups.getGroup(groupId).toString();
			unformatedGroupName = groups.getGroup(groupId).getName();
		}catch(Exception e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			finish();
			return;
		}

		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationPropertiesCustom data = (ConfigurationPropertiesCustom) config.customData;
			if (data != null){
				oldCache = data.cache;
			}
		}

		// get any passed cache
		if (passedCache != null){
			oldCache = passedCache;
			passedCache = null;
		}

		// initialize layout
		initializeLayout();	

		// grab cursor for all the groups
		getPictures();
		fillPictures();

		// move to correct position
		if (picturesAdapater.size() > initialPicturePosition) 
			gallery.setSelection(initialPicturePosition, false);
	}

	@Override
	public void onPause(){
		overridePendingTransition(0, R.anim.picture_scale_down_animation);
		if (adapter != null){
			adapter.imageLoader.stopThreads();
			adapter.nameLoader.stopThreads();
		}
		super.onPause();
	}

	@Override
	public void onResume(){
		if (adapter != null){
			adapter.imageLoader.restartThreads();
			adapter.nameLoader.restartThreads();
		}
		super.onResume();
	}

	// fill list with the pictures
	private void fillPictures() {
		// save index and top position
		int index = Gallery.INVALID_POSITION;
		index = gallery.getFirstVisiblePosition();
		if (index == 0)
			index = Gallery.INVALID_POSITION;
				
		// set adapter
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		adapter = new PicturesGridAdapter(this, picturesAdapater);
		if (oldCache != null){
			adapter.restoreMemoryCache(oldCache);
			oldCache = null;
		}
		gallery.setAdapter(adapter);
		
		// restore position
		if (index >= picturesAdapater.size())
			index = picturesAdapater.size()-1;
		if (index != Gallery.INVALID_POSITION)
			gallery.setSelection(index);	
	}

	/**
	 * Find the cursor required for pictures
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

		overridePendingTransition(R.anim.picture_scale_up_animation, R.anim.fade_out);

		// set main view
		setContentView(R.layout.single_picture_gallery);

		// grab pointers to objects
		gallery = (CustomGallery)findViewById(R.id.galleryView);
		gallery.setImageViewTouchId(R.id.picture);
		takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
		groupNameText = (TextView) findViewById(R.id.groupName);

		// add click listener
		gallery.setOnItemClickListener(pictureClick);

		// set alpha of take button picture
		takePictureButton.setAlpha(Utils.PICTURE_ALPHA);

		// set font style
		groupNameText.setTypeface(Typeface.SANS_SERIF);

		// set the name
		groupNameText.setText(Html.fromHtml(formattedGroupName));

		// determine size of screen
		Display display = getWindowManager().getDefaultDisplay();
		pictureWindowWidth = display.getWidth();
		pictureWindowHeight = display.getHeight();
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
		ConfigurationPropertiesCustom data = new ConfigurationPropertiesCustom();
		data.cache = adapter.getMemoryCache();

		configurationProperties.customData = data;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Add the menu items
		menu.add(0, MENU_ITEMS.SET_AS_DEFAULT_PICTURE.ordinal(), 0, "Set as Group Picture");
		menu.add(0, MENU_ITEMS.SHARE_PICTURE.ordinal(), 0, "Share Picture");
		menu.add(0, MENU_ITEMS.ADD_TO_ANOTHER_GROUP.ordinal(), 0, "Add to another group");
		MenuItem delete = menu.add(0, MENU_ITEMS.DELETE_PICTURE.ordinal(), 0, "Delete Picture");
		MenuItem rotateCW = menu.add(0, MENU_ITEMS.ROTATE_CW.ordinal(), 0, "Rotate");
		MenuItem rotateCCW = menu.add(0, MENU_ITEMS.ROTATE_CCW.ordinal(), 0, "Rotate");


		// add icons
		delete.setIcon(R.drawable.delete);
		rotateCW.setIcon(R.drawable.rotate_cw);
		rotateCCW.setIcon(R.drawable.rotate_ccw);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS id = MENU_ITEMS.convert(item.getItemId());
		View view = gallery.getChildAt(0);

		// move to correct location
		picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
		String path = picturesAdapater.getFullPicturePath();
		String thumbPath = picturesAdapater.getThumbnailPath();
		TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(picturesAdapater.getRowId(), groupId);

		boolean isFullRotated = false;
		boolean isThumbRotated = false;

		// decide on what each button should do
		switch(id) {
		case ROTATE_CCW:

			// rotate teh picture in the background
			try{
				com.tools.ImageProcessing.rotateExif(path, -1);//, new OnRotateCallback());
				isFullRotated = true;
				com.tools.ImageProcessing.rotateExif(thumbPath, -1);//, new OnRotateCallback());
				isThumbRotated = true;
			}catch(IOException e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				if (!isFullRotated && !isThumbRotated)
					Utils.showCustomToast(ctx, "Rotation error. Full picture probably not downloaded yet", true, 1);
				else if (isFullRotated && !isThumbRotated){
					Utils.showCustomToast(ctx, "Rotation error.", true, 1);
					try {
						com.tools.ImageProcessing.rotateExif(path, 1);
					} catch (IOException e1) {
						Log.e("TAG", Log.getStackTraceString(e1));
					}
				}
				return true;
			}
			adapter.imageLoader.clearCacheAtId(picturesAdapater.getRowId());

			/*
			// rotation animation
			RotateAnimation rot = new RotateAnimation(
					0, -90,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);

			// add animations to set
			AnimationSet set = new AnimationSet(false);
			set.addAnimation(rot);
			set.setFillAfter(true);
			set.setDuration(300);
			view.startAnimation(set);
			 */

			adapter.imageLoader.DisplayImage(picturesAdapater.getRowId(), loaderData, loaderData, (ImageViewTouch)view.findViewById(R.id.picture), null);

			return true;
		case SET_AS_DEFAULT_PICTURE:
			GroupsAdapter groups = new GroupsAdapter(this);
			groups.setPictureId(groupId, picturesAdapater.getRowId());
			return true;
		case ROTATE_CW:
			// rotate teh picture in the background
			isFullRotated = false;
			isThumbRotated = false;
			try{
				com.tools.ImageProcessing.rotateExif(path, 1);//, new OnRotateCallback());
				isFullRotated = true;
				com.tools.ImageProcessing.rotateExif(thumbPath, 1);//, new OnRotateCallback());
				isThumbRotated = true;
			}catch(IOException e){
				Log.w(Utils.LOG_TAG, Log.getStackTraceString(e));
				if (!isFullRotated && !isThumbRotated)
					Utils.showCustomToast(ctx, "Rotation error. Full picture probably not downloaded yet", true, 1);
				else if (isFullRotated && !isThumbRotated){
					Utils.showCustomToast(ctx, "Rotation error.", true, 1);
					try {
						com.tools.ImageProcessing.rotateExif(path, -1);
					} catch (IOException e1) {
						Log.e("TAG", Log.getStackTraceString(e1));
					}
				}
				return true;
			}
			adapter.imageLoader.clearCacheAtId(picturesAdapater.getRowId());

			/*
			// rotation animation
			RotateAnimation rot2 = new RotateAnimation(
					0, 90,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);

			// add animations to set
			AnimationSet set2 = new AnimationSet(false);
			set2.addAnimation(rot2);
			set2.setFillAfter(true);
			set2.setDuration(300);
			view.startAnimation(set2);
			 */

			adapter.imageLoader.DisplayImage(picturesAdapater.getRowId(), loaderData, loaderData, (ImageViewTouch)view.findViewById(R.id.picture), null);
			return true;
		case SHARE_PICTURE:
			sharePicture();
			return true;
		case ADD_TO_ANOTHER_GROUP:
			showChooseGroup();
			return true;
		case DELETE_PICTURE:

			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						deletePicture();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
					}
				}
			};

			// show the dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog dialog = builder.setMessage("Are you sure you want to delete this picture? It will be gone from all groups forever.").setPositiveButton("Yes", dialogClickListener)
					.setNegativeButton("No", dialogClickListener).create();
			addDialog(dialog);
			dialog.show();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * Delete the picture at the given location
	 */
	private void deletePicture(){
		ArrayList<String> bars = new ArrayList<String>(1);
		bars.add(FETCHING_DATA_TAG);
		try {
			picturesAdapater.deletePicture(bars, this, new PicturesAdapter.ItemsFinished<SinglePictureGallery>(){

				@Override
				public void onItemsFinishedUI(SinglePictureGallery act,
						Exception e) {
					if (e != null){
						String msg = e.getMessage();
						if (msg == null || msg.length() == 0)
							msg = "Could not delete picture.";
						Utils.showCustomToast(act, msg, true, 1);
					}else{
						Utils.showCustomToast(act, "Picture deleted", true, 1);
						getPictures();
						fillPictures();
					}
				}

				@Override
				public void onItemsFinishedBackground(SinglePictureGallery act,
						Exception e) {					
				}
			});
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null || msg.length() == 0)
				msg = "Could not delete picture.";
			Utils.showCustomToast(ctx, msg, true, 1);
		}
	}

	private static class OnRotateCallback
	implements CustomAsyncTask.FinishedCallback<SinglePictureGallery, IOException>{

		@Override
		public void onFinish(
				SinglePictureGallery activity,
				IOException result) {
			if (result != null){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(result));
				if (activity != null)
					Utils.showCustomToast(activity, "Picture could not be rotated", true, 1);
			}

		}
	};

	/**
	 * Share the current picture with a sharing intent
	 */
	private void sharePicture(){
		// create subject, body, and prompt
		String shareBody = "Take a look at my picture from Share Bear. You can share instantly by downloading Share Bear at " + Utils.APP_URL;
		String shareSubject = "A Picture from my Share Bear group " + unformatedGroupName;
		String prompt = "Share picture";

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
	}

	private void showChooseGroup(){
		// grab the spinner
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);
		if (spinner.isShown())
			return;

		// grab all groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(this);
		final ArrayList<Group> groups = groupsAdapter.getAllGroups();

		// make sure there are groups
		if (groups == null || groups.size() == 0){
			Utils.showCustomToast(this, "No groups to add to. Create one First", true, 1);
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
					return;
				}else
					isChangeGroupShowing = !isChangeGroupShowing;

				long sourceGroupId = groups.get(position).getRowId();
				addToAnotherGroup(sourceGroupId);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {

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
	 * Add this picture to another group
	 * @param sourceGroupId the groupRowId to move to
	 */
	private void addToAnotherGroup(long sourceGroupId){
		ArrayList<String> bars = new ArrayList<String>(1);
		bars.add(FETCHING_DATA_TAG);
		try {
			picturesAdapater.addPictureToGroup(bars, this, groupId, sourceGroupId, new PicturesAdapter.ItemsFinished<SinglePictureGallery>(){

				@Override
				public void onItemsFinishedUI(SinglePictureGallery act,
						Exception e) {
					if (e != null){
						String msg = e.getMessage();
						if (msg == null || msg.length() == 0)
							msg = "Could not copy picture.";
						Utils.showCustomToast(act, msg, true, 1);
					}else{
						Utils.showCustomToast(act, "Picture copied", true, 1);
					}
				}

				@Override
				public void onItemsFinishedBackground(SinglePictureGallery act,
						Exception e) {					
				}
			});
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null || msg.length() == 0)
				msg = "Could not copy picture.";
			Utils.showCustomToast(ctx, msg, true, 1);
		}
	}

	/**
	 * Additional configuration properties
	 *
	 */
	private static class ConfigurationPropertiesCustom{
		com.tools.images.MemoryCache<Long> cache = null;	
	}

	@Override
	protected void onDestroyOverride() {
		com.tools.NoDefaultSpinner spinner = (com.tools.NoDefaultSpinner) findViewById(R.id.spinner);
		ViewGroup enclosingFrame = (ViewGroup) findViewById(R.id.enclosingFrame);
		enclosingFrame.removeView(spinner);
		gallery.setAdapter(null);	
	}

	private class PicturesGridAdapter
	extends BaseAdapter {

		private PicturesAdapter data;
		private LayoutInflater inflater = null;
		private com.tools.images.ImageLoader<Long, TwoObjects<Long, Long>, TwoObjects<Long, Long>> imageLoader; 
		private com.tools.ViewLoader<Long, Long, String, TextView> nameLoader;

		public PicturesGridAdapter(Activity a, PicturesAdapter pictures) {
			data = pictures;
			inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			imageLoader = new com.tools.images.ImageLoader<Long, TwoObjects<Long, Long>, TwoObjects<Long, Long>>(
					android.R.color.transparent,
					pictureWindowWidth,
					pictureWindowHeight,
					true,
					PicturesAdapter.imageLoaderCallback(ctx, reloadName));		

			nameLoader = new ViewLoader<Long, Long, String, TextView>(
					"Photographer ...",
					5,
					new LoadData<Long, String, TextView>() {

						@Override
						public String onGetData(Long key) {
							UsersAdapter users = new UsersAdapter(ctx);
							users.fetchUser(key);
							String name = users.getName();
							if (name.length() == 0)
								name = null;
							return name;
						}

						@Override
						public void onBindView(String data, TextView view) {
							view.setText(data);
						}
					});
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
		public void restoreMemoryCache(com.tools.images.MemoryCache<Long> mem){
			imageLoader.restoreMemoryCache(mem);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			// inflate a new view if we have to
			View vi=convertView;
			if(convertView==null){
				vi = inflater.inflate(R.layout.single_picture_item, null);
				ImageView image2 = (ImageView)vi.findViewById(R.id.picture);
				FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) image2.getLayoutParams();
				params.height = pictureWindowHeight;
				params.width = pictureWindowWidth;
				image2.setLayoutParams(params);
			}

			// grab the items to display
			ImageViewTouch image = (ImageViewTouch)vi.findViewById(R.id.picture);
			TextView name = (TextView) vi.findViewById(R.id.personWhoTookPicture);
			ProgressBar progress = (ProgressBar)vi.findViewById(R.id.fullPictureProgressBar);

			// move to correct location and fill views
			if (data.moveToPosition(position)){
				TwoObjects<Long, Long> loaderData = new TwoObjects<Long, Long>(data.getRowId(), groupId);
				imageLoader.DisplayImage(data.getRowId(), loaderData, loaderData, image, progress);
				nameLoader.DisplayView(data.getRowId(), data.getUserIdWhoTook(), name);
				nameViews.put(data.getRowId(), new WeakReference<TextView>(name));
			}

			// return the view
			return vi;
		}
		
		private HashMap<Long, WeakReference<TextView>> nameViews = new HashMap<Long, WeakReference<TextView>>();
		private ThumbnailListener reloadName = new ThumbnailListener() {
			
			@Override
			public void onThumbNailDownloaded(final long pictureId) {
				// null leave
				if (pictureId <= 0)
					return;

				// get textView
				WeakReference<TextView> weak = nameViews.get(pictureId);
				if (weak == null)
					return;
				final TextView view = weak.get();
				if (view == null)
					return;

				PicturesAdapter pics = new PicturesAdapter(ctx);
				pics.fetchPicture(pictureId);
				final long userId = pics.getUserIdWhoTook();
				pics.close();
				((Activity)view.getContext()).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						nameLoader.DisplayView(pictureId, userId, view);
					}

				});
			}
		};
	}
}
