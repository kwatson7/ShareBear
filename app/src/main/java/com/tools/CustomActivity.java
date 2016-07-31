package com.tools;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

/**
 * Custom activity that forces some configurations to better deal with configuration changes and asynctasks. <p>
 * Make sure to add any CustomAsyncTask to asyncArray when created, so the code knows to detach and attach properly.
 * @author Kyle
 *
 */
public abstract class CustomActivity
extends Activity{

	/**
	 * The context of this activity
	 */
	protected final Context ctx = this;
	
	/** The instance of the configuration properties */
	protected ConfigurationProperties configurationProperties = null;
	
	/** The list of async tasks, that will be automatically attached and detached to activity on configruation chagnes */
	private ArrayList<CustomAsyncTask> asyncArray = 
		new ArrayList<CustomAsyncTask>();
	
	/**
	 * Add a task to the list of async tasks
	 * that will be automatically attached and detached to activity
	 * on configuration changes
	 * @param task
	 */
	protected void addTask(CustomAsyncTask task){
		asyncArray.add(task);
	}
	
	/** The list of dialogs that you want destroyed when activity is destroyed */
	private ArrayList<Dialog> dialogArray = new ArrayList<Dialog>();
	
	/**
	 * Add a dialog to the list of dialogs to be destroyed when activity is destroyed
	 * @param dialog
	 */
	protected void addDialog(Dialog dialog){
		dialogArray.add(dialog);
	}

	/**
	 * Indicate where in the asynctask we are located. Used in onAsyncExecute for an attached asynctask
	 * @author Kyle
	 *
	 */
	public static enum AsyncTypeCall {
		PRE, PROGRESS, POST	}
	
	@Override
	public final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// load in configuration properties
		configurationProperties = (ConfigurationProperties) getLastNonConfigurationInstance();
		
		// now the users overriden onCreate
		onCreateOverride(savedInstanceState);
		
		// attach tasks to this and dialog
		if (configurationProperties != null){
			ArrayList<CustomAsyncTask> array = configurationProperties.asyncArrayConfig;
			if (array != null){
				ArrayList<Integer> toRemove = new ArrayList<Integer>();
				for (int i = 0; i < array.size(); i++){
					CustomAsyncTask task = array.get(i);
					if (task != null){
						if (task.isFinished() || task.isCancelled()){
							task.detach();
							task = null;
							toRemove.add(i);
						}else
							task.attach(this);
					}else
						toRemove.add(i);
				}
				
				// remove list
				Collections.sort(toRemove, Collections.reverseOrder());
				for (int i : toRemove)
				    array.remove(i);
					
			}
			asyncArray = array;
		}
	}
	
	/**
	 * This is the onCreate that is overridden. Not the standard onCreate. configurationProperties is an instance of ConfigurationProperties
	 * and is loaded prior to calling this function.
	 * @param savedInstanceState
	 */
	protected abstract void onCreateOverride(Bundle savedInstanceState);
	
	
	/**
	 * Allow access to this method from outside class. Used to call inside public AsyncTasks' ui methods
	 * Companion method is onAsync
	 * @param requestCode 		Integer code to signify which async task made this call
	 * @param asyncTypeCall 	Enum type, showing where in async this call took place
	 * @param data 				Any data that needs to be passed in to the calling activity
	 */
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall, Object data){}
	
	/**
	 * Should call within onCreateOverride or anywhere else the layout needs to be initialized.
	 */
	protected abstract void initializeLayout();
	
	/**
	 * Create a class that is passed out on onRetainNonConfigurationInstance
	 *
	 */
	protected static class ConfigurationProperties{
		protected ArrayList<CustomAsyncTask> asyncArrayConfig;
		public Object customData;
	}
	
	@Override
	public final Object onRetainNonConfigurationInstance(){
		detachAndStoreAsyncs();
		additionalConfigurationStoring();
		return configurationProperties;
	}
	
	/**
	 * This is called in onRetainNonConfigurationInstance. Make sure configurationProperties is setup properly
	 * @return
	 */
	private void detachAndStoreAsyncs(){
		
		// detach task from activity 
		if (asyncArray != null){
			for (com.tools.CustomAsyncTask task : asyncArray)
				if (task != null)
					task.detach();
		}
		
		// save everything needed in this object
		configurationProperties = new ConfigurationProperties();
		configurationProperties.asyncArrayConfig = asyncArray;
	}
	
	/**
	 * Additional work that is to be done on the configurationProperties property in CustomActivity
	 * after asyncs have been detached and stored. Add data to customData in configurationProperties.
	 */
	protected abstract void additionalConfigurationStoring();
	
	@Override
	protected final void onDestroy(){
		cancelAsyncs();
		dismissDialogs();
		onDestroyOverride();
		super.onDestroy();
	}
	
	/**
	 * If this subclass wants to override onDestroy, do it here. The CustomActivity class cancels the attached tasks and
	 * calls super.onD
	 */
	protected abstract void onDestroyOverride();
	
	private void cancelAsyncs(){
		
		// close dialogs
		if (asyncArray != null){
			for (com.tools.CustomAsyncTask task : asyncArray)
				if (task != null)
					if (task.dialog != null)
						try{
							task.dialog.dismiss();
						}catch (Exception e){}


		}
		
		// cancel the attached asyncs
		if (isFinishing()){	
			if (asyncArray != null){
				for (com.tools.CustomAsyncTask task : asyncArray)
					if (task != null){
						if (task.isCancelOnActivityDestroy())
							task.cancel(true);
						if (task.dialog != null)
							try{
								task.dialog.dismiss();
							}catch (Exception e){}
					}
				
			}
		}
	}
	
	private void dismissDialogs(){
		if (dialogArray == null)
			return;
		for (Dialog item : dialogArray){
			if (item != null && item.isShowing())
				item.dismiss();
		}
	}
}