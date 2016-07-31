package com.tools;

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
public class MemoryCacheGeneric <ID_TYPE, VALUE_TYPE> {
	
	// private variables
    private HashMap<ID_TYPE, SoftReference<VALUE_TYPE>> cache = 
    	new HashMap<ID_TYPE, SoftReference<VALUE_TYPE>>(); 			// Hashmap holding soft reference to data
    
    /**
     * store the data in this memory cache
     * @param key The key lookup to store the data
     * @param data The data
     */
    public synchronized void putData(ID_TYPE key, VALUE_TYPE data){
    	if (data == null)
    		return;
    	
    	// store in cache
        cache.put(key, new SoftReference<VALUE_TYPE>(data));
    }

    /**
     * Clear the cache
     */
    public synchronized void clear() {
        cache.clear();
    }
    
    /**
     * Get the data at the key value
     * @param key the key for the data to get
     * @return the data stored in this location, or null if none of if the softreference has been cleared
     */
    public synchronized VALUE_TYPE getData(ID_TYPE key){
    	// if no key, then just return null
    	if (!cache.containsKey(key))
    		return null;
    	
    	SoftReference<VALUE_TYPE> ref = cache.get(key);
    	return ref.get();
    }
}