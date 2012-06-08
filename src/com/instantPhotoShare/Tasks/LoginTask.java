package com.instantPhotoShare.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.tools.CustomActivity;
import com.tools.ServerPost.ServerReturn;

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
		LoginTask<ACTIVITY_TYPE>.ReturnFromLoginTask serverResponse = new ReturnFromLoginTask();
		try {
			serverResponse = new ReturnFromLoginTask(Utils.postToServer(USER_LOGIN, getDataToPost(), null, null));
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse.setError(e);
		}

		// SAVE to users database
		if(serverResponse.isSuccess() && !saveValuesFromServer(serverResponse))
			serverResponse = new ReturnFromLoginTask(COULD_NOT_STORE_LOCALLY, "Could not save user into database");

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
			String reason = result.getDetailErrorMessage();
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
	extends ShareBearServerReturn{

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
		private ReturnFromLoginTask(ServerReturn toCopy) {
			super(toCopy);
		}
		
		private ReturnFromLoginTask(String errorCode, String detailErrorMessage){
			super();
			setError(errorCode, detailErrorMessage);
		}

		private ReturnFromLoginTask(){
			super();
		}

		/**
		 * The json must have either <br>
		 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
		 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE <br>
		 * Also if successfull must have KEY_USER_ID and KEY_UNIQUE_KEY
		 */
		@Override
		protected boolean isSuccessCustom2(){

			// now check that we have userId and secretCode
			if (getUserId() == -1 || getSecretCode().length() == 0){
				Log.e(Utils.LOG_TAG, "Incorrect format return from LoginTask");
				return false;		
			}
			return true;
		}

		/**
		 * Gets the user ID stored in this object. Returns -1 if there isn't one. <br>
		 * That should never happen, because an illegal argument exceptions should have been
		 * thrown if this were the case, when object was created.
		 * @return
		 */
		public long getUserId() {
			try {
				return getMessageObject().getLong(KEY_USER_ID);
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
				return getMessageObject().getString(KEY_UNIQUE_KEY);
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
