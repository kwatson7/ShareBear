package com.instantPhotoShare;

import org.json.JSONObject;

import com.tools.ServerPost.ServerReturn;

public class GetGroupsServerReturn
extends ShareBearServerReturn{

	// constants
	private static final String KEY_DATE_CREATED = "date_created";
	private static final String KEY_OWNER_ID = "owner_id";
	private static final String KEY_NAME = "name";
	private static final String KEY_USER_COUNT = "user_count";
	//TODO: read user_count from this return
	private static final String KEY_PHOTO_COUNT = "photo_count";
	//TODO: save photo_count to the database
	
	public GetGroupsServerReturn(ServerReturn toCopy) {
		super(toCopy);
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
	 * Return the name of the group, "" if none
	 * @param serverId The serverId of the thumbnail
	 * @return The name of the group
	 */
	public String getName(long serverId){
		// return null if unsuccessful
		if (!isSuccess())
			return "";
		
		// grab the base64 data
		JSONObject json = getItemObject(serverId);
		if(json == null)
			return "";
		return json.optString(KEY_NAME);
	}
	
	/**
	 * Return the number of pictures that are in this group on the server
	 * @param serverId
	 * @return The number of pictures, -1 if there was an error or unavailable
	 */
	public int getNPictures(long serverId){

		// return null if unsuccessful
		if (!isSuccess())
			return -1;

		// grab the base64 data
		JSONObject json = getItemObject(serverId);
		if(json == null)
			return -1;
		return json.optInt(KEY_PHOTO_COUNT);
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
		String date = json.optString(KEY_DATE_CREATED);
		
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
		
		// grab the item at this index
		JSONObject json = getMessageObject();
		if (json == null)
			return null;
		JSONObject item = json.optJSONObject(String.valueOf(serverId));
		return item;
		
	}
}
