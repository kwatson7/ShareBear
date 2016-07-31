package com.tools;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.widget.ProgressBar;

public class DownloadFile {

	// constants
	private static final int BUFFER_SIZE = 1024; 							// buffer size for downloading files
	private static final String LOG_TAG = "DownloadFile"; 					// The log tag
	private static final int SKIP_EVERY_N_ON_PUBLISH = 5;  					// MUSTN'T BE negative or 0

	// class members
	private String url; 													// the url download file from

	/**
	 * Create a DownloadFile object
	 * @param url the url to download file from
	 */
	public DownloadFile(String url){
		// store inputs
		this.url = url;
	}

	/**
	 * Download the file and store it to the given filename on a background thread
	 * @param act The calling activity
	 * @param saveFilePath The path to save the file to
	 * @param showDialog should we show a dialog? If the dialog is showing, and the user hits back, the download will cancel
	 * @param progressBars A string array of id identifiers for progress bars that will update as the file downloads
	 * @param callback A callback to call when we are finished downloading. Use a static class to avoid activity leak.
	 */
	public <ACTIVITY_TYPE extends CustomActivity>
	void downloadFileBackground(
			ACTIVITY_TYPE act,
			String saveFilePath,
			boolean showDialog,
			ArrayList<String> progressBars,
			GetFileCallback<ACTIVITY_TYPE> callback){

		// create the task
		DownloadFileAsync<ACTIVITY_TYPE> task = new DownloadFileAsync<ACTIVITY_TYPE>(
				act,
				saveFilePath,
				showDialog,
				progressBars,
				callback);
		
		// execute it
		task.execute();		
	}

	/**
	 * Download a file from the given url <br>
	 * Call on background thread as it is slow
	 * @param saveFilePath The local path to save the file
	 * @return True if we downloaded successfully and false otherwise. If false, error logs are written
	 */
	public boolean downloadFile(String saveFilePath){
		return downloadFile(saveFilePath, null);
	}
	
	/**
	 * Download a file from the given url <br>
	 * Call on background thread as it is slow
	 * @param saveFilePath The local path to save the file
	 * @param callback This is used to show the progress as we download the file and check if we should cancel
	 * @return True if we downloaded successfully and false otherwise. If false, error logs are written
	 */
	private boolean downloadFile(String saveFilePath, PublishFileProgress callback){

		// initialize some variables
		OutputStream output = null;
		InputStream input = null;
		
		// wrap in to try catch, so we can perform cleanup
		try{
			
			// write the required folders
			com.tools.Tools.writeRequiredFolders(saveFilePath);
			
			// make sure the save file path is accessible
			output = new FileOutputStream(saveFilePath);

			// open the url connection
			URL url2 = new URL(url);
			HttpURLConnection  connection = (HttpURLConnection) url2.openConnection();
			connection.connect();
			
			// this will be useful so that you can show a typical 0-100% progress bar
			int fileLength = connection.getContentLength();

			// download the file
			input = new BufferedInputStream(url2.openStream());

			// setup for downloading
			byte data[] = new byte[BUFFER_SIZE];
			int count;
			long total = 0;
			int i = 0;

			// write in buffered increments
			while ((count = input.read(data)) != -1) {
				if (callback != null && callback.shouldWeCancel())
					return false;
				
				// publishing the progress....
				total += count;
				if (SKIP_EVERY_N_ON_PUBLISH <= 0 || i % SKIP_EVERY_N_ON_PUBLISH == 0 && callback != null)
					callback.onProgress((int) (total * 100 / fileLength));
				i++;
				
				output.write(data, 0, count);
			}
		}catch(FileNotFoundException e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return false;
		} catch (IOException e) {
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return false;
		}finally{
			
			// perform cleanup
			if (output != null){
				try{ 
					output.flush();
				}catch(Exception e){
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
			}
			if (output != null){
				try{ 
					output.close();
				}catch(Exception e){
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
			}
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, Log.getStackTraceString(e));
				return true;
			}
		}

		// successful
		return true;
	}

	/**
	 * class used to post to server in the background
	 */
	private class DownloadFileAsync <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Integer, Boolean>{

		// member variables
		private GetFileCallback<ACTIVITY_TYPE> callback;
		private String saveFilePath;
		private boolean showDialog;
		private boolean cancelTask = false;

		// constants
		private static final String DIALOG_TITLE = "Downloading File";
		private static final String DIALOG_MESSAGE = "Downloading...";

		/**
		 * Download a file on a backgroudn thread
		 * @param act The activity to call task
		 * @param saveFilePath The file to save to
		 * @param showDialog Should we show a progress dialog?
		 * @param progressBars Progress bars to update (The string identifiers). Null if none
		 * @param callback The callback to call when we are done downloading. Can be null
		 */
		private DownloadFileAsync(
				ACTIVITY_TYPE act,
				String saveFilePath,
				boolean showDialog,
				ArrayList<String> progressBars,
				final GetFileCallback<ACTIVITY_TYPE> callback) {
			super(
					act,
					-1,
					false,
					true,
					progressBars);
			this.callback = callback;
			this.saveFilePath = saveFilePath;
			this.showDialog = showDialog;
			attach(act);
		}

		@Override
		protected void onPreExecute() {			
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			//boolean result = downloadFileHelper(saveFilePath);
			boolean result = downloadFile(saveFilePath, new PublishFileProgress() {
				
				@Override
				public boolean shouldWeCancel() {
					// check if we should cancel
					return (cancelTask || isCancelled());
				}
				
				@Override
				public void onProgress(int percentComplete) {
					publishProgress(percentComplete);	
				}
			});
			
			if (callback != null)
				callback.onPostFinished(callingActivity, result, saveFilePath);
			return result;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {	
			// set progress for dialog
			if (showDialog && dialog != null)
				dialog.setProgress(progress[0]);

			// fill progress bars
			if (progressBars != null){
				for (int i = 0; i < progressBars.size(); i++){
					ProgressBar bar = getProgressBar(i);
					if (bar == null)
						continue;
					bar.setMax(100);
					bar.setProgress(progress[0]);
				}
			}
		}

		@Override
		protected void onPostExectueOverride(Boolean result) {
			if (callback != null)
				callback.onPostFinishedUiThread(callingActivity, result, saveFilePath);
		}

		@Override
		protected void setupDialog() {
			if (!showDialog)
				return;

			// show dialog for this long process
			if (callingActivity != null){
				dialog = new ProgressDialog(callingActivity);
				dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				dialog.setTitle(DIALOG_TITLE);
				dialog.setMessage(DIALOG_MESSAGE);
				dialog.setIndeterminate(false);
				dialog.setCancelable(true);
				dialog.setMax(100);
				dialog.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						cancelTask = true;
					}
				});
			}
		}
	}

	public interface GetFileCallback <ACTIVITY_TYPE extends CustomActivity>{
		/**
		 * This is called when we are done downloading the file from the server on the background thread
		 * @param the activity that is currently active after the task completed
		 * @param result True if we were successful, false otherwise
		 * @param fileName The filename we attempted to save to
		 */
		public void onPostFinished(ACTIVITY_TYPE act, boolean result, String fileName);

		/**
		 * This is called when we are done downloading the file from the server on the ui thread
		 * @param the activity that is currently active after the task completed
		 * @param result True if we were successful, false otherwise
		 * @param fileName The filename we attempted to save to
		 */
		public void onPostFinishedUiThread(ACTIVITY_TYPE act, boolean result, String fileName);
	}
	
	private interface PublishFileProgress{
		/**
		 * This will run on the background thread as we download the file
		 * @param percentComplete The percent complete the file has downloaded
		 */
		public void onProgress(int percentComplete);
		/**
		 * If there are any values you want to interogate that determine if we should cancel the download
		 * @return true to cancel, false to not cancel
		 */
		public boolean shouldWeCancel();
	}
}
