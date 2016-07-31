package com.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.ProgressBar;

public class ServerPost {

	// constants
	private static final Charset ENCODING_TYPE = Charset.forName("UTF-8"); // the type of the data
	private static final int RANDOM_FILENAME_LENGTH = 64; 			// if we create a random filename, the length of the filename
	private static final int GOOD_RETURN_CODE = 200; 				// The return code for a successful sync with server
	private static final String LOG_TAG = "ServerPost"; 			// The log tag

	// class members
	private String url; 											// the url to post to
	private MultipartEntity multipartEntity; 						// the form post holder
	private boolean keepFullReturn = false;
	private int timeoutConnection = 5000; 							// Default to wait for timeout to connect			
	private int timeoutSocket = 90000; 								// Default length of time to wait for returned data
	private String filePath = null;
	private BinarayDownloader binaryDownloader = null;

	/**
	 * Create a server post object
	 * @param url the url to post to
	 */
	public ServerPost(String url){
		// store inputs
		this.url = url;

		// initialize the data
		initializeOptions();
	}

	/**
	 * Add a string value to the post
	 * @param key the key to associate with the data
	 * @param value the value to post
	 */
	public void addData(String key, String value){
		try{
			multipartEntity.addPart(key, new StringBody(value, ENCODING_TYPE));
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Add a file to the post with a random file name attached
	 * @param key the key to identify the file
	 * @param data the byte[] data of the file
	 * @param fileType the type of file
	 * @return the filename assigned to the data
	 */
	public String addFile(String key, byte[] data, FileType fileType){
		String fileName = com.tools.Tools.randomString(RANDOM_FILENAME_LENGTH);
		multipartEntity.addPart(key, new ByteArrayBody(data, fileType.getType(), fileName));
		return fileName;
	}

	/**
	 * Add a file to the post
	 * @param key the key to identify the file
	 * @param data the byte[] data of the file
	 * @param fileType the type of file
	 * @param fileName the filename to store with the file
	 */
	public void addFile(String key, byte[] data, FileType fileType, String fileName){
		multipartEntity.addPart(key, new ByteArrayBody(data, fileType.getType(), fileName));
	}

	/**
	 * Add file to the post. If the file doesn't exist, a log error is posted 
	 * @param key the key to assign to data
	 * @param path The path name of file.
	 * @return true if we successfully added the data, false if the file doesn't exist
	 */
	public boolean addFile(String key, String path, FileType fileType){
		// check that the file exists
		if (path == null || path.length() == 0){
			Log.e("ServerPost", "bad file input into addFile");
			return false;
		}
		File file = new File(path);
		if (!file.exists()){
			Log.e("ServerPost", "File " + path + " does not exist");
			return false;
		}

		// add the file to the post
		multipartEntity.addPart(key, new FileBody(file, fileType.getType()));
		return true;
	}

	/**
	 * Post the data to the url
	 * @param progressBar the progressBar to write file download progress to. Null if none
	 * @return the server response
	 */
	public ServerReturn post(ProgressBar progressBar){
		
		WeakReference<ProgressBar> weakProgress = new WeakReference<ProgressBar>(progressBar);
		progressBar = null;

		// set the connection parameters
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		// initialize http client and post to correct page
		HttpClient client = new DefaultHttpClient(httpParameters);

		// initialize post
		HttpPost httpPost = new HttpPost(url);

		// set to not open tcp connection
		httpPost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

		// set the values to the post
		httpPost.setEntity(multipartEntity);

		// default statusCode
		int statusCode= -1;

		// the return values
		String serverReturnValue = "";
		String serverReturnValueLastLine = "";

		// send post
		try {
			// actual send
			//TODO: can get out of memory error here
			HttpResponse response = client.execute(httpPost);

			// check what kind of return
			StatusLine statusLine = response.getStatusLine();
			statusCode = statusLine.getStatusCode();

			// good return
			if (statusCode == GOOD_RETURN_CODE) {
				// read return
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				long dataLength = entity.getContentLength();

				// read the response as a string
				if (filePath == null){
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					StringBuilder builder = null;
					if (keepFullReturn)
						builder = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						if (keepFullReturn){
							builder.append(line);
							builder.append("\n");
						}
						serverReturnValueLastLine = line;
					}
					content.close();
					if (keepFullReturn)
						serverReturnValue = builder.toString();
					else
						serverReturnValue = serverReturnValueLastLine;
				}else{
					// ammend .tmp to file
					String tmpFile = new String(filePath + ".part");
					
					// read the response as a file
					ServerReturn out;
					if (binaryDownloader == null){
						long count = com.tools.Tools.writeInputStreamToFile(content, tmpFile, dataLength, weakProgress.get());
						if (count > 0)
							out =  new ServerReturn(true);
						else{
							out = new ServerReturn();
							out.setError(ServerReturn.CODE_NO_BYTES_WRITTEN, ServerReturn.NO_BYTES_WRITTEN_STRING);
						}
					}else{
						SuccessReason result = binaryDownloader.readInputStream(content, tmpFile, dataLength, weakProgress);
						if (result.getSuccess())
							out = new ServerReturn(result.getReason(), result.getReason());
						else{
							out = new ServerReturn();
							out.setError(ServerReturn.BAD_CUSTOM_BINARY_RETURN, result.getReason());
						}
					}
					
					// successful download, move file
					if (out.isSuccess()){
						File oldFile = new File(tmpFile);
						File newFile = new File(filePath);
						if (oldFile.exists() && oldFile.isFile()){
							if (newFile.exists())
								newFile.delete();
							boolean success = oldFile.renameTo(newFile);
							if (!success){
								out.setError(new IOException("Temp file could not be renamed"));
							}
						}	
					}
					
					return out;
				}

				// bad return code
			} else {
				ServerReturn value = new ServerReturn(statusCode);
				return value;
			}

			// different failures	
		}catch( UnknownHostException e){
			ServerReturn value = new ServerReturn(e);
			return value;
		} catch (IOException e) {
			ServerReturn value = new ServerReturn(e);
			return value;
		}

		// successful return
		ServerReturn value = new ServerReturn(serverReturnValue, serverReturnValueLastLine);
		return value;
	}

	/**
	 * Post the data to the server and run the callback when completed. onPostFinishedUiThread is called first
	 * @param act The activity to post the callback to. Can be null
	 * @param progresssBar, progressBar to post filedownload progress to. Null if none.
	 * @param indeterminateProgressBarTags, tags to indeterminate progressbars to show while posting, null if none
	 * @param callback the callback to call when post is finished
	 */
	public <ACTIVITY_TYPE extends CustomActivity>
	void postInBackground(
			ACTIVITY_TYPE  act,
			ProgressBar progressBar,
			ArrayList<String> indeterminateProgressBarTags,
			final PostCallback<ACTIVITY_TYPE> callback){

		// setup background thread and execute
		PostAsync<ACTIVITY_TYPE> task = 
			new PostAsync<ACTIVITY_TYPE>(act, progressBar, indeterminateProgressBarTags, callback);
			task.execute();
	}

	/**
	 * Set the timeout to wait for a connection in milliseconds. Defaults to 5,000ms
	 * @param connectionTimeoutMs The timeout in ms
	 */
	public void setConnectionTimeout(int connectionTimeoutMs){
		timeoutConnection = connectionTimeoutMs;
	}

	/**
	 * Set a custom callback to be used to handle binary return from server
	 * @param binaryDownloader The downloader to use. Set to null to use default (that just parses raw binary and writes to file).
	 */
	public void setCustomBinaryDownloader(BinarayDownloader binaryDownloader){
		this.binaryDownloader = binaryDownloader;
	}

	/**
	 * @param value true to keep entire return, false to only keep last line of return. Defaults to false.
	 */
	public void setKeepFullReturn(boolean value){
		keepFullReturn = value;
	}

	/**
	 * Instead of reading response from server as JSON, we write it to file. <br>
	 * If it's null, then we don't write to file
	 * @param filePath The file to write the data to
	 */
	public void setSaveFilePath(String filePath){
		this.filePath = filePath;
	}

	/**
	 * Set the timeout to wait for data in milliseconds. Defaults to 30,000ms
	 * @param socketTimeoutMs The timeout in ms
	 */
	public void setSocketTimeout(int socketTimeoutMs){
		timeoutSocket = socketTimeoutMs;
	}

	/**
	 * Various initialization steps
	 */
	private void initializeOptions(){

		// initialize entity
		multipartEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
	}

	public interface BinarayDownloader{
		/**
		 * read the response from the server as an input stream and write data to a file.
		 * The required folders should be be written if not present. <br>
		 * @See com.tools.Tools.writeInputStreamToFile
		 * @param inputStream The input stream to read from
		 * @param filePath The path to write to
		 * @param dataLength the total length of the data, negative number if unknown
		 * @param weakProgress the progress bar to write progress to, null if unknown or not going to happen
		 * @throws IOException
		 * @returns The type of return from the server. Success says if we were successfull, and reason, has either the failed reason, or the return from the server on success.
		 */
		public SuccessReason readInputStream(
				InputStream inputStream,
				String filePath,
				long dataLength,
				WeakReference<ProgressBar> weakProgress)
		throws IOException;
	}

	/**
	 * Different file types
	 */
	public enum FileType {
		// different file types
		JPEG("image/jpeg");

		// the type
		private String type;

		/**
		 *  construct with a string
		 * @param type
		 */
		private FileType(String type) {
			this.type = type;
		}

		/**
		 *  return the type of file
		 * @return
		 */
		public String getType() {
			return type;
		}
	}



	/**
	 * Callback when we are done posting to the server and we can read the response
	 * @author kwatson
	 *
	 * @param <ACTIVITY_TYPE>
	 */
	public interface PostCallback <ACTIVITY_TYPE extends CustomActivity>{
		/**
		 * This is called when we are done posting to the server on the background thread
		 * @param the activity that is currently active after the task completed
		 * @param result The server result
		 */
		public void onPostFinished(ACTIVITY_TYPE act, ServerReturn result);

		/**
		 * This is called on the ui thread when the post is finished
		 * @param the activity that is currently active after the task completed
		 * @param result The server result
		 */
		public void onPostFinishedUiThread(ACTIVITY_TYPE act, ServerReturn result);
	}

	/**
	 * The response we receive when we are finished posting to the server
	 * @author kwatson
	 *
	 */
	public static class ServerReturn{

		// member variables
		private String serverReturnValue = "";
		private String serverReturnValueLastLine = "";
		private String detailErrorMessage = "";
		private String errorCode = "";
		private JSONArray jsonArray = null;
		private JSONObject jsonObject = null;
		private boolean isClearStringReturnsOnJsonConversion = true;

		// builtin error codes
		/**
		 * Returned with a code other than 200
		 */
		public static final String BAD_CODE = "BAD_CODE";
		/**
		 * Error with the post
		 */
		public static final String CLIENT_PROTOCOL_ERROR = "CLIENT_PROTOCOL_ERROR";
		/**
		 * Error reading return
		 */
		public static final String IO_EXCEPTION = "IO_EXCEPTION";
		/**
		 * Generic exception error
		 */
		public static final String GENERIC_EXCEPTION = "GENERIC_EXCEPTION";

		/**
		 * Unknown error code
		 */
		public static final String UNKNOWN_ERROR_CODE = "UNKNOWN_ERROR_CODE";
		private static final String UNKNOWN_ERROR_STRING = "Unknown error";

		/**
		 * Unknown host
		 */
		public static final String UNKNOWN_HOST_EXCEPTION = "UNKNOWN_HOST_EXCEPTION";
		private static final String UNKNOWN_HOST_EXCEPTION_STRING = "Error accessing host. Check internet connection: ";

		/**
		 * No byte data was written to file
		 */
		public static final String CODE_NO_BYTES_WRITTEN = "CODE_NO_BYTES_WRITTEN";
		private static final String NO_BYTES_WRITTEN_STRING = "No data written to file";
		
		/**
		 * The custom binary downloader returned a bad value
		 */
		public static final String BAD_CUSTOM_BINARY_RETURN = "BAD_CUSTOM_BINARY_RETURN";

		/**
		 * Initialize return with unknown error
		 */
		public ServerReturn(){
			setError(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_STRING);
		}

		/**
		 * Copy a serverReturn object
		 * @param toCopy
		 */
		public ServerReturn(ServerReturn toCopy){
			this.serverReturnValue = toCopy.serverReturnValue;
			this.serverReturnValueLastLine = toCopy.serverReturnValueLastLine;
			this.detailErrorMessage = toCopy.detailErrorMessage;
			this.errorCode = toCopy.errorCode;
			this.jsonArray = toCopy.jsonArray;
			this.jsonObject = toCopy.jsonObject;
		}

		/**
		 * Create a completed server return item
		 * @param serverReturnValue the entire return from the server
		 * @param serverReturnValueLastLine just the last line return from the server
		 */
		public ServerReturn(
				String serverReturnValue,
				String serverReturnValueLastLine){
			// store values
			this.serverReturnValue = serverReturnValue;
			this.serverReturnValueLastLine = serverReturnValueLastLine;
		}

		/**
		 * Initialize a serverReturn that just says we succeeded.
		 * @param success
		 */
		private ServerReturn(boolean success){
			if (!success)
				setError(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_STRING);
			else
				setError("", "");
		}

		/**
		 * Create a return value that is just the bad code.
		 * @param code the return code from the server
		 */
		private ServerReturn(int code){
			detailErrorMessage = String.valueOf(code);
			errorCode = BAD_CODE;
		}

		/**
		 * Create a server return that had a bad io
		 * @param e the exception that was thrown
		 */
		private ServerReturn(IOException e){
			errorCode = IO_EXCEPTION;
			detailErrorMessage = e.getMessage();
		}

		private ServerReturn(UnknownHostException e){
			errorCode = UNKNOWN_HOST_EXCEPTION;
			detailErrorMessage = UNKNOWN_HOST_EXCEPTION_STRING + e.getMessage();
		}

		/**
		 * @return the detail message of the return
		 */
		public String getDetailErrorMessage(){
			if (detailErrorMessage == null)
				return "";
			else
				return detailErrorMessage;
		}

		/**
		 * Return the error code of the return
		 * @return
		 */
		public String getErrorCode(){
			return errorCode;
		}

		/**
		 * Return the server output as a JSON Array. Will be null if we could not convert to a JSON Array
		 * @return
		 */
		final public JSONArray getJSONArray(){
			if (jsonArray != null)
				return jsonArray;
			JSONArray out = null;
			try {
				out = new JSONArray(getServerReturnLastLine());
			} catch (JSONException e) {
				jsonArray = null;
				out = null;
				Log.e("ServerPost", getServerReturnLastLine());
				Log.e("ServerPost", Log.getStackTraceString(e));
			}

			jsonArray = out;

			if (jsonArray != null && isClearStringReturnsOnJsonConversion){
				serverReturnValue = null;
				serverReturnValueLastLine = null;
			}
			return out;
		}

		/**
		 * Return the server output as a JSON object. Will be null if we could not convert to a JSON object
		 * @return
		 */
		final public JSONObject getJSONObject(){
			if (jsonObject != null)
				return jsonObject;
			JSONObject out = null;
			try {
				out = new JSONObject(getServerReturnLastLine());
			} catch (JSONException e) {
				jsonObject = null;
				out = null;
				Log.e("ServerPost", getServerReturnLastLine());
				Log.e("ServerPost", Log.getStackTraceString(e));
			}
			jsonObject = out;

			if (jsonObject != null && isClearStringReturnsOnJsonConversion){
				serverReturnValue = null;
				serverReturnValueLastLine = null;
			}
			return out;
		}

		/**
		 * @return the server return value
		 */
		public String getServerReturn(){
			return serverReturnValue;
		}

		/**
		 * @return Only the last line of the server return value
		 */
		final public String getServerReturnLastLine(){
			return serverReturnValueLastLine;
		}

		/**
		 * Return true if we don't have any errors
		 * @return
		 */
		final public boolean isSuccess(){
			return ((errorCode == null || errorCode.length() == 0) && isSuccessCustom());
		}

		/**
		 * Set the error of the return value with the given exception.
		 * @param e the exception
		 */
		public void setError(Exception e){
			this.errorCode = GENERIC_EXCEPTION;
			this.detailErrorMessage = e.getMessage();
		}

		/**
		 * Set the error message for the return
		 * @param errorCode The identifying error code
		 * @param detailErrorMessage The detailed error message
		 */
		protected void setError(String errorCode, String detailErrorMessage){
			this.errorCode = errorCode;
			this.detailErrorMessage = detailErrorMessage;
		}

		/**
		 * This method must also be true to be considered a success. <br>
		 * Override this if you want. Default always returns true
		 * @return True for custom success and false otherwise. Defaults to true, unless overriden
		 */
		protected boolean isSuccessCustom(){
			return true;
		}
	}

	/**
	 * class used to post to server in the background
	 */
	private class PostAsync <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, ServerReturn>{

		private PostCallback<ACTIVITY_TYPE> callback;
		private WeakReference<ProgressBar> prog;

		/**
		 * Post to the server on a background thread
		 * @param act Teh activity that called this
		 * @param prog A progressbar to update if downloading file, null if none
		 * @param indeterminateProgressBarTags find the tag in the activity to show indeterminate progressbar, null if none
		 * @param callback The callback when done, musn't be null
		 */
		private PostAsync(
				ACTIVITY_TYPE act,
				ProgressBar prog,
				ArrayList<String> indeterminateProgressBarTags,
				final PostCallback<ACTIVITY_TYPE> callback) {
			super(
					act,
					-1,
					false,
					false,
					indeterminateProgressBarTags);
			this.callback = callback;
			this.prog = new WeakReference<ProgressBar>(prog);
		}

		@Override
		protected ServerReturn doInBackground(Void... params) {
			ServerReturn result = post(prog.get());
			callback.onPostFinished(callingActivity, result);
			return result;
		}

		@Override
		protected void onPostExectueOverride(ServerReturn result) {
			callback.onPostFinishedUiThread(callingActivity, result);
		}

		@Override
		protected void onPreExecute() {			
		}

		@Override
		protected void onProgressUpdate(Void... progress) {			
		}

		@Override
		protected void setupDialog() {
		}
	}
}
