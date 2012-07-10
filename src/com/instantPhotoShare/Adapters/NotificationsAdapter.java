package com.instantPhotoShare.Adapters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.instantPhotoShare.Utils;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

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
	private static final String KEY_IS_NEW = "KEY_IS_NEW";

	// types
	private static final String NOTIFICATION_HELPER_DATA_TYPE = " text";
	private static final String IS_NEW_TYPE = " BOOLEAN DEFAULT '1'";

	// other constants
	private static final String SORT_ORDER = 
		KEY_IS_NEW + " DESC, "
		+KEY_ROW_ID + " DESC";						// sort the notifications newest, and then the most recent

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
		+KEY_IS_NEW + IS_NEW_TYPE + ", "
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
		if (oldVersion < 9 && newVersion >= 9){
			String upgradeQuery = 
				"ALTER TABLE " +
				TABLE_NAME + " ADD COLUMN " + 
				KEY_IS_NEW + " "+
				IS_NEW_TYPE;
			out.add(upgradeQuery);
		}

		return out;
	}

	/**
	 * 
	 * @return the rowId of this notification, or -1 if none
	 */
	public long getRowId(){
		if (!checkCursor())
			return -1;
		else
			return getLong(KEY_ROW_ID);
	}

	/**
	 * Return the main message of this notification. Will return "" if we could not read correctly.
	 * @return
	 */
	public String getMessage(){
		if (!checkCursor())
			return "";
		else
			return getString(KEY_MESSAGE);
	}

	/**
	 * Return if this notification is new. False if we can't read.
	 * @return
	 */
	public boolean isNew(){
		if (!checkCursor())
			return false;
		else
			return getBoolean(KEY_IS_NEW);
	}

	/**
	 * How long ago this notification was made. It is a formatted string. Returns Unknown if we cannot read.
	 * @return
	 */
	public String getTimeAgoNotification(){
		long nowMs;
		long dateMs;
		try {
			nowMs = Utils.parseMilliseconds(Utils.getNowTime());
			dateMs = Utils.parseMilliseconds(getDate());
		} catch (ParseException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			return "Unknown";
		}
		double minutes = (nowMs - dateMs)/60000.0f;
		return com.tools.Tools.convertMinutesToFormattedString(minutes, 0, 2);
	}

	/**
	 * Return the date the notification was made, or 1900-01-01 00:00:00 if none.
	 * @return The 
	 */
	public String getDate(){
		if (!checkCursor())
			return "1900-01-01 00:00:00";
		else
			return getString(KEY_DATE);
	}

	/**
	 * Return the notification type. Will return WARNING if cannot be read
	 * @return
	 */
	public NOTIFICATION_TYPES getType(){
		if (!checkCursor())
			return NOTIFICATION_TYPES.WARNING;
		else
			return NOTIFICATION_TYPES.convert(getInt(KEY_NOTIFICATION_TYPE));
	}

	/**
	 * Return the helper data associated with this notification as a long. -1 if not present
	 * @return
	 */
	public long getHelperLong(){
		if (!checkCursor())
			return -1;
		else
			try{
				return Long.valueOf(getString(KEY_NOTIFICATION_HELPER_DATA));
			}catch(NumberFormatException e){
				return -1;
			}
	}
	
	/**
	 * Set the current notification to the new value
	 * @param isNew true for new, false for old
	 */
	public void setIsNew(boolean isNew){
		long rowId = getRowId();
		if (rowId == -1)
			return;
		
		// which row to update
		String where = KEY_ROW_ID + " =?";
		
		// what to update with
		ContentValues values = new ContentValues(1);
		values.put(KEY_IS_NEW, isNew);
		
		// update the values
		database.update(TABLE_NAME, values, where, new String[] {String.valueOf(rowId)});
	}
	
	/**
	 * Set the notification for all notifications to be as given
	 * @param isNew true for new, false for old
	 */
	public void setAllIsNew(boolean isNew){
		
		// what to update with
		ContentValues values = new ContentValues(1);
		values.put(KEY_IS_NEW, isNew);
		
		// update the values
		database.update(TABLE_NAME, values, null, null);
	}
	
	public void deleteAll(){
		database.delete(TABLE_NAME, null, null);
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
	 * @return Cursor over all pictures
	 */
	public void fetchAllNotifications() {

		setCursor(database.query(TABLE_NAME, null, null, null, null, null, SORT_ORDER));		
	}	

	/**
	 * Fetch all notifications on a background thread. The notificatiosn will be returned in the callback
	 * @param <ACTIVITY_TYPE>
	 * @param act The caling activity
	 * @param callback The callback to run when we are finished
	 */
	public <ACTIVITY_TYPE extends CustomActivity> void
	fetchAllNotificationsInBackground(ACTIVITY_TYPE act, CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, NotificationsAdapter> callback){
		new GetAllNotifications<ACTIVITY_TYPE>(act, callback).execute();
	}

	/**
	 * Return an arraylist of all the messages
	 * 
	 * @return ArrayList of all notifications
	 */
	public ArrayList<String> fetchAllNotificationMessages() {

		// grab cursor to pictures
		fetchAllNotifications();

		// initialize array
		ArrayList<String> messages = new ArrayList<String>();

		// loop across cursor grabbing picture names
		while (moveToNext()){
			messages.add(getString(KEY_MESSAGE));
		}

		close();
		return messages;
	}

	/**
	 * Return a Cursor positioned at the notification that matches the given rowId 
	 * @param rowId id of picture to retrieve
	 */
	public void fetchNotification(long rowId){

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

		setCursor(cursor);
		moveToFirst();
	}

	/**
	 * Return the number of new notifications. -1 if we failed to read
	 * @return
	 */
	private int getNumberNewNotificationsHelper(){
		String query = "select count(*) from " + TABLE_NAME + " where " + KEY_IS_NEW + " ='1'";
		Cursor countCursor = database.rawQuery(query, null);
		if (countCursor == null || !countCursor.moveToFirst()){
			if (countCursor != null)
				countCursor.close();
			return -1;
		}
		int count= countCursor.getInt(0);
		countCursor.close();
		return count;
	}

	/**
	 * Determine the number of new notifications. The number will be returned in teh callback
	 * @param <ACTIVITY_TYPE>
	 * @param act The caling activity, can be null
	 * @param callback The callback to run when we are finished
	 */
	public <ACTIVITY_TYPE extends CustomActivity> void
	getNumberNewNotifications(ACTIVITY_TYPE act, CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, Integer> callback){
		new CountNewNotifications<ACTIVITY_TYPE>(act, callback).execute();
	}

	/**
	 * Class for counting the number of unread tasks
	 * @param <ACTIVITY_TYPE>
	 */
	private class CountNewNotifications <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, Integer>{

		public CountNewNotifications(
				ACTIVITY_TYPE act,
				CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, Integer> callback) {
			super(act, -1, false, true,null);
			setFinishedCallback(callback);
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected Integer doInBackground(Void... params) {
			return getNumberNewNotificationsHelper();
		}

		@Override
		protected void onProgressUpdate(Void... progress) {			
		}

		@Override
		protected void onPostExectueOverride(Integer result) {

		}

		@Override
		protected void setupDialog() {

		}
	}

	/**
	 * Class for getting all the notifications on a background thread. Shows a dialog
	 * @param <ACTIVITY_TYPE>
	 */
	private class GetAllNotifications <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, NotificationsAdapter>{

		public GetAllNotifications(
				ACTIVITY_TYPE act,
				CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, NotificationsAdapter> callback) {
			super(act, -1, false, true, null);
			setFinishedCallback(callback);
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected NotificationsAdapter doInBackground(Void... params) {
			fetchAllNotifications();
			return NotificationsAdapter.this;
		}

		@Override
		protected void onProgressUpdate(Void... progress) {			
		}

		@Override
		protected void onPostExectueOverride(NotificationsAdapter result) {

		}

		@Override
		protected void setupDialog() {
			if (callingActivity != null){
				dialog = new ProgressDialog(callingActivity);
				dialog.setMessage("Loading Notifications...");
				dialog.setIndeterminate(true);
			}
		}
	}
}
