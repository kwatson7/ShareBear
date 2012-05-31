package com.instantPhotoShare;

import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class ThumbnailServerReturn
extends ShareBearServerReturn{

	// constants
	private static final int IGNORE_CHARACTERS_BEGINNING_BASE64 = 23;
	private static final int BASE64_FORMAT = Base64.DEFAULT;
	
	public ThumbnailServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Return thumbnail data as a byte array.
	 * @param serverId The serverId of the thumbnail data
	 * @return The byte[] array of data. Will be null if not present.
	 */
	public byte[] getThumbnailBytes(long serverId){		
		
		// return null if unsuccessful
		if (!isSuccess())
			return null;
		
		// grab the base64 data
		JSONObject json = getMessageObject();
		String base64 = json.optString(String.valueOf(serverId));
		
		// check null and length
		if (base64 == null || base64.length() < IGNORE_CHARACTERS_BEGINNING_BASE64)
			return null;
		
		// extract important values
		byte[] base64Bytes = new byte[base64.length() - IGNORE_CHARACTERS_BEGINNING_BASE64];
		base64.getBytes(IGNORE_CHARACTERS_BEGINNING_BASE64, base64.length(), base64Bytes, 0);
		
		// clear base64
		base64 = null;
		System.gc();
		
		// now convert to byte
		try{
			return Base64.decode(base64Bytes, BASE64_FORMAT);
		}catch (RuntimeException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
}
