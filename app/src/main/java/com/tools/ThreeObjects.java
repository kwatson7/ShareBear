package com.tools;

/** Stores generic three objects in class
 * Also sortable on the first object if it is a string
 * */
public class ThreeObjects <TYPE1, TYPE2, TYPE3>
implements Comparable<ThreeObjects<String, TYPE2, TYPE3>>
{
	public TYPE1 mObject1;
	public TYPE2 mObject2;
	public TYPE3 mObject3;
	public String mDescription;

	/**
	 * Constructor for generic three object storage. Description defaults to "".
	 * @param obj1 First Object
	 * @param obj2 Second Object
	 * @param obj3 Third Object
	 */
	public ThreeObjects(TYPE1 obj1, TYPE2 obj2, TYPE3 obj3){
		mObject1 = obj1;
		mObject2 = obj2;
		mObject3 = obj3;
		mDescription = "";
	}
	
	/**
	 * Constructor for generic two object storage
	 * @param obj1 First object
	 * @param obj2 Second object
	 * @param obj3 Third object
	 * @param description Description of object
	 */
	public ThreeObjects(TYPE1 obj1, TYPE2 obj2, TYPE3 obj3, String description){
		mObject1 = obj1;
		mObject2 = obj2;
		mObject3 = obj3;
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
	public int compareTo(ThreeObjects<String, TYPE2, TYPE3> input) {
		return ((String)this.mObject1).compareToIgnoreCase((String)input.mObject1);
	}
}