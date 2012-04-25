package com.instantPhotoShare;

import java.util.Hashtable;
import java.util.Set;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactCheckedArray{
	private Hashtable<Long, ContactCheckedItem> hash;
	private int nChecked = 0;
	
	public ContactCheckedArray() {
		hash = new Hashtable<Long, ContactCheckedItem>();
	}
	
	public void setItem(ContactCheckedItem item){
		// see if we need to add or subtract from total contacts that are checked
		ContactCheckedItem selection = hash.get(item.contactId);
		boolean wasChecked = false;
		if (selection == null || !selection.isChecked)
			wasChecked = false;
		else
			wasChecked = true;
		boolean isChecked = item.isChecked;
		if (!wasChecked && isChecked)
			nChecked++;
		if (wasChecked && !isChecked)
			nChecked--;	
		
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
		// see if we need to add or subtract from total contacts that are checked
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			selection = new ContactCheckedItem(contactId);
		if (!selection.isChecked && isChecked)
			nChecked++;
		if (selection.isChecked && !isChecked)
			nChecked--;
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
	
	public int getNChecked(){
		return nChecked;
	}
	
	public int getN(){
		return hash.size();
	}
	
	public Set<Long> getKeySet(){
		return hash.keySet();
	}
	
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
	
	public String getDefaultContactMethod(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.defaultContactMethod;
	}
	
	public Uri getLookupUri(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.getLookupUri();
	}
	
	public String getLookupKey(Long contactId){
		ContactCheckedItem selection = hash.get(contactId);
		if (selection == null)
			return null;
		return selection.lookupKey;
	}
	
	public static class ContactCheckedItem{
		private boolean isChecked = false;
		private String displayName = "";
		private String defaultContactMethod = "";
		private String lookupKey = "";
		private long contactId = -1;
		
		private Uri getLookupUri(){
			if ((lookupKey == null || lookupKey.length() == 0) &&
					(contactId == -1))
				return null;
			if (lookupKey == null || lookupKey.length() == 0)
				return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
			else{
				return ContactsContract.Contacts.getLookupUri(contactId, lookupKey);	
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
				String lookupKey){
			if (displayName == null)
				displayName = "";
			if (defaultContactMethod == null)
				defaultContactMethod = "";
			if (lookupKey == null)
				lookupKey = "";
			this.contactId = contactId;
			this.isChecked = isChecked;
			this.displayName = displayName;
			this.defaultContactMethod = defaultContactMethod;
			this.lookupKey = lookupKey;	
		}
	}
}
