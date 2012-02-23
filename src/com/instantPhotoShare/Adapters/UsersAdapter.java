package com.instantPhotoShare.Adapters;


import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class UsersAdapter
extends TableAdapter{

	/** Table name */
	public static final String TABLE_NAME = "userInfo";
	
	// user table keys
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_SERVER_ID = "serverId";
	public static final String KEY_EMAILS = "userEmails";
	public static final String KEY_PHONES = "userPhones";	
	public static final String KEY_PICTURE_ID = "pictureId";
	public static final String KEY_DATE_JOINED = "dateJoined";
	public static final String KEY_HAS_ACCOUNT = "hasAccount";
	public static final String KEY_IS_UPDATING = "isUpdating";
	public static final String KEY_IS_SYNCED = "isSynced";
	public static final String KEY_DEFAULT_CONTACT = "defaultContact";
	public static final String KEY_CONTACTS_ROW_ID = "contactsRowId";
	
	/** Table creation string */
	public static String TABLE_CREATE = 
		"create table "
		+TABLE_NAME +" ("
		+KEY_ROW_ID +" integer primary key autoincrement, "
		+KEY_NAME +" text, "
		+KEY_SERVER_ID +" integer, "
		+KEY_EMAILS +" text, "
		+KEY_PHONES +" text, "
		+KEY_PICTURE_ID +" integer, "
		+KEY_DATE_JOINED +" text, "
		+KEY_HAS_ACCOUNT +" boolean DEFAULT 'FALSE', "
		+KEY_IS_UPDATING +" boolean DEFAULT 'FALSE', "
		+KEY_IS_SYNCED +" boolean DEFAULT 'FALSE', "
		+KEY_DEFAULT_CONTACT + " TEXT, "
		+KEY_CONTACTS_ROW_ID + " integer DEFAULT '-1', "
		+"foreign key(" +KEY_PICTURE_ID +") references " +PicturesAdapter.TABLE_NAME +"(" +PicturesAdapter.KEY_ROW_ID + ")" 
		+");";
	
	public UsersAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Get a cursor for all the users that are linked to the group groupId
	 * in UsersInGroups table
	 * @param groupId
	 * @return A cursor on the users that match the groupId in UsersInGroups table
	 */
	public Cursor getUsersInGroup(long groupId){

		// create the query where we match up all the pictures that are in the group
		String query = 
			"SELECT users.* FROM "
			+UsersAdapter.TABLE_NAME + " users "
			+" INNER JOIN "
			+UsersInGroupsAdapter.TABLE_NAME + " groups "
			+" ON "
			+"users." + UsersAdapter.KEY_ROW_ID + " = "
			+"groups." + UsersInGroupsAdapter.KEY_USER_ID
			+" WHERE "
			+"groups." + UsersInGroupsAdapter.KEY_GROUP_ID
			+"=?";

		// do the query
		Cursor cursor = database.rawQuery(
				query,
				new String[]{String.valueOf(groupId)});

		// return the cursor
		return cursor;
	}
	
	/**
	 * Get the row id from the phone's contact database that aligns with this "user" rowId
	 * @param rowId The rowId in the "user" database
	 * @return the rowId in the contacts database on the phone, -1 if none.
	 */
	public int getRowIdFromContactsDatabase(int rowId){
		
		return getIntFrowRowId(rowId, KEY_CONTACTS_ROW_ID);
	}
	
	/**
	 * Update or create new link between this current user and the contacts database user
	 * @param usersRowId The row id of the "users" database this contact is located at
	 * @param contactsRowId The row id in the contacts database
	 */
	public void setRowIdFromContactsDatabase(int usersRowId, int contactsRowId){
		
		// the value to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);	
		
		// The where clause
		String where = 
			KEY_ROW_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(usersRowId)};
		
		// update the row
		database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
	}
	
	/**
	 * Get the default contact info, either an email string or phone string, for this row id.
	 * This searches on the "users" databse rowId. To search on the contacts database row id, see getDefaultContactInfoFromContactsDatabaseRowId
	 * @param rowId the rowId of the contact
	 * @return a string of either an email or phone, null if none found.
	 */
	public String getDefaultContactInfo(int rowId){
		
		return getStringFrowRowId(rowId, KEY_DEFAULT_CONTACT);
	}
	
	/**
	 * Get the default contact info, either an email string or phone string, for a given contacts database row id.
	 * This searches on the "contacts" databse rowId. To search on the "users" database row id, see getDefaultContactInfo
	 * @param rowId the rowId of the contact in the contacts databse
	 * @return a string of either an email or phone, null if none found.
	 */
	public String getDefaultContactInfoFromContactsDatabaseRowId(int rowId){
		ArrayList<String> tmp = getStringsFrowColumnName(KEY_CONTACTS_ROW_ID, String.valueOf(rowId), new String[] {KEY_DEFAULT_CONTACT});
		if (tmp.size() > 0)
			return tmp.get(0);
		else
			return null;
	}
	
	/**
	 * Set the default contact info based on the id from the contacts database, not the "users" database
	 * @param contactsRowId The contact Id
	 * @param defaultContact The default contact info, ie "johnsmith@gmail.com"
	 */
	public void setDefaultContactInfoFromContactsDatabaseRowId(int contactsRowId, String defaultContact){
		// the value to update
		ContentValues values = new ContentValues(2);
		values.put(KEY_CONTACTS_ROW_ID, contactsRowId);	
		values.put(KEY_DEFAULT_CONTACT, defaultContact);
		
		// The where clause
		String where = 
			KEY_CONTACTS_ROW_ID + " = ?";
		
		// The selection args
		String[] selectionArgs = {String.valueOf(contactsRowId)};
		
		// update the row
		int affected = database.update(TABLE_NAME,
				values,
				where,
				selectionArgs);
		if (affected == 0){
			int test = (int) database.insert(TABLE_NAME, null, values);
			int kyle = 6;
		}
	}
	
	/**
	 * Get the rowId in this database given the contacts Database row id
	 * @param contactRowId The contacts Database rowId
	 * @return the "users" database row id, -1 if not found.
	 */
	public int getRowIdGivenContactsDatabaseRowId(int contactRowId){
		
		// default output
		int output = -1;
		
		// create the query
		String selection = 
			KEY_CONTACTS_ROW_ID+ " = ?";
		
		// the selection args
		String[] selectionArgs = {String.valueOf(contactRowId)};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {KEY_ROW_ID},
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the row id
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROW_ID));
		cursor.close();
		return output;
	}
	
	
	// helper functions
	
	/**
	 * Get an int from the table for a given rowID and a given column. -1 if none found
	 * @param rowId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, -1 if none found.
	 */
	private int getIntFrowRowId(int rowId, String columnName){
		
		// default output
		int output = -1;
		
		// create the query
		String selection = 
			KEY_ROW_ID + " = ?";
		
		// the selection args
		String[] selectionArgs = {String.valueOf(rowId)};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the row id
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}
	
	/**
	 * Get a string from the table for a given rowID and a given column. null if none found
	 * @param rowId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, null if none found.
	 */
	private String getStringFrowRowId(int rowId, String columnName){
		
		// default output
		String output = null;
		
		// create the query
		String selection = 
			KEY_ROW_ID + " = ?";
		
		// the selection args
		String[] selectionArgs = {String.valueOf(rowId)};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the row id
		output =  cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}
	
	/**
	 * Get an array of strings returned from grabbing the values at an array of column name.
	 * The rows are determined by searching the columnName == columnValue.
	 * If multiple rows, then only the first is returned
	 * The output array will be empty if there are no matches
	 * @param columnName the column name to search on
	 * @param columnValue the value at the searching column
	 * @param returnColumns, the name of all the columns we want returned
	 * @return the value at all of the columns in returnColumns
	 */
	private ArrayList<String> getStringsFrowColumnName(
			String columnName,
			String columnValue,
			String[] returnColumns){
		
		// default output
		ArrayList<String> output = new ArrayList<String>(returnColumns.length);
		
		// create the query
		String selection = 
			columnName + " = ?";
		
		// the selection args
		String[] selectionArgs = {columnValue};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				returnColumns,
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the values
		for (int i = 0; i < returnColumns.length; i++)
			output.add(i, cursor.getString(cursor.getColumnIndexOrThrow(returnColumns[i])));
		cursor.close();
		return output;
	}
	
	/**
	 * Get an int of string returned from grabbing the value at a column name.
	 * The rows are determined by searching the columnName == columnValue.
	 * If multiple rows, then only the first is returned.
	 * Default value is -1;
	 * @param columnName the column name to search on
	 * @param columnValue the value at the searching column
	 * @param returnColumn, the name of the column we want returned
	 * @return the value at the column in returnColumn
	 */
	private int getIntFrowColumnName(
			String columnName,
			String columnValue,
			String returnColumn){
		
		// default output
		int output = -1;
		
		// create the query
		String selection = 
			columnName + " = ?";
		
		// the selection args
		String[] selectionArgs = {columnValue};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String[]{returnColumn},
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the value
		output =  cursor.getInt(cursor.getColumnIndexOrThrow(returnColumn));
		cursor.close();
		return output;
	}
	
	/**
	 * Get a string from the table for a given rowID and a given column. null if none found
	 * @param rowId the rowId to search on
	 * @param columnName The column name will throw exception if name is not found
	 * @return the value at that row and column name, null if none found.
	 */
	private String getStringFrowContactsDatabaseRowId(int rowId, String columnName){
		
		// default output
		String output = null;
		
		// create the query
		String selection = 
			KEY_ROW_ID + " = ?";
		
		// the selection args
		String[] selectionArgs = {String.valueOf(rowId)};
		
		// do the query
		Cursor cursor = database.query(
				TABLE_NAME,
				new String [] {columnName},
				selection,
				selectionArgs,
				null,
				null,
				null);
		
		// check for null
		if (cursor == null)
			return output;
		
		// move to first position in cursor
		if (!cursor.moveToFirst()){
			cursor.close();
			return output;
		}
		
		// grab the row id
		output =  cursor.getString(cursor.getColumnIndexOrThrow(columnName));
		cursor.close();
		return output;
	}
}
