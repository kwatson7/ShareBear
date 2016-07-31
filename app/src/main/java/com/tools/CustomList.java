package com.tools;

public interface CustomList <TYPE>{
	/**
	 * Size of the list
	 * @return
	 */
	public int size();
	
	/**
	 * Return the object at the specified index.
	 * @param index
	 * @return
	 */
	public TYPE get(int index);
	
	/**
	 * Return the object at the current location.
	 * @return
	 */
	public TYPE get();
	
	/**
	 * Move current position to the first index.
	 * @return True if successful, false if not. If the list is empty, return false.
	 */
	public boolean moveToFirst();
	
	/**
	 * Move to the next position in list. If we move past the end, the return false, or if empty return false.
	 * @return
	 */
	public boolean moveToNext();
	
	/**
	 * Return current position. Return -1 if not at a valid position.
	 * @return
	 */
	public int getPosition();
	
	/**
	 * Move list to position in list. -1 is valid, and so is anywhere along the length of the list.
	 * @param index The index to move to.
	 * @return Return true if successful, false if not
	 */
	public boolean moveToPosition(int index);
	
	/**
	 * Move to the last position in the list.
	 * @return True if successful, false if not.
	 */
	public boolean moveToLast();
	
	/**
	 * Take care of any closing that must take place. Some implemenations will do nothing here, <br>
	 * or null out the underlying data, or for a cursor, just close it.
	 */
	public void close();

	/**
	 * Resize the array to only sample this subset of indeces
	 * @param subsetIds
	 */
	public void resample(int[] subsetIds);
}
