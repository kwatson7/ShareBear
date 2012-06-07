package com.instantPhotoShare.Activities;

import java.util.ArrayList;
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
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ShutterCallback;
import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tools.CameraHelper;
import com.tools.CustomActivity;
import com.tools.MultipleCheckPopUp;

public class TakePicture 
extends CustomActivity
implements SurfaceHolder.Callback{

	// private misc variables
	private CustomActivity act = this; 													// the activity
	private ArrayList<Group> selectedGroups = new ArrayList<GroupsAdapter.Group>(); 	// this list of selected groups
	private boolean isChangeGroupShowing = false;										// boolean to keep track if group selector is showing
	
	// pointers to graphics
	private com.tools.TextViewMarquee groupsPortraitView;								// The groups we have selected
	private SurfaceView surfaceView; 		 											// The surface view for camera
	private ImageView goButton; 														// the take picture button
	private LinearLayout bottomLinearLayout; 											// the layout holding keep and retake
	private ImageView lastPictureButton; 												// button for last picture taken
	private ImageButton goHomeButton; 													// button to send us home
	
	// private constants
	private static final String DEFAULT_PRIVATE_NAME = "Private"; 						// String to show when no group is selected
	private static long ANIM_DURATION = 500; 											// how many ms to take for the rotation
	private static int PIXEL_FORMAT = PixelFormat.RGB_565; 								// pixel format of preview
	
	// camera private variables
	private android.hardware.Camera camera = null;					// camera object
	private Boolean isTryingToTakePicture = false;					// boolean used by autofocus callback
	private boolean isFocused = false; 								// keep track if camer is focused
	private CameraHelper cameraHelper; 								// helper for taking care of camera rotations and other camera stuff
	private int camRotation = 0; 									// the current rotation of the camera
	private byte[] camBytes = null; 								// The bytes of the camera data
	private SurfaceHolder previewHolder = null; 					// holds the camera preview
	private boolean isSurfaceCreated = false; 						// boolean to keep track if surface is created.
	private boolean isWaitingForPictureSave = false; 				// is the take picture deactivated and we are waiting for user to choose
	private MediaPlayer shutterSound = null; 						// sound for shutter
	
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
						!isWaitingForPictureSave){
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
        surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        goButton = (ImageView) findViewById(R.id.goButton);
        bottomLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutBottom);
        lastPictureButton = (ImageView) findViewById(R.id.goToGallery);
        goHomeButton = (ImageButton) findViewById(R.id.homeButton);

        // read the last groups
        readLastGroups();

        // set the string to the current groups
        setGroupsString(); 

        // class for keeping camera orientated properly, and force to stay in landscape
		cameraHelper = new CameraHelper(null, Utils.IMAGE_QUALITY, false, onRotate);
		cameraHelper.onCreate(this);

		// setup surface for holding camera
		//getWindow().setFormat(PixelFormat.TRANSLUCENT)
		previewHolder = surfaceView.getHolder();
		previewHolder.addCallback(this);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		previewHolder.setFormat(PIXEL_FORMAT);
		
		// set the picture thumbnail
		setLastPictureImage();
		
		// setup long click listener for button
		goButton.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {

				// cancel if we are already auto-focusing
				camera.cancelAutoFocus();

				// start the auto-focus
				camera.autoFocus(myAutoFocusCallback);

				return false;
			}
		});
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
	    cameraHelper.onResume(this);
	    
	    setupForNewPicture();
	    
	    // get the api version
	    int currentApiVersion = android.os.Build.VERSION.SDK_INT;
	    
	    // open the correct back facing camera
	    if (currentApiVersion >= Build.VERSION_CODES.GINGERBREAD) {
	    	Camera.CameraInfo info=new Camera.CameraInfo();

	    	// loop across all cameras
	    	for (int i=0; i < Camera.getNumberOfCameras(); i++) {
	    		Camera.getCameraInfo(i, info);

	    		// open the back facing one
	    		if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
	    			camera = Camera.open(i);
	    			break;
	    		}
	    	}
	    }

	    // open the camera if it is still null
	    if (camera == null) {
	    	camera = Camera.open();
	    }
	    	
	    //TODO: this is messy, and we should be utilizing surfaceChanged properly.

	    // update the rotateHelper
	    if (cameraHelper != null)
			cameraHelper.updateCam(camera);
	    
	    // set sizes manually
	    try {
	    	cameraHelper.setSurfacePreviewHolderSize(
	    			this,
	    			null,
	    			null,
	    			null,
	    			true,
	    			surfaceView);
	    } catch (Exception e) {
	    	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
	    }

	    // change the surface manually
	    if (isSurfaceCreated){
	    	surfaceCreated(previewHolder);
	    	surfaceChanged(previewHolder, PIXEL_FORMAT, surfaceView.getWidth(), surfaceView.getHeight());
	    }

	}

	@Override
	protected void onPause() {
	    
		// stop the preview
		cameraHelper.stopPreview();

		// release teh camera
		cameraHelper.updateCam(null);
		camera.release();
		camera=null;

		// other pauses that need to be called.
		super.onPause();
		cameraHelper.onPause();
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isSurfaceCreated = false;
	}
	
	@Override
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDestroyOverride() {		
	}
	
	public void surfaceClicked(View v){
		//TODO: complete
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
					//Toast.makeText(act, "must select at lesat one group", Toast.LENGTH_SHORT).show();
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
					Toast.makeText(act, "must select at lesat one group", Toast.LENGTH_SHORT).show();
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

	@Override
	public void surfaceChanged(
			SurfaceHolder holder,
			int format,
			int width,
			int height) {
		
		// set sizes manually
	    try {
	    	cameraHelper.setSurfaceSizePreviewSizeCameraSize(
	    			this,
	    			Camera.Parameters.FLASH_MODE_AUTO,
	    			null,
	    			null,
	    			null,
	    			true,
	    			surfaceView);
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
	    }
	    
		if (!isWaitingForPictureSave)
			cameraHelper.startPreview();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		isSurfaceCreated = true;
		try {
			camera.setPreviewDisplay(previewHolder);  
		}
		catch (Throwable t) {
			Log.e("InstantPhotoShare-surfaceCreated",
					"Exception in setPreviewDisplay()", t);
			Toast.makeText(this,
					t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
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
	private void prepareButtonsForPictureTake(){
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
		isWaitingForPictureSave = false;

		// start preview again
		cameraHelper.startPreview();
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
			prepareButtonsForPictureTake();
			
			// only save data if data is not null
			if (data != null) {

				// get rotation of camera
				camRotation = cameraHelper.getRotation();
				 
				// store camera data
				camBytes = data;

				// keep track that we are trying to take a picture
				isTryingToTakePicture = false;
				isFocused = false;
				isWaitingForPictureSave = true;
				
			// camera not generating any data	
			}else{
				Toast.makeText(act, "**Problem ** Camera is not generating any jpeg data", Toast.LENGTH_LONG).show();
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
			isFocused = true;
			
			// if we are trying to take a picture then make 
			// that callback and tell this class that we are no longer previewing
			if (isTryingToTakePicture){

				// take the picture
				AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
				camera.takePicture(shutterCallback, null, pictureCallback); 
				cameraHelper.setIsPreviewRunning(false);
			}
		}
	};

	/** Go button clicked. Start autofocus, disable button and then take picture */
	public void goClicked(View view) {

		// check if the preview is running, if not, we can't take a picture. We should never have gotten here
		if (isWaitingForPictureSave){
			Toast.makeText(this, "Complete last picture first.", Toast.LENGTH_LONG).show();
			return;
		}
		
		// currently trying to take a picture, so dont' take another
		if (isTryingToTakePicture)
			return;
		
		// now we are trying to take a pciture, so keep track
		isTryingToTakePicture = true;
		
		// start the auto-focus or take the picture if we need to.
		if (!isFocused){
			camera.cancelAutoFocus();
			camera.autoFocus(myAutoFocusCallback);
		}else{
			AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			camera.takePicture(shutterCallback, null, pictureCallback); 
			cameraHelper.setIsPreviewRunning(false);
		}
	}
}
