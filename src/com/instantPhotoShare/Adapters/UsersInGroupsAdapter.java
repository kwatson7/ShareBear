package com.instantPhotoShare.Adapters;


import android.content.Context;

public class UsersInGroupsAdapter
extends TableAdapter{

	/** Table name */
	public static final String TABLE_NAME = "UsersInGroupsInfo";
	
	// group table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_USER_ID = "userId";
	public static final String KEY_GROUP_ID = "groupId";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	
	/** Table creation string */
	public static final String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_USER_ID +" integer not null, "
		+KEY_GROUP_ID +" integr not null, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_USER_ID +") references " +UsersAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_GROUP_ID +") references " +GroupsAdapter.TABLE_NAME +"(" +GroupsAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public UsersInGroupsAdapter(Context context) {
		super(context);
	}
}
