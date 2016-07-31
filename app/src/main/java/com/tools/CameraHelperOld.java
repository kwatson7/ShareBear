package com.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

public class CameraHelperOld{

	//TODO: make sure we are not leaking
	//TODO: incoroprate more from ShareBear>TakePicture.java into these calls.
	//TODO: look into using weak references

	// private member variables
	private OrientationEventListener mOrientationEventListener; 					// The listener to call when orientation changes
	private Orientation mOrientation = Orientation.ORIENTATION_LANDSCAPE_NORMAL;	// The current orientation of the camera
	private int jpegQuality = 90; 													// quality of the image
	private Camera mCamera = null; 													// The current camera
	private Boolean mChangeParameters; 												// Do we change parameters inside this class?
	private OnRotationCallback callback; 											// Call this when the phone orientation changes
	private boolean isPreviewRunning = false;										// keep track if the preview is currently running.
	private boolean isPreviewStarting = false; 										// keep track if we are currently in the process of starting preview
	private String flashMode = Camera.Parameters.FLASH_MODE_AUTO;
	
	// orientation enum
	public enum Orientation{
		ORIENTATION_PORTRAIT_NORMAL(90),ORIENTATION_LANDSCAPE_NORMAL(0),
		ORIENTATION_PORTRAIT_INVERTED(270), ORIENTATION_LANDSCAPE_INVERTED(180);

		private int orientationAngle = 0;

		private Orientation(int angle){
			orientationAngle = angle;
		}

		/**
		 * The angle of rotation for this orientation
		 * @return
		 */
		public int getAngle(){
			return orientationAngle;
		}
	}

	/** Class to keep camera surface from rotating and also to keep track of 
	 * orientation of camera so the picture is stored correctly.
	 * <p>
	 * You must call:<p>
	 *      (1)this class's onResume in the calling activity's onResume<br>
	 *      (2)this class's onPause in the calling activity's onPause<br>
	 *      (3)this class's onCreate in the calling activity's onCreate<br>
	 *      (4)this class's updateCam when the calling activity's camera is updated<br>
	 * @param cam, pass null if not activated yet
	 * @param jpegQuality 1-100, the jpeg quality, a good value is usually ~90
	 * @param changeParameters, Boolean to change the parameters of the rotation in
	 * the camera settings. This should be true if you want the camera drivers to rotate the 
	 * image for you, or use getRotation() manually later to do it yourself
	 * @param callback to happen when surface is rotated. Null if none desired
	 */
	public CameraHelperOld(
			Camera cam,
			int jpegQuality,
			Boolean changeParameters,
			OnRotationCallback callback){

		// store image quality
		if (jpegQuality < 1)
			jpegQuality = 1;
		else if (jpegQuality > 100)
			jpegQuality = 100;
		this.jpegQuality = jpegQuality;

		// store camera
		updateCam(cam);

		// store other values
		this.mChangeParameters = changeParameters;
		this.callback = callback;
	}

	/**
	 * Set the rotation callback. This is called when the screen changes orientation
	 * @param callback
	 */
	public void setRotationCallback(OnRotationCallback callback){
		this.callback = callback;
	}

	/**
	 * call this in the calling activity's onResume. MUST be done
	 * 
	 */
	public void onResume(Context ctx) {

		// prepare listener for orientation changes
		if (mOrientationEventListener == null) {            
			mOrientationEventListener = new OrientationEventListener(ctx, SensorManager.SENSOR_DELAY_NORMAL) {

				@Override
				public void onOrientationChanged(int orientation) {

					// determine our orientation based on sensor response
					Orientation lastOrientation = mOrientation;

					// pick the closest of the 4 orientations
					if (orientation >= 315 || orientation < 45)                   
						mOrientation = Orientation.ORIENTATION_PORTRAIT_NORMAL;
					else if (orientation < 315 && orientation >= 225)
						mOrientation = Orientation.ORIENTATION_LANDSCAPE_NORMAL;
					else if (orientation < 225 && orientation >= 135)
						mOrientation = Orientation.ORIENTATION_PORTRAIT_INVERTED;
					else // orientation <135 && orientation > 45
						mOrientation = Orientation.ORIENTATION_LANDSCAPE_INVERTED;                     

					if (lastOrientation != mOrientation) {
						changeRotation(mOrientation, lastOrientation);
					}
				}
			};
		}

		// enable the listener
		if (mOrientationEventListener.canDetectOrientation()) {
			mOrientationEventListener.enable();
		}
	}

	/**
	 * call this in the calling activity's onPause. MUST be done
	 */
	public void onPause() {
		mOrientationEventListener.disable();
		mOrientationEventListener = null;
		updateCam(null);
	}

	/**
	 * Performs required action to accommodate new orientation
	 * @param orientation
	 * @param lastOrientation
	 */
	private void changeRotation(Orientation orientation, Orientation lastOrientation) {

		// main switching of camera
		if (!(mChangeParameters == null || !mChangeParameters || mCamera == null)){
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setRotation(orientation.getAngle());
			mCamera.setParameters(parameters);	
		}

		// post callback
		if (callback != null)
			callback.onRotation(orientation.getAngle(), lastOrientation.getAngle());
	}

	/**
	 * call this in the calling activity's onCreate. MUST be done
	 */
	public void onCreate(Activity act){

		// force portrait layout
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		//act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE | ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	}

	/** 
	 * call this function to get the camera rotation, usually at the time of taking the picture
	 * @return value will be 0, 90, 180, or 270
	 */
	public int getRotation(){
		return mOrientation.getAngle();
	}

	/**
	 * Get the parameters for the given camera, or null if no camera available
	 * @return The parameters of the current camera or null
	 */
	private Parameters getParameters(){
		if (mCamera != null)
			return mCamera.getParameters();
		else
			return null;
	}

	/**
	 * Determine if zoom is supported for this camera.
	 * @return True if zoom supporetd, false otehrwise
	 */
	public boolean isZoomSupported(){
		Parameters parameters = getParameters();
		if (parameters == null)
			return false;

		boolean val = parameters.isSmoothZoomSupported();
		if (!val)
			val = parameters.isZoomSupported();

		return val;
	}

	/**
	 * Cleanly set the zoom for the camera. Will check if we are outside zoom bounds and truncate accordingly.
	 * Will not error if we don't have a camera or have a camera that can't zoom. Will attempt to smooth zoom if we can, else normal zoom.
	 * @param zoomLevel The zoomLevel we want to reach. Scale from 0 - 1 (maxZoom)
	 */
	public void setZoom(float zoomLevel){

		// get the parameters
		Parameters parameters = getParameters();
		if (parameters == null)
			return;

		// make sure we can zoom
		if (!isZoomSupported())
			return;

		// put zoomLevel within bounds
		if (zoomLevel < 0)
			zoomLevel = 0;
		if (zoomLevel > 1)
			zoomLevel = 1;

		// scale the zoom correclty.
		int maxZoom = parameters.getMaxZoom();
		int zoom = Math.round(zoomLevel*maxZoom);

		// set the zoom
		if (parameters.isSmoothZoomSupported())
			mCamera.startSmoothZoom(zoom);
		else{
			parameters.setZoom(zoom);
			mCamera.setParameters(parameters);
		}
	}

	/**
	 * Set the flash to the given mode. See getSupportedFlashModes()
	 * @param flashMode The mode to set
	 * @return true if successfully set, false otherewise
	 */
	public boolean setFlash(String flashMode){
		Parameters params = getParameters();
		List<String> flashList = getSupportedFlashModes();
		if (flashList == null || !flashList.contains(flashMode))
			return false;
		params.setFlashMode(flashMode);
		mCamera.setParameters(params);
		return true;
	}
	
	/**
	 * Get the current flashmode. Can be null if no camera is set.
	 * @return
	 */
	public String getFlashMode(){
		Parameters params = getParameters();
		if (params == null)
			return null;
		
		return params.getFlashMode();
	}

	/** call this in the calling activity, when camera is updated. <br>
	 * MUST be done <br>
	 * Also stops any currently running previews on old camera and assumes new camera does not have a preview running
	 * Will set the flash mode to whatever it was last, or auto if there was no last
	 * */
	public void updateCam(Camera newCam){
		// stop any running previews
		stopPreview();

		String oldFlashMode = getFlashMode();
		if (oldFlashMode != null)
			flashMode = oldFlashMode;
		if (flashMode == null)
			flashMode = Camera.Parameters.FLASH_MODE_AUTO;
		
		// set new camera
		mCamera = newCam;	

		// set the quality
		if (mCamera != null){
			Parameters params = mCamera.getParameters();
			params.setJpegQuality(jpegQuality);
			mCamera.setParameters(params);
			setFlash(flashMode);
		}
	}

	/**
	 * Determine the optimal width and height, based on max size and optimal choice
	 * @param sizes The list of possible sizes, usually from the camera properties
	 * @param maxWH The maximum width and height
	 * @param optWH The ideal width and height
	 * @return the best width and height from the list of sizes
	 */
	public static WidthHeight getBestWidthHeight(List <Size> sizes, WidthHeight maxWH, WidthHeight optWH){

		// check if none
		if (sizes.isEmpty())
			return null;

		// loop through possible ones and find the ones that are below the max
		ArrayList <Size> belowMax = new ArrayList<Size>();
		for (Iterator<Size> it = sizes.iterator (); it.hasNext();) {
			Size s = it.next ();
			if (maxWH == null)
				belowMax.add(s);
			else if (s.width <= maxWH.width && s.height <= maxWH.height)
				belowMax.add(s);
		}

		// check if none
		if (belowMax.isEmpty())
			return null;

		// function to check optimal is diff(width)^2 + diff(height)^2, and aspect ratio is 10x more important
		WidthHeight result = new WidthHeight(0, 0);
		double fitness = 1e12;
		double tmpFitness;
		for (Iterator<Size> it = belowMax.iterator (); it.hasNext();) {
			Size s = it.next ();
			tmpFitness = (double) Math.sqrt(Math.pow(s.width - optWH.width, 2) + 
					Math.pow(s.height - optWH.height, 2))/(optWH.height*.5+optWH.width*.5)+
					Math.abs((double)optWH.width/optWH.height - (double)s.width/s.height)*10;
			if (tmpFitness < fitness){
				fitness = tmpFitness;
				result.width = s.width;
				result.height = s.height;
			}
		}

		// check if nothing matched
		if (result.width == 0 && result.height == 0)
			result = null;

		// return result
		return result;
	}

	/**
	 * Get the preview size that fits within the given width and height and has the largest area
	 * @param width Width of are where preview can go in pixels
	 * @param height Height of where preview can go in pixels
	 * @param parameters camera parameters that store the possible preview sizes
	 * @return the best preview size
	 */
	public static Camera.Size getBestPreviewSized(
			int width,
			int height,
			Camera.Parameters parameters) {
		Camera.Size result=null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result=size;
				}
				else {
					int resultArea=result.width * result.height;
					int newArea=size.width * size.height;

					if (newArea > resultArea) {
						result=size;
					}
				}
			}
		}

		return(result);
	}

	/**
	 * Return the allowable flash modes of the camera
	 * Make sure to turn on in manifest:<br>
	 * <uses-permission android:name="android.permission.CAMERA" /> <br>
	 * <uses-feature android:name="android.hardware.camera" /> <br>
	 * <uses-feature android:name="android.hardware.camera.autofocus" /> <br>
	 * @return a list of supported flash modes for this camera
	 */
	public List<String> getSupportedFlashModes(){
		Parameters params = getParameters();
		if (params == null)
			return null;
		else
			return params.getSupportedFlashModes();
	}


	/**
	 * Set the surfaceView size to be within the set limits and scaled correctly. Also set the camera and preivew size.
	 * @param act An activity required to set some various parameters
	 * @param optimalWidthHeight The desired WidthHeight to make the final picture. Null if the maximum of camera is desired.
	 * @param maxWidthHeight The max WidthHeight to make the camera size. Null if maximum is desired
	 * @param windowSize The max WidthHeight to fit the preview in. Null if the full screen is desired.
	 * @param switchOrientation if orientation of layout is opposite of orientation of camera. Usually true if layout is portrait.
	 * @param surfaceView The surfaceView to manipulate.
	 * @throws Exception if we cannot find sizes for preview or camera that are acceptable.
	 */
	public void setSurfaceSizePreviewSizeCameraSize(
			Activity act,
			WidthHeight optimalWidthHeight,
			WidthHeight maxWidthHeight,
			WidthHeight windowSize,
			boolean switchOrientation,
			SurfaceView surfaceView)
					throws Exception {

		// stop the preview
		stopPreview();

		// grab default parameters
		android.hardware.Camera.Parameters params = mCamera.getParameters();

		// set orientation
		int orientation = ((WindowManager) act.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		int rotation = 0;
		if (orientation == 0)
			rotation = 90;
		else if (orientation == 3)
			rotation = 180;
		else if (orientation == 1)
			rotation = 0;
		mCamera.setDisplayOrientation(rotation);

		// get possible preview sizes and image sizes
		List <Size> sizes = params.getSupportedPictureSizes();
		List<Size> previewSizes = params.getSupportedPreviewSizes();

		// determine the max camera size
		WidthHeight max = new WidthHeight(0, 0);
		long pix = 0;
		for (Size item : sizes){
			long tmp = item.height*item.width;
			if (tmp > pix){
				pix = tmp;
				max.height=item.height;
				max.width=item.width;
			}
		}

		// optimal is the max if null was input
		if (optimalWidthHeight == null)
			optimalWidthHeight = new WidthHeight(max.width, max.height);

		// max is the max if null was input
		if (maxWidthHeight == null)
			maxWidthHeight = new WidthHeight(max.width, max.height);

		// max preview is window if null input
		if (windowSize == null){
			windowSize = new WidthHeight(
					act.getWindowManager().getDefaultDisplay().getWidth(), 
					act.getWindowManager().getDefaultDisplay().getHeight());
			if (switchOrientation)
				windowSize = windowSize.switchDimensions();
		}

		// get the image size that is closest to our optimal and set it
		WidthHeight bestWidthHeight = null;
		bestWidthHeight = getBestWidthHeight(sizes, maxWidthHeight, optimalWidthHeight);
		if (bestWidthHeight == null){
			throw new Exception("Could not find a camera size below maxWidthHeight and close to optimalWidthHeight");
		}else{
			params.setPictureSize(bestWidthHeight.width, bestWidthHeight.height);
		}

		// get the preview size that is closest to the image size
		WidthHeight bestWidthHeightPreivew = null;
		bestWidthHeightPreivew = 
				getBestWidthHeight(previewSizes, maxWidthHeight, bestWidthHeight);
		if (bestWidthHeightPreivew == null)
			throw new Exception("Could not find a camera preview size.");

		// determine how best to fit camera preview into surface
		params.setPreviewSize(bestWidthHeightPreivew.width, bestWidthHeightPreivew.height);
		WidthHeight fitWindowWidthHeight = ImageProcessing.fitNoCrop(bestWidthHeightPreivew, windowSize);
		if (switchOrientation)
			fitWindowWidthHeight = fitWindowWidthHeight.switchDimensions();

		// change height, but only if need be.
		if (surfaceView.getWidth() != fitWindowWidthHeight.width ||
				surfaceView.getHeight() != fitWindowWidthHeight.height){
			LayoutParams surfaceParams = surfaceView.getLayoutParams();
			surfaceParams.height = fitWindowWidthHeight.height;
			surfaceParams.width = fitWindowWidthHeight.width;
			surfaceView.setLayoutParams(surfaceParams);
		}

		// actually set the  parameters to camera
		mCamera.setParameters(params);		
	}

	/**
	 * Set the surfaceView size to be within the set limits and scaled correctly.
	 * @param act An activity required to set some various parameters
	 * @param optimalWidthHeight The desired WidthHeight to make the final picture. Null if the maximum of camera is desired.
	 * @param maxWidthHeight The max WidthHeight to make the camera size. Null if maximum is desired
	 * @param windowSize The max WidthHeight to fit the preview in. Null if the full screen is desired.
	 * @param switchOrientation if orientation of layout is opposite of orientation of camera. Usually true if layout is portrait.
	 * @param surfaceView The surfaceView to manipulate.
	 * @throws Exception if we cannot find sizes for preview or camera that are acceptable.
	 */
	public void setSurfacePreviewHolderSize(
			Activity act,
			WidthHeight optimalWidthHeight,
			WidthHeight maxWidthHeight,
			WidthHeight windowSize,
			boolean switchOrientation,
			SurfaceView surfaceView)
					throws Exception {

		// stop the preview
		stopPreview();

		// grab default parameters
		android.hardware.Camera.Parameters params = mCamera.getParameters();

		// get possible preview sizes and image sizes
		List <Size> sizes = params.getSupportedPictureSizes();
		List<Size> previewSizes = params.getSupportedPreviewSizes();

		// determine the max camera size
		WidthHeight max = new WidthHeight(0, 0);
		long pix = 0;
		for (Size item : sizes){
			long tmp = item.height*item.width;
			if (tmp > pix){
				pix = tmp;
				max.height=item.height;
				max.width=item.width;
			}
		}

		// optimal is the max if null was input
		if (optimalWidthHeight == null)
			optimalWidthHeight = new WidthHeight(max.width, max.height);

		// max is the max if null was input
		if (maxWidthHeight == null)
			maxWidthHeight = new WidthHeight(max.width, max.height);

		// max preview is window if null input
		if (windowSize == null){
			windowSize = new WidthHeight(
					act.getWindowManager().getDefaultDisplay().getWidth(), 
					act.getWindowManager().getDefaultDisplay().getHeight());
			if (switchOrientation)
				windowSize = windowSize.switchDimensions();
		}

		// get the image size that is closest to our optima
		WidthHeight bestWidthHeight = null;
		bestWidthHeight = getBestWidthHeight(sizes, maxWidthHeight, optimalWidthHeight);
		if (bestWidthHeight == null)
			throw new Exception("Could not find a camera size below maxWidthHeight and close to optimalWidthHeight");

		// get the preview size that is closest to the image size
		WidthHeight bestWidthHeightPreivew = null;
		bestWidthHeightPreivew = 
				getBestWidthHeight(previewSizes, maxWidthHeight, bestWidthHeight);
		if (bestWidthHeightPreivew == null)
			throw new Exception("Could not find a camera preview size.");

		// determine how best to fit camera preview into surface
		WidthHeight fitWindowWidthHeight = ImageProcessing.fitNoCrop(bestWidthHeightPreivew, windowSize);
		if (switchOrientation)
			fitWindowWidthHeight = fitWindowWidthHeight.switchDimensions();

		// change height, but only if need be.
		if (surfaceView.getWidth() != fitWindowWidthHeight.width ||
				surfaceView.getHeight() != fitWindowWidthHeight.height){
			LayoutParams surfaceParams = surfaceView.getLayoutParams();
			surfaceParams.height = fitWindowWidthHeight.height;
			surfaceParams.width = fitWindowWidthHeight.width;
			surfaceView.setLayoutParams(surfaceParams);
		}	
	}

	/**
	 * Start the camera preview. If it is currently running, then it will stop it and restart.
	 * If mCamera == null, then nothing will happen. This happens on a background thread.
	 */
	public synchronized void startPreview(){
		//TODO: this used to be a synchronized method, see if iw need it
		if (isPreviewStarting)
			return;
		if (mCamera == null){
			isPreviewRunning = false;
			return;
		}
		isPreviewStarting = true;
		if (isPreviewRunning)
			mCamera.stopPreview();

		//TODO: see if we want a runnable, seems to leak and also may cause screen freeze
		//	new Thread(new Runnable() {
		//	public void run() {
		mCamera.startPreview();
		isPreviewRunning = true;
		isPreviewStarting = false;
		//	}
		//	}).start();
	}

	/**
	 * Stop the camera preview. If null camera or already stopped, nothing happens.
	 */
	public synchronized void stopPreview(){
		if (mCamera == null){
			isPreviewRunning = false;
			return;
		}
		if (isPreviewRunning)
			mCamera.stopPreview();
		isPreviewRunning = false;

	}

	/**
	 * Manually set if preview is currently running. <br>
	 * Normally use stopPreview or startPreview, but you can use this call if some other method has started or stopped the preivew and we
	 * want to keep track of this. For example, if you called TakePicture.
	 * @param running
	 */
	public synchronized void setIsPreviewRunning(boolean running){
		isPreviewRunning = running;
	}

	/**
	 * Is the preview currently running
	 * @return
	 */
	public boolean isPreviewRunning(){
		return isPreviewRunning;
	}

	public static abstract class OnRotationCallback{	
		/**
		 * Called when the orientation changes. Angles are 0, 90, 180, 270
		 * @param orientation The new orientation angle
		 * @param lastOrientation The old orientation angle
		 */
		public abstract void onRotation(int orientation, int lastOrientation);
	}
}
