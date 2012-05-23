package com.instantPhotoShare;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class FullSizeServerReturn
extends ShareBearServerReturn{

	public FullSizeServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Grab the image data. If not present then, null will be returned.
	 * @return
	 */
	public String getImageData(){
		// grab the base64 data
		String base64 = getMessage();
		
		// strip off useless data in front
		if (base64 == null || base64.length() < 23)
			return null;
		base64 = base64.substring(23);
		
		return base64;
	}
	
	/**
	 * Return image data as a byte array.
	 * @return The byte[] array of data. Will be null if not present.
	 */
	public byte[] getImageBytes(){
		// grab the base64
		String base64 = getImageData();
		
		// now convert to byte
		try{
			return Base64.decode(base64, Base64.DEFAULT);
		}catch (RuntimeException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
}
