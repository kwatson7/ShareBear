package com.instantPhotoShare;

import org.json.JSONArray;
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
public class ShareBearServerReturn
extends ServerPost.ServerReturn{

	// keys returned from server
	private static final String KEY_STATUS = "response_status";				// The key for the message indicating success or failure
	private static final String KEY_RESPONSE_MESSAGE = "response_message"; 	// The key with the returned data
	private static final String KEY_RESPONSE_CODE = "response_code"; 			// The key indicating the type of response

	// values for the server
	private static final String FAILURE = "failed"; 						// The value for a failed status
	private static final String SUCCESS = "success"; 						// The value for a success status/

	// different error codes
	private static final String NO_JSON_DATA_CODE = "NO_JSON_DATA";
	private static final String NO_JSON_DATA_MESSAGE = "No return from the server";
	private static final String INCORRECT_FORMAT_CODE = "INCORRECT_FORMAT_CODE";
	private static final String INCORRECT_FORMAT_CODE_STRING = "Server Error";
	public static final String EMAIL_VALIDATION_ERROR = "EMAIL_VALIDATION_ERROR";
	
	// member variables
	private JSONObject messageObject = null;

	/**
	 * Initialize a ShareBearServerReturn object with a generic ServerReturn object.
	 * @param toCopy
	 */
	public ShareBearServerReturn(ServerReturn toCopy){
		super(toCopy);
	}

	public ShareBearServerReturn(){
		super();
	}

	public ShareBearServerReturn(String errorCode, String errorMessage){
		super();
		setError(errorCode, errorMessage);
	}

	/**
	 * Requires that we have KEY_STATUS, and KEY_STATUS must be SUCCESS
	 */
	@Override
	final protected boolean isSuccessCustom(){
		JSONObject json = getJSONObject();

		// make sure there is json data
		if (json == null){
			setError(NO_JSON_DATA_CODE, NO_JSON_DATA_MESSAGE);
			Log.e(Utils.LOG_TAG, "no json data. This should not happen");
			return false;
		}

		// check that 3 important fields are present
		if (!json.has(KEY_STATUS) || !json.has(KEY_RESPONSE_MESSAGE) || !json.has(KEY_RESPONSE_CODE)){
			if (this.getErrorCode() == null || this.getErrorCode().length() == 0)
				setError(INCORRECT_FORMAT_CODE, INCORRECT_FORMAT_CODE_STRING);
			return false;
		}
		if (!json.optString(KEY_STATUS).equalsIgnoreCase(SUCCESS)){
			setError(getResponseCode(), getMessage());
			return false;
		}

		// if we made it here, then we're good
		return isSuccessCustom2();
	}

	/**
	 * Get the status code of this return. "" if none.
	 * @return
	 */
	public String getStatus(){
		JSONObject json = getJSONObject();
		if (json == null)
			return "";
		else
			return getJSONObject().optString(KEY_STATUS);
	}

	/**
	 * This method always returns true. Override it to add further constraints to isSuccess();
	 * @return
	 */
	protected boolean isSuccessCustom2(){
		return true;
	}

	/**
	 * Get the response_message of this object, "" if not found
	 * @return
	 */
	public String getMessage(){
		JSONObject json = getJSONObject();
		if (json == null)
			return "";
		else
			return getJSONObject().optString(KEY_RESPONSE_MESSAGE);
	}

	/**
	 * Get the response code, "" if none.
	 * @return
	 */
	public String getResponseCode(){
		JSONObject json = getJSONObject();
		if (json == null)
			return "";
		else
			return getJSONObject().optString(KEY_RESPONSE_CODE);
	}

	/**
	 * Return the mssage as a json object. Will be null if we could not convert
	 * @return
	 */
	public JSONObject getMessageObject(){
		if (messageObject != null)
			return messageObject;
		
		JSONObject json;
		try {
			json = new JSONObject(getMessage());
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
		messageObject = json;
		return json;
	}

	/**
	 * Return the message as a json array. Will be null if we could not convert
	 * @return
	 */
	public JSONArray getMessageArray(){
		JSONArray json;
		try {
			json = new JSONArray(getMessage());
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
		return json;
	}
	
	/**
	 * Check if we had an email validation error
	 * @return true if email validation error, false otherwise
	 */
	public boolean isEmailValidationError(){
		// success, can't be email error
		if (isSuccess())
			return false;
		
		// no error code, can't be email error
		String errCode = getErrorCode();
		if (errCode == null)
			return false;
		
		// compare to expected string
		if (errCode.compareToIgnoreCase(EMAIL_VALIDATION_ERROR) == 0)
			return true;
		else
			return false;
	}
}
