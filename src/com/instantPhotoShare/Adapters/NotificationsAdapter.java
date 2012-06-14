package com.instantPhotoShare.Adapters;

import java.util.ArrayList;

import com.instantPhotoShare.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class NotificationsAdapter
extends TableAdapter <NotificationsAdapter>{

	/** Table name */
	public static final String TABLE_NAME = "notifications";
	
	// notification table keys
	private static final String KEY_ROW_ID = "_id";
	private static final String KEY_MESSAGE = "message";
	private static final String KEY_DATE = "date";
	private static final String KEY_NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
	private static final String KEY_NOTIFICATION_HELPER_DATA = "KEY_NOTIFICATION_HELPER_DATA";
	
	// types
	private static final String NOTIFICATION_HELPER_DATA_TYPE = " text";
	
	// notification types
	public enum NOTIFICATION_TYPES {
		/** There was some type of device error */
		DEVICE_ERROR,
		/** Some type of server error */
		SERVER_ERROR,
		/** Miscellaneous */
		MISC,
		/** Warning */
		WARNING,
		/** You have been added to a new group */
		ADD_TO_NEW_GROUP,
		/** There is a new picture in one of your groups */
		NEW_PICTURE_IN_GROUP;
		private static NOTIFICATION_TYPES convert(int value)
		{
			return NOTIFICATION_TYPES.class.getEnumConstants()[value];
		}
	}
	
	/** table creation string */
	public static final String TABLE_CREATE =
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_NOTIFICATION_TYPE +" integer not null, "
		+KEY_MESSAGE +" text not null, "
		+KEY_NOTIFICATION_HELPER_DATA + NOTIFICATION_HELPER_DATA_TYPE + ", "
		+KEY_DATE +" text not null"
		+");";
	
	public NotificationsAdapter(Context context) {
		super(context);
	}

	/**
	 * Create a notification tagged with the current time
	 * @param message The message of the notification.
	 * @param helperData, data that can be accessed later to help further specify this notification.
	 * @return The id of the row created, -1 if failed.
	 */
	public long createNotification(
			String message,
			NOTIFICATION_TYPES notificationType,
			String helperData){		
		
		ContentValues values = new ContentValues();
		values.put(KEY_MESSAGE, message);
		values.put(KEY_DATE, Utils.getNowTime());
		if (helperData != null)
			values.put(KEY_NOTIFICATION_HELPER_DATA, helperData);
		values.put(KEY_NOTIFICATION_TYPE, notificationType.ordinal());
		
		return database.insert(TABLE_NAME, null, values);
	}
	
	/**
	 * Return a list of SQL statements to perform upon an upgrade from oldVersion to newVersion.
	 * @param oldVersion old version of database
	 * @param newVersion new version of database
	 * @return The arraylist of sql statements. Will not be null.
	 */
	public static ArrayList<String> upgradeStrings(int oldVersion, int newVersion){
		ArrayList<String> out = new ArrayList<String>(1);
		if (oldVersion < 8 && newVersion >= 8){
			String upgradeQuery = 
					"ALTER TABLE " +
							TABLE_NAME + " ADD COLUMN " + 
							KEY_NOTIFICATION_HELPER_DATA + " "+
							NOTIFICATION_HELPER_DATA_TYPE;
			out.add(upgradeQuery);
		}

		return out;
	}

	/**
	 * Delete the notification with the given rowId
	 * 
	 * @param serverId id of picture to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteNotification(long rowId) {

		return database.delete(
				TABLE_NAME,
				KEY_ROW_ID + "='" + rowId + "'",
				null) > 0;
	}
	
	/**
	 * Get the notification type represented by a string from an integer.
	 * @param ordinal The ordinal integer represented which enum in NOTIFICATION_TYPES
	 * @return The string of the notification type.
	 */
	public static String getNotificationType(int ordinal){
		return NOTIFICATION_TYPES.convert(ordinal).toString();
	}

	/**
	 * Return a Cursor over the list of all pictures in the database
	 * 
	 * @return Cursor over all pictures
	 */
	public Cursor fetchAllNotifications() {

		return database.query(TABLE_NAME, null, null, null, null, null, null);		
	}	

	/**
	 * Return an arraylist of all the messages
	 * 
	 * @return ArrayList of all picture names
	 */
	public ArrayList<String> fetchAllNotificationMessages() {

		// grab cursor to pictures
		Cursor cursor = fetchAllNotifications();

		// initialize array
		ArrayList<String> messages = new ArrayList<String>();

		// null cursor, just return empty arraylist
		if (cursor==null)
			return messages;

		// loop across cursor grabbing picture names
		while (cursor.moveToNext()){
			messages.add(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE)));
		}

		cursor.close();
		return messages;
	}

	/**
	 * Return a Cursor positioned at the notification that matches the given rowId
	 * 
	 * @param rowId id of picture to retrieve
	 * @return Cursor positioned to matching message, if found
	 */
	public Cursor fetchNotification(long rowId){

		Cursor cursor =

			database.query(
					true,
					TABLE_NAME,
					null,
					KEY_ROW_ID + "='" + rowId +"'",
					null,
					null,
					null,
					null,
					null);

		return cursor;
	}

	@Override
	protected void setColumnNumbers() throws IllegalArgumentException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
		
	}
}
