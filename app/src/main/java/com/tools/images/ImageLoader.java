package com.tools.images;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tools.ImageProcessing;
import com.tools.SuccessReason;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ImageLoader<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE>{

	// private variables
	private MemoryCache<ID_TYPE> memoryCache = new MemoryCache<ID_TYPE>(); 	// This stores the bitmaps in memory
	private Map<ImageView, ID_TYPE> imageViews =
			Collections.synchronizedMap(new WeakHashMap<ImageView, ID_TYPE>()); // keeps track of links between views and pictures	
	private ExecutorService executorService;  								// run the threads
	private final int stub_id;	 											// The resource id of the default image

	private final int desiredWidth; 										// The desired width of full size image
	private final int desiredHeight; 										// The desired screen height of full size iamge
	private final boolean showFullImage; 									// boolean to display full image or just thumbnail
	private LoadImage<THUMBNAIL_TYPE, FULL_IMAGE_TYPE> loadImageCallback;	// callback to load images
	private Map<ID_TYPE, Object> thumbnailsLoadingLocks = Collections.synchronizedMap(new HashMap<ID_TYPE, Object>());
	private Map<ID_TYPE, Object> fullSizeLoadingLocks = Collections.synchronizedMap(new HashMap<ID_TYPE, Object>());

	// constants
	private static final int MAX_THREADS = 15; 								// max threads to spawn
	private static final long REQUIRED_BYTES = 4000000; 					// we must have this many bytes or we will clear the cache
	private static final long DOWNLOAD_TIMEOUT = 30000; 					// time in milliseconds to wait for image to download
	private static final String LOG_TAG = "com.tools";

	/**
	 * Create an image loader that asynchonously loads images both from file and the webs. <br>
	 * See stopThreads and restartThreads
	 * @param defaultImageId The resource id of the default image to display when no data is available
	 * @param desiredWidth The max desired width of the full size image on screen
	 * @param desiredHeight The max desired height of the full size image on screen
	 * @param showFullImage Boolean to display full sized image, or just thumbnail.
	 * @param loadImage callback used to load the bitmaps
	 */
	public ImageLoader(
			int defaultImageId,
			int desiredWidth,
			int desiredHeight,
			boolean showFullImage,
			LoadImage<THUMBNAIL_TYPE, FULL_IMAGE_TYPE> loadImage){

		executorService=Executors.newFixedThreadPool(MAX_THREADS);
		stub_id = defaultImageId;
		this.desiredHeight = desiredHeight;
		this.desiredWidth = desiredWidth;
		this.showFullImage = showFullImage;
		this.loadImageCallback = loadImage;
	}

	/**
	 * Try to create the thumbnail from the full picture
	 * @param thumbPath the desired thumbnail path
	 * @param fullFile the path to the full file
	 * @param maxPixelSize the maximum size in pixels for any dimension of the thumbnail. 
	 * @param forceBase2 forcing the downsizing to be powers of 2 (ie 2,4,8). Faster, but obviously less specific size is allowable.
	 * @param imageQuality 0-100 quality setting (90 is usually a good compromise of size and quality)
	 * @return true if successful, false otherwise
	 */
	public static boolean createThumbnailFromFull(
			String thumbPath,
			String fullFile,
			int maxPixelSize,
			boolean forceBase2,
			int imageQuality){

		// open the full file
		if (fullFile == null || thumbPath == null || fullFile.length() == 0 || thumbPath.length() == 0)
			return false;
		RandomAccessFile f = null;
		try{
			f = new RandomAccessFile(fullFile, "r");
		}catch (FileNotFoundException  e){
			return false;
		}

		// read the file data
		byte[] b = null;
		ExifInterface exif = null;
		try{
			b = new byte[(int)f.length()];
			f.read(b);
			f.close();

			// read the orientaion
			exif = new ExifInterface(fullFile);
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}

		// grab the rotation
		int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_UNDEFINED);

		// create the byte array
		Bitmap thumbnail = ImageProcessing.makeThumbnail(
				b,
				rotation,
				maxPixelSize,
				forceBase2);
		byte[] thumbByte = ImageProcessing.getByteArray(thumbnail, imageQuality);

		// save the thumbnail
		SuccessReason thumbnailSave = 
				ImageProcessing.saveByteDataToFile(
						null,
						thumbByte,
						false,
						thumbPath,
						ExifInterface.ORIENTATION_NORMAL,
						false);

		return thumbnailSave.getSuccess();
	}

	/**
	 * Return the properly rotated full image, null if can't be found or any other error <br>
	 * Make sure to only call NOT on main ui thread <br>
	 * It will be scaled down, so as not to cause memory crash
	 * @param inputData the data that needs to be resized
	 * @param angle the angle to rotate the data
	 * @param desiredWidth the desired width of the image, will not necessarily create a bitmap of this exact size, but no larger than this
	 * @param desiredHeight the desired height of the image, will not necessarily create a bitmap of this exact size, but no larger than this
	 * @return The bitmap or null if failed.
	 */
	public static Bitmap getFullImage(
			byte[] inputData,
			float angle,
			int desiredWidth,
			int desiredHeight){
		
		try{
			if (inputData != null && inputData.length != 0){

				// if angle is 90 or 270, then switch height and width
				if (Math.round(angle+90) % 180 == 0){
					int tmp = desiredHeight;
					desiredHeight = desiredWidth;
					desiredWidth = tmp;
				}
				
				//decode image size
				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeByteArray(inputData, 0, inputData.length, o);

				// find the correct scale size
				double scale = ((double)Math.max((double)o.outHeight/desiredHeight, (double)o.outWidth/desiredWidth));
				int intScale = (int)Math.pow(2, Math.ceil(com.tools.MathTools.log2(scale)));
				if (intScale < 1)
					intScale = 1;

				// now actually do the resizeing
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = intScale;
				Bitmap bitmap = BitmapFactory.decodeByteArray(inputData, 0, inputData.length, options);	
				
				if(bitmap == null)
					return null;
				
				// now do the rotation
				if (angle != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(angle);

					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
							bitmap.getHeight(), matrix, true);
				}

				return bitmap;
			}else
				return null;
		}catch(Exception e){
			Log.e("ImageLoader", Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Return the properly rotated full image, null if can't be found or any other error <br>
	 * Make sure to only call NOT on main ui thread <br>
	 * It will be scaled down, so as not to cause memory crash
	 * @param path the path to the file
	 * @param desiredWidth the desired width of the image, will not necessarily create a bitmap of this exact size, but no larger than this
	 * @param desiredHeight the desired height of the image, will not necessarily create a bitmap of this exact size, but no larger than this
	 * @return The bitmap or null if failed.
	 */
	public static Bitmap getFullImage(
			String path,
			int desiredWidth,
			int desiredHeight){

		try{
			if (path != null && path.length() != 0 && (new File(path)).exists()){
				
				float angle =  ImageProcessing.getExifOrientationAngle(path);	
				
				// if angle is 90 or 270, then switch height and width
				if (Math.round(angle+90) % 180 == 0){
					int tmp = desiredHeight;
					desiredHeight = desiredWidth;
					desiredWidth = tmp;
				}
				
				// make the file
				File file = new File(path);

				//decode image size
				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(file),null,o);	

				// find the correct scale size
				double scale = ((double)Math.max((double)o.outHeight/desiredHeight, (double)o.outWidth/desiredWidth));
				int intScale = (int)Math.pow(2, Math.ceil(com.tools.MathTools.log2(scale)));
				if (intScale < 1)
					intScale = 1;

				// now actually do the resizeing
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = intScale;
				Bitmap bitmap = null;
				try{
					bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
				}catch(OutOfMemoryError e1){
					Log.e("ImageLoader", "out of memory - garbage collecting first");
					System.gc();
					try{
						bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
					}catch(OutOfMemoryError e2){
						Log.e("ImageLoader", "out of memory - scale down to 2x smaller size");
						options.inSampleSize = options.inSampleSize*2;
						try{
							bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
						}catch(OutOfMemoryError e3){
							Log.e("ImageLoader", "out of memory -scale down to 4x smaller size");
							options.inSampleSize = options.inSampleSize*2;
							bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
						}
					}
				}
				if(bitmap == null)
					return null;	

				// now do the rotation
				if (angle != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(angle);

					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
							bitmap.getHeight(), matrix, true);
				}

				return bitmap;
			}else
				return null;
		}catch(FileNotFoundException e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return null;

		} catch (IOException e) {
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Read a picture from the given path, return null if unsuffessful <br>
	 * Make sure to NOT call on main UI thread because it's slow <br>
	 * Will be properly rotated based on exif data stored in image. This is getThumbnail, because if it's a full size image, we will probably crash when rotating. <br>
	 * Warning this decodes and re-encodes the file
	 * @param path The path to the image
	 * @param imageQuality The image quality to re-encode file
	 * @return the byte array
	 */
	public static byte[] getThumbnailAsByteArray(String path, int imageQuality){
		Bitmap bmp = com.tools.Tools.getThumbnail(path);
		if (bmp == null)
			return null;

		// turn back into byte array
		ByteArrayOutputStream out = new ByteArrayOutputStream(bmp.getWidth()*bmp.getHeight());
		bmp.compress(Bitmap.CompressFormat.JPEG, imageQuality, out);   
		byte[] result = out.toByteArray();

		return result;

		//TODO: we shouldn't have to decode the file and re-encode to just read the byte array of image data.
	}

	/**
	 * Clear the cache and reset didWeReceivedOomError
	 */
	public void clearCache() {
		memoryCache.clear();
	}

	/**
	 * Clear memory if we don't have enough space available
	 */
	public synchronized void clearCacheIfNeeded(){
		if(Runtime.getRuntime().freeMemory() < REQUIRED_BYTES)
			clearCache();
	}

	/**
	 * Launch async runnable to show this image
	 * @param pictureRowId 	The picture rowId
	 * @param thumbnail The data required to find / create the thumbnail
	 * @param fullPictuure The data required to find / create the full image
	 * @param imageView The imageView to put the image
	 * @param progressBar a progressbar to show, null if none.
	 */
	public void DisplayImage(
			ID_TYPE pictureRowId,
			THUMBNAIL_TYPE thumbnail,
			FULL_IMAGE_TYPE fullPictuure,
			ImageView imageView,
			ProgressBar progressBar)
	{

		WeakReference<ProgressBar> weakProgress = new WeakReference<ProgressBar>(progressBar);
		progressBar = null;

		// create the object containing all the relevant data
		PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> data =
				new PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE>(
						pictureRowId,
						thumbnail,
						fullPictuure,
						imageView,
						weakProgress.get());

				// store the links
				imageViews.put(imageView, pictureRowId);

				// attempt to access cached full picture
				Bitmap bitmap = null;
				if (showFullImage)
					bitmap = memoryCache.getFullPicture(pictureRowId);

				// no full picture, so queue the photo loader, and check for thumbnail
				if (bitmap == null){
					bitmap = memoryCache.getThumbnail(pictureRowId);
					if (bitmap == null)
						queuePhoto(data, true);
					else if (showFullImage)
						queuePhoto(data, false);
				}

				// see if we have a bitmap to access
				if(bitmap!=null)
					imageView.setImageBitmap(bitmap);

				// otherwise just show the default image
				else
					imageView.setImageResource(stub_id);
	}
	
	/**
	 * Null the reference at a particular picture id.
	 * @param id The id to null at
	 */
	public void clearCacheAtId(ID_TYPE id){
		memoryCache.clearCacheAt(id);
	}

	/**
	 * Return the memory cache.<br>
	 * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
	 * for example on orientation changes *****
	 * @return
	 */
	public MemoryCache<ID_TYPE> getMemoryCache(){
		return memoryCache;
	}

	/**
	 * Restart running threads. Usually call this on activity onResume();
	 * If threads already running, null operation.
	 */
	public void restartThreads(){
		if (executorService == null)
			executorService=Executors.newFixedThreadPool(MAX_THREADS);
	}

	/**
	 * Set the memory cache to this new value, clearing old one.
	 * @see getMemoryCache.
	 * @param mem
	 */
	public void restoreMemoryCache(MemoryCache<ID_TYPE> mem){
		if (memoryCache != null && mem != memoryCache)
			memoryCache.clear();
		memoryCache = mem;
	}

	/**
	 * Stop background threads, usually call this on activity onPause
	 */
	public void stopThreads(){
		if (executorService != null)
			executorService.shutdown();
		executorService = null;
	}

	/**
	 * Add this photo to the download queue
	 * @param url The url of the file to download
	 * @param imageView The imageView to put the bitmap
	 * @param getThumbnailFirst should we get thumbnail first (true), or do we only need the full picture (false)
	 */
	private void queuePhoto(PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> data, boolean getThumbnailFirst)
	{
		if (executorService != null){
			executorService.submit(new PhotosLoader(data, getThumbnailFirst, showFullImage));
			/*
			Future<?> future = executorService.submit(new PhotosLoader(data, getThumbnailFirst, showFullImage));
			try {
				future.get();
			} catch (ExecutionException e) {
				Throwable rootException = e.getCause();
				int kyle = 6;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Throwable rootException2 = e.getCause();
				int kyle2 = 6;
			}
			*/
		}
	}

	/**
	 * Check if this imageView is being re-used
	 * @param photoToLoad
	 * @return boolean if true
	 */
	boolean imageViewReused(PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> photoToLoad){
		ImageView image = photoToLoad.imageViewSoftReference.get();
		if (image == null)
			return true;
		ID_TYPE rowId = imageViews.get(image);
		if(rowId==null || !photoToLoad.pictureId.equals(rowId))
			return true;
		return false;
	}

	/**
	 * Used to grab bitmap data for generic identifiers of thumbnail and full pictures.
	 * See static methods in ImageLoader for helpful methods for loading images
	 * @author Kyle
	 *
	 * @param <THUMBNAIL_TYPE>
	 * @param <FULL_IMAGE_TYPE>
	 */
	public interface LoadImage <THUMBNAIL_TYPE, FULL_IMAGE_TYPE>{
		/**
		 * Create thumbnail data from the full sized image. This is only called if the thumbnail data is missing.<br>
		 * This is not required.
		 * @param thumbnailData
		 * @param fullSizeData
		 */
		public void createThumbnailFromFull(THUMBNAIL_TYPE thumbnailData, FULL_IMAGE_TYPE fullSizeData);
		/**
		 * Load in a full size picture. This will load after the thumbnail has loaded. It should be a high def image
		 * @param fullSizeData The data needed to get the picture
		 * @param desiredWidth The max width this image should be to avoid memory errors
		 * @param desiredHeight The max height this image should be to avoid memory errors
		 * @return The bitmap, or null if unsuccessful
		 */
		public Bitmap onFullSizeLocal(FULL_IMAGE_TYPE fullSizeData, int desiredWidth, int desiredHeight);
		/**
		 * Load in a full size picture from the web only if onFullSizeLocal returns null. This will load after the thumbnail has loaded. It should be a high def image
		 * @param fullSizeData The data needed to get the picture
		 * @param desiredWidth The max width this image should be to avoid memory errors
		 * @param desiredHeight The max height this image should be to avoid memory errors
		 * @param weakProgess, A weak reference to a progressBar to update file progress, weakProgress.get() can return null
		 * @return The bitmap, or null if unsuccessful
		 */
		public Bitmap onFullSizeWeb(FULL_IMAGE_TYPE fullSizeData, int desiredWidth, int desiredHeight, WeakReference<ProgressBar> weakProgress);
		/**
		 * Load in a thumbnail. This should be a small picture that loads quickly and ideally does not require resizing
		 * @param thumbnailData The data needed to get the picture
		 * @return The bitmap, or null if unsuccessful
		 */
		public Bitmap onThumbnailLocal(THUMBNAIL_TYPE thumbnailData);
		/**
		 * Load in a thumbnail from the web if thumbnailLocal return null. <br>
		 * This should be a small picture that loads quickly and ideally does not require resizing
		 * @param thumbnailData The data needed to get the picture
		 * @return The bitmap, or null if unsuccessful
		 */
		public Bitmap onThumbnailWeb(THUMBNAIL_TYPE thumbnailData);
	}

	//Used to display bitmap in the UI thread
	private class BitmapDisplayer
	implements Runnable
	{
		Bitmap bitmap;
		PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> photoToLoad;
		public BitmapDisplayer(
				Bitmap b,
				PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> p){

			bitmap=b;
			photoToLoad=p;
		}
		public void run()
		{
			if(imageViewReused(photoToLoad))
				return;
			ImageView image = photoToLoad.imageViewSoftReference.get();
			if (image == null)
				return;
			if(bitmap!=null)
				image.setImageBitmap(bitmap);
			else
				image.setImageResource(stub_id);
		}
	}

	private class PhotosLoader
	implements Runnable {
		PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> photoToLoad;
		boolean getThumbnailFirst;
		boolean showFullImage;
		PhotosLoader(
				PhotoToLoad<ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE> photoToLoad,
				boolean getThumbnailFirst,
				boolean showFullImage){
			this.photoToLoad=photoToLoad;
			this.getThumbnailFirst = getThumbnailFirst;
			this.showFullImage = showFullImage;
		}

		@Override
		public void run() {
			// this is a recycle view, so don't do anything
			if(imageViewReused(photoToLoad))
				return;

			// create a lock to control access to this thread for this object
			if (thumbnailsLoadingLocks.get(photoToLoad.pictureId) == null)
				thumbnailsLoadingLocks.put(photoToLoad.pictureId, new Object());
			if (fullSizeLoadingLocks.get(photoToLoad.pictureId) == null)
				fullSizeLoadingLocks.put(photoToLoad.pictureId, new Object());

			// synchronize access to only allow for each picture single access to this block, so as not to allow mutliple grabs of the same file
			//synchronized (thumbnailsLoadingLocks.get(photoToLoad.pictureId)) {

			// should we grab the thumbnail first?
			if (getThumbnailFirst){
				Bitmap bmp = memoryCache.getThumbnail(photoToLoad.pictureId);
				if (bmp == null)
					bmp = loadImageCallback.onThumbnailLocal(photoToLoad.thumbnail);
				if (bmp == null)
					bmp = loadImageCallback.onThumbnailWeb(photoToLoad.thumbnail);
				if (bmp == null)
					loadImageCallback.createThumbnailFromFull(photoToLoad.thumbnail, photoToLoad.fullPicture);
				if (bmp != null)
					memoryCache.putThumbnail(photoToLoad.pictureId, bmp);

				// recycled view
				if(imageViewReused(photoToLoad))
					return;

				// load the bitmap on the ui thread
				if (bmp != null){
					BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
					ImageView image = photoToLoad.imageViewSoftReference.get();
					if (image != null){
						Activity a=(Activity)image.getContext();
						a.runOnUiThread(bd);
					}
				}
			}
			//}

			//synchronized (fullSizeLoadingLocks.get(photoToLoad.pictureId)) {

			// grab the full picture
			if (showFullImage){
				Bitmap fullBmp = memoryCache.getFullPicture(photoToLoad.pictureId);
				clearCacheIfNeeded();
				if (fullBmp == null)
					fullBmp = loadImageCallback.onFullSizeLocal(photoToLoad.fullPicture, desiredWidth, desiredHeight);
				if (fullBmp == null)
					fullBmp = loadImageCallback.onFullSizeWeb(photoToLoad.fullPicture, desiredWidth, desiredHeight, photoToLoad.weakProgress);
				if (fullBmp != null)
					memoryCache.putFullPicture(photoToLoad.pictureId, fullBmp);

				// recycled view
				if(imageViewReused(photoToLoad))
					return;

				// load the bitmap on the ui thread
				if (fullBmp != null){
					BitmapDisplayer bd = new BitmapDisplayer(fullBmp, photoToLoad);
					ImageView image = photoToLoad.imageViewSoftReference.get();
					if (image != null){
						Activity a=(Activity)image.getContext();
						a.runOnUiThread(bd);
					}
				}
			}
			//}
		}
	}

	//Task for the queue
	private static class PhotoToLoad <ID_TYPE, THUMBNAIL_TYPE, FULL_IMAGE_TYPE>
	{
		public THUMBNAIL_TYPE thumbnail;
		public FULL_IMAGE_TYPE fullPicture;
		public SoftReference<ImageView> imageViewSoftReference;
		public WeakReference<ProgressBar> weakProgress;
		public ID_TYPE pictureId;
		public PhotoToLoad(
				ID_TYPE pictureId,
				THUMBNAIL_TYPE thumbnail,
				FULL_IMAGE_TYPE fullPicture,
				ImageView image,
				ProgressBar progressBar){
			this.thumbnail = thumbnail;
			this.fullPicture = fullPicture;
			this.pictureId = pictureId;
			imageViewSoftReference = new SoftReference<ImageView>(image);
			this.weakProgress = new WeakReference<ProgressBar>(progressBar);
		}
	}
}
