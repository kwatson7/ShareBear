package com.instantPhotoShare;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.SparseArray;

import com.tools.ServerPost.ServerReturn;

public class ShareBearServerReturnWithArray
extends ShareBearServerReturn{
	
	// member fields
	SparseArray<JSONObject> userObjects = new SparseArray<JSONObject>();
	
	public ShareBearServerReturnWithArray(ServerReturn toCopy) {
		super(toCopy);
	}

	/**
	 * Return the number of items in this array return
	 * @return
	 */
	public int getNItems(){
		if (!isSuccess())
			return 0;
		else
			return getMessageArray().length();
	}
	
	/**
	 * Return a long from the given array number and key value, -1 if can't be found
	 * @param index the index of which object to grab
	 * @param key The key to grab at the given index
	 * @return The long value, -1 if can't be found
	 */
	protected long getLongFromMessageArray(int index, String key){
		// return null if unsuccessful
		if (!isSuccess())
			return -1;
		
		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return -1;
		return json.optLong(key, -1);
	}
	
	/**
	 * Return a string from the given array number and key value, "" if can't be found
	 * @param index the index of which object to grab
	 * @param key The key to grab at the given index
	 * @return The string value,"" if can't be found
	 */
	protected String getStringFromMessageArray(int index, String key){
		// return null if unsuccessful
		if (!isSuccess())
			return "";
		
		// grab the data
		JSONObject json = getItemObject(index);
		if(json == null)
			return "";
		return json.optString(key, "");
	}
	
	/**
	 * Get the json object for this given index, null if unsuccessful
	 * @param index The index of which item to grab info from
	 * @return The object for this thumbnail, or null
	 */
	protected JSONObject getItemObject(int index){
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
