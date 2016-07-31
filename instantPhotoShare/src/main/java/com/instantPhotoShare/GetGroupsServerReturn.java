package com.instantPhotoShare;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.SparseArray;

import com.tools.ServerPost.ServerReturn;

public class GetGroupsServerReturn
extends ShareBearServerReturn{

	// constants
	private static final String KEY_DATE_CREATED = ServerKeys.GetGroups.RETURN_KEY_DATE_CREATED;
	private static final String KEY_OWNER_ID = ServerKeys.GetGroups.RETURN_KEY_OWNER_ID;
	private static final String KEY_NAME = ServerKeys.GetGroups.RETURN_KEY_NAME;
	private static final String KEY_GROUP_ID = ServerKeys.GetGroups.RETURN_KEY_GROUP_ID;
	private static final String KEY_USER_COUNT = ServerKeys.GetGroups.RETURN_KEY_USER_COUNT;
	//TODO: read user_count from this return
	private static final String KEY_PHOTO_COUNT = ServerKeys.GetGroups.RETURN_KEY_PHOTO_COUNT;
	
	// member fields
	SparseArray<JSONObject> userObjects = new SparseArray<JSONObject>();
	
	public GetGroupsServerReturn(ServerReturn toCopy) {
		super(toCopy);
	}
	
	/**
	 * Return the serverId of the user who created the picture, or -1 if none
	 * @param index the group index to grab
	 * @return The serverId of the user
	 */
	public long getUserServerIdWhoCreated(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return -1;
		
		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return -1;
		return json.optLong(KEY_OWNER_ID, -1);
	}
	
	/**
	 * Return the number of groups in this object
	 * @return
	 */
	public int getNGroups(){
		if (!isSuccess())
			return 0;
		else
			return getMessageArray().length();
	}
	
	/**
	 * Return the serverId of the group or -1 if none
	 * @param index the index of the group to grab
	 * @return The serverId of the group or -1 if none
	 */
	public long getGroupServerId(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return -1;

		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return -1;
		return json.optLong(KEY_GROUP_ID, -1);
	}
	
	/**
	 * Return the name of the group, "" if none
	 * @param index The index of the group to grab info from
	 * @return The name of the group
	 */
	public String getName(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return "";
		
		// grab the base64 data
		JSONObject json = getItemObject(index);
		if(json == null)
			return "";
		return json.optString(KEY_NAME);
	}
	
	/**
	 * Return the number of pictures that are in this group on the server
	 * @param index the index of which group to grab
	 * @return The number of pictures, -1 if there was an error or unavailable
	 */
	public int getNPictures(int index){

		// return null if unsuccessful
		if (!isSuccess())
			return -1;

		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return -1;
		return json.optInt(KEY_PHOTO_COUNT);
	}
	
	/**
	 * Return the date the picture was create or 1900-01-01 01:00:00 if not known
	 * @param index The index of group to grab from
	 * @return The date
	 */
	public String getDateCreated(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return "1900-01-01 01:00:00";
		
		// grab the base64 data
		JSONObject json = getItemObject(index);
		if(json == null)
			return "1900-01-01 01:00:00";
		String date = json.optString(KEY_DATE_CREATED);
		
		if (date == null || date.length() == 0)
			return "1900-01-01 01:00:00";
		else
			return date;
	}
	
	/**
	 * Get the json object for this given index, null if unsuccessful
	 * @param index The index of which item to grab info from
	 * @return The object for this thumbnail, or null
	 */
	private JSONObject getItemObject(int index){
		// return null if unsuccessful
		if (!isSuccess())
			return null;

		// see if we've retreived it already
		JSONObject item = userObjects.get(index);
		if (item != null)
			return item;

		// grab the item at this index
		JSONArray array = getMessageArray();
		if (array == null)
			return null;
		item = array.optJSONObject(index);
		userObjects.put(index, item);
		return item;
	}
	
	@Override
	protected boolean isSuccessCustom2(){
		if (getMessageArray() == null)
			return false;
		else
			return true;
	}
}
