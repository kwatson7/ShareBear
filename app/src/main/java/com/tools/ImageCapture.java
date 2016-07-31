package com.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;

public class ImageCapture {

	// constants
	private static final String CAMERA_INTENT_DATA_KEY = "data";
	private static final String BITMAP_KEY = "ImageCapture.BITMAP_KEY";
	private static final String FILE_PATH_KEY = "ImageCapture.FILE_PATH_KEY";
	private static final String FILE_URI_KEY = "ImageCapture.FILE_URI_KEY";
	private static final String KEY_TO_DELETE_PICTURE = "KEY_TO_DELETE_PICTURE";

	// member variables
	private String filePath = null;

	public ImageCapture() {
		
	}

	/**
	 * Create the intent required to start the camera.
	 * @return
	 */
	public Intent createImageCaptureIntent(Context ctx){

		filePath = null;

		// keep track if this will be a big image or not
		boolean canWeMakeBigImage = true;
		String storageState = Environment.getExternalStorageState();

		// we can only do a large image if we have media access
		File photoFile = null;
		if(storageState.equals(Environment.MEDIA_MOUNTED)) {

			// determine a path 
			String path = Environment.getExternalStorageDirectory().getName()
			+ File.separatorChar + "Android/data/" + ctx.getPackageName() + "/files/" + com.tools.Tools.randomString(25) + ".jpg";
			photoFile = new File(path);
			filePath = path;
			Log.i(ctx.getPackageName(), path);

			// create the needed file
			try {
				if(photoFile.exists() == false) {
					photoFile.getParentFile().mkdirs();
					photoFile.createNewFile();
				}

			} catch (IOException e) {
				Log.w(ctx.getPackageName(), "Could not create file.", e);
				canWeMakeBigImage = false;
			}
		}else
			canWeMakeBigImage = false;

		Log.i(ctx.getPackageName(), "Image used is big = " + canWeMakeBigImage);

		// now make the intent
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (canWeMakeBigImage){
			Uri fileUri = Uri.fromFile(photoFile);
			intent.putExtra( MediaStore.EXTRA_OUTPUT, fileUri);	
		}

		return intent;
	}
	
	/**
	 * Create an intent that when launched will ask the user to select an image
	 * @return
	 * @see get
	 */
	public static Intent createImageSelectionIntent(){
		Intent i = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		return i;
	}
	
	/**
	 * Create an intent to start an activity with the given data. <br>
	 * This data should have been returned from an activity that was launched with createImageSelectionIntent <br>
	 * This should be called within onActivityResult
	 * @param ctx The calling context
	 * @param newClassToStart the new activity to start
	 * @param resultCode the resultCode of the returning intent
	 * @param data The returned data
	 * @return the intent to tart the activity. If there is no data or not RESULT_OK, null is returned
	 */
	public static Intent createIntentWithCorrectExtrasAfterSelection(Context ctx, Class<?> newClassToStart, int resultCode, Intent data){
		
		if (resultCode == Activity.RESULT_OK && data != null){
			// create the new intent
			Intent intent = new Intent(ctx, newClassToStart);

			// now add the path to the file
			Uri selectedImage = data.getData();
			if (selectedImage != null)
				intent.putExtra(FILE_URI_KEY, selectedImage);

			return intent;
		}else
			return null;
	}
	
	/**
	 * Create an intent to laucnh a new activity with the correct data required to read the image <br>
	 * This should be called from within onActivityResult() after createImageCaptureIntent()
	 * @param ctx The calling context
	 * @param newClassToStart the new class to start
	 * @param resultCode The result code from the camera return
	 * @param data The data returned from camera
	 * @return the new intent. null if there is no data returned from camera
	 */
	public Intent createIntentWithCorrectExtrasAfterCapture(Context ctx, Class<?> newClassToStart, int resultCode, Intent data){

		if (resultCode == Activity.RESULT_OK){
			// create the new intent
			Intent intent = new Intent(ctx, newClassToStart);
			intent.putExtra(KEY_TO_DELETE_PICTURE, true);

			// add the bitmap data if present
			if (data != null){
				Object object = data.getExtras().get(CAMERA_INTENT_DATA_KEY); 
				if (object != null){
					Bitmap photo = (Bitmap) object;
					intent.putExtra(BITMAP_KEY, photo);
				}
			}

			// now add the path to the file
			String path = getFilePath();
			if (path != null && path.length() > 0)
				intent.putExtra(FILE_PATH_KEY, path);

			return intent;
		}else
			return null;
	}
	
	/**
	 * Create an intent to pass picture data to a new activity, just use this ImageCapture.getBitmap in new activity
	 * @param ctx The context that will launch the intent
	 * @param newClassToStart the new activity to start
	 * @param bitmap any bitmap data usuall only for small files
	 * @param filePath the filepath where the image is stored bigger files
	 * @param toDelete If the picture file (if present) should be deleted when finished being read in getBitmap
	 * @return the intent to lauch the activity with knowledge of bitmaps
	 */
	public static Intent createIntentToPassPhoto(Context ctx, Class<?> newClassToStart, Bitmap bitmap, String filePath, boolean toDelete){

		// create the new intent
		Intent intent = new Intent(ctx, newClassToStart);

		// add the bitmap data if present
		if (bitmap != null)
			intent.putExtra(BITMAP_KEY, bitmap);


		// now add the path to the file
		if (filePath != null && filePath.length() > 0 && (new File(filePath)).exists())
			intent.putExtra(FILE_PATH_KEY, filePath);
		
		intent.putExtra(KEY_TO_DELETE_PICTURE, toDelete);

		return intent;
	}
	
	/**
	 * Get the bitmap from the Bundle scaled down to fit the screen
	 * @param act the activity this is called within, so we can set the maximum picture size to be within window
	 * @param extras the extras containing the data
	 * @return the bitmap
	 */
	public static Bitmap getBitmap(Activity act, Bundle extras){
		// no extras
		if (extras == null)
			return null;
		
		// the max picture size
		Display display = act.getWindowManager().getDefaultDisplay();
		int maxPixelSize = Math.max(display.getWidth(), display.getHeight()); 
		String path = null;
		
		// should we delte picture
		boolean deleteFile = extras.getBoolean(KEY_TO_DELETE_PICTURE,false);
		
		// try to read the big file first
		path = extras.getString(FILE_PATH_KEY);
		
		// try from uri also if we need to
		if (path == null || path.length() == 0){
			// now try to read from URI
			Uri uri = extras.getParcelable(FILE_URI_KEY);
			if (uri != null){
				String[] filePathColumn = {MediaStore.Images.Media.DATA};
	        
				Cursor cursor = act.getContentResolver().query(uri, filePathColumn, null, null, null);
				if (cursor != null && cursor.moveToFirst()){
					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			        path = cursor.getString(columnIndex);
			        cursor.close();
				}else if (cursor != null)
					cursor.close();	
			}
		}
		
		// now actually read the bitmap from the path
		if (path != null && path.length() > 0){
			File file = new File(path);
			if (file.exists()){
				Bitmap bmp = com.tools.ImageProcessing.makeThumbnail(path, maxPixelSize, true);
				// delete the file
				if (deleteFile)
					file.delete();
				
				// we have data, so return it
				if (bmp != null)
					return bmp;	
			}
		}
		
		// if we made it here, then there was no big file, so send the small file
		Object object = extras.get(BITMAP_KEY); 
		if (object == null){
			return null;
		}
		return (Bitmap) object;	
	}
	

	/**
	 * Return the path where the picture is stored
	 * @return
	 */
	private String getFilePath(){
		return filePath;
	}

	
	
	/**
	 * Determine if the current device is one of the phones with the image capture bug
	 * that doesn't allow for full size images from camera intent
	 * @return
	 */
	private static boolean hasImageCaptureBug() {

		// list of known devices that have the bug
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");

		// this device
		String currentDevice = android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
		+ android.os.Build.DEVICE;

		// is this device one of the bad ones
		return devices.contains(currentDevice);
	}
}
