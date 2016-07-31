package com.instantPhotoShare;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Simple picture database access helper class. Defines the basic CRUD operations
 * for the picture info, and gives the ability to list all pictures as well as
 * retrieve or modify a specific picture.
 * 
 */
public class Database {

	private static final String DATABASE_NAME = "data.db";
	private static final int DATABASE_VERSION = 1;

	private static final String TAG = "InstantPhotoShareDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context mCtx;

	// picture table keys
	private static final String DATABASE_TABLE_PICTURE = "pictureInfo";
	public static final String KEY_PICTURE_SERVER_ID = "_id";
	public static final String KEY_PICTURE_PATH = "picturePath";
	public static final String KEY_PICTURE_THUMBNAIL_PATH = "pictureThumbnailPath";	
	public static final String KEY_PICTURE_DATE_TAKEN = "dateTaken";
	public static final String KEY_PICTURE_USER_ID_TOOK_PICTURE = "userIdWhoTookPicture";
	public static final String KEY_PICTURE_GROUP_ID = "pictureGroupId";
	public static final String KEY_PICTURE_LATITUDE = "pictureLatitude";
	public static final String KEY_PICTURE_LONGITUE = "pictureLongitude";
	public static final String KEY_PICTURE_IS_UPDATING = "isUpdating";
	public static final String KEY_PICTURE_IS_SYNCED = "isSynced";
	
	// group table keys
	private static final String DATABASE_TABLE_GROUP = "groupInfo";
	public static final String KEY_GROUP_NAME = "groupName";
	public static final String KEY_GROUP_SERVER_ID = "_id";
	public static final String KEY_GROUP_PICTURE_ID = "groupPictureId";	
	public static final String KEY_GROUP_DATE_CREATED = "dateCreated";
	public static final String KEY_GROUP_USER_ID_CREATED = "userIdWhoCreatedGroup";
	public static final String KEY_GROUP_ALLOW_OTHERS_ADD_MEMBERS = "allowOthersAddMembers";
	public static final String KEY_GROUP_LATITUDE = "latitudeGroup";
	public static final String KEY_GROUP_LONGITUDE = "longitudeGroup";
	public static final String KEY_GROUP_ALLOW_PUBLIC_WITHIN_DISTANCE = "allowPublicWithinDistance";
	public static final String KEY_GROUP_IS_UPDATING = "isUpdating";
	public static final String KEY_GROUP_IS_SYNCED = "isSynced";
	
	// user table keys
	private static final String DATABASE_TABLE_USERS = "userInfo";
	public static final String KEY_USER_NAME = "name";
	public static final String KEY_USER_SERVER_ID = "_id";
	public static final String KEY_USER_EMAILS = "userEmails";
	public static final String KEY_USER_PHONES = "userPhones";	
	public static final String KEY_USER_PICTURE_ID = "pictureId";
	public static final String KEY_USER_DATE_JOINED = "dateJoined";
	public static final String KEY_USER_HAS_ACCOUNT = "hasAccount";
	public static final String KEY_USER_IS_UPDATING = "isUpdating";
	public static final String KEY_USER_IS_SYNCED = "isSynced";
	
	// comments table keys
	private static final String DATABASE_TABLE_COMMENTS = "commentInfo";
	public static final String KEY_COMMENTS_SERVER_ID = "_id";
	public static final String KEY_COMMENTS_USER_ID = "commentsUserId";
	public static final String KEY_COMMENTS_PICTURE_ID = "commentsPictureId";
	public static final String KEY_COMMENTS_DATE_MADE = "commentsDateMade";
	public static final String KEY_COMMENTS_TEXT = "commentsText";
	public static final String KEY_COMMENTS_THUMBS_UP = "commentsThumbsUp";
	public static final String KEY_COMMENTS_FLAG_INNAPROPRIATE = "commentsFlagInnapropriate";
	public static final String KEY_COMMENTS_IS_UPDATING = "isUpdating";
	public static final String KEY_COMMENTS_IS_SYNCED = "isSynced";

	// Database creation sql statements
	private static final String PICTURE_DATABASE_CREATE =
		"create table "
		+DATABASE_TABLE_PICTURE +" ("
		+KEY_PICTURE_SERVER_ID +" integer primary key, "
		+KEY_PICTURE_PATH +" text, "
		+KEY_PICTURE_THUMBNAIL_PATH +" text, "
		+KEY_PICTURE_DATE_TAKEN +" text, "
		+KEY_PICTURE_USER_ID_TOOK_PICTURE +" integer not null, "
		+KEY_PICTURE_LATITUDE +" DOUBLE, "
		+KEY_PICTURE_LONGITUE +" DOUBLE, "
		+KEY_PICTURE_GROUP_ID +" integer, not null"
		+KEY_PICTURE_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_PICTURE_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_PICTURE_USER_ID_TOOK_PICTURE +") references " +DATABASE_TABLE_USERS +"(" +KEY_USER_SERVER_ID + "), "
		+"foreign key(" +KEY_PICTURE_GROUP_ID +") references " +DATABASE_TABLE_GROUP +"(" +KEY_GROUP_SERVER_ID + ")"
		+");";

	
	private static final String GROUP_DATABASE_CREATE = 
		"create table "
		+DATABASE_TABLE_GROUP +" ("
		+KEY_GROUP_NAME +" text not null, "
		+KEY_GROUP_SERVER_ID +" integer primary key, "
		+KEY_GROUP_PICTURE_ID +" integer not null, "
		+KEY_GROUP_DATE_CREATED +" text not null, "
		+KEY_GROUP_USER_ID_CREATED +" integer not null, "
		+KEY_GROUP_ALLOW_OTHERS_ADD_MEMBERS +" boolean DEFAULT 'TRUE', "
		+KEY_GROUP_LATITUDE +" double, "
		+KEY_GROUP_LONGITUDE +" double, "
		+KEY_GROUP_ALLOW_PUBLIC_WITHIN_DISTANCE +" double, "
		+KEY_GROUP_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_GROUP_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_GROUP_PICTURE_ID +") references " +DATABASE_TABLE_PICTURE +"(" +KEY_PICTURE_SERVER_ID + "), " 
		+"foreign key(" +KEY_GROUP_USER_ID_CREATED +") references " +DATABASE_TABLE_USERS +"(" +KEY_USER_SERVER_ID + ")" 
		+");";
	
	private static String USER_DATABASE_CREATE = 
		"create table "
		+DATABASE_TABLE_USERS +" ("
		+KEY_USER_NAME +" text, "
		+KEY_USER_SERVER_ID +" integer primary key, "
		+KEY_USER_EMAILS +" text, "
		+KEY_USER_PHONES +" text, "
		+KEY_USER_PICTURE_ID +" integer, "
		+KEY_USER_DATE_JOINED +" text, "
		+KEY_USER_HAS_ACCOUNT +" boolean DEFAULT 'FALSE', "
		+KEY_USER_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_USER_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_USER_PICTURE_ID +") references " +DATABASE_TABLE_PICTURE +"(" +KEY_PICTURE_SERVER_ID + ")" 
		+");";
	
	private static String COMMENTS_DATABASE_CREATE = 
		"create table "
		+DATABASE_TABLE_COMMENTS +" ("
		+KEY_COMMENTS_SERVER_ID +" integer primary, "
		+KEY_COMMENTS_USER_ID +" integer not null, "
		+KEY_COMMENTS_PICTURE_ID +" integer not null, "
		+KEY_COMMENTS_DATE_MADE + " text not null, "
		+KEY_COMMENTS_TEXT + " text, "
		+KEY_COMMENTS_THUMBS_UP + " boolean default 'false', "
		+KEY_COMMENTS_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_COMMENTS_FLAG_INNAPROPRIATE + " boolean default 'false', "
		+KEY_COMMENTS_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+"foreign key(" +KEY_COMMENTS_USER_ID +") references " +DATABASE_TABLE_USERS +"(" +KEY_USER_SERVER_ID + "), " 
		+"foreign key(" +KEY_COMMENTS_PICTURE_ID +") references " +DATABASE_TABLE_PICTURE +"(" +KEY_PICTURE_SERVER_ID + ")" 
		+");";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(PICTURE_DATABASE_CREATE);
			db.execSQL(GROUP_DATABASE_CREATE);
			db.execSQL(USER_DATABASE_CREATE);
			db.execSQL(COMMENTS_DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE_PICTURE);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE_GROUP);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE_USERS);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE_COMMENTS);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public Database(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public Database open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}


	/**
	 * Create a new picture using the info. If the picture is
	 * successfully created return the new rowId for that picture, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param serverId The id of picture on the server
	 * @param path The path to the picture on the phone
	 * @param pathThumbnail The path to the thumbnail on the phone
	 * @param dateTaken Date the picture was taken
	 * @param idWhoTook The id of the user who took the picture
	 * @param isSynced mark true, if we pulled data from server, so we know it's already synced,
	 * or false, if we have just taken the picture and have not yet synced to server.
	 * @return rowId or -1 if failed
	 */
	public long createPicture(
			int serverId, 
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			int idWhoTook, 
			boolean isSynced){		
		
		ContentValues values = new ContentValues();
		values.put(KEY_PICTURE_SERVER_ID, serverId);
		values.put(KEY_PICTURE_PATH, path);
		values.put(KEY_PICTURE_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_PICTURE_DATE_TAKEN, dateTaken);
		values.put(KEY_PICTURE_USER_ID_TOOK_PICTURE, idWhoTook);
		values.put(KEY_PICTURE_IS_SYNCED, isSynced);

		return mDb.insert(DATABASE_TABLE_PICTURE, null, values);
	}

	/**
	 * Delete the picture with the given rowId
	 * 
	 * @param rowId id of picture to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deletePicture(long rowId) {

		return mDb.delete(
				DATABASE_TABLE_PICTURE,
				KEY_PICTURE_SERVER_ID + "='" + rowId + "'",
				null) > 0;
	}

	/**
	 * Return a Cursor over the list of all pictures in the database
	 * 
	 * @return Cursor over all pictures
	 */
	public Cursor fetchAllPictures() {

		return mDb.query(DATABASE_TABLE_PICTURE, null, null, null, null, null, null);
	}	

	/**
	 * Return an arraylist of all the picture file names
	 * 
	 * @return ArrayList of all picture names
	 */
	public ArrayList<String> fetchAllPictureNames() {

		// grab cursor to pictures
		Cursor cursor = fetchAllPictures();

		// initialize array
		ArrayList<String> pictures = new ArrayList<String>();

		// null cursor, just return empty arraylist
		if (cursor==null)
			return pictures;

		// if empty cursor then return empty arraylist
		if (!cursor.moveToFirst()){
			cursor.close();
			return pictures;
		}

		// loop across cursor grabbing picture names
		while (true){
			pictures.add(cursor.getString(cursor.getColumnIndex(KEY_PICTURE_PATH)));
			if (!cursor.moveToNext())
				break;
		}

		cursor.close();
		return pictures;
	}

	/**
	 * Return a Cursor positioned at the picture that matches the given rowId
	 * 
	 * @param rowId id of picture to retrieve
	 * @return Cursor positioned to matching picture, if found
	 * @throws SQLException if picture could not be found/retrieved
	 */
	public Cursor fetchPicture(long rowId) throws SQLException {

		Cursor mCursor =

			mDb.query(
					true,
					DATABASE_TABLE_PICTURE,
					null,
					KEY_PICTURE_SERVER_ID + "='" + rowId +"'",
					null,
					null,
					null,
					null,
					null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Update the picture using the details provided. The picture to be updated is
	 * specified using the rowId, and it is altered to use the values passed in
	 * 
	 * @param rowId id of picture to update
	 * @return true if the picture was successfully updated, false otherwise
	 */
	public boolean updatePicture(
			long rowId, 
			int serverId, 
			String path, 
			String pathThumbnail, 
			String dateTaken, 
			int idWhoTook, 
			boolean isSynced){		
		
		ContentValues values = new ContentValues();
		values.put(KEY_PICTURE_SERVER_ID, serverId);
		values.put(KEY_PICTURE_PATH, path);
		values.put(KEY_PICTURE_THUMBNAIL_PATH, pathThumbnail);
		values.put(KEY_PICTURE_DATE_TAKEN, dateTaken);
		values.put(KEY_PICTURE_USER_ID_TOOK_PICTURE, idWhoTook);
		values.put(KEY_PICTURE_IS_SYNCED, isSynced);

		return mDb.update(
				DATABASE_TABLE_PICTURE,
				values,
				KEY_PICTURE_SERVER_ID + "='" + rowId + "'", null) > 0;	
	}
}
