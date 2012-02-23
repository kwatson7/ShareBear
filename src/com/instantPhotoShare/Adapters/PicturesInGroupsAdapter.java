package com.instantPhotoShare.Adapters;


import android.content.Context;

public class PicturesInGroupsAdapter
extends TableAdapter{

	/** Table name */
	public static final String TABLE_NAME = "PicturesInGroupsInfo";
	
	// group table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_GROUP_ID = "groupId";
	public static final String KEY_PICTURE_ID = "pictureId";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	
	/** Table creation string */
	public static final String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_GROUP_ID +" integer not null, "
		+KEY_PICTURE_ID +" integer not null, "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_GROUP_ID +") references " +GroupsAdapter.TABLE_NAME +"(" +GroupsAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public PicturesInGroupsAdapter(Context context) {
		super(context);
	}
}
