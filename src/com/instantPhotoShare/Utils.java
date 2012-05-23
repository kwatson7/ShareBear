package com.instantPhotoShare;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tools.CustomActivity;
import com.tools.ServerPost;
import com.tools.ServerPost.FileType;
import com.tools.ServerPost.ServerReturn;
import com.tools.TwoObjects;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.widget.ImageView;

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
	public static final long SECONDS_SINCE_UPDATE_RESET = 15;
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
	 * @param jsonData the "data" json data to post
	 * @param imageData thumbnail, full picture data, null if none
	 * @param act The activity that calls the background task, can be null
	 * @param callback callback to run when return from server
	 */
	public static <ACTIVITY_TYPE extends CustomActivity>
	void postToServer(
			String action,
			String jsonData,
			TwoObjects<byte[], byte[]> imageData,
			ACTIVITY_TYPE act,
			com.tools.ServerPost.PostCallback<ACTIVITY_TYPE> callback){

		// make the post
		com.tools.ServerPost post = new ServerPost(Prefs.BASE_URL + Prefs.REQUEST_PAGE);

		// set values
		post.addData(KEY_ACTION, action);
		post.addData(KEY_DATA, jsonData);
		if (imageData != null){
			post.addFile(KEY_FULLSIZE, imageData.mObject1, FileType.JPEG);
			post.addFile(KEY_THUMBNAIL, imageData.mObject2, FileType.JPEG);
		}
		
		// post to server
		post.postInBackground(act, callback);
	}
	
	/**
	 * Post data to the server
	 * @param action the action to take
	 * @param jsonData the data to post
	 * @param imageData the thumbnail, and picture data, null if there is no data
	 * @return the result of the post
	 */
	static private ShareBearServerReturn postToServerHelper(
			String action,
			String jsonData,
			TwoObjects<byte[], byte[]> imageData){
				
		// make the post
		com.tools.ServerPost post = new ServerPost(Prefs.BASE_URL + Prefs.REQUEST_PAGE);
		
		// set values
		post.addData(KEY_ACTION, action);
		post.addData(KEY_DATA, jsonData);
		if (imageData != null){
			post.addFile(KEY_FULLSIZE, imageData.mObject1, FileType.JPEG);
			post.addFile(KEY_THUMBNAIL, imageData.mObject2, FileType.JPEG);
		}
		
		// post to server
		ServerReturn result = post.post();
		
		// convert to ServerJSON
		return new ShareBearServerReturn(result);
	}

	/**
	 * Post values to the server. The json response will always have either <p>
	 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
	 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE
	 * @param action The action to take on the server
	 * @param data The json data to post to server
	 * @param imageData Two objects, the full image byte[] and the thumbnail byte[]
	 * @return The json response from server.
	 */
	static public ShareBearServerReturn postToServer(
			String action,
			JSONObject data,
			TwoObjects<byte[], byte[]> imageData){

		return postToServerHelper(action, data.toString(), imageData);
	}
	
	/**
	 * Post values to the server. The json response will always have either <p>
	 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
	 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE
	 * @param action The action to take on the server
	 * @param data The json data to post to server
	 * @param imageData Two objects, the full image byte[] and the thumbnail byte[]
	 * @return The json response from server.
	 */
	static public ShareBearServerReturn postToServer(
			String action,
			JSONArray data,
			TwoObjects<byte[], byte[]> imageData){

		return postToServerHelper(action, data.toString(), imageData);
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
}
