package com.instantPhotoShare;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	
	// strings for accessing preferences file
	private static final String PREF_FILE = "appInfo.prefs";
	private static final int MODE_PRIVATE = Context.MODE_PRIVATE;
	private static final String USER_NAME = "USER_NAME";
	private static final String FIRST_NAME = "FIRST_NAME";
	private static final String LAST_NAME = "LAST_NAME";
	private static final String USER_ID = "USER_ID";
	private static final String SECRET_CODE = "SECRET_CODE";
	private static final String PHONE_NUMBERS = "PHONE_NUMBERS";
	private static final String EMAIL_ADDRESSES = "EMAIL_ADDRESSES";
	private static final String MAIN_PHONE = "MAIN_PHONE";
	private static final String PASSWORD = "PASSWORD";
	
	// default values
	public static final String DEFAULT_STRING = null;
	public static final long DEFAULT_LONG = -1;
	public static final int DEFAULT_INT = -1;
	
	// static values, not stored in file
	public static final String BASE_URL = "http://www.brennanmaxinc.com/";
	public static final String REQUEST_PAGE = "request.php";
	
	/**
	 * get the user name from the preference file, that can be accessed by any of the package classes
	 * @return the user name. Null if it does not exist
	 */
	public static String getUserName(Context ctx){
		return getStringPref(ctx, USER_NAME);
	}
	
	/**
	 * Set the username in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setUserName(Context ctx, String name){
		setStringPref(ctx, USER_NAME, name);
	}
	
	/**
	 * get the FIRST name from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getFirstName(Context ctx){
		return getStringPref(ctx, FIRST_NAME);
	}
	
	/**
	 * Set the password in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param password The password to set
	 */
	public static void setPassword(Context ctx, String password){
		setStringPref(ctx, PASSWORD, password);
	}
	
	/**
	 * get the password from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getPassword(Context ctx){
		return getStringPref(ctx, PASSWORD);
	}
	
	/**
	 * Set the last name in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setLastName(Context ctx, String name){
		setStringPref(ctx, LAST_NAME, name);
	}
	
	/**
	 * get the last name from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getLastName(Context ctx){
		return getStringPref(ctx, LAST_NAME);
	}
	
	/**
	 * Set the first name in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setFirstName(Context ctx, String name){
		setStringPref(ctx, FIRST_NAME, name);
	}
	
	/**
	 * get the last name from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getMainPhone(Context ctx){
		return getStringPref(ctx, MAIN_PHONE);
	}
	
	/**
	 * Set the first name in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setMainPhone(Context ctx, String phone){
		setStringPref(ctx, MAIN_PHONE, phone);
	}
	
	/**
	 * get the user id from the preference file, that can be accessed by any of the package classes
	 * @return the user id. -1 if it does not exist
	 */
	public static long getUserId(Context ctx){
		return getLongPref(ctx, USER_ID);
	}
	
	/**
	 * Set the userId in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setUserId(Context ctx, Long userId){
		setLongPref(ctx, USER_ID, userId);
	}
	
	/**
	 * Get the secret code that is used for sending to server to verify user. Distinct from password
	 * @param ctx Context of calling space where preferences file is stored
	 * @return The code
	 */
	public static String getSecretCode(Context ctx){
		return getStringPref(ctx, SECRET_CODE);
	}
	
	/**
	 * Set the secret code that is used for sending to server to verify user. Distinct from password
	 * @param ctx Context of calling space where preferences file is stored
	 * @param secretCode
	 */
	public static void setSecretCode(Context ctx, String secretCode){
		setStringPref(ctx, SECRET_CODE, secretCode);
	}
	
	/**
	 * Set the list of phone numbers, it should be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 * @param phones comma delimited list of phone numbers
	 */
	public static void setPhoneNumbers(Context ctx, String phones){
		setStringPref(ctx, PHONE_NUMBERS, phones);
	}
	
	/**
	 * Get the list of phone numbers, it will be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 */
	public static String getPhoneNumbers(Context ctx){
		return getStringPref(ctx, PHONE_NUMBERS);
	}
	
	/**
	 * Set the list of email addresses, it should be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 * @param emails comma delimited list of email addresses
	 */
	public static void setEmailAddresses(Context ctx, String emails){
		setStringPref(ctx, EMAIL_ADDRESSES, emails);
	}
	
	/**
	 * Get the list of email addresses, it should be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 * @param emails comma delimited list of email addresses
	 */
	public static String getEmailAddresses(Context ctx){
		return getStringPref(ctx, EMAIL_ADDRESSES);
	}
	
	public static Person getPerson(Context ctx){
		// initialize person
		Person person = new Person();
		
		// fill the person with values
		person.setPhoneArray(getPhoneNumbers(ctx));
		person.setFirstName(getFirstName(ctx));
		person.setLastName(getLastName(ctx));
		person.setEmailArray(getEmailAddresses(ctx));
		person.setMainPhone(getMainPhone(ctx));
		person.setId(getUserId(ctx));
		person.setPassword(getPassword(ctx));
		person.setUserName(getUserName(ctx));
		
		// return the person
		return person;
	}
	
	public static void setPerson (Context ctx, Person person){
		Prefs.setEmailAddresses(ctx, person.getEmailsAsString());
		Prefs.setFirstName(ctx, person.getFirstName());
		Prefs.setUserId(ctx, person.getId());
		Prefs.setLastName(ctx, person.getLastName());
		Prefs.setMainPhone(ctx, person.getMainPhone());
		Prefs.setPassword(ctx, person.getPassword());
		Prefs.setPhoneNumbers(ctx, person.getPhonesAsString());
		Prefs.setUserName(ctx, person.getUserName());
	}
	
	
	// private methods used for helper inside this class
	/**
	 * Get the preference object
	 * @param ctx The context used to access object
	 * @return
	 */
	private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE);
    }
	
    private static String getStringPref(Context ctx, String pref) {
        return getPrefs(ctx).getString(pref, DEFAULT_STRING);
    }
    public static void setStringPref(Context ctx, String pref, String value) {
        getPrefs(ctx).edit().putString(pref, value).commit();
    }
    
    private static long getLongPref(Context ctx, String pref) {
        return getPrefs(ctx).getLong(pref, DEFAULT_LONG);
    }
    public static void setLongPref(Context ctx, String pref, long value) {
        getPrefs(ctx).edit().putLong(pref, value).commit();
    }
}