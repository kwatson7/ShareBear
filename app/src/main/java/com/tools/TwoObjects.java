package com.tools;

import java.io.Serializable;

/** Stores generic two objects in class.
 * Also sortable on the first object if it is a String*/
public class TwoObjects <TYPE1, TYPE2>
implements Comparable<TwoObjects<String, TYPE2>>, 
Serializable
{

	private static final long serialVersionUID = 7342182516607618554L;
	public TYPE1 mObject1;
	public TYPE2 mObject2;
	public String mDescription;

	/**
	 * Constructor for generic two object storage. Description defaults to "".
	 * @param obj1 First Object
	 * @param obj2 Second Object
	 */
	public TwoObjects(TYPE1 obj1, TYPE2 obj2){
		mObject1 = obj1;
		mObject2 = obj2;
		mDescription = "";
	}
	
	/**
	 * Constructor for generic two object storage
	 * @param obj1 First object
	 * @param obj2 Second object
	 * @param description Description of object
	 */
	public TwoObjects(TYPE1 obj1, TYPE2 obj2, String description){
		mObject1 = obj1;
		mObject2 = obj2;
		mDescription = description;
	}
	
	/**
	 * The toString for this object is the toString of the first object
	 */
	@Override
	public String toString(){
		return mObject1.toString();
	}

	@Override
	public int compareTo(TwoObjects<String, TYPE2> input) {
		return ((String)this.mObject1).compareToIgnoreCase((String)input.mObject1);
	}
}