package com.instantPhotoShare;

import java.io.InputStream;

import com.instantPhotoShare.Adapters.UsersAdapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.ImageView;

public class ContactCursorWrapper {

	// column numbers in cursor
	private int mContactIdColumn; 			// The column where contact id is stored
	private int mNameColumn;				// the column where diplay_name is stored
	private int mPhotoIdColumn; 			// The column where photo_id is stored
	private int mLookupKeyColumn; 			// The column where the lookup key is stored
	
	// misc private variables
	private Cursor cursor; 					// The cursor storing the info of contacts
	
	/**
	 * Default null constructor
	 */
	public ContactCursorWrapper() {	}
	
	/**
	 * Constructor that takes cursor as input
	 * @param cursor The contacts database cursor
	 */
	public ContactCursorWrapper(Cursor cursor){
		setCursor(cursor);
	}
	
	/**
	 * Close the cursor
	 */
	public void close(){
		if (cursor != null)
			cursor.close();
	}
	
	/**
	 * Close the cursor and then clear connections to it.
	 */
	public void clear(){
		if (cursor != null)
			cursor.close();
		if (cursor != null)
			cursor = null;
	}
	
	/**
	 * Assign a cursor to this object and then determine the column headers
	 * @param cursor
	 */
	public void setCursor(Cursor cursor){
		clear();
		this.cursor = cursor;
		setColumnNumbers();
	}
	
	/**
	 * Return the name of the contact. Will return null if the cursor is unreadable.
	 * @return the name of the contact. Will return null if the cursor is unreadable.
	 */
	public String getName(){
		if (checkCursor())
			return cursor.getString(mNameColumn);
		else
			return null;
	}
	
	/**
	 * Return the row id of the contact, or -1 if the cursor is unreadable
	 * @return the row id of the contact, or -1 if the cursor is unreadable
	 */
	public long getId(){
		if (checkCursor())
			return cursor.getLong(mContactIdColumn);
		else
			return -1;
	}
	
	/**
	 * The lookup key used to search contacts
	 * @return
	 */
	public String getLookupKey(){
		if (!checkCursor())
			return "";
		return cursor.getString(mLookupKeyColumn);
	}
	
	/**
	 * 
	 * @return the uri of the contact
	 */
	public Uri getUri(ContentResolver cr){
		if (!checkCursor())
			return null;
		
		String lookupKey = getLookupKey();
		long contactId = getId();
		
		// no lookup key and contactId is -1, so there is no Uri
		if ((lookupKey == null || lookupKey.length() == 0) &&
				(contactId == -1))
			return null;
		
		// no key, just contact
		if (lookupKey == null || lookupKey.length() == 0)
			return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
		// no contact, just key
		else if (contactId == -1)
			return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
		// both
		else{
			Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
			return ContactsContract.Contacts.lookupContact(cr, uri);
		}
	}
	
	/**
	 * Return the photo id of the contact or -1 if the cursor is unreadable.
	 * @return the photo id of the contact or -1 if the cursor is unreadable.
	 */
	public long getPhotoId(){
		if (checkCursor())
			return cursor.getLong(mPhotoIdColumn);
		else
			return -1;
	}
	
	/**
	 * Move to the first position of the cursor.
	 * @return Return true if successful and false otherwise
	 */
	public boolean moveToFirst(){
		if (checkCursor())
			return cursor.moveToFirst();
		else
			return false;
	}
	
	/**
	 * Move to the next position in the cursor
	 * @return true if successful, false otherwise.
	 */
	public boolean moveToNext(){
		if (checkCursor())
			return cursor.moveToNext();
		else
			return false;
	}
	
	/**
	 * The columns this cursor returns
	 * @return
	 */
	public String[] getColumnsArray(){
		return new String[]{
				ContactsContract.Contacts.DISPLAY_NAME, 
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.PHOTO_ID,
				ContactsContract.Contacts.LOOKUP_KEY
		};
	}
	
	/**
	 * Get the cursor that this class holds. Usually this should be avoided. Most processes can be performed without calling this.
	 * @return
	 */
	public Cursor getCursor(){
		return cursor;
	}
	
	/**
	 * Search on the contacts database for the input name. This will search on partial name.
	 * For example, if "br" is input, names such as "bryan smith" and "john brant" will be selected.
	 * However, inside the name such as "Abrigale" will not return.
	 * @param act The activity which called this and where the database search will be performed in.
	 * @param searchName The name to search on
	 * @param onlyShowContactsFromVisibleGroup true if only show contacts that are in visible groups and false otherwise. Usually do true.
	 */
	public void searchDatabaseForName(
			Activity act,
			String searchName,
			boolean onlyShowContactsFromVisibleGroup){
		
		// grab cursor from search result grabbing names of interest
		Cursor curs = com.tools.CustomCursors.getContactProjection
			(act, getColumnsArray(), searchName, onlyShowContactsFromVisibleGroup);
		
		// close old cursor if need be and assign new one
		setCursor(curs);
	}
	
	/**
	 * Move the underlying to a specific position
	 * @param position Return true if successful and false otherwise. See cursor.moveToPosition for more help.
	 * @return
	 */
	public boolean moveToPosition(int position){
		if (checkCursor())
			return cursor.moveToPosition(position);
		else
			return false;
	}
	
	/**
	 * Set an imageView to the users contact bitmap.
	 * @param ctx The context to perform a databse lookup on
	 * @param image The imageView to set.
	 * @return Return true if successful and false otherwise
	 */
	public boolean setImageToContactBitmap(Context ctx, ImageView image){
		
		// only proceed if this pointer is valid
    	if (image != null){

    		// grab the phone id of this contact and load bitmap
    		InputStream input = ContactsContract.Contacts.
    			openContactPhotoInputStream(ctx.getContentResolver(), getUri(ctx.getContentResolver()));
    		if (input != null) {
    			image.setImageBitmap(BitmapFactory.decodeStream(input));
    			return true;
    		}else
    			return false;
    	}else
    		return false;
	}
	
	/**
	 * Get from the "users" database the default contact info, either email string or phone string
	 * @return
	 */
	public String getDefaultContact(Context ctx){
		// the users adpater
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchUserByContactsId(getId());
		String out = users.getDefaultContactMethod();
		users.close();
		return out;
	}
	
	// Private helper methods
	/**
	 * Determine the column numbers from the cursor. If the columns to be read change in here. Change in getColumnsArray as well.
	 * @throws IllegalArgumentException if columns don't exist
	 */
	private void setColumnNumbers(){
		
		// can only do this with a non null cursor
		if (cursor == null)
			return;
		
		// now find the column numbers for our strings
		try{
			mContactIdColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
			mNameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
			mPhotoIdColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID);
			mLookupKeyColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException("Inputted cursor into ContactCursorWrapper does not have all the required columns");
		}
	}
	
	/**
	 * Checks if cursor is available to be read. Meaning it musn't be null nor before the first or after the last
	 * @return True if cursor can be read, and false otherwise
	 */
	private boolean checkCursor(){
		if (cursor == null ||
				cursor.isBeforeFirst() ||
				cursor.isAfterLast())
			return false;
		else
			return true;
	}
}
