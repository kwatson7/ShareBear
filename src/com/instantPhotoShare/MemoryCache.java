package com.instantPhotoShare;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import com.tools.TwoObjects;

import android.graphics.Bitmap;

public class MemoryCache {
	// private variables
    private HashMap<Long, TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>> cache = 
    	new HashMap<Long, TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>>(); 			// Hashmap holding thumbnail and full image bmp
    
    /**
     * store the thumbnail in this memory cache
     * @param pictureRowId The picture rowId this is linked to
     * @param bitmap The thumbnail bitmap
     */
    public void putThumbnail(Long pictureRowId, Bitmap bitmap){
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// null data
    	if (data == null){
    		data = new  TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>
    		(new SoftReference<Bitmap>(bitmap), null);
    	}else{
    		// put the thumbnail in the correct spot
    		data.mObject1 = new SoftReference<Bitmap>(bitmap);
    	}
    	
    	// store in cache
        cache.put(pictureRowId, data);
    }
    
    /**
     * store the full picture in this memory cache
     * @param pictureRowId The picture rowId this is linked to
     * @param bitmap The full picture bitmap
     */
    public void putFullPicture(Long pictureRowId, Bitmap bitmap){
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// null data
    	if (data == null){
    		data = new  TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>
    		(null, new SoftReference<Bitmap>(bitmap));
    	}else{
    		// put the full picture in the correct spot
    		data.mObject2 = new SoftReference<Bitmap>(bitmap);
    	}
    	
    	// store in cache
        cache.put(pictureRowId, data);
    }
    
    /**
     * Put the thumbnail and full picture bitmaps in the memory cache
     * @param pictureRowId The picture rowId this is linked to
     * @param thumbnail The thumbnail bitmap
     * @param fullPicture The full picture bitmap
     */
    public void putPictures(Long pictureRowId, Bitmap thumbnail, Bitmap fullPicture){
        cache.put(pictureRowId, new  TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>
        	(new SoftReference<Bitmap>(thumbnail), new SoftReference<Bitmap>(fullPicture)));
    }

    /**
     * Clear the cache
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Get the thumbnail at the picture rowId
     * @param pictureRowId the picture rowId
     * @return the thumbnail bitmap stored in this location, or null if none
     */
    public Bitmap getThumbnail(Long pictureRowId){
    	// if no key, then just return null
    	if (!cache.containsKey(pictureRowId))
    		return null;
    	
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// now return the actual bitmap
    	if (data != null && data.mObject1 != null)
    		return data.mObject1.get();
    	else
    		return null;
    	
    }
    
    /**
     * Get the full picture at the picture rowId
     * @param pictureRowId the picture rowId
     * @return the full picture bitmap stored in this location, or null if none
     */
    public Bitmap getFullPicture(Long pictureRowId){
    	// if no key, then just return null
    	if (!cache.containsKey(pictureRowId))
    		return null;
    	
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// now return the actual bitmap
    	if (data != null && data.mObject2 != null)
    		return data.mObject2.get();
    	else
    		return null;
    	
    }
}