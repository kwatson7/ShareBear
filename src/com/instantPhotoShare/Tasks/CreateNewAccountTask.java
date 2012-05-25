package com.instantPhotoShare.Tasks;

import org.json.JSONException;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.instantPhotoShare.Person;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.ShareBearServerReturn;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.tools.CustomActivity;
import com.tools.ServerPost.ServerReturn;

public class CreateNewAccountTask<ACTIVITY_TYPE extends CustomActivity>
	extends com.tools.CustomAsyncTask <ACTIVITY_TYPE, Integer, CreateNewAccountTask<ACTIVITY_TYPE>.ReturnFromCreateNewAccountTask>{

	// different error codes
	private static final String USERNAME_EXISTS_ERROR = "USERNAME_EXISTS_ERROR";
	private static final String CREATE_USER_INSERT_ERROR = "CREATE_USER_INSERT_ERROR";
	private static final String USER_ID_ERROR = "USER_ID_ERROR";
	private static final String COULD_NOT_STORE_LOCALLY = "COULD_NOT_STORE_LOCALLY";

	// codes to be sent to server
	private static final String CREATE_USER = "create_user";
	
	// field that Person class needs to access to know what data we need to send to server
	public static final String PERSON_FIRST_NAME = "person_fname";
	public static final String PERSON_LAST_NAME = "person_lname";
	public static final String PERSON_PHONE = "phone_number";
	public static final String PERSON_EMAIL = "person_email";
	public static final String USER_NAME = "user_name";
	public static final String PASSWORD = "password";

	// the person to pass in
	Person person;

	/**
	 * Constructor.
	 * @param act the calling activity
	 * @param requestId A custom id to identify the asynctask to the calling activity
	 * @param progressbars an array list of strings for the progress bar ids to be shown/hidden when started/finished
	 * the subclassed task, make sure to call isCanceled() periodically in doInBackground, to break.
	 */
	public CreateNewAccountTask(
			ACTIVITY_TYPE act,
			int requestId,
			Person person) {
		super(act, requestId, true, false, null);
		this.person = person;
	}

	@Override
	protected void onPreExecute() {	}

	@Override
	protected ReturnFromCreateNewAccountTask doInBackground(Void... params) {

		// post data to server and get response
		ReturnFromCreateNewAccountTask serverResponse = new ReturnFromCreateNewAccountTask();
		try {
			serverResponse = new ReturnFromCreateNewAccountTask(Utils.postToServer(CREATE_USER, person.getNewUserInfo(), null));
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			serverResponse.setError(e);
		}

		// SAVE to users database
		if(serverResponse.isSuccess() && !saveValuesFromServer(serverResponse))
			serverResponse.setError(COULD_NOT_STORE_LOCALLY, "Could not save user into database");
		return serverResponse;
	}

	/**
	 * Save all personal info to database
	 * @param taskValue The value returned from CreateNewAccountTask
	 */
	private boolean saveValuesFromServer(ReturnFromCreateNewAccountTask taskValue){
    	
		// grab values from form data
		String firstName =  person.getFirstName();
    	String lastName = person.getLastName();
    	String phone = person.getMainPhone();
    	String email = person.getEmailsAsString();
    	String user = person.getUserName();
    	String pass = person.getPassword();
    	
		// create new user for ourselves
		UsersAdapter adapter = new UsersAdapter(applicationCtx);
		long rowId = adapter.makeNewUser(
				applicationCtx,
				firstName,
				lastName,
				email,
				phone,
				null,
				Utils.getNowTime(),
				true,
				"",
				null);
		
		// check failure or not
		if (rowId == -1)
			return false;
		
		// store the rest of the settings
		Prefs.setUserRowId(applicationCtx, rowId);
		Prefs.setUserServerId(applicationCtx, taskValue.getUserId());
		Prefs.setUserName(applicationCtx, user);
		Prefs.setSecretCode(applicationCtx, taskValue.getSecretCode());
		Prefs.setPassword(applicationCtx, pass);
		
		return true;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {	}

	@Override
	protected void onPostExectueOverride(ReturnFromCreateNewAccountTask result) {

		//make a toast saying if we were succesfful or not
		if (result.isSuccess())
			Toast.makeText(callingActivity,
					"Successfully created account on server",
					Toast.LENGTH_SHORT).show();
		else{
			String reason = result.getDetailErrorMessage();
			if (callingActivity != null)
				Toast.makeText(callingActivity,
						"Failed to create account because:\n" + reason,
						Toast.LENGTH_LONG).show();
		}

		// send result back to activity
		if (callingActivity != null)
			sendObjectToActivityFromPostExecute(result);

	}

	@Override
	protected void setupDialog() {
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Creating Account");
			dialog.setMessage("Please Wait");
			dialog.setIndeterminate(true);
		}
	}

	public class ReturnFromCreateNewAccountTask
	extends ShareBearServerReturn{

		// KEYS in JSON
		private static final String KEY_UNIQUE_KEY = "secret_code";
		private static final String KEY_USER_ID = "user_id";

		/**
		 * Intiailize a ReturnFromLoginTask object from a ServerJSON object.
		 * @param toCopy
		 */
		protected ReturnFromCreateNewAccountTask(ServerReturn toCopy) {
			super(toCopy);
		}

		public ReturnFromCreateNewAccountTask() {
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
			if (isSuccess()){
				if (getUserId() == -1 || getSecretCode().length() == 0)
					Log.e(Utils.LOG_TAG, "not correct info back from server in CreateNewAccountTask");
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
	}
}


