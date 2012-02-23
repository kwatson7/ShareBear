package com.instantPhotoShare.Adapters;


public class CommentsAdapter {
	
	/** Table name */
	public static final String TABLE_NAME = "commentInfo";
	
	// comments table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_SERVER_ID = "serverId";
	public static final String KEY_USER_ID = "commentsUserId";
	public static final String KEY_PICTURE_ID = "commentsPictureId";
	public static final String KEY_GROUP_ID = "commentsGroupId";
	public static final String KEY_DATE_MADE = "commentsDateMade";
	public static final String KEY_TEXT = "commentsText";
	public static final String KEY_THUMBS_UP = "commentsThumbsUp";
	public static final String KEY_FLAG_INNAPROPRIATE = "commentsFlagInnapropriate";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	
	public static String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_SERVER_ID +" integer, "
		+KEY_USER_ID +" integer not null, "
		+KEY_PICTURE_ID +" integer not null, "
		+KEY_DATE_MADE + " text not null, "
		+KEY_GROUP_ID + " integer not null, "
		+KEY_TEXT + " text, "
		+KEY_THUMBS_UP + " boolean default 'false', "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_FLAG_INNAPROPRIATE + " boolean default 'false', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_USER_ID +") references " +UsersAdapter.TABLE_NAME +"(" +UsersAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_GROUP_ID +") references " +GroupsAdapter.TABLE_NAME +"(" +GroupsAdapter.KEY_ROW_ID + "), " 
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + ")" 
		+");";
}
