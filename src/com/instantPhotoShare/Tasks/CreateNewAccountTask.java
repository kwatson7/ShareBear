package com.instantPhotoShare.Tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.instantPhotoShare.Person;
import com.instantPhotoShare.Prefs;
import com.tools.CustomActivity;

public class CreateNewAccountTask extends com.tools.CustomAsyncTask<Void, Integer, ReturnFromCreateNewAccountTask>{

	// static settings
	private static final String BASE_URL = Prefs.BASE_URL;
	protected static final String REQUEST_PAGE = Prefs.REQUEST_PAGE;
	private static final String STATUS = "response_status";
	private static final String FAILURE = "failure";
	protected static final String SUCCESS = "success";
	private static final String SUCCESS_MESSAGE = "success_message";
	private static final String ERROR_MESSAGE = "error_message";
	private static final String DATA = "data";
	private static final int GOOD_RETURN_CODE = 200;
	private static final String UNIQUE_KEY = "unique_key";
	private static final String USER_ID = "user_id";
	
	// codes to be sent to server
	private static final String ACTION = "action";
	private static final String CREATE_USER = "create_user";
	public static final String PERSON_FIRST_NAME = "person_fname";
	public static final String PERSON_LAST_NAME = "person_lname";
	public static final String PERSON_PHONE = "mail_phone";
	public static final String PERSON_EMAIL = "person_email";
	public static final String MAIN_PHONE = "main_phone";
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
	public CreateNewAccountTask(CustomActivity act,
			int requestId,
			Person person) {
		super(act, requestId, true, false, null);
		this.person = person;
	}

	@Override
	protected void onPreExecute() {	}

	@Override
	protected ReturnFromCreateNewAccountTask doInBackground(Void... params) {

		// post create user to url and get output
 		ReturnFromCreateNewAccountTask output = null;
		try {
			output = postValue(CREATE_USER, person);
		} catch (JSONException e) {
			output = new ReturnFromCreateNewAccountTask(FAILURE, "Failed because " + e.getMessage());
		}
		
		return output;
	}


	@Override
	protected void onProgressUpdate(Integer... progress) {	}

	@Override
	protected void onPostExectueOverride(ReturnFromCreateNewAccountTask result) {
		
		// if good success save data to prefs
		if (result.isSuccess()){
			Prefs.setPerson(applicationCtx, person);
			Prefs.setSecretCode(this.applicationCtx, result.secretCode);
			Prefs.setUserId(applicationCtx, result.userId);
		}
		
		//make a toast saying if we were succesfful or not
		if (result.isSuccess())
			Toast.makeText(callingActivity,
					"Successfully created account on server",
					Toast.LENGTH_SHORT).show();
		else{
			String reason = result.reason;
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
	
	private ReturnFromCreateNewAccountTask postValue(String action, Person person) throws JSONException{

		// output
		ReturnFromCreateNewAccountTask output;
		
		// initialize result string
		String result = "";
		StringBuilder builder = new StringBuilder();

		// initialize http client and post to correct page
		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(
				BASE_URL + REQUEST_PAGE);

		// set to not open tcp connection
		httpPost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

		// the particular action and all the user info
		ArrayList<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
		nameValuePairs.add(new BasicNameValuePair(ACTION, action));
		nameValuePairs.add(new BasicNameValuePair(DATA, person.getNewUserInfo().toString()));;

		// set the values to the post
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			output =  new ReturnFromCreateNewAccountTask(
						FAILURE,
						e1.toString());
			return output;
		}

		int statusCode= -1;
		
		// send post
		try {
			// actual send
			HttpResponse response = client.execute(httpPost);

			// check what kind of return
			StatusLine statusLine = response.getStatusLine();
			statusCode = statusLine.getStatusCode();

			// good return
			if (statusCode == GOOD_RETURN_CODE) {
				// read return
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line + "\n");
				}
				content.close();
				result = builder.toString();

				// bad return	
			} else {
				output =  new ReturnFromCreateNewAccountTask(
						FAILURE,
						statusCode);
				return output;
			}

			// different failures	
		} catch (ClientProtocolException e) {
			output =  new ReturnFromCreateNewAccountTask(
					FAILURE,
					e.toString(),
					statusCode);
			return output;
		} catch (IOException e) {
			output =  new ReturnFromCreateNewAccountTask(
					FAILURE,
					e.toString(),
					statusCode);
			return output;
		}

		//try parse the string to a JSON object
		try{
			JSONObject json = new JSONObject(result);
			output = new ReturnFromCreateNewAccountTask(
					json.optString(STATUS),
					json.optString(ERROR_MESSAGE),
					statusCode,
					json.optLong(USER_ID),
					json.optString(UNIQUE_KEY));
					
		}catch(JSONException e){
			output =  new ReturnFromCreateNewAccountTask(
					FAILURE,
					"Error parsing data "+e.toString(),
					statusCode);
			return output;
		}

		// return the final json object
		return output;	
	}
}


