package com.instantPhotoShare;

import org.json.JSONException;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class FullSizeServerReturn
extends ShareBearServerReturn{

	// constants
	private static final int IGNORE_CHARACTERS_BEGINNING_BASE64 = 23;
	private static final String KEY_DATA = "image";
	private static final int BASE64_FORMAT = Base64.DEFAULT;
	
	public FullSizeServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Grab the image data. If not present then, null will be returned.
	 * @return
	 */
	public String getImageData(){
		if (!isSuccess())
			return null;
		
		// grab the base64 data
		String base64 = null;
		try {
			base64 = getMessageObject().getString(KEY_DATA);
		} catch (JSONException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
		
		// strip off useless data in front
		if (base64 == null || base64.length() < IGNORE_CHARACTERS_BEGINNING_BASE64)
			return null;
		base64 = base64.substring(IGNORE_CHARACTERS_BEGINNING_BASE64);
		
		return base64;
	}
	
	/**
	 * Return image data as a byte array.
	 * @return The byte[] array of data. Will be null if not present.
	 */
	public byte[] getImageBytes(){
		if (!isSuccess())
			return null;
		
		// grab the base64
		String base64 = getImageData();
		
		// now convert to byte
		try{
			return Base64.decode(base64, BASE64_FORMAT);
		}catch (RuntimeException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
}
