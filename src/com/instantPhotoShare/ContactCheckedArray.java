package com.instantPhotoShare;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactCheckedArray{
	private Hashtable<Long, ContactCheckedItem> hash;
	private int nChecked = 0;
	
	/**
	 * An array that holds checked contacts, and the total number of checked
	 */
	public ContactCheckedArray() {
		hash = new Hashtable<Long, ContactCheckedItem>();
	}
	
	/**
	 * Set a full contact. Can overwrite an already existing contact.
	 * @param item
	 */
	public void setItem(ContactCheckedItem item){
		// grab the item if there is one
		ContactCheckedItem selection = hash.get(item.contactId);
		
		// was it checked before?
		boolean wasChecked = false;
		if (selection == null || !selection.isChecked)
			wasChecked = false;
		else
			wasChecked = true;
		
		// dont override id if we have an old one
		if (item.usersDatabaseRowId == -1 && selection != null)
			item.usersDatabaseRowId = selection.usersDatabaseRowId;
		
		// are we adding or subtracting or not doing anything to total checked
		boolean isChecked = item.isChecked;
		if (!wasChecked && isChecked)
			nChecked++;
		if (wasChecked && !isChecked)
			nChecked--;	
		
		// store the item
		hash.put(item.contactId, item);
	}
			
	
	/**
	 * determine if user at the contactId has been checked
	 * @param contactId
	 * @return
	 */
	public boolean isChecked(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return false;
		if (!selection.isChecked)
			return false;
		else
			return true;
	}
	
	/**
	 * set a current users checked value
	 * @param contactId The contactId to set
	 * @param isChecked The value of true (checked) or false (not checked)
	 */
	public void setIsChecked(Long contactId, boolean isChecked){
		// grab the item if there is one
		ContactCheckedItem selection = hash.get(contactId);
		
		// create a new item if need be
		if (selection == null)
			selection = new ContactCheckedItem(contactId);
		
		// determine if we are adding or subtracting checked items
		if (!selection.isChecked && isChecked)
			nChecked++;
		if (selection.isChecked && !isChecked)
			nChecked--;
		
		// store it
		selection.isChecked = isChecked;
		hash.put(contactId, selection);
	}
	
	public void setDefaultContactDONTUSE(Long contactId, String defaultContact){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			selection = new ContactCheckedItem(contactId);
		selection.defaultContactMethod = defaultContact;
		hash.put(contactId, selection);
	}
	
	public void setDisplayNameDONTUSE(Long contactId, String displayName){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			selection = new ContactCheckedItem(contactId);
		selection.displayName = displayName;
		hash.put(contactId, selection);
	}
	
	/**
	 * 
	 * @return the total number of checked itesm
	 */
	public int getNChecked(){
		return nChecked;
	}
	
	/**
	 * 
	 * @return the total number of items (counting non checked items)
	 */
	public int getN(){
		return hash.size();
	}
	
	/**
	 * 
	 * @return grab all the keys that are currently stored
	 */
	private Set<Long> getKeySet(){
		return hash.keySet();
	}
	
	/**
	 * 
	 * @return the list of keys that are checked
	 */
	public Set<Long> getCheckedKeys(){
		Set<Long> keys = getKeySet();
		Set<Long> output = new HashSet<Long>(getNChecked());
		for (Long item : keys)
			if (isChecked(item))
				output.add(item);
		return output;
	}
	
	/**
	 * Clear all items
	 */
	public void clearAll(){
		hash.clear();
		nChecked = 0;
	}
	
	public String getDisplayName(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.displayName;
	}
	
	/**
	 * 
	 * @param contactId
	 * @return comma separated list of default contact methods for the given contact id
	 */
	public String getDefaultContactMethod(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.defaultContactMethod;
	}
	
	/**
	 * Return the the uri for the contact, that can be used to query the contact with: <br>
	 * contentResolver.query(uri...)
	 */
	public Uri getUri(ContentResolver cr, Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.getUri(cr);
	}
	
	/**
	 * 
	 * @param contactId
	 * @return The lookup key for the given contact
	 */
	public String getLookupKey(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.lookupKey;
	}
	
	public static class ContactCheckedItem{
		
		// member variables
		private boolean isChecked = false; 				// is this contact selected
		private String displayName = ""; 				// the name of the contact
		private String defaultContactMethod = ""; 		// the comma separated list of contact methods
		private String lookupKey = ""; 					// the lookup key for the contact
		private long contactId = -1; 					// the contacts database row id
		private long usersDatabaseRowId = -1; 			// The users databse row id
		
		/**
		 * Return the the uri for the contact, that can be used to query the contact with: <br>
		 * contentResolver.query(uri...)
		 */
		private Uri getUri(ContentResolver cr){
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
		
		private ContactCheckedItem(long contactId){
			this.contactId = contactId;
		}
		
		public ContactCheckedItem(
				long contactId,
				boolean isChecked,
				String displayName,
				String defaultContactMethod,
				String lookupKey,
				long usersDatabaseRowId){
			
			// don't allow null
			if (displayName == null)
				displayName = "";
			if (defaultContactMethod == null)
				defaultContactMethod = "";
			if (lookupKey == null)
				lookupKey = "";
			
			// store inputs
			this.contactId = contactId;
			this.isChecked = isChecked;
			this.displayName = displayName;
			this.defaultContactMethod = defaultContactMethod;
			this.lookupKey = lookupKey;	
			this.usersDatabaseRowId = usersDatabaseRowId;
		}
	}
}
