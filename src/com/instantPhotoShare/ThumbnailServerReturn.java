package com.instantPhotoShare;

import java.util.HashMap;

import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class ThumbnailServerReturn
extends ShareBearServerReturn{

	// constants
	private static final int IGNORE_CHARACTERS_BEGINNING_BASE64 = 23; 		// these characters should be ingnored when reading base64
	private static final int BASE64_FORMAT = Base64.DEFAULT; 				// type of encoding
	private static final String KEY_THUMBNAIL_DATA = "thumbnail_data";
	private static final String KEY_OWNER_ID = "owner_id";
	private static final String KEY_DATE_UPLOADED = "date_uploaded";
	
	// member
	HashMap<Long, JSONObject> thumbnailObjects = new HashMap<Long, JSONObject>(10);
	
	
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
		JSONObject json = getItemObject(serverId);
		if(json == null)
			return null;
		String base64 = json.optString(KEY_THUMBNAIL_DATA);
		
		// check null and length
		if (base64 == null || base64.length() < IGNORE_CHARACTERS_BEGINNING_BASE64)
			return null;
		
		// extract important values
		byte[] base64Bytes = new byte[base64.length() - IGNORE_CHARACTERS_BEGINNING_BASE64];
		base64.getBytes(IGNORE_CHARACTERS_BEGINNING_BASE64, base64.length(), base64Bytes, 0);
		
		// clear base64
		base64 = null;
		
		// now convert to byte
		try{
			return Base64.decode(base64Bytes, BASE64_FORMAT);
		}catch (RuntimeException e){
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
	
	/**
	 * Return the serverId of the user who created the picture, or -1 if none
	 * @param serverId The serverId of the thumbnail
	 * @return The serverId of the user
	 */
	public long getUserServerIdWhoCreated(long serverId){
		// return null if unsuccessful
		if (!isSuccess())
			return -1;
		
		// grab the base64 data
		JSONObject json = getItemObject(serverId);
		if(json == null)
			return -1;
		return json.optLong(KEY_OWNER_ID, -1);
	}
	
	/**
	 * Return the date the picture was create or 1900-01-01 01:00:00 if not known
	 * @param serverId The serverId of the thumbnail
	 * @return The date
	 */
	public String getDateCreated(long serverId){
		// return null if unsuccessful
		if (!isSuccess())
			return "1900-01-01 01:00:00";
		
		// grab the base64 data
		JSONObject json = getItemObject(serverId);
		if(json == null)
			return "1900-01-01 01:00:00";
		String date = json.optString(KEY_DATE_UPLOADED);
		
		if (date == null || date.length() == 0)
			return "1900-01-01 01:00:00";
		else
			return date;
	}
	
	/**
	 * Get the json object for this given serverId, null if unsuccseefsull
	 * @param serverId The serverId of the object to get
	 * @return The object for this thumbnial, or null
	 */
	private JSONObject getItemObject(long serverId){
		// return null if unsuccessful
		if (!isSuccess())
			return null;
		
		// see if we've retreived it already
		JSONObject message = thumbnailObjects.get(serverId);
		if (message != null)
			return message;
		
		// grab the item at this index
		message = getMessageObject();
		if (message == null)
			return null;
		JSONObject item = message.optJSONObject(String.valueOf(serverId));
		thumbnailObjects.put(serverId, item);
		return item;
		
	}
}
