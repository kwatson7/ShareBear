package com.instantPhotoShare.Adapters;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TableAdapter {

	// Database protected fields
	protected SQLiteDatabase database;
	protected DatabaseHelper dbHelper;
	
	public TableAdapter(Context context) {
		dbHelper = DatabaseHelper.getHelper(context);
		open();
	}

	/**
	 * Open the database and store the writableDatabase in database variable
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}	
}
