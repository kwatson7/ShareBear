package com.tools;

import java.util.HashMap;
import android.app.Activity;
import android.database.Cursor;

public class CursorWrapper <TYPE extends CursorWrapper<TYPE>>
implements CustomList<TYPE>{

	// class variables
	private int currentIndex = -1; 						// the position of the cursor
	private Cursor cursor; 								// the working cursor
	private HashMap <Integer, Integer> posRowMapping; 	// linking desired position, cursor row
	
	/**
	 * Set the working cursor. Set to null to close the cursor
	 */
	protected final void setCursor(Cursor cursor){
		clear();
		this.cursor = cursor;
		if (cursor == null)
			currentIndex = -1;
		else
			currentIndex = cursor.getPosition();
		
		initializeMap();
		setColumnNumbers();
	}
	
	/**
	 * Copy the cursor wrapper, but the cursors are still linked to one another, as the data is not copied
	 * @return
	 */
	private CursorWrapper<TYPE> copy(){
		CursorWrapper<TYPE> out = new CursorWrapper<TYPE>();
		out.setCursor(cursor);
		out.currentIndex = -1;
		
		return out;
	}
	
	/**
	 * Return the working cursor
	 * @return
	 */
	private Cursor getCursor(){
		return cursor;
	}
	
	/**
	 * Get a string from a column by the column name <br>
	 * @param columnName
	 * @return
	 * @throws IllegalArgumentException if the column doesn't exist
	 */
	protected final String getString(String columnName){
		return getCursor().getString(getCursor().getColumnIndexOrThrow(columnName));
	}
	
	/**
	 * Get a long from a column by the column name
	 * @param columnName
	 * @return
	 * @throws IllegalArgumentException if the column doesn't exist
	 */
	protected final Long getLong(String columnName){
		return getCursor().getLong(getCursor().getColumnIndexOrThrow(columnName));
	}
	
	/**
	 * Get an int from a column by the column name
	 * @param columnName
	 * @return
	 * @throws IllegalArgumentException if the column doesn't exist
	 */
	protected final int getInt(String columnName){
		return getCursor().getInt(getCursor().getColumnIndexOrThrow(columnName));
	}
	
	/**
	 * Get a boolean from a column by the column name
	 * @param columnName
	 * @throws IllegalArgumentException if the column doesn't exist
	 * @return
	 */
	protected final boolean getBoolean(String columnName){
		int col = getCursor().getColumnIndexOrThrow(columnName);
		return getBoolean(col);
	}
	
	/**
	 * Get a string from a column by the column number
	 * @param column
	 * @return
	 */
	protected final String getString(int column){
		return getCursor().getString(column);
	}
	
	/**
	 * Get a string from a column by the column number
	 * @param column
	 * @return
	 */
	protected final Long getLong(int column){
		return getCursor().getLong(column);
	}
	
	/**
	 * Get a string from a column by the column number
	 * @param column
	 * @return
	 */
	protected final int getInt(int column){
		return getCursor().getInt(column);
	}
	
	/**
	 * Get a string from a column by the column number
	 * @param column
	 * @return
	 */
	protected final boolean getBoolean(int column){
		String str = getCursor().getString(column);
		int val = getCursor().getInt(column);
		return (str.equalsIgnoreCase("true") || val > 0);
	}
	
	/**
	 * Returns the column number of the given name
	 * @param col The column name
	 * @return The column number
	 * @throws IllegalArgumentException if the column name does not exist
	 */
	protected final int getColumnIndexOrThrow(String col){
		return getCursor().getColumnIndexOrThrow(col);
	}
	
	/**
	 * Close the cursor and then clear connections to it.
	 */
	public final void clear(){
		if (cursor != null)
			cursor.close();
		cursor = null;
		initializeMap();
	}
	
	@Override
	public final void close(){
		if (cursor != null)
			cursor.close();
	}


	@Override
	public final int size() {
		if (cursor == null || cursor.isClosed())
			return 0;
		if (posRowMapping == null)
			return getCursor().getCount();
		else
			return posRowMapping.size();
	}

	@Override
	public final boolean moveToFirst() {
		if (size() < 1)
			return false;
		else{
			if (moveToPosition(0)){
				return true;
			}else
				return false;
		}
	}

	@Override
	public final boolean moveToNext() {
		if (size() < 1 || currentIndex + 1 >= size())
			return false;
		else{
			return (moveToPosition(currentIndex+1));
		}
	}

	@Override
	public final int getPosition() {
		return currentIndex;
	}

	@Override
	public final boolean moveToPosition(int index) {
		if (size() < 1)
			return false;
		if (index < -1 || index >= size())
			return false;
		else{
			if (getCursor().moveToPosition(getCursorRow(index))){
				currentIndex = index;
				return true;
			}else
				return false;
		}
	}

	@Override
	public final boolean moveToLast() {
		if (size() < 1)
			return false;
		else{
			if (moveToPosition(size()-1)){
				return true;
			}else
				return false;
		}
	}

	@Override
	public final TYPE get() {
		if (currentIndex >=0 && currentIndex < size())
			return get(currentIndex);
		else
			return null;
	}
	
	@Override 
	public final TYPE get(int index){
		moveToPosition(index);
		return (TYPE) this;
		//TODO: figure out the correc way to declare class to avoid this warning.
		//return get();
	}	
		
	// protected helper methods
	/**
	 * Determine the column numbers from the cursor. If the columns to be read change in here. Change in getColumnsArray as well.
	 * @throws IllegalArgumentException if columns don't exist
	 */
	protected void setColumnNumbers()
	throws IllegalArgumentException{}
		
	/**
	 * Checks if cursor is available to be read. Meaning it musn't be null nor before the first or after the last
	 * @return True if cursor can be read, and false otherwise
	 */
	protected final boolean checkCursor(){
		if (cursor == null || 
				cursor.isClosed() || 
				getPosition() < 0 || 
				getPosition() >= size())
			return false;
		else
			return true;
	}
	
	/**
	 * Is the cursor valid. Currently just checks if it is null
	 * @return
	 */
	protected final boolean isCursorValid(){
		if (cursor == null)
			return false;
		else
			return true;
	}
	
	/**
	 * Allow this activity to start managing this cursor to close and open properly with lifecycle.
	 * @param act The activity to manage the cursor
	 */
	public final void startManagingCursor(Activity act){
		if (isCursorValid())
			act.startManagingCursor(cursor);
	}
	
	/**
	 * Allow this activity to start managing this cursor to close and open properly with lifecycle.
	 * @param act the activity to stop managing
	 */
	public final void stopManagingCursor(Activity act){
		if (isCursorValid())
			act.stopManagingCursor(cursor);
	}
	
	@Override
	public final void resample(int[] subsetIds){
		
		if (subsetIds == null){
			posRowMapping = null;
			return;
		}
		// set hashmap to new values
		HashMap<Integer, Integer> newHash = new HashMap<Integer, Integer>(subsetIds.length);
		for (int i = 0; i < subsetIds.length; i++)
			newHash.put(i, getCursorRow(subsetIds[i]));
		posRowMapping = newHash;
		currentIndex = -1;
	}	
	
	/**
	 * Initialize the map linking array ids to column rows. Defaults to 1-1 match
	 */
	private void initializeMap(){
		// just set to null, because getCursorRow does the default 1-1 linking for us
		posRowMapping = null;
		// initialize hashmap
		//for (int i = 0; i < size(); i++)
		//	posRowMapping.put(i, i);
	}
	
	/**
	 * Return which row relates to the given arrayIndex
	 * @param arrayIndex
	 * @return
	 */
	private int getCursorRow(int arrayIndex){
		if (posRowMapping == null)
			return arrayIndex;
		return posRowMapping.get(arrayIndex);
	}
}

