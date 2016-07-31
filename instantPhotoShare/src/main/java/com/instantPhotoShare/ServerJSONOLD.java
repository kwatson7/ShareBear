package com.instantPhotoShare;

import org.json.JSONException;
import org.json.JSONObject;

import com.tools.ServerPost;
import com.tools.ServerPost.ServerReturn;

import android.util.Log;

/**
 * The json response from the server will always have either <p>
 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE <p>
 * The json functionality is enclosed inside this class and only accessible through custom methods.
 */
public class ServerJSONOLD{

	// keys for the server
	/** The key for the message upon success */
	private static final String KEY_SUCCESS_MESSAGE = "success_message";
	/** The key for the error message */
	private static final String KEY_ERROR_MESSAGE = "error_message";
	/** The key for the error code */
	private static final String KEY_ERROR_CODE = "error_code";
	/** The key for the status */
	private static final String KEY_STATUS = "response_status";

	// values for the server
	/** The value for a failed status */
	private static final String FAILURE = "failed";
	/** The value for a success status */
	private static final String SUCCESS = "success";
	/** The value for multiple responses. Will be treated as success */
	private static final String MULTIPLE = "multiple";
	/** Default error value if unknown */
	private static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";
	/** Default error code if unknown */
	private static final String UNKNOWN_ERROR_CODE = "UNKNOWN";
	/** Conversion code for a json conversion error */
	private static final String JSON_CONVERSION_CODE = "JSON_CONVERSION_CODE";
	private static final String SERVER_MESSAGE = "Server Error";
	private static final String SERVER_JSON_INCORRECT_FORMAT = "SERVER_JSON_INCORRECT_FORMAT";
	
	// other constants
	/** This string precedes any int HTTP resposne codes */
	private static final String BAD_HTTP_RESPONSE_HELPER_STRING = "HTTP_RESPONSE_";
	
	// the private json object
	protected JSONObject jsonObject;

	/**
	 * Creates a new ServerJSON with name/value mappings from the JSON string.
	 * @param json a JSON-encoded string containing an object. 
	 * 
	 * @throws JSONException  if the parse fails or doesn't yield a JSONObject.  
	 */
	public ServerJSONOLD(String json){
		try {
			jsonObject = new JSONObject(json);
		} catch (JSONException e) {
			try{
				jsonObject = new JSONObject();
				jsonObject.put(KEY_STATUS, FAILURE);
				jsonObject.put(KEY_ERROR_MESSAGE, SERVER_MESSAGE);
				jsonObject.put(KEY_ERROR_CODE, JSON_CONVERSION_CODE);
				Log.e(this.getClass().getPackage().getName(), json);
			}catch (JSONException ee) {
				Log.e(this.getClass().getPackage().getName(), ee.getMessage());
			}
		}
		checkAcceptable();
	}
	
	public ServerJSONOLD(ServerReturn serverReturn){
		// the default error results
		ServerJSONOLD output = ServerJSONOLD.getDefaultFailure();
		
		// check there were no errors
		// check there were no errors
		if (!serverReturn.isSuccess()){
			output.setErrorMessage(serverReturn.getDetailErrorMessage(), serverReturn.getErrorCode());
		}else{
			output = new ServerJSONOLD(serverReturn.getServerReturnLastLine());
		}
		
		// copy json data
		jsonObject = output.jsonObject;
		checkAcceptable();
	}

	/**
	 * The json must have either <p>
	 * 1. At least 2 keys, KEY_STATUS, and KEY_SUCCESS_MESSAGE or <br>
	 * 2. At least 3 keys, KEY_STATUS, KEY_ERROR_MESSAGE, and KEY_ERROR_CODE
	 */
	private void checkAcceptable(){
		
		// check required values for success
		if (isSuccess()){
			if (!jsonObject.has(KEY_STATUS) || !jsonObject.has(KEY_SUCCESS_MESSAGE)){
				try{
					Log.e(Utils.LOG_TAG, jsonObject.toString());
					jsonObject = new JSONObject();
					jsonObject.put(KEY_STATUS, FAILURE);
					jsonObject.put(KEY_ERROR_MESSAGE, SERVER_MESSAGE);
					jsonObject.put(KEY_ERROR_CODE, SERVER_JSON_INCORRECT_FORMAT);	
				}catch (JSONException e) {
					Log.e(Utils.LOG_TAG, e.getMessage());
				}
			}	
		// check required for failure
		}else{
			if (!jsonObject.has(KEY_STATUS) || !jsonObject.has(KEY_ERROR_MESSAGE) || !jsonObject.has(KEY_ERROR_CODE)){
				try{
					Log.e(Utils.LOG_TAG, jsonObject.toString());
					jsonObject = new JSONObject();
					jsonObject.put(KEY_STATUS, FAILURE);
					jsonObject.put(KEY_ERROR_MESSAGE, SERVER_MESSAGE);
					jsonObject.put(KEY_ERROR_CODE, SERVER_JSON_INCORRECT_FORMAT);	
				}catch (JSONException e) {
					Log.e(Utils.LOG_TAG, e.getMessage());
				}
			}
		}
		checkAcceptableSub();
	}
	
	/**
	 * This can be overriden to perform additional error checking in addition to checkAcceptable.
	 */
	protected void checkAcceptableSub(){
		// do nothing
	}
	
	/**
	 * Creates a ServerJSON object with no name/value mappings. 
	 */
	private ServerJSONOLD() {
		jsonObject = new JSONObject();
	}
	
	/**
	 * Initializes a new ServerJSON object from the original
	 * @param toCopy The original object to copy.
	 */
	protected ServerJSONOLD(ServerJSONOLD toCopy){
		jsonObject = toCopy.jsonObject;
		checkAcceptable();
	}

	/**
	 * If the return from the server is "success" return true, else false. <br>
	 * If there is no proper key in the JSON, then also returns false.
	 * @return
	 */
	public boolean isSuccess(){
		if (jsonObject.optString(KEY_STATUS).equalsIgnoreCase(SUCCESS))
			return true;
		else
			return false;
	}

	/**
	 * Get the default failure ServerJSON object
	 * @return
	 */
	public static ServerJSONOLD getDefaultFailure(){
		ServerJSONOLD defaultOutput = new ServerJSONOLD();
		try{
			defaultOutput.jsonObject.put(KEY_STATUS, FAILURE);
			defaultOutput.jsonObject.put(KEY_ERROR_MESSAGE, UNKNOWN_ERROR_MESSAGE);
			defaultOutput.jsonObject.put(KEY_ERROR_CODE, UNKNOWN_ERROR_CODE);
		}catch (JSONException e) {
			Log.e(defaultOutput.getClass().getPackage().getName(), e.getMessage());
		}
		defaultOutput.checkAcceptable();
		return defaultOutput;
	}
	
	/**
	 * Change the error message of the object. If error or errorCode are null or empty, then default is used.<br>
	 * Also change the status to failure, and remove any success message.
	 * @param error The error string message
	 * @param errorCode The error code that can be used for identification
	 */
	public void setErrorMessage(String error, String errorCode){
		
		// can't be null, if null, then defaults are used
		if (error == null || error.length() == 0)
			error = UNKNOWN_ERROR_MESSAGE;
		if (errorCode == null || errorCode.length() == 0)
			errorCode = UNKNOWN_ERROR_CODE;
		
		// now assign the values
		try {
			// put the error message in.
			jsonObject.put(KEY_ERROR_MESSAGE, error);
			jsonObject.put(KEY_ERROR_CODE, errorCode);
			
			// change status to failure
			jsonObject.put(KEY_STATUS, FAILURE);
			
			// remove success key
			jsonObject.remove(KEY_SUCCESS_MESSAGE);
			
		} catch (JSONException e) {
			Log.e(this.getClass().getPackage().getName(), e.getMessage());
		}
	}
	
	/**
	 * Change the error message of the object. If error or errorCode are null or empty, then default is used.<br>
	 * Also change the status to failure, and remove any success message.
	 * @param error The error string message
	 * @param errorCode The http response code. The code is saved as a string with BAD_HTTP_RESPONSE_HELPER_STRING preceding the code number.
	 */
	public void setErrorMessage(String error, int errorCode){
		String code = BAD_HTTP_RESPONSE_HELPER_STRING + errorCode;
		setErrorMessage(error, code);
	}
	
	/**
	 * Return either the success message or error message depending if we are sussessful or not (the value of isSuccess()).
	 * If there is no message, then an empty string is returned, but this shouldn't happen.
	 * @return
	 */
	public String getMessage(){
		if (isSuccess())
			try {
				return jsonObject.getString(KEY_SUCCESS_MESSAGE);
			} catch (JSONException e) {
				return "";
			}
		else
			try {
				return jsonObject.getString(KEY_ERROR_MESSAGE);
			} catch (JSONException e) {
				return "";
			}
	}
	
	/**
	 * Return the error code. If there is Success, then there is no code and an empty string is returned.
	 * @return
	 */
	public String getErrorCode(){
		if (isSuccess())
			return "";
		else
			return jsonObject.optString(KEY_ERROR_CODE);
	}
	
	/**
	 * Return the json object that this object wraps arround.
	 * @return
	 */
	private JSONObject getJson(){
		return jsonObject;
	}
}
