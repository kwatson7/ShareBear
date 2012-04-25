package com.instantPhotoShare;

import java.io.ByteArrayOutputStream;
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

import com.instantPhotoShare.images.ImageViewTouch;
import com.tools.SuccessReason;
import com.tools.WidthHeight;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.ImageView;

public class ImageLoaderTouch {
    
	// private variables
    private MemoryCache memoryCache = new MemoryCache(); 							// This stores the bitmaps in memory
    private Map<ImageViewTouch, PhotoToLoad> imageViews =
    	Collections.synchronizedMap(new WeakHashMap<ImageViewTouch, PhotoToLoad>()); 	// keeps track of links between views and pictures	
    private ExecutorService executorService;  										// run the threads
    private final int stub_id;	 													// The resource id of the default image
    private final int desiredWidth; 												// The desired width of full size image
    private final int desiredHeight; 												// The desired screen height of full size iamge
    private final boolean showFullImage; 											// boolean to display full image or just thumbnail
    
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
    public ImageLoaderTouch(int defaultImageId, int desiredWidth, int desiredHeight, boolean showFullImage){
       // fileCache=new FileCache(context);
        executorService=Executors.newFixedThreadPool(MAX_THREADS);
        stub_id = defaultImageId;
        this.desiredHeight = desiredHeight;
        this.desiredWidth = desiredWidth;
        this.showFullImage = showFullImage;
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
    		String thumbnailPath,
    		String fullPicturePath,
    		ImageViewTouch imageView)
    {
    	
    	// create the object containing all the relevant data
    	PhotoToLoad data =
    		new PhotoToLoad(pictureRowId, thumbnailPath, fullPicturePath, imageView);
    	
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
        	imageView.setImageBitmapReset( bitmap, 0, true );
        
        // otherwise just show the default image
        else
        {
            imageView.setImageResource(stub_id);
        }
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
     * Read the thumbnail from the path and if it existsm return null if unsuffessful <br>
     * Make sure to NOT call on main UI thread
     * @param path
     * @return
     */
    private Bitmap getThumbnail(String path){
    	if (path != null && path.length() != 0 && (new File(path)).exists()){
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
    		
    		/*
    		// resize
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			byte[] byteArray = baos.toByteArray();
			byteArray = com.tools.Tools.resizeByteArray(
					byteArray,
					new WidthHeight(desiredWidth, desiredHeight),
					"resizeSmall",
					null,
					0f);
			bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			*/

    		return bmp;
		}
    	else	
    		return null;
    }
    
    /**
     * Try to create the thumbnail from the full picture
     * @param thumbPath
     * @param fullFile
     */
    private void createThumbnailFromFull(String thumbPath, String fullFile){
    	// open the full file
    	if (fullFile == null || thumbPath == null || fullFile.length() == 0 || thumbPath.length() == 0)
    		return;
    	RandomAccessFile f = null;
    	try{
    		f = new RandomAccessFile(fullFile, "r");
    	}catch (FileNotFoundException  e){
    		return;
    	}
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
    		return;
    	}

		int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_UNDEFINED);
		
		// create the byte array
		byte[] thumbnail = com.tools.Tools.makeThumbnail(
				b,
				rotation,
				Utils.MAX_THUMBNAIL_DIMENSION,
				Utils.FORCE_BASE2_THUMBNAIL_RESIZE);

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
	}

    
    /**
     * Return the properly rotated full image, null if can't be found or any other error <br>
     * Make sure to only call NOT on main ui thread
     * @param path
     * @return
     */
    private Bitmap getFullImage(String path){
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
    			
    			/*
    			// resize
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
    			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    			byte[] byteArray = baos.toByteArray();
    			byteArray = com.tools.Tools.resizeByteArray(
    					byteArray,
    					new WidthHeight(desiredWidth, desiredHeight),
    					"resizeSmall",
    					null,
    					0f);
    			bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    			*/
    			
    			return bitmap;
    		}else
    			return null;
    	}catch(FileNotFoundException e){
    		return null;
    	}catch(Exception e){
    		clearCache(); // possible memory error, so clear cache
    		return null;
    	}
    }

    private Bitmap getThumbnailFromServer(long pictureRowId){
    	return null;
    }
    
    private Bitmap getFullPictureFromServer(long pictureRowId){
    	return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public String thumbnailPath;
        public String fullFilePath;
        public ImageViewTouch imageView;
        public Long pictureRowId;
        public PhotoToLoad(
        		Long pictureRowId,
        		String thumbnailPath,
        		String fullFilePath,
        		ImageViewTouch i){
            this.thumbnailPath = thumbnailPath;
            this.fullFilePath = fullFilePath;
            this.pictureRowId = pictureRowId;
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
        		Bitmap bmp = getThumbnail(photoToLoad.thumbnailPath);
        		if (bmp == null)
        			createThumbnailFromFull(photoToLoad.thumbnailPath, photoToLoad.fullFilePath);
        		memoryCache.putThumbnail(photoToLoad.pictureRowId, bmp);

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
        		Bitmap fullBmp = getFullImage((photoToLoad.fullFilePath));
        		memoryCache.putFullPicture(photoToLoad.pictureRowId, fullBmp);

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
        Long rowId = imageViews.get(photoToLoad.imageView).pictureRowId;
        if(rowId==null || !rowId.equals(photoToLoad.pictureRowId))
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
            	photoToLoad.imageView.setImageBitmapReset( bitmap, 0, true );
            else
                photoToLoad.imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        memoryCache.clear();
     //   fileCache.clear();
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
