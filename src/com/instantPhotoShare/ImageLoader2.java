package com.instantPhotoShare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tools.SuccessReason;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.ImageView;

public class ImageLoader2<THUMBNAIL_TYPE, FULL_IMAGE_TYPE>{
    
	// private variables
    private MemoryCache memoryCache = new MemoryCache(); 							// This stores the bitmaps in memory
    private Map<ImageView, PhotoToLoad> imageViews =
    	Collections.synchronizedMap(new WeakHashMap<ImageView, PhotoToLoad>()); 	// keeps track of links between views and pictures	
    private ExecutorService executorService;  										// run the threads
    private final int stub_id;	 													// The resource id of the default image
    private final int desiredWidth; 												// The desired width of full size image
    private final int desiredHeight; 												// The desired screen height of full size iamge
    private final boolean showFullImage; 											// boolean to display full image or just thumbnail
    private LoadImage<THUMBNAIL_TYPE, FULL_IMAGE_TYPE> loadImageCallback;
    
    // constants
    private static final int MAX_THREADS = 15; 										// max threads to spawn
    
    /**
     * Create an image loader that asynchonously loads images both from file and the webs. <br>
     * See stopThreads and restartThreads
     * @param defaultImageId The resource id of the default image to display when no data is available
     * @param desiredWidth The max desired width of the full size image on screen
     * @param desiredHeight The max desired height of the full size image on screen
     * @param showFullImage Boolean to display full sized image, or just thumbnail.
     */
    public ImageLoader2(
    		int defaultImageId,
    		int desiredWidth,
    		int desiredHeight,
    		boolean showFullImage,
    		LoadImage<THUMBNAIL_TYPE, FULL_IMAGE_TYPE> loadImageCallback){

        executorService=Executors.newFixedThreadPool(MAX_THREADS);
        stub_id = defaultImageId;
        this.desiredHeight = desiredHeight;
        this.desiredWidth = desiredWidth;
        this.showFullImage = showFullImage;
        this.loadImageCallback = loadImageCallback;
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
    	 * @return The bitmap, or null if unsuccessful
    	 */
    	public Bitmap onFullSizeWeb(FULL_IMAGE_TYPE fullSizeData, int desiredWidth, int desiredHeight);
    	/**
    	 * Create thumbnail data from the full sized image. This is only called if the thumbnail data is missing.<br>
    	 * This is not required.
    	 * @param thumbnailData
    	 * @param fullSizeData
    	 */
    	public void createThumbnailFromFull(THUMBNAIL_TYPE thumbnailData, FULL_IMAGE_TYPE fullSizeData);
    }
    
    /**
     * Launch async runnable to show this image
     * @param pictureRowId 	The picture rowId
     * @param thumbnailPath The thumbnail path
     * @param fullPicturePath The full file image path
     * @param imageView The imageView to put the image
     */
    public void DisplayImage(
    		Long pictureRowId,
    		THUMBNAIL_TYPE thumbnail,
    		FULL_IMAGE_TYPE fullPictuure,
    		ImageView imageView)
    {
    	
    	// create the object containing all the relevant data
    	PhotoToLoad data =
    		new PhotoToLoad(pictureRowId, thumbnail, fullPictuure, imageView);
    	
    	// store the links
        imageViews.put(imageView, data);
        
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
     * Add this photo to the download queue
     * @param url The url of the file to download
     * @param imageView The imageView to put the bitmap
     * @param getThumbnailFirst should we get thumbnail first (true), or do we only need the full picture (false)
     */
    private void queuePhoto(PhotoToLoad data, boolean getThumbnailFirst)
    {
    	if (executorService != null)
    		executorService.submit(new PhotosLoader(data, getThumbnailFirst, showFullImage));
    }
    
    /**
     * Read a picture from the given path, return null if unsuffessful <br>
     * Make sure to NOT call on main UI thread because it's slow <br>
     * Will be properly rotated based on exif data stored in image
     * @param path
     * @return the bitmap
     */
    public static Bitmap getThumbnail(String path){
    	// open the path if it exists
    	if (path != null && path.length() != 0 && (new File(path)).exists()){
    		
    		// read the bitmap
    		Bitmap bmp = BitmapFactory.decodeFile(path);
    		if (bmp == null)
    			return bmp;
    		
    		// now do the rotation
    		float angle =  com.tools.Tools.getExifOrientationAngle(path);
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
     * Try to create the thumbnail from the full picture
     * @param thumbPath the desired thumbnail path
     * @param fullFile the path to the full file
     * @return true if successful, false otherwise
     */
    public boolean createThumbnailFromFull(String thumbPath, String fullFile){
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
		byte[] thumbnail = com.tools.Tools.makeThumbnail(
				b,
				rotation,
				Utils.MAX_THUMBNAIL_DIMENSION,
				Utils.FORCE_BASE2_THUMBNAIL_RESIZE,
				Utils.IMAGE_QUALITY);

		// save the thumbnail
		SuccessReason thumbnailSave = 
			com.tools.Tools.saveByteDataToFile(
					null,
					thumbnail,
					"",
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

    			// make the file
    			File file = new File(path);

    			//decode image size
    			BitmapFactory.Options o = new BitmapFactory.Options();
    			o.inJustDecodeBounds = true;
    			BitmapFactory.decodeStream(new FileInputStream(file),null,o);	

    			// find the correct scale size
    			double scale = ((double)Math.max((double)o.outHeight/desiredHeight, (double)o.outWidth/desiredWidth));
    			int intScale = (int)Math.pow(2, Math.ceil(com.tools.MathTools.log2(scale)));

    			// now actually do the resizeing
    			BitmapFactory.Options options = new BitmapFactory.Options();
    			options.inSampleSize = intScale;
    			Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
    			float angle =  com.tools.Tools.getExifOrientationAngle(path);		

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
    		return null;
    	}catch(Exception e){
    		return null;
    	}
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public THUMBNAIL_TYPE thumbnail;
        public FULL_IMAGE_TYPE fullPicture;
        public ImageView imageView;
        public Long pictureId;
        public PhotoToLoad(
        		Long pictureRowId,
        		THUMBNAIL_TYPE thumbnail,
        		FULL_IMAGE_TYPE fullPicture,
        		ImageView i){
            this.thumbnail = thumbnail;
            this.fullPicture = fullPicture;
            this.pictureId = pictureId;
            imageView=i;
        }
    }
    
    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        boolean getThumbnailFirst;
        boolean showFullImage;
        PhotosLoader(PhotoToLoad photoToLoad, boolean getThumbnailFirst, boolean showFullImage){
            this.photoToLoad=photoToLoad;
            this.getThumbnailFirst = getThumbnailFirst;
            this.showFullImage = showFullImage;
        }
        
        @Override
        public void run() {
        	// this is a recycle view, so don't do anything
        	if(imageViewReused(photoToLoad))
        		return;

        	// should we grab the thumbnail first?
        	if (getThumbnailFirst){
        		Bitmap bmp = loadImageCallback.onThumbnailLocal(photoToLoad.thumbnail);
        		if (bmp == null)
        			bmp = loadImageCallback.onThumbnailWeb(photoToLoad.thumbnail);
        		if (bmp == null)
        			loadImageCallback.createThumbnailFromFull(photoToLoad.thumbnail, photoToLoad.fullPicture);
        		memoryCache.putThumbnail(photoToLoad.pictureId, bmp);

        		// recycled view
        		if(imageViewReused(photoToLoad))
        			return;

        		// load the bitmap on the ui thread
        		if (bmp != null){
        			BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
        			Activity a=(Activity)photoToLoad.imageView.getContext();
        			a.runOnUiThread(bd);
        		}
        	}
        	
        	// grab the full picture
        	if (showFullImage){
        		Bitmap fullBmp = loadImageCallback.onFullSizeLocal(photoToLoad.fullPicture, desiredWidth, desiredHeight);
        		if (fullBmp == null)
        			fullBmp = loadImageCallback.onFullSizeWeb(photoToLoad.fullPicture, desiredWidth, desiredHeight);
        		memoryCache.putFullPicture(photoToLoad.pictureId, fullBmp);

        		// recycled view
        		if(imageViewReused(photoToLoad))
        			return;

        		// load the bitmap on the ui thread
        		if (fullBmp != null){
        			BitmapDisplayer bd = new BitmapDisplayer(fullBmp, photoToLoad);
        			Activity a=(Activity)photoToLoad.imageView.getContext();
        			a.runOnUiThread(bd);
        		}
        	}
        }
    }
    
    /**
     * Check if this imageView is being re-used
     * @param photoToLoad
     * @return boolean if true
     */
    boolean imageViewReused(PhotoToLoad photoToLoad){
        Long rowId = imageViews.get(photoToLoad.imageView).pictureId;
        if(rowId==null || !rowId.equals(photoToLoad.pictureId))
            return true;
        return false;
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;
        public BitmapDisplayer(Bitmap b, PhotoToLoad p){bitmap=b;photoToLoad=p;}
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null)
                photoToLoad.imageView.setImageBitmap(bitmap);
            else
                photoToLoad.imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        memoryCache.clear();
    }
    
    /**
     * Stop background threads, usually call this on activity onPause
     */
    public void stopThreads(){
    	executorService.shutdown();
    	executorService = null;
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
     * Return the memory cache.<br>
     * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
     * for example on orientation changes *****
     * @return
     */
    public MemoryCache getMemoryCache(){
    	return memoryCache;
    }
    
    /**
     * Set the memory cache to this new value, clearing old one.
     * @see getMemoryCache.
     * @param mem
     */
    public void restoreMemoryCache(MemoryCache mem){
    	if (memoryCache != null)
    		memoryCache.clear();
    	memoryCache = mem;
    }
}
