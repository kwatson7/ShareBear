package com.tools;


/**
 * Stores two string together. Also these strings are comparable and can
 * thus be sorted. The sorting is performed on the first string, and then the second if the firsts are equal.
 * The first string is the main string and two objects are considered equal if the first strings match. The 
 * 2nd string is a support string.
 * @author Kyle
 *
 */
public class TwoStrings implements Comparable<TwoStrings>{

	public String mObject1;
	public String mObject2;
	public String mDescription;
	
	/**
	 * Constructor for simply storing two strings.
	 * @param obj1 First string
	 * @param obj2 Second string
	 */
	public TwoStrings(String obj1, String obj2) {
		mObject1 = obj1;
		mObject2 = obj2;
		mDescription = "";
	}
	
	/**
	 * Constructor for simply storing two strings and their description
	 * @param obj1 First string
	 * @param obj2 Second string
	 * @param description The description of the object in question
	 */
	public TwoStrings(String obj1, String obj2, String description){
		mObject1 = obj1;
		mObject2 = obj2;
		mDescription = description;
	}
	
	// the compare function. Sorts on the first string and then the 2nd if they are equal
//	@Override
	public int compareTo(TwoStrings input) {
		int output = this.mObject1.compareToIgnoreCase(input.mObject1);
		if (output == 0)
			output = this.mObject2.compareToIgnoreCase(input.mObject2);

		return output;
	}
	
	/**
	 * These two objects are considered equal if their first strings match
	 */
	@Override
	public boolean equals(Object o){
		if (!( o instanceof TwoStrings))
			return false;
		String e = this.mObject1;
		String input = ((TwoStrings) o).mObject1;
		return (input==null ? e==null : input.equals(e));
	}
	
	@Override
	public int hashCode(){
		if (mObject1 == null)
			return 0;
		else
			return mObject1.hashCode();
	}
	
	/**
	 * Return the first string as the string of the object
	 */
	@Override
	public String toString(){
		return mObject1;
	}
	
	/**
	 * Combine string1 and string2 with the given separator, ignoring nulls and empty values. <br>
	 * For example, string1 = "", and string2 = "Washington", output is just "Washington", but <br>
	 * if string1 = "George", and string2 = "Washington", output is just "George Washington" <br>
	 * @param separator
	 * @return
	 */
	public String combineStrings(String separator){
		//Create the StringBuilder
		StringBuilder builder = new StringBuilder();
		
		// add the first string
		if (mObject1 != null && mObject1.length() != 0)
			builder.append(mObject1);
		
		// the separator
		if (builder.length() > 0)
			builder.append(separator);
		
		// the 2nd string
		if (mObject2 != null && mObject2.length() != 0)
			builder.append(mObject2);
		
		// conver to string
		return builder.toString();
	}
}
