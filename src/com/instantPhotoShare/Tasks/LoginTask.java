package com.instantPhotoShare.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ServerJSON;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.tools.CustomActivity;

public class LoginTask <ACTIVITY_TYPE extends CustomActivity>
	extends com.tools.CustomAsyncTask<ACTIVITY_TYPE, Integer, LoginTask<ACTIVITY_TYPE>.ReturnFromLoginTask>{

	// codes to be sent to server
	private static final String USER_LOGIN = "user_login";
	
	// login errors
	private static final String INCORRECT_LOGIN_ERROR = "INCORRECT_LOGIN_ERROR";
	private static final String COULD_NOT_STORE_LOCALLY = "COULD_NOT_STORE_LOCALLY";

	// the username and password
	String user;
	String pass;

	/**
	 * Constructor.
	 * @param act the calling activity
	 * @param requestId A custom id to identify the asynctask to the calling activity
	 * @param user username
	 * @param pass password
	 */
	public LoginTask(
			ACTIVITY_TYPE act,
			int requestId,
			String user,
			String pass) {
		super(act, requestId, true, false, null);
		this.user = user;
		this.pass = pass;
	}

	@Override
	protected void onPreExecute() {	}

	@Override
	protected ReturnFromLoginTask doInBackground(Void... params) {
		
		// post data to server and get response
		LoginTask<ACTIVITY_TYPE>.ReturnFromLoginTask serverResponse = null;
		try {
			serverResponse = new ReturnFromLoginTask(Utils.postToServer(USER_LOGIN, getDataToPost(), null));
		} catch (JSONException e) {
			serverResponse = new ReturnFromLoginTask(ServerJSON.getDefaultFailure());
			serverResponse.setErrorMessage(e.getMessage(), "JSONException");
		}
		
		// SAVE to users database
		if(serverResponse.isSuccess() && !saveValuesFromServer(serverResponse))
			serverResponse.setErrorMessage("Could not save user into database", COULD_NOT_STORE_LOCALLY);
		
		return serverResponse;
	}


	@Override
	protected void onProgressUpdate(Integer... progress) {	}

	@Override
	protected void onPostExectueOverride(ReturnFromLoginTask result) {

		//make a toast saying if we were succesfful or not
		if (result.isSuccess()){
			Toast.makeText(callingActivity,
					"Successfully logged in",
					Toast.LENGTH_SHORT).show();
		}else{
			String reason = result.getMessage();
			if (callingActivity != null)
				Toast.makeText(callingActivity,
						"Failed to login because:\n" + reason,
						Toast.LENGTH_LONG).show();
		}

		// send result back to activity
		if (callingActivity != null)
			sendObjectToActivityFromPostExecute(result);
	}
	
	/**
	 * Save values returned from login Task into preferences. Shows a toast if not successful
	 * @param returnVal
	 */
	private boolean saveValuesFromServer(LoginTask<ACTIVITY_TYPE>.ReturnFromLoginTask returnVal){
		// create new user for ourselves
		UsersAdapter adapter = new UsersAdapter(applicationCtx);
		long rowId = adapter.makeNewUser(
				applicationCtx,
				"",
				"",
				"",
				"",
				null,
				null,
				true,
				"",
				null);
		
		// check failure or not
		if (rowId == -1)
			return false;
		
		// store the rest of the settings
		Prefs.setUserRowId(applicationCtx, rowId);
		Prefs.setUserServerId(applicationCtx, returnVal.getUserId());
		Prefs.setUserName(applicationCtx, returnVal.getUserName());
		Prefs.setSecretCode(applicationCtx, returnVal.getSecretCode());
		Prefs.setPassword(applicationCtx, returnVal.getPassword());
		
		return true;
	}

	@Override
	protected void setupDialog() {
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Logging in");
			dialog.setMessage("Please Wait...");
			dialog.setIndeterminate(true);
		}
	}

	/**
	 * Return a list of parameter value pairs required to login on server
	 * @return a JSOBObject
	 * @throws JSONException 
	 */
	private JSONObject getDataToPost()
	throws JSONException{	

		// add values to json object
		JSONObject json = new JSONObject();
		json.put(CreateNewAccountTask.USER_NAME, user);
		json.put(CreateNewAccountTask.PASSWORD, pass);
		return json;
	}

	public class ReturnFromLoginTask
	extends ServerJSON{

		/** The userName that we used to login simply taken from enclosing class */
		private String userName = user;
		/** The password that we used to login simply taken from enclosing class */
		private String password = pass;
		
		// KEYS in JSON
		private static final String KEY_UNIQUE_KEY = "secret_code";
		private static final String KEY_USER_ID = "user_id";
		
		/**
		 * Intiailize a ReturnFromLoginTask object from a ServerJSON object.
		 * @param toCopy
		 */
		protected ReturnFromLoginTask(ServerJSON toCopy) {
			super(toCopy);
		}
		
		/**
		 * The json must have either <br>
		 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
		 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE <br>
		 * Also if successfull must have KEY_USER_ID and KEY_UNIQUE_KEY
		 */
		protected void checkAcceptableSub(){
			
			// now check that we have userId and secretCode
			if (isSuccess()){
				if (getUserId() == -1 || getSecretCode().length() == 0)
					throw new IllegalArgumentException("ReturnFromLoginTask " + 
							KEY_USER_ID + " key and " + 
							KEY_UNIQUE_KEY + " key.");
					
			}		
		}

		/**
		 * Gets the user ID stored in this object. Returns -1 if there isn't one. <br>
		 * That should never happen, because an illegal argument exceptions should have been
		 * thrown if this were the case, when object was created.
		 * @return
		 */
		public long getUserId() {
			try {
				return jsonObject.getLong(KEY_USER_ID);
			} catch (JSONException e) {
				return -1;
			}
		}

		/**
		 * Get the secret code stored in object. If empty, then there isn't one, but this should never happen,
		 * because an illegal argument exceptions should have been
		 * thrown if this were the case, when object was created.
		 * @return
		 */
		public String getSecretCode() {
			try {
				return jsonObject.getString(KEY_UNIQUE_KEY);
			} catch (JSONException e) {
				return "";
			}
		}

		/**
		 * The user name used to login
		 * @return
		 */
		public String getUserName() {
			return userName;
		}

		/**
		 * The password used to login
		 * @return
		 */
		public String getPassword() {
			return password;
		}
	}
}

//public class ReturnFromLoginTaskOld {
//
//	// static constants
//	public static final String SUCCESS = LoginTask.SUCCESS;
//	
//	/** The status of the return from server */
//	private String status;
//	/** If there was an error, the reason for the error, "" otherwise*/
//	protected String reason = "";
//	/** If there was a return, the status code, -1 otherwise */
//	private int code = -1;
//	/** The userId returned from server */
//	private long userId;
//	/** the sercetCode returned from server */
//	private String secretCode;
//	
//	private String userName = user;
//	private String password = pass;
//	
//	protected ReturnFromLoginTaskOld
//	(String status,
//			String reason,
//			int code){
//		
//		this.status = status;
//		this.reason = reason;
//		this.code = code;
//
//	}
//	
//	public ReturnFromLoginTaskOld
//	(String status,
//			String reason){
//		
//		this.status = status;
//		this.reason = reason;
//	}
//	
//	public ReturnFromLoginTaskOld(
//			String status,
//			int code){
//		this.status = status;
//		this.code = code;
//		this.reason = "StatusCode = " + code;
//	}
//	
//	public ReturnFromLoginTaskOld(
//			String status,
//			String reason,
//			int code,
//			long userId,
//			String secretCode){
//		this.status = status;
//		this.code = code;
//		this.setUserId(userId);
//		this.setSecretCode(secretCode);
//		this.reason = reason;
//	}
//			
//	/**
//	 * If the return from the server is "success"
//	 * @return
//	 */
//	public boolean isSuccess(){
//		if (status.equals(SUCCESS))
//			return true;
//		else
//			return false;
//	}
//
//	private void setUserId(long userId) {
//		this.userId = userId;
//	}
//
//	public long getUserId() {
//		return userId;
//	}
//
//	private void setSecretCode(String secretCode) {
//		this.secretCode = secretCode;
//	}
//
//	public String getSecretCode() {
//		return secretCode;
//	}
//
//	private void setUserName(String userName) {
//		this.userName = userName;
//	}
//
//	public String getUserName() {
//		return userName;
//	}
//
//	private void setPassword(String password) {
//		this.password = password;
//	}
//
//	public String getPassword() {
//		return password;
//	}
//}

//private LoginTask.ReturnFromLoginTaskOld postValue(String action, String user, String pass) throws JSONException{
//
//	// output
//	LoginTask.ReturnFromLoginTaskOld output;
//
//	// initialize result string
//	String result = "";
//	StringBuilder builder = new StringBuilder();
//
//	// initialize http client and post to correct page
//	HttpClient client = new DefaultHttpClient();
//	HttpPost httpPost = new HttpPost(
//			BASE_URL + REQUEST_PAGE);
//
//	// set to not open tcp connection
//	httpPost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
//
//	// the particular action and all the user info
//	ArrayList<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
//	nameValuePairs.add(new BasicNameValuePair(ACTION, action));
//	nameValuePairs.add(new BasicNameValuePair(DATA, getDataToPost().toString()));
//
//	// set the values to the post
//	try {
//		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//	} catch (UnsupportedEncodingException e1) {
//		output =  new LoginTask.ReturnFromLoginTaskOld(
//				FAILURE,
//				e1.toString());
//		return output;
//	}
//
//	int statusCode= -1;
//
//	// send post
//	try {
//		// actual send
//		HttpResponse response = client.execute(httpPost);
//
//		// check what kind of return
//		StatusLine statusLine = response.getStatusLine();
//		statusCode = statusLine.getStatusCode();
//
//		// good return
//		if (statusCode == GOOD_RETURN_CODE) {
//			// read return
//			HttpEntity entity = response.getEntity();
//			InputStream content = entity.getContent();
//			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				builder.append(line + "\n");
//			}
//			content.close();
//			result = builder.toString();
//
//			// bad return	
//		} else {
//			output =  new LoginTask.ReturnFromLoginTaskOld(
//					FAILURE,
//					statusCode);
//			return output;
//		}
//
//		// different failures	
//	} catch (ClientProtocolException e) {
//		output =  new LoginTask.ReturnFromLoginTaskOld(
//				FAILURE,
//				e.toString(),
//				statusCode);
//		return output;
//	} catch (IOException e) {
//		output =  new LoginTask.ReturnFromLoginTaskOld(
//				FAILURE,
//				e.toString(),
//				statusCode);
//		return output;
//	}
//
//	//try parse the string to a JSON object
//	try{
//		JSONObject json = new JSONObject(result);
//		output = new LoginTask.ReturnFromLoginTaskOld(
//				json.optString(STATUS),
//				json.optString(ERROR_MESSAGE),
//				statusCode,
//				json.optLong(USER_ID),
//				json.optString(UNIQUE_KEY));
//
//	}catch(JSONException e){
//		output =  new LoginTask.ReturnFromLoginTaskOld(
//				FAILURE,
//				"Error parsing data "+e.toString(),
//				statusCode);
//		return output;
//	}
//
//	// return the final json object
//	return output;	
//}
