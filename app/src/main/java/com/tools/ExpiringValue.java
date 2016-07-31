package com.tools;

import java.util.Date;

/**
 * A class that will return the currently stored value until the last time it was set was
 * a period of time ago, then it expires and resets to the default value. <br>
 * A good example use case is to check if you are currently downloading a file, but with a timeout
 * if the last time we updated was too long ago.
 * @param <VALUE_TYPE>
 */
public class ExpiringValue <VALUE_TYPE> {

	// member variables
	private float secondsToExpire;
	private VALUE_TYPE currentValue;
	private VALUE_TYPE defaultValue;
	private boolean refreshOnGetAlso;
	private long lastUpdateTime = 0;
	
	/**
	 * Initialize an ExpiringValue
	 * @param secondsToExpire The seconds until currentValue resets to defaultValue
	 * @param currentValue the current value of this object.
	 * @param defaultValue The default value when timer expires
	 * @param <VALUE_TYPE> The type of value to store
	 */
	public ExpiringValue(float secondsToExpire, VALUE_TYPE currentValue, VALUE_TYPE defaultValue, boolean refreshOnGetAlso){
		this.secondsToExpire = secondsToExpire;
		this.defaultValue = defaultValue;
		this.refreshOnGetAlso = refreshOnGetAlso;
		setValue(currentValue);
	}
	
	/**
	 * Update the current value, it will also reset the timer
	 * @param valueToSet The value of this object
	 */
	public void setValue(VALUE_TYPE valueToSet){
		this.currentValue = valueToSet;
		updateTime();
	}
	
	/**
	 * Get the current value of this object. Will be the default value if the last time it was set has expired.
	 * @return The value or default if expired
	 */
	public VALUE_TYPE getValue(){
		Date date = new Date();
		long currentTime = date.getTime();
		if (currentTime - lastUpdateTime > secondsToExpire*1000){
			currentValue = defaultValue;
			if (refreshOnGetAlso)
				updateTime();
			return defaultValue;
		}else{
			if (refreshOnGetAlso)
				updateTime();
			return currentValue;
		}
	}
	
	/**
	 * Update the last update time
	 */
	private void updateTime(){
		Date date = new Date();
		long currentTime = date.getTime();
		lastUpdateTime = currentTime;
	}
}
