package com.instantPhotoShare.Activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.tools.CustomActivity;
import com.tools.TwoObjects;
import com.tools.ViewLoader;
import com.tools.ViewLoader.LoadData;
import com.tools.images.CustomGallery;
import com.tools.images.ImageLoader.LoadImage;
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

	//enums for async calls
	private enum MENU_ITEMS {
		ROTATE_CCW, ROTATE_CW, SET_AS_DEFAULT_PICTURE, SHARE_PICTURE;
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
			e.printStackTrace();
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
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		super.onPause();
	}

	@Override
	public void onResume(){
		if (adapter != null)
			adapter.imageLoader.restartThreads();
		super.onResume();
	}

	// fill list with the pictures
	private void fillPictures() {
		// set adapter
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		adapter = new PicturesGridAdapter(this, picturesAdapater);
		if (oldCache != null){
			adapter.restoreMemoryCache(oldCache);
			oldCache = null;
		}
		gallery.setAdapter(adapter);
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

		overridePendingTransition(R.anim.picture_scale_up_animation, 0);

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
		//	MenuItem rotateCCW = menu.add(0, MENU_ITEMS.ROTATE_CCW.ordinal(), 0, "Rotate CCW");
		//MenuItem setAsGroupPicture = menu.add(0, MENU_ITEMS.ROTATE_CW.ordinal(), 0, "Rotate CW");
		menu.add(0, MENU_ITEMS.SET_AS_DEFAULT_PICTURE.ordinal(), 0, "Set as Group Picture");
		menu.add(0, MENU_ITEMS.SHARE_PICTURE.ordinal(), 0, "Share Picture");

		// add icons
		//	selectAll.setIcon(drawable.emo_im_laughing);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS id = MENU_ITEMS.convert(item.getItemId());
		// decide on what each button should do
		switch(id) {
		case ROTATE_CCW:
			View view = gallery.getChildAt(0);//gallery.getLastVisiblePosition());
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
			//	picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
			//com.tools.Tools.rotateExif(
			//			picturesAdapater.getFullPicturePath(), -1);
			//	adapter.clearCache();
			//	adapter.notifyDataSetChanged();
			return true;
		case SET_AS_DEFAULT_PICTURE:
			picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
			GroupsAdapter groups = new GroupsAdapter(this);
			groups.setPictureId(groupId, picturesAdapater.getRowId());
			return true;
		case ROTATE_CW:
			picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
			try {
				com.tools.ImageProcessing.rotateExif(
						picturesAdapater.getFullPicturePath(), 1);
			} catch (IOException e) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				Toast.makeText(ctx, "Could not rotate picture", Toast.LENGTH_SHORT).show();
				return true;
			}
			adapter.clearCache();
			adapter.notifyDataSetChanged();
			return true;
		case SHARE_PICTURE:
			sharePicture();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * Share the current picture with a sharing intent
	 */
	private void sharePicture(){
		// create subject, body, and prompt
		String shareBody = "Take a look at my picture from Share Bear. To share with me, download Share Bear and we can share instantly!";
		String shareSubject = "A Picture from my Share Bear group " + unformatedGroupName;
		String prompt = "Share picture";

		// grab the picture file
		picturesAdapater.moveToPosition(gallery.getLastVisiblePosition());
		String fileName = picturesAdapater.getFullPicturePath();
		if (fileName == null || fileName.length() == 0 || !(new File(fileName)).exists()){
			Toast.makeText(this, "No full picture. Thumbnail used", Toast.LENGTH_SHORT).show();
			fileName = picturesAdapater.getThumbnailPath();
		}

		// send the intent
		if(!com.tools.Tools.sharePicture(this, shareSubject, shareBody, fileName, prompt))
			Toast.makeText(this, "Picture could not be sent", Toast.LENGTH_SHORT).show();
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
					PicturesAdapter.imageLoaderCallback(ctx));		
			
			nameLoader = new ViewLoader<Long, Long, String, TextView>(
					"Unknown Person",
					new LoadData<Long, String, TextView>() {

						@Override
						public String onGetData(Long key) {
							UsersAdapter users = new UsersAdapter(ctx);
							users.fetchUser(key);
							return users.getName();
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
			}

			// return the view
			return vi;
		}
	}
}
