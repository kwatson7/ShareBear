package com.tools;

import java.util.ArrayList;

import com.tools.CustomActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * custom async task that should be used in conjuction with customActivity. <p>
 * Take care of orientation changes and callbacks to main ui thread properly <p>
 * Example of how to call main ui thread from inside onPreExecute, onPostExectute, or onProgressUpdate.
 * callingActivity.onAsyncExecute(requestId, com.tools.CustomActivity.AsyncTypeCall.PROGRESS, progress[0]);
 * @author Kyle
 *
 * @param <INPUT>
 * @param <PROGRESS>
 * @param <RESULT>
 */
public abstract class CustomAsyncTask <ACTIVITY_TYPE extends CustomActivity, PROGRESS, RESULT>
	extends AsyncTask<Void, PROGRESS, RESULT>{

	/** The calling activity that this task is attached to	 */
	protected ACTIVITY_TYPE callingActivity;
	/** The application context this task is attached to. Use this if we want the task to last after activity quits. */
	protected Context applicationCtx;
	/**  boolean whether we should attach to application as well (longer lifecycle) */
	protected boolean isAttachToApplication;
	/** The calling id  */
	protected int requestId;
	/** boolean to see if we should cancel the task, when the calling activity is finished */
	private boolean isCancelOnActivityDestroy;
	/** array of progress bar ids. Will automatically start and stop, but user can adjust if they'd like */
	protected ArrayList<String> progressBars = null;
	/** A progressDialog to show and hide when starting and completing task. The showing and hiding is done automatically.*/
	protected ProgressDialog dialog = null;
	/** the callback to run when onPostExectueOverride is done */
	private FinishedCallback<ACTIVITY_TYPE, RESULT> finishedCallback;
	
	/**
	 * Constructor. This task will be added to teh calling activities list of tasks to manage.
	 * @param act the calling activity. Can be null, but loss of functionality
	 * @param requestId A custom id to identify the asynctask to the calling activity
	 * @param isAttachToApplication boolean to keep activity attached to application, not just activity
	 * @param isCancelOnActivityDestroy should the calling activity cancel this task when it is destroyed? Also, inside
	 * the subclassed task, make sure to call isCanceled() periodically in doInBackground, to break.
	 * @param progressBars an array of progress bar identifier string that will be automatically started and stopped at the appropriate time
	 */
	public CustomAsyncTask(
			ACTIVITY_TYPE act,
			int requestId,
			boolean isAttachToApplication, 
			boolean isCancelOnActivityDestroy,
			ArrayList<String> progressBars) {
		
		// save inputs
		this.isAttachToApplication = isAttachToApplication;
		this.requestId = requestId;
		this.isCancelOnActivityDestroy = isCancelOnActivityDestroy;
		this.progressBars = progressBars;
		
		// save this task to the list of activity tasks
		if (act != null)
			act.addTask(this);
		
		// attach to the activity
		attach(act);	
	}
	
	/**
	 * This callback will be run after onPostExecuteOverride on the UI thread. <br>
	 * @param callback the callback to run.
	 */
	public void setFinishedCallback(FinishedCallback<ACTIVITY_TYPE, RESULT> callback){
		finishedCallback = callback;
	}

	@Override
	protected abstract void onPreExecute();
	
	@Override
	protected abstract RESULT doInBackground(Void... params);

	@Override
	protected abstract void onProgressUpdate(PROGRESS... progress);

	@Override
	protected void onPostExecute(RESULT result) {
		
		// close the attached dialog
		hideDialog();
            
		// users method, then detach
		onPostExectueOverride(result);
		
		// the callback if we have one
		if (finishedCallback != null){
			finishedCallback.onFinish(callingActivity, result);
			finishedCallback = null;
		}
		
		// detach from calling context to free the link
		detachAll();
	}
	
	@Override
	protected void onCancelled() {
		
		// close the attached dialog
		hideDialog();
		
		// detach from calling context to free the link
		detachAll();
	}
	
	/**
	 * This should be overriden in place of onPostExecute. This is called inside onPostExecute, and then after detachAll() is called.
	 * @param result
	 */
	protected abstract void onPostExectueOverride(RESULT result);

	/**
	 * Set the context for the UI interface that is currently in use. Use the command:<p>
	 * *@Override <br>
     *public Object onRetainNonConfigurationInstance() { <br>
     *   myAsyncTask.detach();<br>
     *   return myAsyncTask;} <br>
     *   And in onCreate(): <br>
     *   if( (myAsyncTask = <br>
	 * (MyAsyncTask)getLastNonConfigurationInstance()) != null) <br>
     *           myAsyncTask.attach(this);  // Give my AsyncTask the new Activity reference} <br>
	 * @param ctx The calling context
	 */
	public void attach(ACTIVITY_TYPE act){
		callingActivity = act;
		if (this.isAttachToApplication && act != null)
			attachToApplicationProcess(act);
		int progress = 0;
		if (dialog != null)
			progress = dialog.getProgress();
		setupDialog();
		showDialog();
		if (dialog != null)
			dialog.setProgress(progress);
	}
	
	/**
	 * Attach to the application context. That way the task can keep running even when activity is destroyed. 
	 * Currently private, but is forseable to make this public
	 * @param act
	 */
	private void attachToApplicationProcess(Activity act){
		if (act != null)
			applicationCtx = act.getApplicationContext();
	}
	
	/**
	 * Detach the calling activity from the async task. Use the command:<p>
	 * *@Override <br>
     * public Object onRetainNonConfigurationInstance() { <br>
     *   myAsyncTask.detach(); <br>
     *   return myAsyncTask;} <br> 
     *   And in onCreate(): <br>
     *   if( (myAsyncTask = <br>
	 * (MyAsyncTask)getLastNonConfigurationInstance()) != null) <br>
     *           myAsyncTask.attach(this);  // Give my AsyncTask the new Activity reference} <br>
	 */
	public final void detach(){
		callingActivity = null;
	}
	
	/**
	 * Detach from both calling activity and application context. Used when task is finished
	 */
	private final void detachAll(){
		callingActivity = null;
		applicationCtx = null;
	}
	
	/**
	 * Let any outside activities know that we are now finished.
	 * @return
	 */
	final public boolean isFinished(){
		return (this.getStatus() == AsyncTask.Status.FINISHED);
	}
	
	/**
	 * Setup a dialog that you want shown during this asynctask. A typical setup is:<p>
	 * if (callingActivity != null){<br>
	 *		dialog = new ProgressDialog(callingActivity);<br>
	 *		dialog.setTitle("Creating Account");<br>
	 *		dialog.setMessage("Please Wait");<br>
	 *		dialog.setIndeterminate(true);<br>
	 *	}<p>
	 *or<p>
	 *dialog = null; // for no dialog
	 */
	protected abstract void setupDialog();
	
	/**
	 * Show the dialog that is not null
	 */
	private void showDialog(){
		if (dialog != null)
			dialog.show();
		
		// progress bars
		if (progressBars != null && callingActivity != null){
			for (String item : progressBars){
				if (item != null){
					try{
						int id = callingActivity.getResources().getIdentifier(item, "id", callingActivity.getPackageName());
						if (id != 0){
							ProgressBar bar = (ProgressBar) callingActivity.findViewById(id);
							if (bar != null)
								bar.setVisibility(ProgressBar.VISIBLE);
						}
					}catch (Exception e){
						Log.d("CustomAsyncTask", Log.getStackTraceString(e));
					}
				}
			}
		}
	}
	
	/**
	 * Hide the dialog that is not null
	 */
	private void hideDialog(){
		if (dialog != null)
            dialog.hide();
		
		// progress bars
		if (progressBars != null && callingActivity != null){
			for (String item : progressBars){
				if (item != null){
					try{
						int id = callingActivity.getResources().getIdentifier(item, "id", callingActivity.getPackageName());
						if (id != 0){
							ProgressBar bar = (ProgressBar) callingActivity.findViewById(id);
							if (bar != null)
								bar.setVisibility(ProgressBar.GONE);
						}
					}catch (Exception e){
						Log.e("CustomAsyncTask", Log.getStackTraceString(e));
					}
				}
			}
		}
	}
	
	/**
	 * Get the progress bar at the given index (if we input an array of progressbar identifiers). 
	 * This will always grab a progress bar from the current activity.
	 * @param i The index to grab
	 * @return The progressbar or null if none could be found.
	 */
	protected ProgressBar getProgressBar(int i){
		// make sure we have an activity and progress bars
		if (callingActivity == null || progressBars == null)
			return null;
		
		// initialize output
		ProgressBar bar = null;
		
		// try to grab the id
		try{
			String item = progressBars.get(i);
			int id = callingActivity.getResources().getIdentifier(item, "id", callingActivity.getPackageName());
			if (id != 0){
				bar = (ProgressBar) callingActivity.findViewById(id);
			}
		}catch (Exception e){
			Log.e("CustomAsyncTask", Log.getStackTraceString(e));
		}
		
		return bar;
	}
	
	/**
	 * Send object back to calling activity. This will appear in onAsyncExecute.
	 * If calling activity is null, then nothing will occur.
	 * Only call this from onPreExecute.
	 * @param object The object to send to calling activity
	 */
	protected void sendObjectToActivityFromPreExecute(Object object){
		if (callingActivity != null)
			callingActivity.onAsyncExecute(requestId, com.tools.CustomActivity.AsyncTypeCall.PRE, object);
	}
	
	/**
	 * Send object back to calling activity. This will appear in onAsyncExecute.
	 * If calling activity is null, then nothing will occur.
	 * Only call this from onProgress.
	 * @param object The object to send to calling activity
	 */
	protected void sendObjectToActivityFromProgress(Object object){
		if (callingActivity != null)
			callingActivity.onAsyncExecute(requestId, com.tools.CustomActivity.AsyncTypeCall.PROGRESS, object);
	}
	
	/**
	 * Send object back to calling activity. This will appear in onAsyncExecute.
	 * If calling activity is null, then nothing will occur.
	 * Only call this from onPostExecute.
	 * @param object The object to send to calling activity
	 */
	protected void sendObjectToActivityFromPostExecute(Object object){
		if (callingActivity != null)
			callingActivity.onAsyncExecute(requestId, com.tools.CustomActivity.AsyncTypeCall.POST, object);
	}
	
	/**
	 * 	boolean to see if we should cancel the task, when the calling activity is finished. CustomActivity takes care of this.
	 * @return
	 */
	final public boolean isCancelOnActivityDestroy(){
		return isCancelOnActivityDestroy;
	}
	
	public interface FinishedCallback<ACTIVITY_TYPE, RESULT>{
		/**
		 * Runs on the UI thread after onPostExectueOverride. <br>
		 * *** NOTE: do not reference member variables of enclosing activity if this is an anonymous call, or the activity will leak.
		 * Use the activity passed in this method to access those variables, not the variables directly. <br>
		 * eg. activity.memberVariable, not memberVariable directly. *** <br>
		 * @param activity The current activity this activity is attached to. Will work across orientation changes.
		 * @param result The result passed from DoInBackground
		 */
		public void onFinish(ACTIVITY_TYPE activity, RESULT result);
	}
}