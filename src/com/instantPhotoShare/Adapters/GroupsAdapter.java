package com.instantPhotoShare.Adapters;


import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.Utils;

import android.content.ContentValues;
import android.content.Context;

public class GroupsAdapter
extends TableAdapter{

	/** Table name */
	public static final String TABLE_NAME = "groupInfo";
	
	// group table keys
	public static final String KEY_ROW_ID = "_id";
	private static final String KEY_NAME = "groupName";
	private static final String KEY_SERVER_ID = "serverId";
	private static final String KEY_PICTURE_ID = "groupPictureId";	
	private static final String KEY_DATE_CREATED = "dateCreated";
	private static final String KEY_USER_ID_CREATED = "userIdWhoCreatedGroup";
	private static final String KEY_ALLOW_OTHERS_ADD_MEMBERS = "allowOthersAddMembers";
	private static final String KEY_LATITUDE = "latitudeGroup";
	private static final String KEY_LONGITUDE = "longitudeGroup";
	private static final String KEY_ALLOW_PUBLIC_WITHIN_DISTANCE = "allowPublicWithinDistance";
	private static final String KEY_IS_UPDATING = "isUpdating";
	private static final String KEY_IS_SYNCED = "isSynced";
	
	/** Table creation string */
	public static final String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_NAME +" text not null, "
		+KEY_SERVER_ID +" integer, "
		+KEY_PICTURE_ID +" integer not null, "
		+KEY_DATE_CREATED +" text not null, "
		+KEY_USER_ID_CREATED +" integer not null, "
		+KEY_ALLOW_OTHERS_ADD_MEMBERS +" boolean DEFAULT 'TRUE', "
		+KEY_LATITUDE +" double, "
		+KEY_LONGITUDE +" double, "
		+KEY_ALLOW_PUBLIC_WITHIN_DISTANCE +" double, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_USER_ID_CREATED +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public GroupsAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Insert a new group into the database. Return the new rowId or -1 if unsuccessful. This group will not have a serverId attached
	 * to it, and its isSynced column will be false. Use set serverId or setSynced to set these.
	 * @param groupName The groupName, must not be null or empty.
	 * @param groupPictureId The picture id to the main picture for this group, -1 or null if not known
	 * @param dateCreated The date this group was created, use Utils.getNowTime() if now, or pass null and this will be called for you.
	 * @param userIdWhoCreated The userId who created this group, if null or -1 is passed, the phones userId will be used.
	 * @param allowOthersToAddMembers boolean to allow others to add members. 
	 * @param latitude The latitude of this group location pass null for no location
	 * @param longitude The longitude of this group location pass null for no location
	 * @param allowPublicWithinDistance allow anybody to join group if there are within this many miles of group (pass -1) to not allow
	 * @return The rowId of the group, -1 if it failed.
	 */
	public long makeNewGroup(
			Context ctx,
			String groupName,
			long groupPictureId,
			String dateCreated,
			long userIdWhoCreated,
			boolean allowOthersToAddMembers,
			Double latitude,
			Double longitude,
			double allowPublicWithinDistance){
		
		// override some values and/or check
		if (dateCreated == null || dateCreated.length() == 0)
			dateCreated = Utils.getNowTime();
		if (groupName == null || groupName.length() ==0)
			throw new IllegalArgumentException("groupName must not be null length > 0");
		if (userIdWhoCreated == -1)
			userIdWhoCreated = Prefs.getUserId(ctx);
		if ((latitude == null || longitude == null) &&
				allowPublicWithinDistance >= 0)
			throw new IllegalArgumentException("cannot allow public within a distance >= 0 if lat or long are null");
		
		
		// create values
		ContentValues values = new ContentValues();
		values.put(KEY_PICTURE_ID, groupPictureId);
		values.put(KEY_DATE_CREATED, dateCreated);
		values.put(KEY_USER_ID_CREATED, userIdWhoCreated);
		values.put(KEY_ALLOW_OTHERS_ADD_MEMBERS, allowOthersToAddMembers);
		values.put(KEY_LATITUDE, latitude);
		values.put(KEY_LATITUDE, longitude);
		values.put(KEY_ALLOW_PUBLIC_WITHIN_DISTANCE, allowPublicWithinDistance);				
		
		// create new row
		long newRow = database.insert(
				TABLE_NAME,
				null,
				values);
		
		return newRow;
	}
			
			
}
