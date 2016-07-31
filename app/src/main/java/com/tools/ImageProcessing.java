package com.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.widget.Toast;

public class ImageProcessing {

	// constants
	private static String IMAGE_TYPE = "image/jpeg";
	private static String LOG_TAG = "com.tools";

	/**
	 * Remove the edges of bitmap by extracting the center region that do not match the given nullColor
	 * @param bitmap the source bitmap
	 * @param nullColor the color that is considered void and we will chopped. For example, for black simply enter: Color.argb(0, 0, 0, 0); 
	 * @return The bitmap with the center extracted, or null if null was entered
	 */
	public static Bitmap bitmapExtractCenter(Bitmap bitmap, int nullColor){

		// null
		if (bitmap == null)
			return null;

		// measure size
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();

		// grab pixel data
		int[] pixels = new int[width*height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		// which row will we start and end and cols as well
		int rowStart = 0;
		int rowEnd = height-1;
		int colStart = 0;
		int colEnd = width-1;

		// find the top that we can crop
		boolean outerBreak = false;
		for (int i = 0; i <height; i++){
			int ii = i*width;
			for (int j = 0; j < width; j++){				
				if (pixels[ii + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			rowStart++;
		}

		// find the bottom that we can crop
		outerBreak = false;
		for (int i = height-1; i >= 0; i--){
			int ii = i*width;
			for (int j = 0; j < width; j++){
				if (pixels[ii + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			rowEnd--;
		}

		// find the left we can crop
		outerBreak = false;
		for (int j = 0; j < width; j++){
			for (int i = 0; i <height; i++){	
				if (pixels[i*width + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			colStart++;
		}

		// find the right that we can crop
		outerBreak = false;
		for (int j = width-1; j >= 0; j--){
			for (int i = 0; i <height; i++){	
				if (pixels[i*width + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			colEnd--;
		}

		// sub index of matrix
		int newWidth = colEnd-colStart+1;
		int newHeight = rowEnd-rowStart+1;
		if (newWidth <= 0 || newHeight <= 0)
			return null;
		return Bitmap.createBitmap(bitmap, colStart, rowStart, newWidth, newHeight);	
	}

	/** Take input of original size image that must fit within fitSize 
	 * and output new size that preserves aspect ratio and fill entire area by cropping.
	 * @param originalSize WidthHeight object of original size object
	 * @param fitSize WidthHeight object that the new object must fit into
	 * @return a WidthHeight object that is the new size
	 * @See fitNoCrop
	 */
	public static WidthHeight fitCrop(WidthHeight originalSize, WidthHeight fitSize){

		// initialize result
		WidthHeight result = null;

		// height hits edge first
		if (originalSize.getAspectRatio() < fitSize.getAspectRatio()){

			result = new WidthHeight(fitSize.width, Math.round(fitSize.width/originalSize.getAspectRatio()));

			// height hits edge first	
		}else{
			result = new WidthHeight(Math.round(fitSize.height*originalSize.getAspectRatio()), fitSize.height);
		}

		return result;
	}

	/** Take input of original Size image that must fit within fitSize 
	 * and output new size that preserves aspect ratio with no cropping.*
	 * @param originalSize WidthHeight object of original size object
	 * @param fitSize WidthHeight object that the new object must fit into
	 * @return a WidthHeight object that is the new size
	 * @See fitCrop
	 */
	public static WidthHeight fitNoCrop(WidthHeight originalSize, WidthHeight fitSize){

		// initialize result
		WidthHeight result = null;

		// width hits edge first
		if (originalSize.getAspectRatio() >= fitSize.getAspectRatio()){

			result = new WidthHeight(fitSize.width, Math.round(fitSize.width/originalSize.getAspectRatio()));

			// height hits edge first	
		}else{
			result = new WidthHeight(Math.round(fitSize.height*originalSize.getAspectRatio()), fitSize.height);
		}

		return result;
	}

	/**
	 * Get byte array data from a bitmap as JPEG
	 * @param bmp The bitmap to compress to byte array
	 * @param imageQuality 0-100 quality setting (90 is usually a good compromise of size and quality)
	 * @return The byte array data. null if we could not convert or null input
	 */
	public static byte[] getByteArray(Bitmap bmp, int imageQuality){
		if (bmp == null)
			return null;

		ByteArrayOutputStream out = new ByteArrayOutputStream(bmp.getWidth()*bmp.getHeight());
		bmp.compress(Bitmap.CompressFormat.JPEG, imageQuality, out);   
		return out.toByteArray();
	}

	/**
	 * Get the orientation angle from exif data. This is the angle clockwise, the raw image must be rotated in order to look correct.
	 * @param exif The exif to extract info from, if null, 0 will be returned
	 * @return The angle
	 */
	public static float getExifOrientationAngle(ExifInterface exif){

		// null exif, return 0 angle
		if (exif==null){
			Log.e(LOG_TAG, "null exif entered into getExifOrientationAngle");
			return 0;
		}

		// read orientation
		int orientation = exif.getAttributeInt(
				ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_NORMAL);

		// these are the angle corresponding to these orientations
		float angle = 0;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
			angle = 90;
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
			angle = 180;
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
			angle = 270;

		return angle;
	}
	
	/**
	 * Helper class for rotating exif in the background
	 * @author Kyle
	 *
	 * @param <ACTIVITY_TYPE>
	 */
	private static class RotateExifInBackgroundClass <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, IOException>{

		private String filename;
		private int direction;
		
		/**
		 * Rotate exif data in the background
		 * @param act Calling activity
		 * @param filename the filename to rotate
		 * @param direction The direction to rotate positive = clockwise, negative = ccw
		 * @param callback Callback to call when finished, can be null
		 */
		public RotateExifInBackgroundClass(
				ACTIVITY_TYPE act,
				String filename,
				int direction,
				CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, IOException> callback) {
			super(act, -1, true, false,
					null);
			this.filename = filename;
			this.direction = direction;
			
			this.setFinishedCallback(callback);
		}
		
		@Override
		protected void onPreExecute() {
		}
		
		@Override
		protected IOException doInBackground(Void... params) {
			IOException e2 = null;
			try {
				rotateExif(filename, direction);
			} catch (IOException e) {
				e2 = e;
			}
			return e2;
		}
		@Override
		protected void onProgressUpdate(Void... progress) {
			
		}
		@Override
		protected void onPostExectueOverride(IOException result) {
			
		}
		@Override
		protected void setupDialog() {
			
		}	
	}

	/**
	 * Read the exif orientation angle from a given file
	 * @param file The file. 
	 * @return The orientation angle stored in the file. The angle we need to rotate cw in order to have a correct image.
	 * @throws IOException if we cannot get an exifInterface from the file
	 */
	public static float getExifOrientationAngle(String file)
	throws IOException{

		// read exif data
		ExifInterface exif = new ExifInterface(file);

		// grab the angle from exif data
		return getExifOrientationAngle(exif);
	}
	
	/**
	 * Create a bitmap thumbnail from the input image byte array. <br>
	 * Will take into account the exifOrientation, but can only handle rotations, not transposing or inversions. <br>
	 * This can only rescale by integer amounts. For example if original image is 128x128, and you input
	 * maxThumbnailDimension as 100, we can only rescale by a factor of 2, so the image will be 64x64. <p>
	 * *** Also If you input too large of a maxThumbnailDimension, you may crash due to memory overflow ***
	 * However this is memory intelligent, meaning it doesn't load the whole bitmap into memory and then resize,
	 * but only sub-samples the image. This is why we can only scale by integer amounts. 
	 * @param imageData The image data to resize
	 * @param exifOrientation the exifOrientation tag. If unknown tag, no rotation is assumed. @See ExifOrientation
	 * @param maxThumbnailDimension The maximum thumbnail dimension in either height or width
	 * @param forceBase2 If we force to downsample by base2, it is faster, but then we can only
	 * resize by a factor of 2,4,8,16...
	 * @return The resized and rotated thumbnail. So the new orientation tag is ExifInterface.ORIENTATION_NORMAL. Null if unsuccessful
	 */
	public static Bitmap makeThumbnail(
			final byte[] imageData,
			int exifOrientation,
			final int maxThumbnailDimension,
			boolean forceBase2){

		if (imageData == null || imageData.length == 0)
			return null;
		
		// setup imageData
		BitmapDecodable<byte[]> decodable = new BitmapDecodable<byte[]>() {

			@Override
			Bitmap onDecode(Options options) {
				return BitmapFactory.decodeByteArray(data, 0, data.length, options);
			}
		};
		decodable.setData(imageData);
		return makeThumbnailHelper(decodable, exifOrientation, maxThumbnailDimension, forceBase2);
	}

	/**
	 * Create a bitmap thumbnail from the input full image file. <br>
	 * Will take into account the exifOrientation, but can only handle rotations, not transposing or inversions. <br>
	 * This can only rescale by integer amounts. For example if original image is 128x128, and you input
	 * maxThumbnailDimension as 100, we can only rescale by a factor of 2, so the image will be 64x64. <p>
	 * *** Also If you input too large of a maxThumbnailDimension, you may crash due to memory overflow ***
	 * However this is memory intelligent, meaning it doesn't load the whole bitmap into memory and then resize,
	 * but only sub-samples the image. This is why we can only scale by integer amounts. 
	 * @param fullImagePath The path to the full image file, we will resize
	 * @param maxThumbnailDimension The maximum thumbnail dimension in either height or width
	 * @param forceBase2 If we force to downsample by base2, it is faster, but then we can only
	 * resize by a factor of 2,4,8,16...
	 * @return The resized and rotated thumbnail. So the new orientation tag is ExifInterface.ORIENTATION_NORMAL Null if unsuccessful
	 * @throws IOException 
	 */
	public static Bitmap makeThumbnail(
			final String fullImagePath,
			final int maxThumbnailDimension,
			boolean forceBase2){

		if (fullImagePath == null || fullImagePath.length() == 0)
			return null;
		
		// setup imageData
		BitmapDecodable<String> decodable = new BitmapDecodable<String>() {

			@Override
			Bitmap onDecode(Options options) {
				return BitmapFactory.decodeFile(data, options);
			}
		};
		decodable.setData(fullImagePath);
		
		// read orientation
		ExifInterface exif;
		try {
			exif = new ExifInterface(fullImagePath);
		} catch (IOException e) {
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
		int orientation = exif.getAttributeInt(
				ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_NORMAL);
		
		// make the thumbnail
		return makeThumbnailHelper(decodable, orientation, maxThumbnailDimension, forceBase2);
	}

	/** 
	 * Resize a byte array keeping aspect ratio. Will either
	 * crop the data, fill extra data with black bars, or resize the image 
	 * to as close to newWidthHeight, but not guaranteed. <br>
	 * *** NOTE: we can get a memory crash for large images. ***
	 * @param input Byte array input data
	 * @param cropFlag "crop", "blackBars", "resizeLarge", "resizeSmall" options for what 
	 * to do with image that doesn't fit new size. 
	 * @param ctx context, usually getApplicationContext,
	 * @param newWidthHeight New desired width and height
	 * @param orientationAngle float for the orientation of the byte array. 
	 * @throws IllegalArgumentException if cropFlag is not the right input type
	 */
	public static Bitmap resizeByteArray(
			byte[] input, 
			WidthHeight newWidthHeight, 
			ResizeType cropFlag, 
			Context ctx, 
			float orientationAngle){

		// create bitmap from data
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inDither = true;
		opt.inPreferredConfig = Config.RGB_565;
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(input, 0, input.length, opt);

		// grab width and height from bitmap
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// check rotation to see if we need to switch width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// check if no resizing required
		if (width == newWidthHeight.width &&
				height == newWidthHeight.height && 
				orientationAngle == 0)
			return bitmapOrg;

		// find new width and height for temporary bitmap object
		WidthHeight tmpWidthHeight = null;
		WidthHeight tmp2 = new WidthHeight(width, height);
		switch(cropFlag){
		case BLACK_BARS:
			tmpWidthHeight = com.tools.ImageProcessing.fitNoCrop(tmp2, newWidthHeight);
			break;
		case RESIZE_SMALL:
			tmpWidthHeight = com.tools.ImageProcessing.fitNoCrop(tmp2, newWidthHeight);
			break;
		case RESIZE_LARGE:
			tmpWidthHeight = com.tools.ImageProcessing.fitCrop(tmp2, newWidthHeight);
			break;
		case CROP:
			tmpWidthHeight = com.tools.ImageProcessing.fitCrop(tmp2, newWidthHeight);
			break;
		default:
			throw new IllegalArgumentException("unkown ResizeType");
		}

		// grab new height and width
		int newWidth = tmpWidthHeight.width;
		int newHeight = tmpWidthHeight.height;

		// calculate the scale
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// set rotation angle based on exif data
		if (orientationAngle != 0)
			matrix.postRotate(orientationAngle);

		// check rotation to see if we need to switch back to original width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// recreate the new Bitmap
		Bitmap tmpResizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		Bitmap resizedBitmap = null;

		// if resizeSmall or resizeLarge, then tmpResizedBitmap is the same as final.
		// Also if the desired output is the same as the tmpSize then, also just copy over
		if (cropFlag == ResizeType.RESIZE_SMALL ||
				cropFlag == ResizeType.RESIZE_LARGE ||
				(tmpResizedBitmap.getWidth() == newWidthHeight.width && 
						tmpResizedBitmap.getHeight() == newWidthHeight.height))
			resizedBitmap = tmpResizedBitmap;

		// crop option, we will grab a subset of the tmpBitmap
		else if (cropFlag == ResizeType.CROP){
			resizedBitmap = Bitmap.createBitmap
			(newWidthHeight.width, newWidthHeight.height, Bitmap.Config.RGB_565);
			int[] pixels = new int[resizedBitmap.getWidth()*resizedBitmap.getHeight()];
			int x = (int) Math.round((tmpResizedBitmap.getWidth() - resizedBitmap.getWidth())/2.0);
			int y = (int) Math.round((tmpResizedBitmap.getHeight() - resizedBitmap.getHeight())/2.0);
			tmpResizedBitmap.getPixels(pixels, 0, resizedBitmap.getWidth(), x, y, 
					resizedBitmap.getWidth(), resizedBitmap.getHeight());
			resizedBitmap.setPixels(pixels, 0, resizedBitmap.getWidth(), 0, 0, 
					resizedBitmap.getWidth(), resizedBitmap.getHeight());
		}

		// the blackBars option, we create a new bitmap that is larger and fill with tmpBitmap
		else {
			resizedBitmap = Bitmap.createBitmap
			(newWidthHeight.width, newWidthHeight.height, Bitmap.Config.RGB_565);
			int[] pixels = new int[resizedBitmap.getWidth()*resizedBitmap.getHeight()];
			int x = (int) -Math.round((tmpResizedBitmap.getWidth() - resizedBitmap.getWidth())/2.0);
			int y = (int) -Math.round((tmpResizedBitmap.getHeight() - resizedBitmap.getHeight())/2.0);
			tmpResizedBitmap.getPixels(pixels, 0, tmpResizedBitmap.getWidth(), 0, 0, 
					tmpResizedBitmap.getWidth(), tmpResizedBitmap.getHeight());
			resizedBitmap.setPixels(pixels, 0, tmpResizedBitmap.getWidth(), x, y, 
					tmpResizedBitmap.getWidth(), tmpResizedBitmap.getHeight());
		}

		// turn back into byte array
		return resizedBitmap;	
	}

	/** Rotate a byte array keeping aspect ratio. 
	 * @param input Byte array input data
	 * @param orientationAngle the orientation of the byte array.
	 * @param imageQuality 0-100 quality setting (90 is usually a good comprimize of size and quality)
	 * @throws IllegalArgumentException if cropFlag is not the right input type
	 */
	public static byte[] rotateByteArrayCHECKBEFOREUSING(
			byte[] input, 
			float orientationAngle,
			int imageQuality){

		// create bitmap from data
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inDither = true;
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(input, 0, input.length, opt);

		// grab width and height from bitmap
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// check rotation to see if we need to switch width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// check if no rotating required
		if (orientationAngle == 0)
			return input;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// set rotation angle based on exif data
		if (orientationAngle != 0)
			matrix.postRotate(orientationAngle);

		// check rotation to see if we need to switch back to original width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		// turn back into byte array
		ByteArrayOutputStream out = new ByteArrayOutputStream(resizedBitmap.getWidth()*resizedBitmap.getHeight());
		resizedBitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, out);   
		byte[] result = out.toByteArray();

		return result;		
	}

	/**
	 * Rotate the exif data in a picture by 90 degrees.
	 * @param filePath The path of the file
	 * @param direction any negative number for ccw, any positive for cw, and 0 does nothing
	 * @throws IOException 
	 */
	public static void rotateExif(String fileName, int direction)
	throws IOException{

		// 0 rotation
		if (direction == 0)
			return;

		// open the exif data
		ExifInterface EI = new ExifInterface(fileName);
		File file = new File(fileName);
		if (!file.exists() || !file.canRead() || !file.canWrite() || !file.isFile())
			throw new IOException("File can't be read");

		// get the angle
		float angle = ImageProcessing.getExifOrientationAngle(EI);

		// rotate the angle
		if (direction < 0)
			angle = angle - 90;
		else
			angle = angle + 90;

		// modulate by 360
		while (angle < 0)
			angle += 360;
		angle = angle % 360;

		// determine the rotation angle
		int exifOrientation = ExifInterface.ORIENTATION_UNDEFINED;
		switch ((int)angle){
		case 0:
			exifOrientation = ExifInterface.ORIENTATION_NORMAL;
			break;
		case 90:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
			break;
		case 180:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
			break;
		case 270:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
			break;
		}

		// save the data
		EI.setAttribute(ExifInterface.TAG_ORIENTATION, ""+exifOrientation);
		EI.saveAttributes();
	}
	
	/**
	 * Rotate exif orientation of image in the background
	 * @param <ACTIVITY_TYPE> The type of activity to return in callback (what activity called this)
	 * @param act the activity calling this
	 * @param filename The filename to rotate 
	 * @param direction positive for 90 deg rotation clocwise and negative for ccw
	 * @param callback The callback called when finished, IOException wil be null if no error occured
	 */
	public static <ACTIVITY_TYPE extends CustomActivity> void rotateExifBackground(
			ACTIVITY_TYPE act,
			String filename,
			int direction,
			CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, IOException> callback){
		(new RotateExifInBackgroundClass<ACTIVITY_TYPE>(act, filename, direction, callback)).execute();
	}

	/** attempts to save byte data from camera to the next default location on the SDcard. 
	 * Does not throw any exceptions, but returns success and any exceptions that were
	 * thrown as strings. Will return filename saved if successful in the reason field.<p>
	 * make sure to put this in your manifest:<br>
	 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	 * @param ctx Context where various android data is pulled from.
	 * @param data Byte array of data to be stored
	 * @param displayToast boolean to display toast message when finished
	 * @param fileNameInput write data to this filename. If null, then writes to next available file on external storage
	 * @param exifOrientation the orientation to store in exif header. See ExifInterface for details. Input null to not save anything.
	 * @param showImageInScanner boolean to show the image in the media scanner. Usually true
	 * @return if we successfully wrote the file, the reason will be the filename, if we failed, the reason is teh reason we failed
	 * */
	@TargetApi(8)
	public static SuccessReason saveByteDataToFile(
			Context ctx, 
			byte[] data, 
			Boolean displayToast, 
			String fileNameInput,
			Integer exifOrientation,
			boolean showImageInScanner){

		//TODO: save correct gps data

		// initialize result and fileName
		SuccessReason result = new SuccessReason(true);
		String fileName = "";

		// try catch wrapper over entire class
		try{
			// create values to store in file, just type and date
			ContentValues values = new android.content.ContentValues(2);
			values.put(MediaColumns.DATE_ADDED, System.currentTimeMillis()); 
			values.put(MediaColumns.MIME_TYPE, IMAGE_TYPE);

			// store into database
			Uri uriTarget = null;
			if (fileNameInput == null){
				uriTarget = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

				// grab filename
				fileName = Tools.getFileNameUri(ctx.getContentResolver(), uriTarget);
			}else
				fileName = fileNameInput;

			// write the folders
			Tools.writeRequiredFolders(fileName);

			// write the file
			FileOutputStream imageFileOS = null;
			try {

				// open the file
				if (fileNameInput==null)
					imageFileOS = (FileOutputStream) ctx.getContentResolver().openOutputStream(uriTarget);
				else{
					imageFileOS = new FileOutputStream(fileNameInput);
				}

				// write the file
				imageFileOS.write(data);
				imageFileOS.flush();
				imageFileOS.close();

				// write orientaiton and/or gps to file
				if (exifOrientation != null){
					ExifInterface EI = new ExifInterface(fileName);
					EI.setAttribute(ExifInterface.TAG_ORIENTATION, ""+exifOrientation);
					//EI.setAttribute(ExifInterface.TAG_GPS_LATITUDE, Tools.convertAngletoString(15.42));
					//EI.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "12/1,2/1,300/100");
					//EI.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
					//EI.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
					EI.saveAttributes();
				}

				// display toast
				if (displayToast)
					Toast.makeText(ctx,
							"Image saved: " + fileName,
							Toast.LENGTH_LONG).show();

				// show the picture in the image scanner
				if (showImageInScanner)
					MediaScannerConnection.scanFile(ctx, new String[] {fileName}, new String[] {IMAGE_TYPE}, null);

			} catch (FileNotFoundException e) {
				result = new SuccessReason(false, result.getReason()+" file not found");
				Log.e(LOG_TAG, Log.getStackTraceString(e));
			} catch (IOException e) {
				String str = e.getLocalizedMessage();
				if (str == null)
					str = e.toString();
				result = new SuccessReason(false, result.getReason()+" "+str);
				Log.e(LOG_TAG, Log.getStackTraceString(e));
			}	


		}catch(Exception e){
			result = new SuccessReason(false, result.getReason()+" "+e.toString());
		}

		// no exceptions then success
		if (result.getReason().length()==0)
			result = new SuccessReason(true, fileName);

		return result;
	}

	/**
	 * Create a bitmap thumbnail from the input image data. <br>
	 * Will take into account the exifOrientation, but can only handle rotations, not transposing or inversions. <br>
	 * This can only rescale by integer amounts. For example if original image is 128x128, and you input
	 * maxThumbnailDimension as 100, we can only rescale by a factor of 2, so the image will be 64x64. <p>
	 * *** Also If you input too large of a maxThumbnailDimension, you may crash due to memory overflow ***
	 * However this is memory intelligent, meaning it doesn't load the whole bitmap into memory and then resize,
	 * but only sub-samples the image. This is why we can only scale by integer amounts. 
	 * @param imageData The image data to resize
	 * @param exifOrientation the exifOrientation tag. If unknown tag, no rotation is assumed. @See ExifOrientation
	 * @param maxThumbnailDimension The maximum thumbnail dimension in either height or width
	 * @param forceBase2 If we force to downsample by base2, it is faster, but then we can only
	 * resize by a factor of 2,4,8,16...
	 * @return The resized and rotated thumbnail. So the new orientation tag is ExifInterface.ORIENTATION_NORMAL, null if unsuccessful
	 */
	private static Bitmap makeThumbnailHelper(
			final BitmapDecodable imageData,
			int exifOrientation,
			final int maxThumbnailDimension,
			boolean forceBase2){

		if (imageData == null)
			return null;

		// determine the size of the image first, so we know at what sample rate to use.
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		imageData.onDecode(options);
		double scale = ((double)Math.max(options.outHeight, options.outWidth))/maxThumbnailDimension;

		// convert to integer scaling ratio to base 2 or not depending on input
		int intScale = 1;
		if (forceBase2)
			intScale = (int)Math.pow(2, Math.ceil(com.tools.MathTools.log2(scale)));
		else
			intScale = (int) Math.ceil(scale);
		if (intScale < 1)
			intScale = 1;

		// now actually do the resizeing
		BitmapFactory.Options options2 = new BitmapFactory.Options();
		options2.inSampleSize = intScale;
		options2.inDither = true;
		Bitmap thumbnailBitmap = imageData.onDecode(options2);
		if (thumbnailBitmap == null)
			return null;

		// determine the rotation angle
		int angle = 0;
		switch (exifOrientation){
		case ExifInterface.ORIENTATION_NORMAL:
			// do nothing
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			angle = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			angle = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			angle = 270;
			break;
		}

		// now do the rotation
		if (angle != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(angle);

			thumbnailBitmap = Bitmap.createBitmap(thumbnailBitmap, 0, 0, thumbnailBitmap.getWidth(),
					thumbnailBitmap.getHeight(), matrix, true);
		}

		return thumbnailBitmap;
	}
	
	/**
	 * Read a picture from the given byte[], return null if unsuffessful <br>
	 * Make sure to NOT call on main UI thread because it's slow <br>
	 * Will not do any resizing, so make sure byte[] is actually small
	 * @param inputData the byte array
	 * @param angle the rotation angle to rotate the data in the CW direction
	 * @return the bitmap The bitmap returned, or null if unsuccessful
	 */
	public static Bitmap getThumbnail(
			byte[] inputData,
			float angle){
		
		// open the path if it exists
		if (inputData != null && inputData.length != 0){

			// read the bitmap
			Bitmap bmp = BitmapFactory.decodeByteArray(inputData, 0, inputData.length);
			if (bmp == null)
				return bmp;

			// now do the rotation
			if (angle != 0) {
				Matrix matrix = new Matrix();
				matrix.postRotate(angle);

				bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
						bmp.getHeight(), matrix, true);
			}
			
			return bmp;
		}
		else	
			return null;
	}

	/**
	 *	Different types of resizing
	 */
	public enum ResizeType{
		/**
		 * Resize data to be the new size while preserving aspect ratio and cropping any exces image that is outside bounds
		 */
		CROP,
		/**
		 * Resize image to be the new size, without losing any data. Insert black bars (letterbox) if we cannot fill entire new field
		 */
		BLACK_BARS,
		/**
		 * Actually resize the data to match the largest dimension.
		 */
		RESIZE_LARGE,
		/**
		 * Actually resize the data to match the smallest dimension
		 */
		RESIZE_SMALL;
	}

	private static abstract class BitmapDecodable<DATA_TYPE>{
		DATA_TYPE data;
		abstract Bitmap onDecode(BitmapFactory.Options options);
		void setData(DATA_TYPE data){
			this.data = data;
		}
	}
}
