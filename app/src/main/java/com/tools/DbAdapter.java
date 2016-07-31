package com.tools;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.*;
import android.database.sqlite.*;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * The DBAdapter class enables program integration with a SQLite database.
 */
public class DbAdapter{
	//TODO: play around with this class and see if I like it and want to start using it.
	// private variables
	private final String databaseName; 			// the name of the database
	private final String tableName; 			// the name of the table
	private final int databaseVersion; 			// The version number
	private final String databaseCreate; 		// The sql string used to create the database
	private SQLiteDatabase db;					// Variable to hold database instant
	private myDBHelper dbHelper; 				// Database open/upgrade helper

	// public variables				
	public static final String KEY_ID = "_id";	// Index Key column
	
	// private static
	private static final String DEFAULT_KEY_TYPE = "text"; 	// The default key option if none input

	// Name of the column index of each column in DB
	public  ArrayList<String> TABLE_KEYS =  new ArrayList<String>();
	public  ArrayList<String> TABLE_OPTIONS = new ArrayList<String>();
	public  final String KEY_TIMESTAMP = "timeStamp";
	public  final int TIMESTAMP_COLUMN = 1;

	/**
	 * Open the database if it exists or create it if it doesn't. Additionally checks if the
	 * table exists and creates it if it doesn't.
	 * @param context Context passed by the parent.
	 * @param table Name of the table to operate on.
	 * @param keys Array of Key values in the table.
	 * @param options Array of options for the Key values.
	 */
	public DbAdapter(Context context, 
			String databaseName,
			int databaseVersion,
			String tableName,
			ArrayList<String> keys,
			ArrayList<String> options){

		// Start initializing all of the variables
		// Assign some inputs to class properties
		this.databaseName = databaseName;
		this.databaseVersion = databaseVersion;
		this.tableName = tableName;

		// grab database creation string
		databaseCreate = createTableCreateString(keys, options);
		Log.v("Database Creation String", databaseCreate);

		// Create a new Helper
		dbHelper = new myDBHelper(context, this.databaseName, null, this.databaseVersion,
				this.tableName, databaseCreate);
	}

	/**
	 * Open the database if it exists or create it if it doesn't. Additionally checks if the
	 * table exists and creates it if it doesn't.
	 * @param context Context passed by the parent.
	 * @param databaseName The name of the database file (eg. data.db)
	 * @param databaseVersion The version number (eg. 1)
	 * @param tableName table Name of the table to operate on. (eg. employees)
	 * @param inputs The column headings and their options (eg. "employeeName", "text not null", "height", "single").
	 * 			Each input column heading requires an option 
	 */
	public DbAdapter(Context context, 
			String databaseName,
			int databaseVersion,
			String tableName,
			String ... inputs)
	throws IllegalArgumentException{

		// Assign some inputs to class properties
		this.databaseName = databaseName;
		this.databaseVersion = databaseVersion;
		this.tableName = tableName;

		// check that inputs are evenly numbered
		if (!MathTools.isEven(inputs.length))
			throw new IllegalArgumentException
			("input strings must be even: columnHeading, headingOptions, ...");

		// grab odd inputs as headings, and even as options
		ArrayList<String> headings = new ArrayList<String>();
		ArrayList<String> options = new ArrayList<String>();
		for (int i = 0; i<inputs.length-1; i+=2){
			headings.add(inputs[i]);
			options.add(inputs[i+1]);
		}

		// grab database creation string
		databaseCreate = createTableCreateString(headings, options);
		Log.v("Database Creation String", databaseCreate);

		// Create a new Helper
		dbHelper = new myDBHelper(context, this.databaseName, null, this.databaseVersion,
				this.tableName, databaseCreate);
	}

	/**
	 * Open the connection to the database.
	 * @return Returns a DBAdapter.
	 * @throws SQLException
	 */
	public DbAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close the connection to the database.
	 */
	public void close() {
		db.close();
	}

	/**
	 * Insert a row into the database.
	 * @param key ArrayList of Keys (column headers).
	 * @param value ArrayList of Key values.
	 * @return Returns the number of the row added.
	 */
	public long insertEntry(ArrayList<String> key, ArrayList<String> value) {
		String timeStamp = new Timestamp(Calendar.getInstance().getTimeInMillis()).toString();
		ContentValues contentValues = new ContentValues();
		for(int i = 0; key.size() > i; i++){
			contentValues.put(key.get(i), value.get(i));
		}
		contentValues.put(KEY_TIMESTAMP, timeStamp);
		Log.v("Database Add", contentValues.toString());
		return db.insert(tableName, null, contentValues);
	}

	/**
	 * Remove a row from the database.
	 * @param rowIndex Number of the row to remove.
	 * @return Returns TRUE if it was deleted, FALSE if failed.
	 */
	public boolean removeEntry(long rowIndex) {
		return db.delete(tableName, KEY_ID + "=" + rowIndex, null) > 0;
	}

	/**
	 * Get all entries in the database sorted by the given value.
	 * @param columns List of columns to include in the result.
	 * @param selection Return rows with the following string only. Null returns all rows.
	 * @param selectionArgs Arguments of the selection.
	 * @param groupBy Group results by.
	 * @param having A filter declare which row groups to include in the cursor.
	 * @param sortBy Column to sort elements by.
	 * @param sortOption ASC for ascending, DESC for descending.
	 * @return Returns a cursor through the results.
	 */
	public Cursor getAllEntries(String[] columns, String selection, String[] selectionArgs,
			String groupBy, String having, String sortBy, String sortOption) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy,
				having, sortBy + " " + sortOption);
	}

	/**
	 * Does the SQL UPDATE function on the table with given SQL string
	 * @param sqlQuery an SQL Query starting at SET
	 */
	public void update(String sqlQuery) {
		db.rawQuery("UPDATE " + tableName + sqlQuery, null);		
	}

	/**
	 * Get all entries in the database sorted by the given value.
	 * @param columns List of columns to include in the result.
	 * @param selection Return rows with the following string only. Null returns all rows.
	 * @param selectionArgs Arguments of the selection.
	 * @param groupBy Group results by.
	 * @param having A filter declare which row groups to include in the cursor.
	 * @param sortBy Column to sort elements by.
	 * @param sortOption ASC for ascending, DESC for descending.
	 * @param limit limiting number of records to return
	 * @return Returns a cursor through the results.
	 */
	public Cursor getAllEntries(String[] columns, String selection, String[] selectionArgs,
			String groupBy, String having, String sortBy, String sortOption, String limit) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy,
				having, sortBy + " " + sortOption, limit);
	}


	/**
	 * This is a function that should only be used if you know what you're doing.
	 * It is only here to clear the appended test data. This clears out all data within
	 * the table specified when the database connection was opened.
	 * @return Returns TRUE if successful. FALSE if not.
	 */
	public boolean clearTable() {
		return db.delete(tableName, null, null) > 0;	
	}

	/**
	 * Update the selected row of the open table.
	 * @param rowIndex Number of the row to update.
	 * @param key ArrayList of Keys (column headers).
	 * @param value ArrayList of Key values.
	 * @return Returns an integer.
	 */
	public int updateEntry(long rowIndex, ArrayList<String> key, ArrayList<String> value) {
		String timeStamp = new Timestamp(Calendar.getInstance().getTimeInMillis()).toString();
		String where = KEY_ID + "=" + rowIndex;
		ContentValues contentValues = new ContentValues();
		for(int i = 0; key.size() > i; i++){
			contentValues.put(key.get(i), value.get(i));
		}
		contentValues.put(KEY_TIMESTAMP, timeStamp);
		return db.update(tableName, contentValues, where, null);
	}

	/**
	 * Create the SQL creation string using the keys and their options.
	 * Options must be either null, where the default option is used (usally text),
	 * length 0, where default is used, length 1, where the option is repeated, or 
	 * the same length as keys.
	 * @param keys
	 * @param options
	 * @return
	 */
	private String createTableCreateString(ArrayList<String> keys, ArrayList<String> options){

		// check null keys or length 0 keys
		if (keys == null || keys.size()==0)
			throw new IllegalArgumentException
			("keys cannot be null or size = 0");

		// check that options is either the same length as keys, of length 1, or null, or length 0.
		// default option is "text".
		int nKeys = keys.size();
		String singleOption = null;
		if (options == null)
			singleOption = DEFAULT_KEY_TYPE;
		else if (options.size()==1)
			singleOption = options.get(0);
		else if (options.size()==0)
			singleOption = DEFAULT_KEY_TYPE;
		else if (options.size() != nKeys)
			throw new IllegalArgumentException
			("key options must be null, length 1, length 0, or the same length as keys");

		// loop across keys filling the table
		String keyString = "";
		for(int i = 0; i < nKeys; i++){

			// add key and option to keyString
			keyString += keys.get(i);
			if (singleOption != null)
				keyString += " " + singleOption;
			else{
				keyString += " " + options.get(i);

				// Add commas to the options elements if there is a next value
				if (i+1 < nKeys)
					keyString += ", ";
			}
		}

		// Create the database creation string.
		keyString = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
		+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_TIMESTAMP + "," + keyString + ");";

		return keyString;
	}

	/**
	 * Helper Class for DBAdapter. Does the job of creating the database and checking
	 * if the database needs an upgrade to new version depending on version number specified
	 * by DBAdapter.
	 */
	private static class myDBHelper extends SQLiteOpenHelper {
		private String creationString;
		private String tableName;
		@SuppressWarnings("unused")
		SQLiteDatabase db;

		/**
		 * Creates a myDBHelper object.
		 * @param context The context where the access is needed
		 * @param name Name of database file
		 * @param factory A CursorFactory, or null to use default CursorFactory
		 * @param version Database version
		 * @param tableName Name of table within database
		 * @param creationString SQL String used to create the database
		 */
		public myDBHelper(Context context, String name, CursorFactory factory,
				int version, String tableName, String creationString) {			
			super(context, name, factory, version);
			this.creationString = creationString;
			this.tableName = tableName;
		}

		/**
		 * Creates the database table.
		 * @param db The database used by this helper to create the table in
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(creationString);
		}

		/**
		 * This method determines if the database needs to be updated or not.
		 * @param db The database used by this helper
		 * @param oldVersion The old database version
		 * @param newVersion The new database version
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Log the version upgrade
			Log.w("TaskDBAdapter", "Upgrading from version " + oldVersion +
					" to " + newVersion + ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			onCreate(db);

		}

		/**
		 * Creates tables when the database is opened if the tables need to be created.
		 * @param db The database used by this helper
		 */
		@Override
		public void onOpen(SQLiteDatabase db) {
			db.execSQL(creationString);
		}

	}	
}
