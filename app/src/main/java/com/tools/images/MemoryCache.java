package com.tools.images;

import java.lang.ref.SoftReference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.tools.TwoObjects;

import android.graphics.Bitmap;

/**
 * Used to store links between ids and thumbnail / full size combos. It is used in ImageLoader
 * @author Kyle
 *
 */
public class MemoryCache <ID_TYPE> {
	
	// private variables
    private HashMap<ID_TYPE, TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>> cache = 
    	new HashMap<ID_TYPE, TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>>>(); 			// Hashmap holding thumbnail and full image bmp
    
    /**
     * store the thumbnail in this memory cache
     * @param pictureRowId The picture rowId this is linked to
     * @param bitmap The thumbnail bitmap
     */
    public synchronized void putThumbnail(ID_TYPE pictureRowId, Bitmap bitmap){
    	if (bitmap == null)
    		return;
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
    public synchronized void putFullPicture(ID_TYPE pictureRowId, Bitmap bitmap){
    	if (bitmap == null)
    		return;
    	
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
     * Clear the bitmaps at the given key
     * @param pictureRowId
     */
    protected synchronized void clearCacheAt(ID_TYPE pictureRowId){
    	cache.remove(pictureRowId);
    }

    /**
     * Clear the cache
     */
    public void clear() {
    	
        cache.clear();
    }
    
    /**
     * Recycle all bitmaps in cache and clear the cache.
     * This should only be used when it's known the bitmaps will no longer be used
     */
    public void recycleBitmapsAndClearCache(){
    	Set<ID_TYPE> keys = cache.keySet();
    	Iterator<ID_TYPE> it = keys.iterator();
    	while(it.hasNext()){
    		ID_TYPE id = it.next();
    		Bitmap full = getFullPicture(id);
    		if (full != null)
    			full.recycle();
    		Bitmap thumb = getThumbnail(id);
    		if (thumb != null)
    			thumb.recycle();
    	}
    	cache.clear();
    }
    
    /**
     * Get the thumbnail at the picture rowId
     * @param pictureRowId the picture rowId
     * @return the thumbnail bitmap stored in this location, or null if none
     */
    public synchronized Bitmap getThumbnail(ID_TYPE pictureRowId){
    	// if no key, then just return null
    	if (!cache.containsKey(pictureRowId))
    		return null;
    	
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// now return the actual bitmap
    	if (data != null && data.mObject1 != null){
    		Bitmap bmp = data.mObject1.get();
    		if (bmp!= null && bmp.isRecycled())
    			return null;
    		else
    			return bmp;
    	}else
    		return null;   	
    }
    
    /**
     * Get the full picture at the picture rowId
     * @param pictureRowId the picture rowId
     * @return the full picture bitmap stored in this location, or null if none
     */
    public synchronized Bitmap getFullPicture(ID_TYPE pictureRowId){
    	// if no key, then just return null
    	if (!cache.containsKey(pictureRowId))
    		return null;
    	
    	// get the map object
    	TwoObjects<SoftReference<Bitmap>, SoftReference<Bitmap>> data = cache.get(pictureRowId);
    	
    	// now return the actual bitmap
    	if (data != null && data.mObject2 != null){
    		Bitmap bmp = data.mObject2.get();
    		if (bmp!= null && bmp.isRecycled())
    			return null;
    		else
    			return bmp;
    	}else
    		return null;
    	
    }
}