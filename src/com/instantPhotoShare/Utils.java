package com.instantPhotoShare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tools.CustomActivity;
import com.tools.ServerPost;
import com.tools.SuccessReason;
import com.tools.ServerPost.FileType;
import com.tools.ServerPost.ServerReturn;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class Utils {

	// public constants
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final char pathsep = '/';
	public static final String appPath = "Share Bear";
	public static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};
	public static final String BASE_PICTURE_NAME = "shareBearPic";
	public static final String BASE_PICTURE_EXTENSION = ".jpg";
	public static final String PICTURE_PATH = "pictures/";
	public static final String THUMBNAIL_PATH = "thumbnails/";
	public static final long SECONDS_SINCE_UPDATE_RESET = 30;
	public static final String DELIM = ",";
	public static final int MAX_THUMBNAIL_DIMENSION = 128;
	public static final boolean FORCE_BASE2_THUMBNAIL_RESIZE = false;
	public static final int PICTURE_ALPHA = 100;
	public static final int BACKGROUND_ALPHA = 40;
	public static final String LOG_TAG = "ShareBear";
	public static int IMAGE_QUALITY = 90;

	// keys for sending to server
	/** The key for the data to post to server */
	private static final String KEY_DATA = "data";
	/** The key for the action to take on server */
	private static final String KEY_ACTION = "action";
	/** The key for posting the image data */
	private static final String KEY_FULLSIZE = "fullsize";
	/** key for the thumbnail */
	private static final String KEY_THUMBNAIL = "thumbnail";
	
	// file download errors
	public static final String CODE_SERVER_ERROR = "CODE_SERVER_ERROR";
	public static final String SERVER_ERROR_MESSAGE = "Server error";
			
	public static void clearApplicationData(Context ctx) {
		File cache = ctx.getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
				}
			}
		}
	}

	private static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}

	public static void CopyStream(InputStream is, OutputStream os)
	{
		final int buffer_size=1024;
		try
		{
			byte[] bytes=new byte[buffer_size];
			for(;;)
			{
				int count=is.read(bytes, 0, buffer_size);
				if(count==-1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch(Exception ex){}
	}

	/**
	 * Get the current date and time formatted as "yyyy-MM-dd HH:mm:ss"
	 * @return The time date string
	 */
	public static String getNowTime(){
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT); 
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	/**
	 * Get the current date with the given millisecondsAgo and time formatted as "yyyy-MM-dd HH:mm:ss"
	 * For example, if it is currently "2012-06-18 12:54:48" and we enter 3, then output would be "2012-06-18 12:54:45"
	 * @param secondsAgo how long ago we want to look
	 * @return The time date string
	 */
	public static String getTimeSecondsAgo(int secondsAgo){
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT); 
		Date date = new Date();
		long timeAgo = date.getTime() - (long)(secondsAgo*1e3);
		Date agoDate = new Date(timeAgo);
		return dateFormat.format(agoDate);
	}

	/**
	 * Return a string that is the path to top level app picture storage.
	 * @return
	 */
	public static String getExternalStorageTopPath(){

		// grab the path
		File external = Environment.getExternalStorageDirectory();
		String path = external.getAbsolutePath();

		// now put / if not there
		if (path.length() > 1 && path.charAt(path.length()-1) != pathsep)
			path += pathsep;

		// now add this app directory
		path += appPath + pathsep;

		return path;
	}

	/**
	 * Make a full file name from path, name and number. ie path = "sdcard/shareBear/folder1/. number = 5. returns: <br>
	 * "sdcard/shareBear/shareBearPic5.jpg"
	 * @param path
	 * @param number
	 * @return
	 */
	public static String makeFullFileName(
			String path,
			long number){
		return path + BASE_PICTURE_NAME + number + BASE_PICTURE_EXTENSION;
	}

	/**
	 * takes a name and returns an allowable file/directory name. It replaces all non acceptable characters
	 * with _. ie file:name returns file_name
	 * @param name input name
	 * @return The acceptible file name
	 */
	public static String getAllowableFileName(String name){

		String out = name;
		for (int i = 0; i < ILLEGAL_CHARACTERS.length; i++)
			out = out.replace(ILLEGAL_CHARACTERS[i], '_');

		return out;
	}

	/**
	 * Post data to server on a background thread
	 * @param action The action to take
	 * @param jsonData the "data" key json data to post
	 * @param fullFilePath the path to the image file with rotation data stored in exif, null if none
	 * @param rotatedThumbnailData thumbnail data that has been rotated already, null if none
	 * @param progressBar to show progress is we are downloading file data.
	 * @param act The activity that calls the background task, can be null
	 * @param callback callback to run when return from server
	 */
	public static <ACTIVITY_TYPE extends CustomActivity>
	void postToServer(
			String action,
			String jsonData,
			String fullFilePath,
			byte[] rotatedThumbnailData,
			ProgressBar progressBar,
			ACTIVITY_TYPE act,
			com.tools.ServerPost.PostCallback<ACTIVITY_TYPE> callback){

		// make the post
		com.tools.ServerPost post = new ServerPost(Prefs.BASE_URL + Prefs.REQUEST_PAGE);

		// set values
		post.addData(KEY_ACTION, action);
		post.addData(KEY_DATA, jsonData);
		if (rotatedThumbnailData != null)
			post.addFile(KEY_THUMBNAIL, rotatedThumbnailData, FileType.JPEG);
		if (fullFilePath != null && fullFilePath.length() != 0)
			post.addFile(KEY_FULLSIZE, fullFilePath, FileType.JPEG);
		
		// post to server
		post.postInBackground(act, progressBar, callback);
	}
	
	/**
	 * Post data to the server helper
	 * @param action the action to take
	 * @param jsonData the data to post
	 * @param fullFilePath the path to the image file with rotation data stored in exif, null if none
	 * @param rotatedThumbnailData thumbnail data that has been rotated already, null if none
	 * @return the result of the post
	 */
	private static ShareBearServerReturn postToServerHelper(
			String action,
			String jsonData,
			String fullFilePath,
			byte[] rotatedThumbnailData){
				
		// make the post
		com.tools.ServerPost post = new ServerPost(Prefs.BASE_URL + Prefs.REQUEST_PAGE);
		
		// set values
		post.addData(KEY_ACTION, action);
		post.addData(KEY_DATA, jsonData);
		if (rotatedThumbnailData != null)
			post.addFile(KEY_THUMBNAIL, rotatedThumbnailData, FileType.JPEG);
		if (fullFilePath != null && fullFilePath.length() != 0)
			post.addFile(KEY_FULLSIZE, fullFilePath, FileType.JPEG);
		
		// post to server
		ServerReturn result = post.post(null);
		
		// convert to ServerJSON
		return new ShareBearServerReturn(result);
	}
	
	private static ServerPost.BinarayDownloader fileDownloader = new ServerPost.BinarayDownloader() {
		
		@Override
		public SuccessReason readInputStream(
				InputStream inputStream,
				String filePath,
				long dataLength,
				WeakReference<ProgressBar> weakProgress)
				throws IOException {
			
			// if there was a bad input file
			if (filePath == null)
				throw(new FileNotFoundException());
			
			// default output
			SuccessReason defaultOutput = new SuccessReason(false, "Unknown failure");
			
			// write the required directories to the file
			com.tools.Tools.writeRequiredFolders(filePath);

			// initialize some variables
			OutputStream output = null;

			// wrap in try-catch, so we can perform cleanup
			try{
				// make sure the save file path is accessible
				output = new FileOutputStream(filePath);

				// setup for downloading
				byte data[] = new byte[com.tools.Tools.BUFFER_SIZE];
				int count;

				// first read how many bytes are for json
				final int firstBytes = 5;
				byte[] jsonLength = new byte[firstBytes]; 
				count = inputStream.read(jsonLength);
				dataLength += -5;
				
				// if not enough read, then error
				if (count != firstBytes){
					Log.e(Utils.LOG_TAG, "not enough bytes read for json length");	
					return (new SuccessReason(false, SERVER_ERROR_MESSAGE));
				}
				
				// convert jsonLength to an int
				int jsonLengthInt = Integer.parseInt(new String(jsonLength));
				dataLength += -jsonLengthInt;
				
				// now read the json
				byte[] jsonData = new byte[jsonLengthInt];
				count = inputStream.read(jsonData);
				if (count != jsonLengthInt){
					Log.e(Utils.LOG_TAG, "not enout bytes read from json");
					return (new SuccessReason(false, SERVER_ERROR_MESSAGE));
				}
				
				// convert the json bytes to string and serverReturn
				String jsonString = new String(jsonData);
				defaultOutput = new SuccessReason(true, jsonString);
				ShareBearServerReturn tmp = new ShareBearServerReturn(new ServerReturn(jsonString, jsonString));
				
				// bad return so exit
				if (!tmp.isSuccess()){
					return (new SuccessReason(true, jsonString));
				}
				
				// now we read the rest of the data and write to file
				long total = 0;
				final long dataLength2 = dataLength;

				// write in buffered increments
				while ((count = inputStream.read(data)) != -1) {
					output.write(data, 0, count);
					total += count;
					
					// show the progress bar
					final ProgressBar prog = weakProgress.get();
					if (prog != null && dataLength > 0){
						prog.setVisibility(View.VISIBLE);
						Activity act = (Activity)prog.getContext();
						final long total2 = total;
						act.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								prog.setProgress((int) (100 * total2 / dataLength2));
								
							}
						});
					}
				}
				
				// no file read
				if (total == 0){
					Log.e(Utils.LOG_TAG, "no file data read");
					return (new SuccessReason(false, SERVER_ERROR_MESSAGE));
				}
			}finally{

				try{
					final ProgressBar prog = weakProgress.get();
					prog.setVisibility(View.INVISIBLE);
				}catch(Exception e){
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
				
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
					if (inputStream != null)
						inputStream.close();
				} catch (IOException e) {
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
			}
			
			return defaultOutput;
		}
	};
	
	/**
	 * Post data to the server to get a file.
	 * @param action the action to take
	 * @param jsonData the data to post
	 * @param fileSavePath the path we save the returned file to.
	 * @param progressBar a progress bar to post file download data to. null if none
	 * @return the result of the post
	 */
	public static ShareBearServerReturn postToServerToGetFile(
			String action,
			String jsonData,
			String fileSavePath,
			ProgressBar progressBar){
		
		WeakReference<ProgressBar> weakProgress = new WeakReference<ProgressBar>(progressBar);
		progressBar = null;
		
		// write the required folders
		try {
			com.tools.Tools.writeRequiredFolders(fileSavePath);
		} catch (IOException e) {
			ServerReturn result = new ServerReturn();
			result.setError(e);
		}
				
		// make the post
		com.tools.ServerPost post = new ServerPost(Prefs.BASE_URL + Prefs.REQUEST_PAGE);
		
		// set values
		post.addData(KEY_ACTION, action);
		post.addData(KEY_DATA, jsonData);
		post.setSaveFilePath(fileSavePath);
		post.setCustomBinaryDownloader(fileDownloader);
		
		// post to server
		return new ShareBearServerReturn(post.post(weakProgress.get()));
	}

	/**
	 * Post data to the server helper
	 * @param action the action to take
	 * @param data the data to post
	 * @param fullFilePath the path to the image file with rotation data stored in exif, null if none
	 * @param rotatedThumbnailData thumbnail data that has been rotated already, null if none
	 * @return the result of the post
	 */
	static public ShareBearServerReturn postToServer(
			String action,
			JSONObject data,
			String fullFilePath,
			byte[] rotatedThumbnailData){

		return postToServerHelper(action, data.toString(), fullFilePath, rotatedThumbnailData);
	}
	
	/**
	 * Post data to the server helper
	 * @param action the action to take
	 * @param data the data to post
	 * @param fullFilePath the path to the image file with rotation data stored in exif, null if none
	 * @param rotatedThumbnailData thumbnail data that has been rotated already, null if none
	 * @return the result of the post
	 */
	static public ShareBearServerReturn postToServer(
			String action,
			JSONArray data,
			String fullFilePath,
			byte[] rotatedThumbnailData){

		return postToServerHelper(action, data.toString(), fullFilePath, rotatedThumbnailData);
	}

	/**
	 * Set the imageview to have the picture at the given path. <br>
	 * Can be used for group thumbnails, like so:<br>
	 * String picPath = group.getPictureThumbnailPath(ctx)
	 * @param ctx A context needed for various operations
	 * @param picPath The picture we want to laod
	 * @param image The imageView
	 * @param alphaMultiplier The default brightness of image is 1, set this to be anything you want (usually 0 - ~3)
	 */
	public static void setBackground(final Context ctx, final String picPath, final ImageView image, final float alphaMultiplier){
		new Thread(new Runnable() {
			public void run() {

				// read the picture path of the group
				Bitmap bmp = null;

				// if no file, then use the default one
				if (picPath != null && picPath.length() != 0 && (new File(picPath)).exists())
					bmp = BitmapFactory.decodeFile(picPath);
				if (bmp == null)
					bmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.icon3);

				// read the pixel from bitmap
				int height = bmp.getHeight();
				int width = bmp.getWidth();
				int[] pixels = new int[height * width];
				bmp.getPixels(pixels, 0, width, 0, 0, width, height);

				// find the average darkness
				double avg = 0;
				int color;
				for (int i = 0; i < pixels.length; i++){

					color = pixels[i];
					int alpha = Color.alpha(color);
					int red = Color.red(color);
					int green = Color.green(color);
					int blue = Color.blue(color);
					avg += alpha*(0.2989*red + 0.5870*green + 0.1140*blue);
				}
				avg = avg/(pixels.length*255*255);

				// make the drawable
				final BitmapDrawable draw = new BitmapDrawable(bmp);
				draw.setAlpha((int)(BACKGROUND_ALPHA*(alphaMultiplier/avg)));

				// post it on the ui thread
				Activity a=(Activity)image.getContext();
				a.runOnUiThread(new Runnable() {
					public void run() {
						image.setImageDrawable(draw);
					}
				});
			}
		}).start();
	}
	
	/**
	 * Parse milliseconds from a correctly formatted date string
	 * @param date The data string of format Utils.DATE_FORMAT
	 * @return The time in milliseconds
	 * @throws ParseException
	 */
	public static long parseMilliseconds(String date)
			throws ParseException{
		DateFormat formatter = new SimpleDateFormat(Utils.DATE_FORMAT);
		return formatter.parse(date).getTime();
	}
}
