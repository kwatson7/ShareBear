package com.instantPhotoShare;

import org.json.JSONObject;

import android.util.Base64;

import com.tools.ServerPost.ServerReturn;

public class ThumbnailServerReturn2
extends ShareBearServerReturn2{

	public ThumbnailServerReturn2(ServerReturn toCopy) {
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
		if (base64 == null || base64.length() < 24)
			return null;
		base64 = base64.substring(24);
		
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
		if (base64 == null)
			return null;
		
		// now convert to byte
		return Base64.decode(base64, Base64.DEFAULT);
	}
}
