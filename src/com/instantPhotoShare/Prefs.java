package com.instantPhotoShare;

import java.util.ArrayList;
import java.util.Iterator;

import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersAdapter.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Prefs {
	
	// strings for accessing preferences file
	public static final String PREF_FILE = "appInfo.prefs";
	private static final int MODE_PRIVATE = Context.MODE_PRIVATE;
	private static final String USER_NAME = "USER_NAME";
	private static final String FIRST_NAME = "FIRST_NAME";
	private static final String LAST_NAME = "LAST_NAME";
	private static final String USER_ROW_ID = "USER_ROW_ID";
	private static final String USER_SERVER_ID = "USER_SERVER_ID";
	private static final String SECRET_CODE = "SECRET_CODE";
	private static final String PHONE_NUMBERS = "PHONE_NUMBERS";
	private static final String EMAIL_ADDRESSES = "EMAIL_ADDRESSES";
	private static final String MAIN_PHONE = "MAIN_PHONE";
	private static final String PASSWORD = "PASSWORD";
	private static final String GROUPS_ARRAY = "GROUPS_ARRAY";
	private static final String MOST_RECENT_PICTURE_ROW_ID = "MOST_RECENT_PICTURE_ROW_ID";
	private static final String MOST_RECENT_GROUP = "MOST_RECENT_GROUP";
	private static final String MOST_RECENT_NOTIFICATION_NUMBER = "MOST_RECENT_NOTIFICATION_NUMBER";
	
	// default values
	public static final String DEFAULT_STRING = null;
	public static final long DEFAULT_LONG = -1;
	public static final int DEFAULT_INT = -1;
	public static final boolean DEFAULT_BOOLEAN = false;
	
	// static values, not stored in file
	public static final String BASE_URL = "http://www.sharebearapp.com/request.php/";//"https://secure1907.hostgator.com/~bheyde1/";//"http://www.brennanmaxinc.com/";
	public static final String REQUEST_PAGE = "request.php";
	
	private static final String PREEMPTIVE_ACCESS_ERROR = "Cannot access before user has been saved in contacts database";
	
	// debugging strings
	public static class debug{
		/** force the id of the user to have a value of 1 and "secret" */
		public static final boolean forceId = false;
		/** allow for multiple updates to database rows when we dont' expect it, or throw exception othrwise */
		public static final boolean allowMultipleUpdates = false;
	}
	
	/**
	 * get the user name from the preference file, that can be accessed by any of the package classes
	 * @return the user name. Null if it does not exist
	 */
	public static String getUserName(Context ctx){
		return getStringPref(ctx, USER_NAME);
	}
	
	/**
	 * read through array and create a string comma separated of rowIds of the input groups
	 * @param ctx The contet to use to write to preferences
	 * @param groups The groups that are currently selected.
	 */
	public static void setGroupIds(Context ctx, ArrayList<com.instantPhotoShare.Adapters.GroupsAdapter.Group> groups){
		// if null, then write empty string
		if (groups == null || groups.size() == 0){
			setStringPref(ctx, GROUPS_ARRAY, "");
			return;
		}
		
		// make the comma separated string
		String str = "";
		for (com.instantPhotoShare.Adapters.GroupsAdapter.Group item : groups){
			// check null
			if (item == null)
				continue;
			
			// read rowId and add to string
			str += item.getRowId() + ",";
		}
		
		// remove last , 
		if (str.length() >= 2)
			str = str.substring(0, str.length()-1);
		
		// write to list
		setStringPref(ctx, GROUPS_ARRAY, str);
	}
	
	/**
	 * read through array and create a string comma separated of rowIds of the input groups
	 * @param ctx The context to use to write to preferences
	 * @param groupIds The group Ids
	 */
	public static void setGroupIds(Context ctx, String groupIds){
		// if null, then write empty string
		if (groupIds == null || groupIds.length() == 0){
			setStringPref(ctx, GROUPS_ARRAY, "");
			return;
		}
		
		// write to list
		setStringPref(ctx, GROUPS_ARRAY, groupIds);
	}
	
	/**
	 * read through array and create a string comma separated of rowIds of the input groups
	 * @param ctx The context to use to write to preferences
	 * @param groupId The groupId that is currently selected. Will throw IllegalArgumentException if groupId doesn't exist
	 */
	public static void setGroupIds(Context ctx, long groupId){
		GroupsAdapter groups = new GroupsAdapter(ctx);
		GroupsAdapter.Group group = groups.getGroup(groupId);
		if (group == null)
			throw new IllegalArgumentException("Group does not exist");
		ArrayList<GroupsAdapter.Group> list = new ArrayList<GroupsAdapter.Group>(1);
		list.add(group);
		setGroupIds(ctx, list);
	}
	
	/**
	 * Read the rowIds of the groups that were stored last. Return an arrayList of those rows.
	 * @param ctx The context to use to read to preferences
	 */
	public static ArrayList<Long> getGroupIds(Context ctx){
		// default output
		ArrayList<Long> output = new ArrayList<Long>();
		
		// grab the string and return if empty
		String list = getStringPref(ctx, GROUPS_ARRAY);
		if (list == null || list.length() == 0)
			return output;
		
		// split the string by ,
		TextUtils.SimpleStringSplitter split = new TextUtils.SimpleStringSplitter(',');
		split.setString(list);
		Iterator<String> iterator = split.iterator();
		while(iterator.hasNext()){
			output.add(Long.parseLong(iterator.next()));
		}
		
		// return the output
		return output;
	}
	
	/**
	 * Set the username in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setUserName(Context ctx, String name){
		setStringPref(ctx, USER_NAME, name);
	}
	
	/**
	 * Set the rowId of the last picture that was taken
	 * @param pictureRowId The rowId of the picture
	 */
	public static void setLastPictureTaken(Context ctx, long pictureRowId){
		setLongPref(ctx, MOST_RECENT_PICTURE_ROW_ID, pictureRowId);
	}
	/**
	 * get the rowId of the last picture that was taken
	 * @param pictureRowId The rowId of the picture
	 */
	public static long getLastPictureTaken(Context ctx){
		return getLongPref(ctx, MOST_RECENT_PICTURE_ROW_ID);
	}
	
	/**
	 * Set the group rowId of the last picture that was taken (only the first group if multiple)
	 * @param groupRowId The rowId of the group
	 */
	public static void setLastGroupOfLastPicture(Context ctx, long groupRowId){
		setLongPref(ctx, MOST_RECENT_GROUP, groupRowId);
	}
	/**
	 * get the rowId of the last picture that was taken
	 * @param pictureRowId The rowId of the picture
	 */
	public static long getLastGroupOfLastPicture(Context ctx){
		return getLongPref(ctx, MOST_RECENT_GROUP);
	}
	
	/**
	 * Set the notification rowId of the most recent notification.
	 * @param notificationRowId The rowId of the notificaation
	 */
	public static void setMostRecentNotificationRowId(Context ctx, long notificationRowId){
		setLongPref(ctx, MOST_RECENT_GROUP, notificationRowId);
	}
	/**
	 * get the rowId of the most recent notification number 
	 * @param ctx Context required to query
	 * @return the rowId
	 */
	public static long getMostRecentNotificationRowId(Context ctx){
		return getLongPref(ctx, MOST_RECENT_NOTIFICATION_NUMBER);
	}
	
	/**
	 * get the FIRST name from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getFirstName(Context ctx){
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			throw new IllegalAccessError(PREEMPTIVE_ACCESS_ERROR);
		UsersAdapter users = new UsersAdapter(ctx);
		User user = users.getUser(userRowId);
		return user.getFirstName();
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
	 * Return whether we should be launching a polling in the background by default
	 * @param ctx Context required
	 * @return true if we should, false otherwise
	 */
	public static boolean isLaunchBackgroundOnStart(Context ctx){
		return getBooleanPref(ctx, getBACKGROUND_POLLING_AUTO_START(ctx), true);
	}
	
	/**
	 * Return whether we should play notification sound on new notification in notification bar
	 * @param ctx Context required
	 * @return true if we should, false otherwise
	 */
	public static boolean isPlayNotificationSound(Context ctx){
		return getBooleanPref(ctx, getBACKGROUND_NOTIFICATION_SOUND(ctx), true);
	}
	
	/**
	 * Set whether we should launch background polling in the background or not
	 * @param ctx Context required
	 * @param isLaunch true if we should, false otherwise
	 */
	public static void setLaunchBackgroundOnStart(Context ctx, boolean isLaunch){
		setBooleanPref(ctx, getBACKGROUND_POLLING_AUTO_START(ctx), isLaunch);
	}
	
	/**
	 * Get the string for querying if we want to do background notifications on phone start
	 * @param ctx Context required to query preference
	 * @return 
	 */
	private static String getBACKGROUND_POLLING_AUTO_START(Context ctx){
		return ctx.getResources().getString(R.string.BACKGROUND_POLLING_AUTO_START);
	}
	
	/**
	 * Get the string for querying if we want to do background notifications sounds
	 * @param ctx Context required to query preference
	 * @return 
	 */
	private static String getBACKGROUND_NOTIFICATION_SOUND(Context ctx){
		return ctx.getResources().getString(R.string.BACKGROUND_NOTIFICATION_SOUND);
	}
	
	/**
	 * Set the last name in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setLastNameNOTFINISHED(Context ctx, String name){
		setStringPref(ctx, LAST_NAME, name);
	}
	
	/**
	 * get the last name from the preference file, that can be accessed by any of the package classes
	 * @return the FIRST name. Null if it does not exist
	 */
	public static String getLastName(Context ctx){
		
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			throw new IllegalAccessError(PREEMPTIVE_ACCESS_ERROR);
		UsersAdapter users = new UsersAdapter(ctx);
		User user = users.getUser(userRowId);
		return user.getLastName();
	}
	
	/**
	 * Set the first name in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setFirstNameNOTFINISHED(Context ctx, String name){
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
	 * get the server user id from the preference file, that can be accessed by any of the package classes
	 * @return the user id. -1 if it does not exist
	 */
	public static long getUserServerId(Context ctx){
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			return -1;
		UsersAdapter users = new UsersAdapter(ctx);
		User user = users.getUser(userRowId);
		return user.getServerId();
	}
	
	/**
	 * Set the user rowId  in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param name The user name to set
	 */
	public static void setUserRowId(Context ctx, Long userId){
		setLongPref(ctx, USER_ROW_ID, userId);
	}
	
	/**
	 * get the user rowId from the preference file, that can be accessed by any of the package classes
	 * @return the user id. -1 if it does not exist
	 */
	public static long getUserRowId(Context ctx){
		return getLongPref(ctx, USER_ROW_ID);
	}
	
	/**
	 * Set the serverId in the preference file that is permanently saved and can be accessed by all package classes.
	 * @param serverId the serverId to set
	 */
	public static void setUserServerId(Context ctx, Long serverId){
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			throw new IllegalAccessError(PREEMPTIVE_ACCESS_ERROR);
		UsersAdapter users = new UsersAdapter(ctx);
		users.setIsSynced(userRowId, true, serverId, true);
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
	public static void setPhoneNumbersNOTFINSISHED(Context ctx, String phones){
		setStringPref(ctx, PHONE_NUMBERS, phones);
	}
	
	/**
	 * Get the list of phone numbers, it will be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 */
	public static String getPhoneNumbers(Context ctx){
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			throw new IllegalAccessError(PREEMPTIVE_ACCESS_ERROR);
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchUser(userRowId);
		String phones = users.getPhones();
		users.close();
		return phones;
	}
	
	/**
	 * Set the list of email addresses, it should be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 * @param emails comma delimited list of email addresses
	 */
	public static void setEmailAddressesNOTFINISHED(Context ctx, String emails){
		setStringPref(ctx, EMAIL_ADDRESSES, emails);
	}
	
	/**
	 * Get the list of email addresses, it should be a comma delimited list
	 * @param ctx Context of calling space where preferences file is stored
	 * @param emails comma delimited list of email addresses
	 */
	public static String getEmailAddresses(Context ctx){
		long userRowId = getUserRowId(ctx);
		if (userRowId == -1)
			throw new IllegalAccessError(PREEMPTIVE_ACCESS_ERROR);
		UsersAdapter users = new UsersAdapter(ctx);
		User user = users.getUser(userRowId);
		return com.tools.Tools.parseArrayIntoString(user.getEmailsArray(), Utils.DELIM);
	}
	
	public static Person getPersonCHECKUSAGE(Context ctx){
		// initialize person
		Person person = new Person();
		
		// fill the person with values
		person.setFirstName(getFirstName(ctx));
		person.setLastName(getLastName(ctx));
		person.setEmailArray(getEmailAddresses(ctx));
		person.setMainPhone(getMainPhone(ctx));
		person.setRowId(getUserRowId(ctx));
		person.setServerId(getUserServerId(ctx));
		person.setPassword(getPassword(ctx));
		person.setUserName(getUserName(ctx));
		
		// return the person
		return person;
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
    public static void setBooleanPref(Context ctx, String pref, boolean value){
    	getPrefs(ctx).edit().putBoolean(pref, value).commit();
    }
    public static boolean getBooleanPref(Context ctx, String pref, boolean defaultValue){
    	return getPrefs(ctx).getBoolean(pref, defaultValue);
    }
}