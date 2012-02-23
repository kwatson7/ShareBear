package com.instantPhotoShare;

import java.util.Hashtable;
import java.util.Set;

import com.tools.TwoObjects;

public class ContactCheckedArray{
	private Hashtable<Long, TwoObjects<Boolean, String>> hash;
	private int nChecked = 0;
	
	public ContactCheckedArray() {
		hash = new Hashtable<Long, TwoObjects<Boolean, String>>();
	}
	
	/**
	 * determine if user at at userId has been checked
	 * @param key
	 * @return
	 */
	public boolean isChecked(long userId){
		TwoObjects<Boolean, String> selection = hash.get(userId);
		if (selection == null)
			return false;
		if (selection.mObject1 == null || !selection.mObject1)
			return false;
		else
			return true;
	}
	
	/**
	 * set a current users checked value
	 * @param userId The userId to set
	 * @param isChecked The value of true (checked) or false (not checked)
	 */
	public void setIsChecked(long userId, boolean isChecked){
		// see if we need to add or subtract from total contacts that are checked
		TwoObjects<Boolean, String> selection = hash.get(userId);
		if (selection == null)
			selection = new TwoObjects<Boolean, String>(false, "");
		if (selection.mObject1 == null)
			selection.mObject1 = false;
		if (!selection.mObject1 && isChecked)
			nChecked++;
		if (selection.mObject1 && !isChecked)
			nChecked--;
		selection.mObject1 = isChecked;
		hash.put(userId, selection);
	}
	
	public void setDefaultContact(long userId, String defaultContact){
		TwoObjects<Boolean, String> selection = hash.get(userId);
		if (selection == null)
			selection = new TwoObjects<Boolean, String>(false, "");
		if (selection.mObject1 == null)
			selection.mObject1 = false;
		selection.mObject2 = defaultContact;
		hash.put(userId, selection);
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
	
	public String getDefaultContactMethod(long userId){
		TwoObjects<Boolean, String> selection = hash.get(userId);
		if (selection == null)
			return null;
		return selection.mObject2;
	}
}
