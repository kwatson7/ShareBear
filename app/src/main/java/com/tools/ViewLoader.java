package com.tools;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.view.View;

/**
 * This asynchroniously will load data and assign it to a view of your choosing. It also stored data in memory,
 * so we don't have to load it again if its needed again. It stores it as soft references, so we don't have
 * to wory about out of memory errors.
 * @author Kyle
 *
 * @param <ID_TYPE> The type that identifies what view we are on. For example, the row in a cursor
 * @param <LOOKUP_TYPE> The lookup that is required to find the data of interest
 * @param <VALUE_TYPE> The data of interest
 * @param <VIEW_TYPE> The type of view we will be assigning data to
 */
public class ViewLoader<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE extends View>{

	// private variables
	private MemoryCacheGeneric<ID_TYPE, VALUE_TYPE> memoryCache = new 
	MemoryCacheGeneric<ID_TYPE, VALUE_TYPE>(); 									// This stores the data in memory
	private Map<VIEW_TYPE, ID_TYPE> views =
		Collections.synchronizedMap(new WeakHashMap<VIEW_TYPE, ID_TYPE>()); 	// keeps track of links between views and data	
	private ExecutorService executorService;  									// run the threads
	private final VALUE_TYPE defaultValue;	 									// The default data to show while there is none
	private LoadData<LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> loadDataCallback;		// callback to load data
	private HashMap<ID_TYPE, Object> dataLoadingLocks = 
		new HashMap<ID_TYPE, Object>();											// locks to be used when loading data, so we don't load the same data twice

	// constants
	private static final int MAX_THREADS = 15; 									// max threads to spawn
	private static final long REQUIRED_BYTES = 1000000; 						// we must have this many bytes or we will clear the cache

	/**
	 * Create a view loader that asynchonously loads data and assigns to views. <br>
	 * See stopThreads and restartThreads
	 * @param defaultValue The default value to use when there is no data yet available
	 * @param callback Callback used to load the data and to bind to views
	 */
	public ViewLoader(
			VALUE_TYPE defaultValue,
			LoadData<LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> callback){

		executorService = Executors.newFixedThreadPool(MAX_THREADS);
		this.defaultValue = defaultValue;
		this.loadDataCallback = callback;
	}
	
	/**
	 * Create a view loader that asynchonously loads data and assigns to views. <br>
	 * See stopThreads and restartThreads
	 * @param defaultValue The default value to use when there is no data yet available
	 * @param maxThreads maximum threads to use. 15 is the default
	 * @param callback Callback used to load the data and to bind to views
	 */
	public ViewLoader(
			VALUE_TYPE defaultValue,
			int maxThreads,
			LoadData<LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> callback){

		executorService = Executors.newFixedThreadPool(maxThreads);
		this.defaultValue = defaultValue;
		this.loadDataCallback = callback;
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
	 * Launch async runnable to show the data
	 * @param dataId The id that identifies this unique data
	 * @param lookupData The key required to find / create the data
	 * @param view The view to put the data
	 */
	public void DisplayView(
			ID_TYPE dataId,
			LOOKUP_TYPE lookupData,
			VIEW_TYPE view)
	{

		// create the object containing all the relevant data
		DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> data = new
		DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE>(
				dataId,
				lookupData,
				view);

		// store the links
		views.put(view, dataId);

		// attempt to access cached data
		VALUE_TYPE valueToBind = memoryCache.getData(dataId);

		// no data picture, so queue the loader
		if (valueToBind == null)
			queueData(data);

		// see if we have a bitmap to access
		if(valueToBind != null)
			loadDataCallback.onBindView(valueToBind, view);

		// otherwise just show the default image
		else
			loadDataCallback.onBindView(defaultValue, view);

	}

	/**
	 * Return the memory cache.<br>
	 * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
	 * for example on orientation changes *****
	 * @return
	 */
	public MemoryCacheGeneric<ID_TYPE, VALUE_TYPE> getMemoryCache(){
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
	public void restoreMemoryCache(MemoryCacheGeneric<ID_TYPE, VALUE_TYPE> mem){
		if (memoryCache != null)
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
	 * Add this data to the load queue
	 * @param data The data to queue up
	 */
	private void queueData(DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> data)
	{
		if (executorService != null)
			executorService.submit(new DataLoader(data));
	}

	/**
	 * Check if this view is being re-used
	 * @param dataToLoad
	 * @return boolean if true
	 */
	boolean viewReused(DataToLoad <ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> dataToLoad){
		VIEW_TYPE view = dataToLoad.viewSoftReference.get();
		if (view == null)
			return true;
		ID_TYPE rowId = views.get(view);
		if(rowId==null || !dataToLoad.dataId.equals(rowId))
			return true;
		return false;
	}

	/**
	 * Used to grab data for generic identifiers of generic data
	 * @author Kyle
	 *
	 * @param <KEY_TYPE> what identifies what data element we are on
	 * @param <VALUE_TYPE> What type of data we will return
	 * @param <VIEW_TYPE> The type of view we will be assigning data to
	 */
	public interface LoadData <KEY_TYPE, VALUE_TYPE, VIEW_TYPE>{
		/**
		 * Load in the data.
		 * @param key The key needed to get the picture
		 * @return The bitmap, or null if unsuccessful
		 */
		public VALUE_TYPE onGetData(KEY_TYPE key);

		/**
		 * Bind the data to the view on the ui thread
		 * @param data The data to bind
		 * @param the view to bind it to (will never be null)
		 */
		public void onBindView(VALUE_TYPE data, VIEW_TYPE view);
	}

	//Used to display bitmap in the UI thread
	private class DataDisplayer
	implements Runnable
	{
		VALUE_TYPE value;
		DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> dataToLoad;

		public DataDisplayer(
				VALUE_TYPE data,
				DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> dataToLoad){

			value = data;
			this.dataToLoad = dataToLoad;
		}
		public void run()
		{
			if(viewReused(dataToLoad))
				return;
			VIEW_TYPE view = dataToLoad.viewSoftReference.get();
			if (view == null)
				return;
			if(value != null)
				loadDataCallback.onBindView(value, view);
			else
				loadDataCallback.onBindView(defaultValue, view);
		}
	}

	private class DataLoader
	implements Runnable {
		DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> dataToLoad;

		DataLoader(
				DataToLoad<ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE> dataToLoad){
			this.dataToLoad = dataToLoad;
		}

		@Override
		public void run() {
			// this is a recycle view, so don't do anything
			if(viewReused(dataToLoad))
				return;

			// create a lock to control access to this thread for this object
			synchronized (ViewLoader.this) {
				if (dataLoadingLocks.get(dataToLoad.dataId) == null)
					dataLoadingLocks.put(dataToLoad.dataId, new Object());
			}

			// synchronize access to only allow for each picture single access to this block, so as not to allow mutliple grabs of the same file
			synchronized (dataLoadingLocks.get(dataToLoad.dataId)) {

				// load the data first from memory
				VALUE_TYPE data = memoryCache.getData(dataToLoad.dataId);
				if (data == null)
					data = loadDataCallback.onGetData(dataToLoad.lookupData);
				if (data != null)
					memoryCache.putData(dataToLoad.dataId, data);

				// recycled view
				if(viewReused(dataToLoad))
					return;

				// load the data on the ui thread
				if (data != null){
					DataDisplayer bd = new DataDisplayer(data, dataToLoad);
					View view = dataToLoad.viewSoftReference.get();
					if (view != null){
						Activity a=(Activity)view.getContext();
						a.runOnUiThread(bd);
					}
				}
			}
		}
	}

	//Task for the queue
	private static class DataToLoad <ID_TYPE, LOOKUP_TYPE, VALUE_TYPE, VIEW_TYPE>
	{
		public ID_TYPE dataId;
		public LOOKUP_TYPE lookupData;
		public SoftReference<VIEW_TYPE> viewSoftReference;
		public DataToLoad(
				ID_TYPE dataId,
				LOOKUP_TYPE lookupData,
				VIEW_TYPE view){
			this.lookupData = lookupData;
			this.dataId = dataId;
			viewSoftReference = new SoftReference<VIEW_TYPE>(view);
		}
	}
}
