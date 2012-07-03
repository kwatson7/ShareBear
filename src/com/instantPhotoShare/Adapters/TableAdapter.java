package com.instantPhotoShare.Adapters;

import java.util.ArrayList;
import java.util.HashSet;

import com.tools.CursorWrapper;
import com.tools.CustomActivity;
import com.tools.TwoObjects;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TableAdapter <TYPE extends CursorWrapper<TYPE>>
extends CursorWrapper<TYPE>{

	// Database protected fields
	protected SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	protected Context ctx;
	
	public TableAdapter(Context context) {
		ctx = context;
		dbHelper = DatabaseHelper.getHelper(context);
		open();
	}
	
	/**
	 * Call this method to upgrade database that require a customActivity
	 * @param act Activity required to show progress dialog
	 */
	public void customUpgrade(CustomActivity act){
		if (dbHelper.getOldVersion() < 10 && DatabaseHelper.DATABASE_VERSION >= 10){
			GroupsAdapter groups = new GroupsAdapter(act);
			groups.upgradeToVersion10(act);
		}
	}

	/**
	 * Open the database and store the writableDatabase in database variable
	 * @throws SQLException
	 */
	private void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	private void closeDatabase() {
		dbHelper.close();
	}	
	
	/**
	 * Return a cursor over all groups and all columns of this database
	 * @return
	 */
	protected Cursor fetchAllCursor(String TABLE_NAME){

		return database.query(TABLE_NAME, null, null, null, null, null, null);
	}

	/**
	 * Create a selection and selection args to search for a given column for various values. <br>
	 * ie. columnName = "_id", and columnValues = {2, 3, 6}. This creates the selection criteria to do a 
	 * query to return the rows that have an _id of 2,3, or 6.
	 * @param columnName The column to search on
	 * @param columnValues The acceptable values for that column
	 * @return TwoObjects, where mObject1 is selection, and mObject2 is selectionArgs
	 */
	protected static <TYPE2>TwoObjects<String, String[]> createSelection(
			String columnName,
			ArrayList <TYPE2> columnValues){

		// create the selection string, and the selection args
		StringBuilder builder = new StringBuilder();
		String[] selectionArgs = new String[columnValues.size()];
		for (int i = 0; i < columnValues.size(); i++){
			builder.append(columnName);
			if (i < columnValues.size() -1)
				builder.append(" = ? OR ");
			else
				builder.append(" = ?");
			selectionArgs[i] = columnValues.get(i).toString();
		}
		String selection = builder.toString();
		
		// return the output
		return new TwoObjects<String, String[]>(selection, selectionArgs);
	}
	
	/**
	 * Query the cursor and return only the values of valuesToCheck that are not present in the database. <br>
	 * For example, if valuesToCheck is {1, 2, 3, 4} and the database at the column keyToCompare only has {1, 2, 5}, then
	 * we will return {3, 4}
	 * @param TABLE_NAME The table to query
	 * @param keyToCompare The key that valuesToCheck correspond to
	 * @param valuesToCheck The values to check if they are already in the table
	 * @return The values that were not already in the table.
	 */
	protected HashSet<String> getNewValues(String TABLE_NAME, String keyToCompare, HashSet<String> valuesToCheck){
		// query the cursor
		Cursor cursor = database.query(
				TABLE_NAME,
				new String[] {keyToCompare},
				null, null, null, null, null);
		
		// null cursor
		if (cursor == null)
			return null;
		
		// loop over cursor filling values
		HashSet<String> values = new HashSet<String>(cursor.getCount());
		while(cursor.moveToNext()){
			values.add(cursor.getString(0));
		}
		cursor.close();
		
		// now remove all values that are already present values
		HashSet<String> workingCopy = new HashSet<String>(valuesToCheck);
		workingCopy.removeAll(values);
		
		return workingCopy;
	}
}
