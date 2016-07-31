package com.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.view.OrientationEventListener;
import android.widget.TextView;

/**
 * @deprecated Use CameraHelper instead
 * @author Kyle
 *
 */
@Deprecated
public class RotateCameraSurface {

	// orientation variables
	private OrientationEventListener mOrientationEventListener;
	private int mOrientation =  -1;
	private static final int ORIENTATION_PORTRAIT_NORMAL =  1;
	private static final int ORIENTATION_PORTRAIT_INVERTED =  2;
	private static final int ORIENTATION_LANDSCAPE_NORMAL =  3;
	private static final int ORIENTATION_LANDSCAPE_INVERTED =  4;

	private Camera mCamera = null;
	private Boolean mChangeParameters;
	private OnRotationCallback callback;
	
	TextView view;

	/** Class to keep camera surface from rotating and also to keep track of 
	 * orientation of camera so the picture is stored correctly.
	 * <p>
	 * You must call:<p>
	 *      (1)this class's onResume in the calling activity's onResume<br>
	 *      (2)this class's onPause in the calling activity's onPause<br>
	 *      (3)this class's onCreate in the calling activity's onCreate<br>
	 *      (4)this class's updateCam when the calling activity's camera is updated<br>
	 * @param cam, pass null if not activated yet
	 * @param changeParameters, Boolean to change the parameters of the rotation in
	 * @param callback to happen when surface is rotated. Null if none desired
	 * the camera settings. This should be true if you want the camera drivers to rotate the 
	 * image for you, or use getRotation() manually later to do it yourself
	 * @deprecated Use CameraHelper instead
	 */
	public RotateCameraSurface(Activity activity, Camera cam, Boolean changeParameters, OnRotationCallback callback){
		mCamera = cam;
		this.mChangeParameters = changeParameters;
		this.callback = callback;

	}

	/** call this in the calling activity's onResume. MUST be done */
	public void onResume(Context ctx) {

		// prepare listener for orientation changes
		if (mOrientationEventListener == null) {            
			mOrientationEventListener = new OrientationEventListener(ctx, SensorManager.SENSOR_DELAY_NORMAL) {

				@Override
				public void onOrientationChanged(int orientation) {

					// determine our orientation based on sensor response
					int lastOrientation = mOrientation;

					if (orientation >= 315 || orientation < 45) {
						if (mOrientation != ORIENTATION_PORTRAIT_NORMAL) {                          
							mOrientation = ORIENTATION_PORTRAIT_NORMAL;
						}
					}
					else if (orientation < 315 && orientation >= 225) {
						if (mOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
							mOrientation = ORIENTATION_LANDSCAPE_NORMAL;
						}                       
					}
					else if (orientation < 225 && orientation >= 135) {
						if (mOrientation != ORIENTATION_PORTRAIT_INVERTED) {
							mOrientation = ORIENTATION_PORTRAIT_INVERTED;
						}                       
					}
					else { // orientation <135 && orientation > 45
						if (mOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
							mOrientation = ORIENTATION_LANDSCAPE_INVERTED;
						}                       
					}   

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

	/** call this in the calling activity's onPause. MUST be done */
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
	private void changeRotation(int orientation, int lastOrientation) {
		
		// main switching of camera
		if (!(mChangeParameters == null || !mChangeParameters || mCamera == null)){
			Camera.Parameters parameters = mCamera.getParameters();
			switch (orientation) {
			case ORIENTATION_PORTRAIT_NORMAL:
				parameters.setRotation(90);
				break;
			case ORIENTATION_LANDSCAPE_NORMAL:
				parameters.setRotation(0);
				break;
			case ORIENTATION_PORTRAIT_INVERTED:
				parameters.setRotation(270);
				break;
			case ORIENTATION_LANDSCAPE_INVERTED:
				parameters.setRotation(180);
				break;
			}

			mCamera.setParameters(parameters);	
		}
		
		// post callback
		if (callback != null)
			callback.onRotation(getRotation(orientation), getRotation(lastOrientation));
	}

	/** call this in the calling activity's onCreate. MUST be done */
	public void onCreate(Activity act){

		// force portrait layout
		//mAct.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR | ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	/** call this function to get the camera rotation, usually at the time of taking the picture
	 * Return value will be 0, 90, 180, or 270 */
	public int getRotation(){
		int rotation = 0;
		switch (mOrientation) {
		case ORIENTATION_PORTRAIT_NORMAL:
			rotation = 90;
			break;
		case ORIENTATION_LANDSCAPE_NORMAL:
			rotation = 0;
			break;
		case ORIENTATION_PORTRAIT_INVERTED:
			rotation = 270;
			break;
		case ORIENTATION_LANDSCAPE_INVERTED:
			rotation = 180;
			break;
		}
		
		return rotation;
	}
	
	/** call this function to get the camera rotation, usually at the time of taking the picture
	 * Return value will be 0, 90, 180, or 270 */
	private int getRotation(int orientation){
		int rotation = 0;
		switch (orientation) {
		case ORIENTATION_PORTRAIT_NORMAL:
			rotation = 90;
			break;
		case ORIENTATION_LANDSCAPE_NORMAL:
			rotation = 0;
			break;
		case ORIENTATION_PORTRAIT_INVERTED:
			rotation = 270;
			break;
		case ORIENTATION_LANDSCAPE_INVERTED:
			rotation = 180;
			break;
		}
		
		return rotation;
	}

	/** call this in the calling activity, when camera is updated. MUST be done */
	public void updateCam(Camera newCam){
		mCamera = newCam;
	}
	
	/** Determine the optimal width and height, based on max size and optimal choice */
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
	 * Get the preview size the fits best into the given width of height
	 * @param width Width of are where preview can go in pixels
	 * @param height Height of where preview can go in pixels
	 * @param parameters camera parameters that store the possible preview sizes
	 * @return the best preview size
	 */
	public Camera.Size getBestPreviewSize(
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
	
	public static abstract class OnRotationCallback{	
		public abstract void onRotation(int orientation, int lastOrientation);
	}
}
