package com.instantPhotoShare;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import com.tools.ServerPost.ServerReturn;

public class ThumbnailServerReturn
extends ShareBearServerReturnWithArray{

	// constants
	private static final int IGNORE_CHARACTERS_BEGINNING_BASE64 = 23; 		// these characters should be ingnored when reading base64
	private static final int BASE64_FORMAT = Base64.DEFAULT; 				// type of encoding
	private static final String KEY_THUMBNAIL_DATA = "thumbnail_data";
	private static final String KEY_OWNER_ID = "owner_id";
	private static final String KEY_DATE_UPLOADED = "date_uploaded";	
	private static final String KEY_THUMBNAIL_ID = "thumbnail_id";
	
	public ThumbnailServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Return thumbnail data as a byte array.
	 * @param index The index of the thumbnail data to grab
	 * @return The byte[] array of data. Will be null if not present.
	 */
	public byte[] getThumbnailBytes(int index){		
			
		// return null if unsuccessful
		if (!isSuccess())
			return null;
		
		// grab the base64 data
		JSONObject json = getItemObject(index);
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
	 * @param index The index of the thumbnail data
	 * @return The serverId of the user who created picture
	 */
	public long getUserServerIdWhoCreated(int index){
		return getLongFromMessageArray(index, KEY_OWNER_ID);
	}
	
	/**
	 * Return the date the picture was create or 1900-01-01 01:00:00 if not known
	 * @param index The index of the picture in question
	 * @return The date
	 */
	public String getDateCreated(int index){
		String date = getStringFromMessageArray(index, KEY_DATE_UPLOADED);
		if (date == null || date.length() == 0)
			date = "1900-01-01 01:00:00";
		
		return date;	
	}
	
	/**
	 * Return the serverId of the picture or -1 if none
	 * @param index the index of the picture to grab
	 * @return The serverId of the picture or -1 if none
	 */
	public long getPictureServerId(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return -1;

		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return -1;
		return json.optLong(KEY_THUMBNAIL_ID, -1);
	}
}
