package com.instantPhotoShare.Adapters;

import java.util.ArrayList;

import com.tools.CursorWrapper;
import com.tools.TwoObjects;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public abstract class TableAdapter <TYPE extends CursorWrapper<TYPE>>
extends CursorWrapper<TYPE>{

	// Database protected fields
	protected SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	
	public TableAdapter(Context context) {
		dbHelper = DatabaseHelper.getHelper(context);
		open();
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
	protected <TYPE2>TwoObjects<String, String[]> createSelection(
			String columnName,
			ArrayList <TYPE2> columnValues){

		// create the selection string, and the selection args
		String selection = "";
		String[] selectionArgs = new String[columnValues.size()];
		for (int i = 0; i < columnValues.size(); i++){
			selection += 
				columnName + " = ? OR ";
			selectionArgs[i] = columnValues.get(i).toString();
		}
		if (selection.length() >= 3)
			selection = selection.substring(0, selection.length()-3);

		// return the output
		return new TwoObjects<String, String[]>(selection, selectionArgs);
	}
}
