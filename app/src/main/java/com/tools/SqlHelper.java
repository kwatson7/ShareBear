package com.tools;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SqlHelper <ACTIVITY_TYPE extends CustomActivity, RESULT>{

	public interface QueryCallback <ACTIVITY_TYPE extends CustomActivity, RESULT>{
		/**
		 * Called when query is finished on the background thread.
		 * @param act the activity currently holding the background task
		 * @param cursor The cursor returned... Can be null
		 * @param exception any exception that occurred. Will be null if no exception
		 */
		public RESULT onQueryFinishedBackgroundThread(ACTIVITY_TYPE act, Cursor cursor, SQLException exception);
		
		/**
		 * Called when query is finished on the UI thread
		 * @param act the activity currently holding the background task
		 * @param cursor The cursor returned... Can be null
		 * @param exception any exception that occurred. Will be null if no exception
		 * @param result the result returned from onQueryFinishedBackgroundThread, can be null
		 */
		public void onQueryFinishedUIThread(ACTIVITY_TYPE act, Cursor cursor, SQLException exception, RESULT result);
	}
	
	/**
	 * Helper class for performing query on background thread
	 * @param <ACTIVITY_TYPE> The activity to return when query finished
	 * @param <RESULT>
	 */
	private static class QueryHelper <ACTIVITY_TYPE extends CustomActivity, RESULT>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, Void>{

		// inputs
		private SQLiteDatabase database;
		private String table;
		private String[] columns;
		private String selection;
		private String[] selectionArgs;
		private String groupBy;
		private String having;
		private String orderBy;
		private String limit;
		private String sqlCommand;
		
		private QueryCallback<ACTIVITY_TYPE, RESULT> callback;
		
		// input type
		private QueryType inputType;
		
		// results
		private Cursor cursor;
		private SQLException exception;
		RESULT result;
		
		public QueryHelper(
				ACTIVITY_TYPE act,
				SQLiteDatabase database,
				String sqlCommand,
				String[] selectionArgs,
				QueryCallback<ACTIVITY_TYPE, RESULT> callback) {
			super(act, -1, true, false, null);
			
			this.database = database;
			this.selectionArgs = selectionArgs;
			this.sqlCommand = sqlCommand;
			this.callback = callback;
			this.inputType = QueryType.RAW_QUERY;
		}
		
		/**
	     * Query the given table
	     *
	     * @param act Calling activity. Can be null, but then will be null on return as well
	     * @param database the database to query
	     * @param table The table name to compile the query against.
	     * @param columns A list of which columns to return. Passing null will
	     *            return all columns, which is discouraged to prevent reading
	     *            data from storage that isn't going to be used.
	     * @param selection A filter declaring which rows to return, formatted as an
	     *            SQL WHERE clause (excluding the WHERE itself). Passing null
	     *            will return all rows for the given table.
	     * @param selectionArgs You may include ?s in selection, which will be
	     *         replaced by the values from selectionArgs, in order that they
	     *         appear in the selection. The values will be bound as Strings.
	     * @param groupBy A filter declaring how to group rows, formatted as an SQL
	     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
	     *            will cause the rows to not be grouped.
	     * @param having A filter declare which row groups to include in the cursor,
	     *            if row grouping is being used, formatted as an SQL HAVING
	     *            clause (excluding the HAVING itself). Passing null will cause
	     *            all row groups to be included, and is required when row
	     *            grouping is not being used.
	     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
	     *            (excluding the ORDER BY itself). Passing null will use the
	     *            default sort order, which may be unordered.
	     * @param limit Limits the number of rows returned by the query,
	     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
	     * @param callback the callback to be run when finished with query
	     * @see Cursor
	     */
		public QueryHelper(
				ACTIVITY_TYPE act,
				SQLiteDatabase database,
				String table,
				String[] columns,
				String selection,
				String[] selectionArgs,
				String groupBy,
				String having,
				String orderBy,
				String limit,
				QueryCallback<ACTIVITY_TYPE, RESULT> callback) {
			
			super(act, -1, true, false, null);
			
			this.database = database;
			this.table = table;
			this.columns = columns;
			this.selection = selection;
			this.selectionArgs = selectionArgs;
			this.groupBy = groupBy;
			this.having = having;
			this.orderBy = orderBy;
			this.limit = limit;
			this.callback = callback;
			this.inputType = QueryType.MANY_INPUTS;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try{
				switch (inputType){
				case MANY_INPUTS:
					cursor = database.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
					break;
				case RAW_QUERY:
					cursor = database.rawQuery(sqlCommand, selectionArgs);
					break;
				}
			}catch(SQLException exception2){
				exception = exception2;
			}
			result = callback.onQueryFinishedBackgroundThread(callingActivity, cursor, exception);
			return null;
		}
		@Override
		protected void onPostExectueOverride(Void tmp) {
			callback.onQueryFinishedUIThread(callingActivity, cursor, exception, result);
		}
		@Override
		protected void onPreExecute() {		
		}
		@Override
		protected void onProgressUpdate(Void... progress) {
		}
		@Override
		protected void setupDialog() {
		}
		
	}
	
	// input type
	private enum QueryType{
		MANY_INPUTS, RAW_QUERY;
	}
	
	// inputs
	private ACTIVITY_TYPE act;
	private SQLiteDatabase database;
	private String table;
	private String[] columns;
	private String selection;
	private String[] selectionArgs;
	private String groupBy;
	private String having;
	private String orderBy;
	private String limit;
	private String sqlCommand;
	private QueryCallback<ACTIVITY_TYPE, RESULT> callback;
	
	// generated values
	private QueryType inputType;
	
	/**
     * Generate a helper that can be used to query in the background
     *
     * @param act the calling activity
     * @param database the database to query
     * @param sqlCommand the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     *     which will be replaced by the values from selectionArgs. The
     *     values will be bound as Strings.
     * @callback the callback to run when finished with the query
     */
	public SqlHelper(
			ACTIVITY_TYPE act,
			SQLiteDatabase database,
			String sqlCommand,
			String[] selectionArgs,
			QueryCallback<ACTIVITY_TYPE, RESULT> callback){
		
		// store inputs
		this.act = act;
		this.database = database;
		this.sqlCommand = sqlCommand;
		this.selectionArgs = selectionArgs;
		this.callback = callback;
		this.inputType = QueryType.RAW_QUERY;
	}
	
	/**
     * Generate a helper that can be used to query in the background
     *
     * @param act Calling activity. Can be null, but then will be null on return as well
     * @param database the database to query
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in order that they
     *         appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @param callback the callback to be run when finished with query
     * @see Cursor
     */
	public SqlHelper(
			ACTIVITY_TYPE act,
			SQLiteDatabase database,
			String table,
			String[] columns,
			String selection,
			String[] selectionArgs,
			String groupBy,
			String having,
			String orderBy,
			String limit,
			QueryCallback<ACTIVITY_TYPE, RESULT> callback){ 
		
		// store inputs
		this.act = act;
		this.database = database;
		this.table = table;
		this.columns = columns;
		this.selection = selection;
		this.selectionArgs = selectionArgs;
		this.groupBy = groupBy;
		this.having = having;
		this.orderBy = orderBy;
		this.limit = limit;
		this.callback = callback;
		this.inputType = QueryType.MANY_INPUTS;
	}
	
	/**
     * Query the data in the background.
     */
	public void queryInBackground(){ 
		
		switch (inputType) {
		case MANY_INPUTS:
			(new QueryHelper<ACTIVITY_TYPE, RESULT>(act, database, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, callback)).execute();
			break;

		case RAW_QUERY:
			(new QueryHelper<ACTIVITY_TYPE, RESULT>(act, database, sqlCommand, selectionArgs, callback)).execute();
			break;
		}
	}
}
