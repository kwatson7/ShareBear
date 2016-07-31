package com.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.hardware.SensorManager;
import android.location.Location;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;


/**
 * Direct copy of MyLocationOverlay with added features such as: 
 * 1. an arrow marker instead of of circle
 * @author Kyle
 *
 */
public class CustomLocationOverlay
extends MyLocationOverlay{

	// default parameters
	private final static int 	PADDING_ACTIVE_ZOOM    	= 50; 				// when considered at edge
	private static final float 	DEFAULT_RESIZE_RATIO 	= (float) .07; 		// amount to resize drawable
	private static final float 	DEFAULT_SCREEN_SIZE 	= (float) 3.88; 	// default screen size linked to RESIZE_RATIO
	private static final float 	MAX_SCALE_INCREASE 		= 3; 				// only scales up for screen size by this amount
	private static final float 	MIN_SCALE_INCREASE 		= (float) 0.5; 		// only scales down for screen size by this amount
	private static int[] 		DEFAULT_RGB 			= {100, 100, 255}; 	// default color of accuracy circle
	private static double 		MIN_ANGLE_CHANGE_UPDATE = 5; 				// minimum angle change required before updating orientation

	// class variables
	private boolean 			mIsShowCompass 			= false; 			// should we show the compass
	private boolean 			mIsUseDefaultBitmap 	= true; 			// should we use default location bitmap
	private float 				mActualResizeRatio		= DEFAULT_RESIZE_RATIO;// The actual resize ratio to use, allow user to change
	private MapController    	mMapController; 							// The controller for the map									
	private Bitmap           	mMarker; 									// The bitmap to show for user location
	private Point            	mCurrentPoint        	= new Point(); 		// The current center of map
	private GestureDetector  	mGestureDetector; 							// a detector for finding double taps
	private SensorEventListener mSensorListener;							// listener for sensor callbacks
	private SensorManager 		mSensorManager; 							// sensor manager for handling sensor callbacks
	private boolean 			mIsSensorRegistered		= false; 			// boolean to track if listeners are registered or not	

	// pitch roll and yaw
	private float	 			mAzimuth; 									// keep track of last azimuth angle in degrees
	private float 				mPitch; 									// keep track of last pitch angle in degrees
	private float 				mRoll;										// keep track of last roll angle in degrees

	public enum LOCATION_CENTERING { 										// enum for various types of centering
		FREE_FLOAT, STAY_ON_SCREEN, STAY_IN_CENTER
	}

	private LOCATION_CENTERING mLocationCentering		= LOCATION_CENTERING.FREE_FLOAT; // free floating map centering

	private int              	mHeight; 									// The height of map
	private int              	mWidth; 									// The width of map

	protected final Paint 		mCirclePaint 			= new Paint();		// the object for paingint the circle around the user position.

	/**
	 * Show the current location with an arrow pointing towards your orientation. Overrides MyLocationOverlay
	 * except with this feature and various others. User must input a drawable to be used as the arrow. 
	 * Default north position is the arrow pointing up, so make sure the drawable points up. Also make sure 
	 * the drawable is square otherwise when rotating, it may be clipped.
	 * registerListenersOnResume must be called in the calling activities onResume, in order to register the listeners
	 * unRegisterListenersOnStop must be callsed eitehr in onStop or onPause to unregister teh listeners.
	 *
	 * @param context
	 * @param mapView
	 * @param drawable for the arrow to be used to point to orientation. Default position is straight up (north).
	 * Enter -1 to use default.
	 */
	public CustomLocationOverlay(Context context,
			MapView mapView,
			int drawableArrow) {

		// the super method
		super(context, mapView);

		// grab the controller
		this.mMapController = mapView.getController();

		// grab the marker to use, or use default if none input
		if (drawableArrow == -1){
			mIsUseDefaultBitmap = true;
		}else{
			this.mMarker = BitmapFactory.decodeResource(context.getResources(),
					drawableArrow);
			mIsUseDefaultBitmap = false;
		}

		// enable compass
		enableCompass();

		// sets color for circle
		this.mCirclePaint.setARGB(0, DEFAULT_RGB[0], DEFAULT_RGB[1], DEFAULT_RGB[2]);
		this.mCirclePaint.setAntiAlias(true);

		// initialize the sensor listeners
		createSensorListeners(context);

		// Gesture detection
		mGestureDetector = new GestureDetector(new SimpleOnGestureListener(){

			// used for double tap to zoom in
			@Override
			public boolean onDoubleTap(MotionEvent event){
				mMapController.zoomInFixing((int)event.getX(), (int)event.getY());
				return false;

			}
		});    	                    
	}

	/**
	 * Create the listeners for the orientation sensors
	 * @param context
	 */
	private void createSensorListeners(Context context){

		// check null first and then create sensor manager
		if (mSensorManager == null)
			mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		// now create the callback listener
		if (mSensorListener == null){
			mSensorListener = new SensorEventListener() {

				public void onSensorChanged(SensorEvent se) {
					// handle orientation changes
					if (se.sensor.getType() == Sensor.TYPE_ORIENTATION){

						// grab pitch, roll and yaw
						float azimuthNew = se.values[0];
						float pitchNew = se.values[1]; 
						float rollNew = se.values[2];

						// calculate change in angles	
						double change = 
							Math.sqrt((azimuthNew - mAzimuth)*(azimuthNew - mAzimuth) + 
									(pitchNew - mPitch)*(pitchNew - mPitch) + 
									(rollNew - mRoll)*(rollNew - mRoll));

						// update angles
						mAzimuth = azimuthNew;
						mRoll = rollNew;
						mPitch = pitchNew;

						// only update graph if we have changed by a minimum threshold
						if (change > MIN_ANGLE_CHANGE_UPDATE)
							onLocationChanged(getLastFix());
					}
				}

				public void onAccuracyChanged(Sensor sensor, int accuracy) {
				}
			};
		}
	}

	/**
	 * This command must be used in the calling activity in its onResume call, to register listeners.
	 */
	public void registerListenersOnResume(Context context){

		// try to make the listeners non null
		createSensorListeners(context);

		// cannot register if null
		if (mSensorManager != null && mSensorListener != null){

			// only register if not already done
			if (mIsSensorRegistered == false){
				mSensorManager.registerListener( mSensorListener, 
						mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), 
						SensorManager.SENSOR_DELAY_NORMAL);
				mIsSensorRegistered = true;
			}

			// since one or more pointers were null, then the sensors were not registered.	
		}else
			mIsSensorRegistered = false;

	}

	/**
	 * This command must be used in the calling activity in its onStop or onPuase call, to unregister listeners.
	 * Then call registerListenersOnResume at the onResume to register them again.
	 */
	public void unRegisterListenersOnStop(){
		// cannot unregister if null
		if (mSensorManager != null && mSensorListener != null)
			mSensorManager.unregisterListener(mSensorListener);

		// they've been unregistered
		mIsSensorRegistered = false;
	}

	@Override
	public boolean draw(android.graphics.Canvas canvas,
            MapView mapView,
            boolean shadow,
            long when){
		
		// the standard draw
		boolean out = super.draw(canvas, mapView, shadow, when);
		if (!shadow){
			drawOtherViews(canvas, mapView, when);
		}
		return out;
	}
	
	/**
	 * Draw any other views. Base class is empty, subclass and override to implement functionality
	 * @param canvas The canvas to draw on
	 * @param mapView The mapview that requested the draw
	 * @param when The timestamp of the draw
	 */
	protected void drawOtherViews(android.graphics.Canvas canvas,
            MapView mapView,
            long when){
		
	}
	
	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) {

		// save current location to pixel points
		mapView.getProjection().toPixels(myLocation, mCurrentPoint);

		// if compass is off, then just use default
		if(!isCompassEnabled() || mIsUseDefaultBitmap){
			super.drawMyLocation(canvas, mapView, lastFix, myLocation, when);
			return;
		}

		// set the height and width of this class
		if (this.mHeight == 0) {
			this.mHeight = mapView.getHeight();
			this.mWidth = mapView.getWidth();
		}

		// grab the custom bitmap
		Bitmap rotatedMarker = getRotatedMarker(canvas);  

		// null marker, then use default
		if (rotatedMarker == null){
			super.drawMyLocation(canvas, mapView, lastFix, myLocation, when);
			return;
		}

		// paint the circle
		this.mCirclePaint.setAlpha(50);
		this.mCirclePaint.setStyle(Style.FILL);
		final float radius = mapView.getProjection().metersToEquatorPixels(getLastFix().getAccuracy());
		canvas.drawCircle(mCurrentPoint.x, mCurrentPoint.y, radius, this.mCirclePaint);
		this.mCirclePaint.setAlpha(150);
		this.mCirclePaint.setStyle(Style.STROKE);
		canvas.drawCircle(mCurrentPoint.x, mCurrentPoint.y, radius, this.mCirclePaint);

		// paint the arrow
		canvas.drawBitmap(rotatedMarker, 
				mCurrentPoint.x - (float)getScaleRatio(canvas)*rotatedMarker.getWidth()/2, 
				mCurrentPoint.y - (float)getScaleRatio(canvas)*rotatedMarker.getHeight()/2, 
				null);
	}

	@Override
	public synchronized void onLocationChanged(Location location) {
		// no location return
		if (location == null)
			return;

		super.onLocationChanged(location);

		// move to current location always
		if (mLocationCentering == LOCATION_CENTERING.STAY_IN_CENTER)
			mMapController.animateTo(getMyLocation());

		// only move to new position if enabled and we are in an border-area
		else if (mMapController != null
				&& mLocationCentering == LOCATION_CENTERING.STAY_ON_SCREEN
				&& !inZoomActiveArea(mCurrentPoint))
			mMapController.animateTo(getMyLocation());

		// free float do nothing
	}

	/**
	 * Determine if we are currently in the active area of the map
	 * @param currentPoint
	 * @return
	 */
	private boolean inZoomActiveArea(Point currentPoint) {
		if ((currentPoint.x > PADDING_ACTIVE_ZOOM && currentPoint.x < mWidth - PADDING_ACTIVE_ZOOM)
				&& (currentPoint.y > PADDING_ACTIVE_ZOOM && currentPoint.y < mHeight - PADDING_ACTIVE_ZOOM)) {
			return true;
		}
		return false;
	}

	/**
	 * Set how the map will recenter when the user location changes
	 * @param centerOnCurrentLocation
	 */
	public void setLocationCentering(LOCATION_CENTERING locationCentering) {

		// save the input setting
		this.mLocationCentering = locationCentering;

		// simulate location changing so we can update
		onLocationChanged(this.getLastFix());
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView){
		this.mLocationCentering = LOCATION_CENTERING.FREE_FLOAT;
		//return false;
		if (mGestureDetector.onTouchEvent(e))
			return true;
		else
			return false;
	}

	/**
	 * Get the bitmap of the rotated arrow according to orientation
	 * @param inputCanvas
	 * @return
	 */
	private Bitmap getRotatedMarker(Canvas inputCanvas){
		float angle = getOrientation();
		if (angle == Float.NaN)
			return null;

		// Create blank bitmap of equal size
		Bitmap rotatedMarker = mMarker.copy(Bitmap.Config.ARGB_8888, true);
		rotatedMarker.eraseColor(0x00000000);

		// Create canvas
		Canvas canvas = new Canvas(rotatedMarker);

		// Create rotation matrix
		Matrix rotateMatrix = new Matrix();
		rotateMatrix.setRotate(angle, canvas.getWidth()/2, canvas.getHeight()/2);
		rotateMatrix.postScale((float)getScaleRatio(inputCanvas), (float)getScaleRatio(inputCanvas));

		// Draw bitmap onto canvas using matrix
		canvas.drawBitmap(mMarker, rotateMatrix, null);

		return rotatedMarker;

	}

	@Override
	protected void drawCompass(android.graphics.Canvas canvas,
			float bearing){
		if (mIsShowCompass)
			super.drawCompass(canvas, bearing);
		else
			return;
	}

	/**
	 * defaults to being 7.0% of s screen that is 3.33x2, and scales up or down accordingly
	 * However maxes out at 3x the size and 0.5 the size
	 * @return
	 */
	private float getScaleRatio(Canvas canvas){

		// The properties of canvas
		int density = canvas.getDensity();
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// the diagonal length of canvas in pixels and physical size
		float diagonal = (float) Math.sqrt((float)(width*width + height*height));
		float physicalSize = diagonal/density;

		// resize scale ratio accordingly
		float scale = physicalSize/DEFAULT_SCREEN_SIZE;
		if (scale > MAX_SCALE_INCREASE)
			scale = MAX_SCALE_INCREASE;
		if (scale < MIN_SCALE_INCREASE)
			scale = MIN_SCALE_INCREASE;

		// The result
		return scale*mActualResizeRatio;
	}

	/**
	 * Decide whether to actually show the compass or not. This is different from enableCompass(), 
	 * where enableCompass turns on or off the actual compass sensor in the phone, this simply
	 * decides whether to show the compass or not. Will only show if enableCompass is on.
	 * @param isShow
	 */
	public void setShowCompass(boolean isShow){
		mIsShowCompass = isShow;
	}

	/**
	 * Accuracy circle defaults to rgb = (100, 100, 255). Change the color here if you want.
	 * Values range from 0-255
	 * @param blue
	 * @param green
	 * @param red
	 */
	public void setAccuracyCircleColor(int red, int green, int blue){
		this.mCirclePaint.setARGB(0, red, green, blue);
	}

	/**
	 * Sets the resize ratio for the bitmap. Defaults to 0.07 which looks good for a 400x400 bitmap.
	 * @param ratio
	 */
	public void setResizeRatio(float ratio){
		mActualResizeRatio = ratio;
	}
}
