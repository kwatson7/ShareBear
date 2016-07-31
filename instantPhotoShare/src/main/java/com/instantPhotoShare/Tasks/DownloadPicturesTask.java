package com.instantPhotoShare.Tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

public class DownloadPicturesTask <ACTIVITY_TYPE extends CustomActivity> extends
CustomAsyncTask<ACTIVITY_TYPE, Integer, DownloadPicturesTask.DownloadOutput>{
	
	private HashSet<Long> picturesSelected;
	private long groupId;
	private ArrayList<Uri> imageUris;
	
	public DownloadPicturesTask(
			ACTIVITY_TYPE act,
			int requestId,
			boolean isCancelOnActivityDestroy,
			ArrayList<String> progressBars,
			HashSet<Long> picturesSelected,
			long groupId) {
		
		super(act, requestId,
				true,
				isCancelOnActivityDestroy,
				progressBars);
		
		this.picturesSelected = picturesSelected;
		this.groupId = groupId;
		if (dialog != null)
			dialog.setMax(picturesSelected.size());
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (dialog != null && dialog.isShowing())
			dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExectueOverride(DownloadOutput result) {
		
	}

	@Override
	protected void setupDialog() {	
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Downloading Pictures");
			dialog.setMessage("Please Wait...");
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			if (isCancelOnActivityDestroy()){
				dialog.setCancelable(true);
				dialog.setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						DownloadPicturesTask.this.cancel(true);
					}
				});
			}
			if (picturesSelected != null)
				dialog.setMax(picturesSelected.size());
		}
	}
	
	@Override
	protected void onCancelled(){
		onPostExecute(new DownloadOutput(true, imageUris.size(), picturesSelected.size(), imageUris));
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected DownloadOutput doInBackground(Void... params) {
		
		// initialize ur list and pictures adapter
		imageUris = new ArrayList<Uri>(picturesSelected.size());
		PicturesAdapter pics = new PicturesAdapter(applicationCtx);
		
		// loop over pictures add add their files to uri list
		Iterator<Long> it = picturesSelected.iterator();
		int i = 0;
		while(it.hasNext()){
			
			// task was cancelled
			if (this.isCancelled())
				return new DownloadOutput(true, imageUris.size(), picturesSelected.size(), imageUris);
			
			// grab the picture at the given rowId
			long rowId = it.next();
			pics.fetchPicture(rowId);
			
			// which path to use.
			String pathToUse = pics.getFullPicturePath();

			// make sure we have the picture first
			Bitmap bmp = pics.getFullImage(10, 10);

			// no full size, download it
			if (bmp == null){

				// no server id, so can't download try thumb
				if (pics.getServerId() == 0 || pics.getServerId() == -1){
					bmp = pics.getThumbnail();
					if (bmp != null){
						pathToUse = pics.getThumbnailPath();
					}else{
						continue;
					}
				}else{

					// now download full
					bmp = PicturesAdapter.getFullImageServer(applicationCtx, rowId, 10, 10, groupId, null);
					if (bmp == null){
						continue;
					}
					pics.fetchPicture(rowId);
					pathToUse = pics.getFullPicturePath();
				}
			}

			// add to list
			File file = new File(pathToUse);
			Uri uri = Uri.fromFile(file);
			imageUris.add(uri);
			i++;
			publishProgress(i);
		}

		pics.close();
		
		return new DownloadOutput(false, imageUris.size(), picturesSelected.size(), imageUris);
	}
	
	public static class DownloadOutput {
		public boolean cancelled = false;
		public int successfulPictures;
		public int attemptedPictures;
		public ArrayList<Uri> imageUris;
		
		public DownloadOutput(boolean cancelled, int successfulPictures, int attemptedPictures, ArrayList<Uri> imageUris) {
			this.cancelled = cancelled;
			this.successfulPictures = successfulPictures;
			this.attemptedPictures = attemptedPictures;
			this.imageUris = imageUris;
		}
	}
	
}