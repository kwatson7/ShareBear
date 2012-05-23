package com.instantPhotoShare;

import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class ThumbnailServerReturn
extends ShareBearServerReturn{

	public ThumbnailServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Grab the thumnail data for the given serverId. If not present then, null will be returned.
	 * @param serverId
	 * @return
	 */
	public String getThumbailData(long serverId){
		// grab the base64 data
		JSONObject json = getMessageObject();
		String base64 = json.optString(String.valueOf(serverId));
		
		// strip off useless data in front
		if (base64 == null || base64.length() < 23)
			return null;
		base64 = base64.substring(23);
		
		return base64;
	}
	
	/**
	 * Return thumbnail data as a byte array.
	 * @param serverId The serverId of the thumbnail data
	 * @return The byte[] array of data. Will be null if not present.
	 */
	public byte[] getThumbnailBytes(long serverId){
		// grab the base64
		String base64 = getThumbailData(serverId);
		
		// now convert to byte
		try{
			return Base64.decode(base64, Base64.DEFAULT);
		}catch (RuntimeException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
}
