package com.instantPhotoShare.Activities;

import java.util.ArrayList;
import java.util.List;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Tasks.SaveTakenPictureTask;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ShutterCallback;
import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.IconContextMenu;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tools.CameraHelper;
import com.tools.CameraHelper.*;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.MultipleCheckPopUp;
import com.tools.images.ImageLoader;

public class TakePicture
extends CustomActivity{

	// private misc variables
	private CustomActivity act = this; 													// the activity
	private ArrayList<Group> selectedGroups = new ArrayList<GroupsAdapter.Group>(); 	// this list of selected groups
	private boolean isChangeGroupShowing = false;										// boolean to keep track if group selector is showing

	// pointers to graphics
	private com.tools.TextViewMarquee groupsPortraitView;								// The groups we have selected
	private ImageView goButton; 														// the take picture button
	private LinearLayout bottomLinearLayout; 											// the layout holding keep and retake
	private ImageView lastPictureButton; 												// button for last picture taken
	private ImageButton goHomeButton; 													// button to send us home
	private android.widget.VerticalSeekBar zoomBar; 									// The zooming bar
	private ImageView flashButton; 														// The flash button setting
	android.widget.IconContextMenu flashMenu;

	// private constants
	private static final String DEFAULT_PRIVATE_NAME = "Private"; 						// String to show when no group is selected
	private static final long ANIM_DURATION = 500; 											// how many ms to take for the rotation
	private static final String loadingPreviewProgressBar = "loadingPreviewProgressBar";
	
	// camera private variables
	private CameraHelper cameraHelper; 							// helper for taking care of camera rotations and other camera stuff
	private int camRotation = 0; 									// the current rotation of the camera
	private byte[] camBytes = null; 								// The bytes of the camera data
	private MediaPlayer shutterSound = null; 						// sound for shutter
	private MediaPlayer autoFocusSound = null; 						// sound for auto-focus
	
	// intent flags
	public static String GROUP_IDS = "GROUP_IDS";

	//enums for async calls
	private enum ASYNC_CALLS {
		SAVE_PICTURE;
		private static ASYNC_CALLS convert(int value)
		{
			return ASYNC_CALLS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		if(createHomeScreenIconIfNeedBe())
			return;

		// load in passed info
		Bundle extras = getIntent().getExtras(); 
		if (extras != null){
			// the group ids we are supposed to show
			String groupIds = extras.getString(GROUP_IDS);
			if (groupIds != null && groupIds.length() > 0)
				Prefs.setGroupIds(act, groupIds);
		}

		initializeLayout();	
	}
	
	@Override
	protected void initializeLayout() {

		// make full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// set layout
		setContentView(R.layout.take_picture);

		// find graphics pointers
		groupsPortraitView = (com.tools.TextViewMarquee) findViewById(R.id.groupsPortrait);
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		goButton = (ImageView) findViewById(R.id.goButton);
		bottomLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutBottom);
		lastPictureButton = (ImageView) findViewById(R.id.goToGallery);
		goHomeButton = (ImageButton) findViewById(R.id.homeButton);
		zoomBar = (android.widget.VerticalSeekBar) findViewById(R.id.zoomBar);
		flashButton = (ImageView) findViewById(R.id.flashButton);

		// read the last groups
		readLastGroups();

		// set the string to the current groups
		setGroupsString(); 

		// class for keeping camera orientated properly, and force to stay in landscape
		cameraHelper = new CameraHelper(Utils.IMAGE_QUALITY, new ExceptionCaught() {
			
			@Override
			public void onExceptionCaught(Exception e) {
				String msg = e.getMessage();
				if (msg == null || msg.length() == 0)
					msg = "Camera not working. Try again.";
				else
					msg += " Try again";
				Utils.showCustomToast(ctx, msg, true, 1);
				finish();
			}
		});
		cameraHelper.onCreate(this, surfaceView);

		// set the picture thumbnail
		setLastPictureImage();

		// setup long click listener for button
		goButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				// cancel if we are already auto-focusing
				cameraHelper.getCamera().cancelAutoFocus();

				// start the auto-focus
				cameraHelper.getCamera().autoFocus(myAutoFocusCallback);

				return false;
			}
		});

		// setup listener for surface view to hide and show settings
		surfaceView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// if we touch, then show zoom
				int action = event.getAction();
				boolean returnValue = false;

				switch(action){
				case MotionEvent.ACTION_DOWN:
					if (cameraHelper.isZoomSupported()){
						if (zoomBar.getVisibility() == View.VISIBLE)
							zoomBar.setVisibility(View.INVISIBLE);
						else
							zoomBar.setVisibility(View.VISIBLE);
					}else
						zoomBar.setVisibility(View.INVISIBLE);
					returnValue = true;
					break;
				case MotionEvent.ACTION_UP:
					break;
				case MotionEvent.ACTION_CANCEL:
					zoomBar.setVisibility(View.INVISIBLE);
					break;
				}

				return returnValue;
			}
		});

		// set listener for seekbar
		zoomBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				// set the zoom accordingly
				cameraHelper.setZoom(progress/100.0f);		
			}
		});

		setupFlashMenu();
	}

	/**
	 * Create a home screen icon for the main activity
	 * @return true if we created the icon, false otherwise
	 */
	private boolean createHomeScreenIconIfNeedBe(){
		// determine if we are supposed to just create a home screen icon
		final Intent intent0 = getIntent();
		final String action = intent0.getAction();

		if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {

			Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
			shortcutIntent.setClassName(this, this.getClass().getName());

			// Then, set up the container intent (the response to the caller)
			Intent intent = new Intent();
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Bear Cam");
			Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
					this,  R.drawable.shutter_icon_inverted);
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			// Now, return the result to the launcher
			setResult(RESULT_OK, intent);
			finish();
			return true;
		} 	
		return false;
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		ASYNC_CALLS task = ASYNC_CALLS.convert(requestCode);
		switch(task){
		case SAVE_PICTURE:
			switch(asyncTypeCall){
			case PROGRESS:
				setLastPictureImage();	
				break;
			}
			break;
		}
	}

	/**
	 * Set the image for the last picture button with that of the last picture
	 */
	private void setLastPictureImage(){
		PicturesAdapter pics = new PicturesAdapter(act);
		pics.fetchPicture(Prefs.getLastPictureTaken(act));
		if (pics.setThumbnail(lastPictureButton))
			lastPictureButton.setVisibility(View.VISIBLE);
		else
			lastPictureButton.setVisibility(View.INVISIBLE);
		pics.close();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event){
		// get the key code pressed and the type of press
		int keyCode = event.getKeyCode();
		int action = event.getAction();

		// on volume button press, take the picture
		if (action == KeyEvent.ACTION_DOWN &&
				(keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
						keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
						!cameraHelper.isWaitingForPictureSave()){
			goClicked(goButton);
			return true;

			// capture up events for volume buttons as well, so it doesn't beep	
		}else if (action == KeyEvent.ACTION_UP &&
				(keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
						keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
			return true;
		}

		// all other keypress do normal
		return super.dispatchKeyEvent(event);	
	}

	/**
	 * Setup menu to be displayed when we want to change the flash
	 */
	private void setupFlashMenu(){
		flashMenu = new IconContextMenu(ctx, R.menu.flash_menu);
		android.widget.IconContextMenu.IconContextItemSelectedListener listener = new android.widget.IconContextMenu.IconContextItemSelectedListener() {

			@Override
			public void onIconContextItemSelected(MenuItem item, Object info) {
				String oldFlash = cameraHelper.getFlashMode();
				switch(item.getItemId()){
				// set the flash to auto
				case R.id.autoFlash:

					// change the default contact method
					if (cameraHelper.setFlash(Camera.Parameters.FLASH_MODE_AUTO)){
						flashButton.setImageDrawable(getResources().getDrawable(R.drawable.flash_auto));
					}else
						Utils.showCustomToast(ctx, "Could not set flash", true, 1);
					break;

					// turn the flash on	
				case R.id.flashOn:
					// change the default contact method
					if (cameraHelper.setFlash(Camera.Parameters.FLASH_MODE_ON)){
						flashButton.setImageDrawable(getResources().getDrawable(R.drawable.flash_on));
					}else
						Utils.showCustomToast(ctx, "Could not set flash", true, 1);
					break;

					// turn the flash off	
				case R.id.flashOff:
					// change the default contact method
					if (cameraHelper.setFlash(Camera.Parameters.FLASH_MODE_OFF)){
						flashButton.setImageDrawable(getResources().getDrawable(R.drawable.flash_off));
					}else
						Utils.showCustomToast(ctx, "Could not set flash", true, 1);
					break;
				}
				
				// update camera if flash mode changed
				if (cameraHelper.getFlashMode() != null && (oldFlash == null ||
						!oldFlash.equalsIgnoreCase(cameraHelper.getFlashMode())) &&
						com.tools.Tools.isDeviceThatRequiresNewCameraOnFlashChange()){
					onPause();
					onResume();
				}
			}
		};
		flashMenu.setOnIconContextItemSelectedListener(listener);
		Integer color = Color.argb(128, 128, 128, 128);
		flashMenu.setBackgroundColor(color);
	}

	/**
	 * When we click the flash button, open a menu
	 * @param v
	 */
	public void onFlashClicked(View v){
		String flashMode = cameraHelper.getFlashMode();
		setupFlashMenu();
		if (flashMode == null)
			return;
		int color = Color.argb(200, 0, 198, 253);
		if (flashMode.compareToIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)==0)
			flashMenu.setSelectedBackgroundColor(color, 0);
		else if (flashMode.compareToIgnoreCase(Camera.Parameters.FLASH_MODE_ON)==0)
			flashMenu.setSelectedBackgroundColor(color, 1);
		else if (flashMode.compareToIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)==0)
			flashMenu.setSelectedBackgroundColor(color, 2);
		flashMenu.show();
	}

	/**
	 * Rotation callback. Rotate the views of interest.
	 */
	private CameraHelper.OnRotationCallback onRotate = 
		new CameraHelper.OnRotationCallback() {

		@Override
		public void onRotation(int orientation, int lastOrientation) {
			goButton.startAnimation(getIconAnimationSet(lastOrientation, orientation));
			lastPictureButton.startAnimation(getIconAnimationSet(lastOrientation, orientation));
			goHomeButton.startAnimation(getIconAnimationSet(lastOrientation, orientation));
		}
	};

	/**
	 * Rotate the take picture icon appropriately
	 * @param start the starting angle of orientation
	 * @param finish the ending angle of orientation
	 * @return
	 */
	private AnimationSet getIconAnimationSet(final int start, final int finish){

		// the orientations are screwed up, so fix
		float angleStart = 90-start;
		float angleEnd = 90-finish;

		// make the rotation go as quickly as possible (not more than 180 deg of rotation)
		angleEnd = (float) (angleEnd - com.tools.MathTools.fix((angleEnd-angleStart)/180.0f)*360);

		// rotation animation
		RotateAnimation rot = new RotateAnimation(
				angleStart, angleEnd,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);

		// add animations to set
		AnimationSet set = new AnimationSet(false);
		set.addAnimation(rot);
		set.setFillAfter(true);
		set.setDuration(ANIM_DURATION);

		// return the set
		return set;
	}

	@Override 
	protected void onResume() {
		super.onResume();

		// open the correct camera
		cameraHelper.openBackCamera();

		cameraHelper.onResume(onRotate, null);
		if (!cameraHelper.isWaitingForPictureSave())
			setupForNewPicture();

		// check if we have a flash
		List<String> modes = cameraHelper.getSupportedFlashModes();
		if (modes == null || modes.size() == 0)
			flashButton.setVisibility(View.INVISIBLE);
		else
			flashButton.setVisibility(View.VISIBLE);
		
		// show toast if only 1 group
		new CustomAsyncTask<TakePicture, Void, Integer>(TakePicture.this, -1, true, true, null){

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
				if (result <= 1 && callingActivity != null)
					Utils.showCustomToast(callingActivity, "You should make a shared group first.", true, 1);
			}

			@Override
			protected void setupDialog() {
				// TODO Auto-generated method stub
				
			}
			
		}.execute();	
	}

	@Override
	protected void onPause() {

		super.onPause();
		
		// other pauses that need to be called.
		cameraHelper.onPause();
		if (shutterSound != null)
			shutterSound.release();
		shutterSound = null;
		if (autoFocusSound != null)
			autoFocusSound.release();
		autoFocusSound = null;
		
	}
	
	@Override
	protected void additionalConfigurationStoring() {

	}

	@Override
	protected void onDestroyOverride() {		
	}

	/**
	 * Go to the gallery of the most recent picture and group
	 * @param v
	 */
	public void goToGallery(View v){
		// the most recent group
		long groupId = Prefs.getLastGroupOfLastPicture(act);

		GroupsAdapter groups = new GroupsAdapter(this);

		// no group, just go to top level of gallery
		if (groupId == -1 || groups.getGroup(groupId) == null){
			Intent intent = new Intent(this, GroupGallery.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return;
		}

		// the picture id
		long picId = Prefs.getLastPictureTaken(act);

		// no picture, just go to top level gallery
		if (picId == -1){
			Intent intent = new Intent(this, GroupGallery.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return;
		}

		// make sure picture is in group
		PicturesAdapter helper = new PicturesAdapter(act);
		if(!helper.isPictureInGroup(picId, groupId)){
			helper.close();
			Intent intent = new Intent(this, GroupGallery.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return;
		}
		helper.close();

		// find position in cursor
		helper.fetchPicturesInGroup(groupId);
		int position = -1;
		while (helper.moveToNext()){
			if (helper.getRowId() == picId){
				position = helper.getPosition();
				break;
			}
		}
		helper.close();

		// couldn't find picture, just go to group
		if (position == -1){
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
				position);

		// load the activity
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);		
	}

	/**
	 * Button clicked to send user back to home. Quit this activity
	 * @param v
	 */
	public void goHomeClicked(View v){
		Intent intent = new Intent(this, MainScreen.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);		
		finish();
	}

	public void changeGroupClicked(View v){
		if (isChangeGroupShowing)
			return;
		isChangeGroupShowing = true;

		// grab all groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(act);
		ArrayList<Group> groups = groupsAdapter.getAllGroups();

		// create dialog for selecting and naming group name
		final MultipleCheckPopUp<Group> chooseGroups = new MultipleCheckPopUp<Group>(
				act, 
				groups,
				"Choose which group(s).",
				false,
				null,
				null);

		// just addign buttons here
		chooseGroups.setButton(AlertDialog.BUTTON_POSITIVE, "Choose", new OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {}});

		// keep track if this dialog is showing
		chooseGroups.setOnDismissListener(new OnDismissListener() {	
			@Override
			public void onDismiss(DialogInterface dialog) {
				isChangeGroupShowing = false;
				if (selectedGroups.size() == 0){
					//Utils.showCustomToast(act, "must select at lesat one group", true, 1);
					//changeGroupClicked(groupsPortraitView);
					finish();
				}	
			}
		});

		// show it
		chooseGroups.show();

		// set the listener for button
		chooseGroups.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				selectedGroups = chooseGroups.getCheckedItemsGeneric();
				//GroupsAdapter selectedGroups2 = (GroupsAdapter) chooseGroups.getCheckedItems();

				// if we are empty, don't allow
				if (selectedGroups.size() == 0){
					Utils.showCustomToast(act, "must select at least one group", true, 1);
					return;
				}

				// save if successful
				Prefs.setGroupIds(act, selectedGroups);
				setGroupsString();		
				chooseGroups.dismiss();
			}
		});
	}

	/**
	 * Use the selectedGroups array to set the string at the top of the camera
	 */
	private void setGroupsString(){
		if (groupsPortraitView == null)
			return;

		// default name
		if (selectedGroups == null || selectedGroups.size() == 0){
			if (groupsPortraitView != null)
				groupsPortraitView.setText(DEFAULT_PRIVATE_NAME);
			return;
		}		

		// grab groups string and remove []
		String str = selectedGroups.toString();
		if (str.length() > 2)
			str = str.substring(1, str.length()-1);

		// set the text
		if (groupsPortraitView != null)
			groupsPortraitView.setText(Html.fromHtml(str));
	}

	/**
	 * Read from the shared Prefs teh last groups that were saved
	 */
	private void readLastGroups(){

		// read which were saved
		ArrayList<Long> list = Prefs.getGroupIds(this);

		// now grab the list of groups that match this
		GroupsAdapter groupsAdapter = new GroupsAdapter(act);
		selectedGroups = groupsAdapter.getGroupsMatchRowIds(list);

		// if we're empty, then make user choose
		if (selectedGroups.size() == 0)
			changeGroupClicked(groupsPortraitView);
	}
	
	/** Reset standard buttons and launch next window */
	public void keepClicked(View view){
		// switch views for buttons
		bottomLinearLayout.setVisibility(LinearLayout.GONE);

		savePicture();

		setupForNewPicture();
	}

	/**
	 * Write the picture to file and save in database
	 */
	private void savePicture(){
		// launch the async task to save picture
		SaveTakenPictureTask<TakePicture> task =  new SaveTakenPictureTask<TakePicture>(
				this,
				ASYNC_CALLS.SAVE_PICTURE.ordinal(),
				camBytes,
				camRotation,
				this.selectedGroups,
				"");
		task.execute();

		camBytes = null;
	}

	/** Switch back to standard buttons */
	public void retakeClicked(View view){
		setupForNewPicture();		
	}

	/** 
	 * Hide certain buttons for taking the picture
	 */
	private void prepareButtonsForPictureSave(){
		goButton.setVisibility(View.INVISIBLE);
		goHomeButton.setVisibility(View.INVISIBLE);
		lastPictureButton.setVisibility(View.INVISIBLE);
		((FrameLayout) findViewById(R.id.bottomPictureButtons)).setVisibility(View.INVISIBLE);

		// show bottom layout
		bottomLinearLayout.setVisibility(LinearLayout.VISIBLE);

		// invalidate views
		goButton.invalidate();
		goHomeButton.invalidate();
		lastPictureButton.invalidate();
		bottomLinearLayout.invalidate();

		// keep track that we are trying to save a picture
		cameraHelper.setTryingToTakePicture(false);
		cameraHelper.setFocused(false);
		cameraHelper.setIsWaitingForPictureSave(true);
	}

	/**
	 * Make screen ready for new picture.
	 */
	public void setupForNewPicture(){

		// switch visibility
		goButton.setVisibility(View.VISIBLE);
		goHomeButton.setVisibility(View.VISIBLE);
		lastPictureButton.setVisibility(View.VISIBLE);
		((FrameLayout) findViewById(R.id.bottomPictureButtons)).setVisibility(View.VISIBLE);

		// show bottom layout
		bottomLinearLayout.setVisibility(LinearLayout.INVISIBLE);

		// invalidate views
		goButton.invalidate();
		goHomeButton.invalidate();
		lastPictureButton.invalidate();
		bottomLinearLayout.invalidate();

		// keep track if we are waiting for the picture to be saved
		cameraHelper.setIsWaitingForPictureSave(false);
		cameraHelper.setTryingToTakePicture(false);
		cameraHelper.setFocused(false);

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		surfaceView.setVisibility(SurfaceView.VISIBLE);
		
		// start preview again
		cameraHelper.startPreview();
		
		// hide preview imageview
		ImageView image = (ImageView)findViewById(R.id.previewImageView);
		com.tools.Tools.recycleImageViewBitmap(image);
		image.setVisibility(View.INVISIBLE);
	}

	private android.hardware.Camera.ShutterCallback shutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {
			AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			AudioManager meng = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
			int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

			if (volume != 0)
			{
				if (shutterSound == null)
					shutterSound = MediaPlayer.create(act, R.raw.camera_click);
				if (shutterSound != null)
					shutterSound.start();
			}

		}
	};

	/** callback for taking a picture that saves important camera byte data*/
	private android.hardware.Camera.PictureCallback pictureCallback = new android.hardware.Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, android.hardware.Camera camera) {

			// disable buttons
			try{
				cameraHelper.getCamera().stopPreview(); // some cameras dont' automatically stop preview, so do it here
			}catch(Exception e){
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
			prepareButtonsForPictureSave();

			// only save data if data is not null
			if (data != null) {

				// get rotation of camera
				camRotation = cameraHelper.getRotation();

				// store camera data
				camBytes = data;
				
				// show actual picture data
				final SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
				ArrayList<String> bars = new ArrayList<String>(1);
				bars.add(loadingPreviewProgressBar);
				(new CustomAsyncTask<TakePicture, Void, Bitmap>(TakePicture.this, -1, false, true, bars) {

					@Override
					protected void onPreExecute() {
					}

					@Override
					protected Bitmap doInBackground(Void... params) {
						return com.tools.images.ImageLoader.getFullImage(camBytes, 90, surfaceView.getWidth(), surfaceView.getHeight());
					}

					@Override
					protected void onProgressUpdate(Void... progress) {

					}

					@Override
					protected void onPostExectueOverride(Bitmap result) {
						if (callingActivity == null || callingActivity.isFinishing() || 
								!callingActivity.cameraHelper.isWaitingForPictureSave())
							return;
						
						// assign picture to imageview
						ImageView image = (ImageView)callingActivity.findViewById(R.id.previewImageView);
						image.setImageBitmap(result);
						image.setVisibility(View.VISIBLE);
						LayoutParams params = image.getLayoutParams();
						SurfaceView surfaceView = (SurfaceView) callingActivity.findViewById(R.id.surface_camera);
						params.height = surfaceView.getHeight();
						params.width = surfaceView.getWidth();
						image.setLayoutParams(params);
						surfaceView.setVisibility(SurfaceView.INVISIBLE);
					}

					@Override
					protected void setupDialog() {

					}
				}).execute();

				// camera not generating any data	
			}else{
				Utils.showCustomToast(act, "**Problem ** Camera is not generating any jpeg data", true, 1);
				setupForNewPicture();
			}
		}
	};	

	/** Simple autofocus callback that re-enables 
	 * the go button and takes a picture if necessary */
	private AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			// keep track that we are currently focused
			cameraHelper.setFocused(true);

			// if we are trying to take a picture then make 
			// that callback and tell this class that we are no longer previewing
			if (cameraHelper.isTryingToTakePicture()){

				// take the picture
				AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
				cameraHelper.takePicture(shutterCallback, null, pictureCallback); 
				cameraHelper.setIsPreviewRunning(false);
				
			}else{
				// play auto-focus sound
				AudioManager meng = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
				int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

				if (volume != 0)
				{
					if (autoFocusSound == null)
						autoFocusSound = MediaPlayer.create(act, R.raw.camera_focus);
					if (autoFocusSound != null)
						autoFocusSound.start();
				}
				
			}
		}
	};

	/** Go button clicked. Start autofocus, disable button and then take picture */
	public void goClicked(View view) {

		/*
		int min = cameraHelper.getCamera().getParameters().getMinExposureCompensation();
		Log.e(Utils.LOG_TAG, "min="+min);
		int max = cameraHelper.getCamera().getParameters().getMaxExposureCompensation();
		Log.e(Utils.LOG_TAG, "max="+max);
		Camera.Parameters params = cameraHelper.getCamera().getParameters();
		int exp = 0;
		params.setExposureCompensation(exp);
		cameraHelper.getCamera().setParameters(params);
		*/
		
		// check if the preview is running, if not, we can't take a picture. We should never have gotten here
		if (cameraHelper.isWaitingForPictureSave()){
			Utils.showCustomToast(this, "Complete last picture first.", true, 1);
			return;
		}

		// currently trying to take a picture, so dont' take another
		if (cameraHelper.isTryingToTakePicture())
			return;

		// now we are trying to take a pciture, so keep track
		cameraHelper.setTryingToTakePicture(true);

		// start the auto-focus or take the picture if we need to.
		if (!cameraHelper.isFocused()){
			cameraHelper.getCamera().cancelAutoFocus();
			cameraHelper.getCamera().autoFocus(myAutoFocusCallback);
		}else{
			AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			cameraHelper.takePicture(shutterCallback, null, pictureCallback); 
			cameraHelper.setIsPreviewRunning(false);
		}
	}
}
