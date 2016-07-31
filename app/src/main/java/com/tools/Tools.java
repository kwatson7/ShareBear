package com.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.EncoderException;

import com.tools.encryption.EncryptionException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * @author Kyle Watson
 *
 */
public class Tools {

	// static variables for use in methods
	/** SMS field to be inserted as a received message */
	public static final int MESSAGE_TYPE_INBOX = 1; 
	/** SMS field to be inserted as a sent message */
	public static final int MESSAGE_TYPE_SENT = 2; 
	/** field to register receiver for sent sms */
	public static final String SENT = "SMS_SENT";
	/** field to register receiver for delivered sms */
	public static final String DELIVERED = "SMS_DELIVERED";
	/** field where sms receivers can store optional string info */
	public static final String OPTION = "OPTION";
	/** field where sms receivers store the number of texts sent */
	public static final String NUM_MESSAGES = "NUM_MESSAGES";
	/** The preferred buffer size for writing input streams to file */
	public static final int BUFFER_SIZE = 1024;

	private static final String LOG_TAG = "Tools";
	private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

	/**
	 * Get a filename from a given uri
	 * @param cr A content resolver required to query the database
	 * @param uri The uri we will query
	 * @return The filename found in the given ur, or null if none.
	 */
	public static String getFileNameUri(ContentResolver cr, Uri uri){

		// what columns we will extract
		String[] projection = {
				MediaStore.Images.ImageColumns.DATA,
				MediaStore.Images.ImageColumns.DISPLAY_NAME}; 

		// query the uri
		String result = null;
		Cursor cur = cr.query(uri, projection, null, null, null); 

		// read the filename
		if (cur!=null && cur.moveToFirst()) { 
			result = cur.getString(0); 
		} 
		if (cur != null)
			cur.close();

		return result;
	} 

	/**
	 * Log to debug the size of the native heap
	 * @param clazz
	 */
	public static void logHeap() {
		Double allocated = Double.valueOf(Debug.getNativeHeapAllocatedSize())/1048576.0;
		Double free = Double.valueOf(Debug.getNativeHeapFreeSize())/1048576.0;
	    Double available = Double.valueOf(Debug.getNativeHeapSize())/1048576.0;
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		Log.d(LOG_TAG, "debug. =================================");
		Log.d(LOG_TAG, "debug.heap native: allocated " + df.format(allocated) + "MB of " + df.format(available) + "MB (" + df.format(free) + "MB free");
		Log.d(LOG_TAG, "debug.memory: allocated: " + df.format(Double.valueOf(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(Double.valueOf(Runtime.getRuntime().maxMemory()/1048576.0))+ "MB (" + df.format(Double.valueOf(Runtime.getRuntime().freeMemory()/1048576.0)) +"MB free)");
		//System.gc();
		//System.gc();

		// don't need to add the following lines, it's just an app specific handling in my app        
		//if (allocated>=(new Double(Runtime.getRuntime().maxMemory())/new Double((1048576))-MEMORY_BUFFER_LIMIT_FOR_RESTART)) {
		//     android.os.Process.killProcess(android.os.Process.myPid());
		// }
	}

	/**
	 * Convert an angle to a string formatted as follows: <br>
	 * deg/1,min/1,sec*1000/1000, for example,
	 * 15.246 as an input would yield: <br>
	 * "15/1,14/1,45600/1000" <br>
	 * This is needed to properly store gps info in exif headers of images
	 * @param angle
	 * @return
	 */
	public static String convertAngletoString(double angle){

		int deg;
		int min;
		int sec;

		deg = (int) angle;
		min = (int) ((angle - deg)*60);
		sec = (int) ((angle - (deg + min/60.0))*3600);
		return deg+"/1,"+min+"/1,"+sec*1000+"/1000";
	}

	/**
	 * Grab phones full number
	 * @param act Activity required to get he phone number
	 * @return The phone number of the phone, null if none found.
	 * @See getMyStrippedPhoneNumber for removing +1
	 */
	public static String getMyPhoneNumber(Activity act){  
		TelephonyManager mTelephonyMgr;  
		mTelephonyMgr = (TelephonyManager)  
		act.getSystemService(Context.TELEPHONY_SERVICE);   
		return mTelephonyMgr.getLine1Number(); 
	}  

	/**
	 * Grab phone number with leading +1 removed... if there
	 * @param act Activity required to get the phone number
	 * @return The phone number with +1 stripped 
	 * @See getMyPhoneNumber to not strip +1
	 */
	public static String getMyStrippedPhoneNumber(Activity act){  
		String s = getMyPhoneNumber(act); 

		if (s != null){
			// strip off 1 and +
			if (s.length()==0)
				return s;
			if (s.charAt(0) == '+')
				s = s.substring(1);
			if (s.length()==0)
				return s;
			if (s.charAt(0) == '1')
				s = s.substring(1);
		}

		return s; 
	}

	/**
	 * Eliminate all but numerics from a string, and returns a new string. The original is unaffected
	 * @param input The original string
	 * @return new string with only numerics present
	 */
	public static String keepOnlyNumerics(String input){
		if (input == null)
			return input;

		// copy the string
		String working = new String(input);
		return working.replaceAll("[^0-9]", "");
	}

	/**
	 * Format phone number to 234-567-8910.
	 * If it cannot be formatted this way, then simply a copy of the
	 * incoming string is returned. Original string is not affected
	 * @param input the input string to format
	 * @return The formatted string
	 */
	public static String formatPhoneNumber(String input){

		// put as only numeric phone number
		String out = fixPhoneString(input);

		// add in - or nothing at all
		if (out.length() != 10)
			out = new String(input);
		else
			out = out.substring(0, 3)+"-"+out.substring(3, 6)+"-"+out.substring(6);

		return out;
	}

	/**
	 * get the string indicated by the tag in the given xml string
	 * @param xml the xml string to parse
	 * @param tag The tag we are looking for
	 * @return The string at the tag, null if not found or any inputs are null
	 */
	public static String getXmlValueAtTag(String xml, String tag){

		// null inputs
		if (xml==null || tag==null)
			return null;

		// look for tag
		int begOfFirstTag = xml.indexOf("<"+tag);
		if (begOfFirstTag==-1)
			return null;

		// find end of tag
		int endOfFirstTag = xml.indexOf(">", begOfFirstTag+tag.length()+1);
		if (endOfFirstTag==-1)
			return null;

		// look for beginning of end tag
		int begOfSecondTag = xml.indexOf("</"+tag, endOfFirstTag+1);
		if (begOfSecondTag==-1)
			return null;

		// find end of end tag
		int endOfSecondTag = xml.indexOf(">", begOfSecondTag+tag.length()+2);
		if (endOfSecondTag==-1)
			return null;

		// all tags were found, so simply look between tags
		return xml.substring(endOfFirstTag+1, begOfSecondTag);

	}

	/**
	 * Hide the keyboard.
	 * @param ctx The context to perform this action
	 * @param view Any view in the context that can request the keyboard to be hidden
	 */
	public static void hideKeyboard(Context ctx, View view){
		InputMethodManager imm = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * Show the keyboard.
	 * @param ctx The context to perform this action
	 * @param view Any view in the context that can request the keyboard to be shown
	 */
	public static void showKeyboard(Context ctx, View view){
		InputMethodManager imm = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, 0);
	}

	/**
	 * Send an sms message. If message is empty or null, or if the phoneNumber is empty or null,
	 *  then nothing happens and simply returns. Can send multi-part messages > 160 characters.
	 *  To pick up action on the receive and delivered put the following code in you main context. Also
	 *  make sure that the receivers are only turned on once, and turned off when not needed 
	 *  anymore else, they will be called over and over. Any user info is stored in the broadcast intents
	 *  under com.tools.Tools.OPTION, and the total number of texts sent (for multi-part-texts) is saved
	 *  under com.tools.Tools.NUM_MESSAGES.
	 *  <pre>
	 * {@code
	 * ctx.registerReceiver(new BroadcastReceiver(){ ... }, new IntentFilter(com.tools.Tools.SENT));
	 * ctx.registerReceiver(new BroadcastReceiver(){ ... }, new IntentFilter(com.tools.Tools.DELIVERED));
	 * </pre>
	 * @param ctx The context to use
	 * @param phoneNumber The phone number to send it to
	 * @param message The message
	 * @param option A string to be sent with the intent for optional info. It is stored in the intent as com.tools.Tools.OPTION
	 * @return the total number of texts attempted to be sent, look in receiver for final notification
	 */
	public static int sendSMS(final Context ctx, String phoneNumber, String message, String option){

		// break out if empty message
		if (message == null 
				|| message.length()==0
				|| phoneNumber == null
				|| phoneNumber.length() == 0)
			return 0;

		// grab the sms manager
		SmsManager sms = SmsManager.getDefault();

		// see if we need to break up the message
		int nMessages = 1;
		ArrayList<String> parts = null;
		parts = sms.divideMessage(message);
		nMessages = parts.size();

		// create intent for sending
		Intent intentSent = new Intent(SENT);
		intentSent.putExtra(OPTION, option);
		intentSent.putExtra(NUM_MESSAGES, nMessages);
		PendingIntent sentPI = PendingIntent.getBroadcast(ctx, 0,
				intentSent, PendingIntent.FLAG_UPDATE_CURRENT);

		// create intent for delivering
		Intent intentDelivered = new Intent(DELIVERED);
		intentDelivered.putExtra(OPTION, option);
		intentDelivered.putExtra(NUM_MESSAGES, nMessages);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(ctx, 0,
				intentDelivered, PendingIntent.FLAG_UPDATE_CURRENT);   

		// send the messages
		for (int i = 0; i < parts.size(); i++)
			sms.sendTextMessage(phoneNumber, null, parts.get(i), sentPI, deliveredPI);

		// save text into database
		insertSMSDatabse(ctx, phoneNumber, message, MESSAGE_TYPE_SENT);

		return nMessages;

		// example broadcast receivers
		/*
		//---when the SMS has been sent---
		ctx.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(ctx, "SMS sent", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(ctx, "Generic failure", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(ctx, "No service", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(ctx, "Null PDU", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(ctx, "Radio off", 
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		//---when the SMS has been delivered---
		ctx.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(ctx, "SMS delivered", 
							Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(ctx, "SMS not delivered", 
							Toast.LENGTH_SHORT).show();
					break;                        
				}
			}
		}, new IntentFilter(DELIVERED));    
		 */ 
	}

	/**
	 * Insert a message into the sms database
	 * @param ctx The context to use to perform this action
	 * @param phoneNumber The phone number attached to the sms
	 * @param message The message of the sms
	 * @param messageType the message is either sent (com.tools.Toos.MESSAGE_TYPE_SENT) 
	 * or received (com.tools.Tools.MESSAGE_TYPE_INBOX)
	 * @return The url of the sms database in which the message was inserted. Null if error occured
	 */
	public static Uri insertSMSDatabse(
			Context ctx,
			String phoneNumber, 
			String message, 
			int messageType){

		// the tags for various setting to go into the database
		//final String PERSON = "person"; 
		final String ADDRESS = "address"; 
		final String DATE = "date"; 
		final String READ = "read"; 
		final String STATUS = "status"; 
		final String TYPE = "type"; 
		final String BODY = "body";  

		// default values
		int status = -1;
		int read = 1;

		// grab current data
		Date date = new Date();

		// fill the values
		ContentValues values = new ContentValues(); 
		values.put(ADDRESS, phoneNumber); 
		values.put(DATE, String.valueOf(date.getTime())); 
		values.put(READ, read); 
		values.put(STATUS, status); 
		values.put(TYPE, messageType); 
		values.put(BODY, message); 
		Uri inserted = null;
		try{
			inserted = ctx.getContentResolver().insert
			(Uri.parse("content://sms//sent"), values);
		}catch(Exception e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));
		}

		return inserted;
	}

	/**
	 * Determine if we have an internet connection or in the middle of aquireing one
	 * @param ctx Required to check
	 * @return true if connected or connectiong and false otherwise
	 */
	public static boolean isOnline(Context ctx) {
	    ConnectivityManager cm =
	        (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * Show a dialog that will only be shown once at startup. 
	 * @param ctx The context where this dialog will be displayed
	 * @param welcomeScreenShownPref The string where the preference is stored
	 * @param title The title of the box
	 * @param text The text in the body
	 * @param toShow Boolean whether to show or not. Input null to read from preferences
	 */
	public static void showOneTimeDialog(
			Context ctx,
			String welcomeScreenShownPref,
			String title,
			String text, 
			Boolean toShow){

		// open the prefs
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

		// second argument is the default to use if the preference can't be found
		if (toShow == null)
			toShow = !prefs.getBoolean(welcomeScreenShownPref, false);

		if (toShow) {
			// here you can launch another activity if you like
			// the code below will display a popup

			new AlertDialog.Builder(ctx).
			setIcon(android.R.drawable.ic_dialog_info).
			setTitle(title).setMessage(Html.fromHtml(text)).
			setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).show();
			if (prefs != null){
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(welcomeScreenShownPref, true);
				editor.commit();
			}
		}
	}

	/**
	 * Create a new group in the android databse
	 * @param ctx The context to use
	 * @param accountType the account type, for example "com.google", null for phone
	 * @param accountName The account name, for example "user@gmail.com", null for phone
	 * @param groupName The group name, fore example "friends"
	 * @return The group ID is returned
	 */
	public static String makeNewGroup(
			Context ctx,
			String accountType,
			String accountName,
			String groupName){

		// grab the content resolver, put group name, accountType, and accountName into database and create
		ContentResolver cr = ctx.getContentResolver();
		ContentValues groupValues = new ContentValues();
		groupValues.put(ContactsContract.Groups.TITLE, groupName);
		groupValues.put(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		groupValues.put(ContactsContract.Groups.ACCOUNT_TYPE, accountType);
		Uri uri = cr.insert(ContactsContract.Groups.CONTENT_URI, groupValues);

		// the group ID
		return uri.getLastPathSegment();
	}

	/**
	 * Take in an amount of minutes and convert it to a human readable output.
	 * For example, if minutes=384, then the output will be:
	 * "6 hours and 24 minutes".
	 * <p></p>
	 * The allowable outputs are years (365 days), weeks, days, hours, minutes, seconds
	 * <p></p>
	 * @param minutes the minutes to convert
	 * @param maxDecimals The maximum number of decimals to show in seconds. (-1 shows the max)
	 * @param maxCategories The maximum number of categories to show (-1 shows max). For example, if
	 * maxCategories = 2, then we would show 2 days, 6 hours (and then chop off all remaining times)
	 * @return the human readable string
	 */
	public static String convertMinutesToFormattedString(double minutes, int maxDecimals, int maxCategories){

		// the output string
		String output = "";

		// keep track of how many categories we have shown
		int categoriesShown = 0;
		if (maxCategories == -1)
			maxCategories = 1000;

		// find how many years
		int years = (int) Math.floor(minutes/(365*60*24));
		if (years > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (years == 1)
				output = years + " year, ";
			else
				output = years + " years, ";
			minutes = minutes - (years*365*60*24);
		}

		// how many weeks
		int weeks = (int) Math.floor(minutes/(60*24*7));
		if (weeks > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (weeks == 1)
				output += weeks + " week, ";
			else
				output += weeks + " weeks, ";
			minutes = minutes - (weeks*60*24*7);
		}

		// how many days
		int days = (int) Math.floor(minutes/(60*24));
		if (days > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (days == 1)
				output += days + " day, ";
			else
				output += days + " days, ";
			minutes = minutes - (days*60*24);
		}

		// how many hours
		int hours = (int) Math.floor(minutes/(60));
		if (hours > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (hours == 1)
				output += hours + " hour, ";
			else
				output += hours + " hours, ";
			minutes = minutes - (hours*60);
		}

		// how many minutes
		int minutesDisp = (int) Math.floor(minutes);
		if (minutesDisp > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (minutesDisp == 1)
				output += minutesDisp + " minute, ";
			else
				output += minutesDisp + " minutes, ";
			minutes = minutes - (minutesDisp);
		}

		// how many seconds
		float seconds =  (float) (minutes*60.0);
		if (maxDecimals != -1)
			seconds = (float) com.tools.MathTools.round(seconds, maxDecimals);
		if (seconds > 0 && categoriesShown < maxCategories){
			categoriesShown++;
			if (seconds == 1)
				output += new BigDecimal(seconds).stripTrailingZeros().toPlainString() + " second, ";
			else
				output += new BigDecimal(seconds).stripTrailingZeros().toPlainString()  + " seconds, ";
		}

		// return the string minus the last comma and space
		if (output.length() >= 2)
			return output.substring(0, output.length()-2);		
		else
			return output;
	}
	
	/**
	 * Take in an amount of milliseconds and convert it to a human readable output.
	 * For example, if minutes=384, then the output will be:
	 * "6 hours and 24 minutes".
	 * <p></p>
	 * The allowable outputs are years (365 days), weeks, days, hours, minutes, seconds
	 * <p></p>
	 * If milliseconds is 70000 and minFractionToShowNextCategory is 0.25, that means we
	 * currently have 70 seconds to display, which will format to 1 minute and 10 seconds... however
	 * since the last category (10 seconds) only adds 1/6th or 0.16 extra information to 1 minute
	 * and the min threshold input is 0.25 only "1 minute" will be displayed. The input time
	 * would have to be 75000 before the (now 15 seconds) would be displayed 
	 * @param milliseconds the milliseconds to convert
	 * @param minFractionToShowNextCategory Minimum amount of extra info needed to show next category.
	 * @return the human readable string
	 */
	public static String convertMillisecondsToFormattedString(double milliseconds, double minFractionToShowNextCategory){

		// if 0 then return 0 seconds
		if (milliseconds == 0)
			return "0 seconds";

		// the output string
		String output = "";
		
		// if negative then change string
		if (milliseconds < 0){
			milliseconds = -milliseconds;
			output = "- ";
		}

		// convert milliseconds to minutes
		double minutes = milliseconds/60000;
		
		// keep track of base amount to know when to stop adding new info
		double baseMinutes = 0;
		
		// find how many years
		int years = (int) Math.floor(minutes/(365*60*24));
		if (years > 0){
			if (years == 1)
				output = years + " year, ";
			else
				output = years + " years, ";
			baseMinutes = (years*365*60*24);
			minutes = minutes - baseMinutes;
		}

		// how many weeks
		int weeks = (int) Math.floor(minutes/(60*24*7));
		if (weeks > 0 && (baseMinutes == 0 || minutes/baseMinutes >= minFractionToShowNextCategory)){
			if (weeks == 1)
				output += weeks + " week, ";
			else
				output += weeks + " weeks, ";
			minutes = minutes - (weeks*60*24*7);
			if (baseMinutes == 0)
				baseMinutes = weeks*60*24*7;
		}

		// how many days
		int days = (int) Math.floor(minutes/(60*24));
		if (days > 0  && (baseMinutes == 0 || minutes/baseMinutes >= minFractionToShowNextCategory)){
			if (days == 1)
				output += days + " day, ";
			else
				output += days + " days, ";
			minutes = minutes - (days*60*24);
			if (baseMinutes == 0)
				baseMinutes = days*60*24;
		}

		// how many hours
		int hours = (int) Math.floor(minutes/(60));
		if (hours > 0 && (baseMinutes == 0 || minutes/baseMinutes >= minFractionToShowNextCategory)){
			if (hours == 1)
				output += hours + " hour, ";
			else
				output += hours + " hours, ";
			minutes = minutes - (hours*60);
			if (baseMinutes == 0)
				baseMinutes = hours*60;
		}

		// how many minutes
		int minutesDisp = (int) Math.floor(minutes);
		if (minutesDisp > 0 && (baseMinutes == 0 || minutes/baseMinutes >= minFractionToShowNextCategory)){
			if (minutesDisp == 1)
				output += minutesDisp + " minute, ";
			else
				output += minutesDisp + " minutes, ";
			minutes = minutes - (minutesDisp);
			if (baseMinutes == 0)
				baseMinutes = minutesDisp;
		}

		// how many seconds
		double seconds =  minutes*60.0;
		if (seconds > 0 && (baseMinutes == 0 || minutes/baseMinutes >= minFractionToShowNextCategory)){
			// then only go out to decimal that still meets minFraction
			double secondsToDisplay = 0;
			int maxDecimal = -1;
			for (int decimal = -1; decimal < 6; decimal++){
				if (baseMinutes != 0){
					if (Math.abs(seconds-secondsToDisplay) > baseMinutes*minFractionToShowNextCategory){
						secondsToDisplay += com.tools.MathTools.round(seconds-secondsToDisplay, decimal);
						maxDecimal = decimal;
					}
				}else{
					secondsToDisplay += com.tools.MathTools.round(seconds-secondsToDisplay, decimal);
					baseMinutes = secondsToDisplay/60;
					maxDecimal = decimal;
				}
			}

			if (secondsToDisplay == 1)
				output += new BigDecimal(secondsToDisplay).setScale(
						maxDecimal, BigDecimal.ROUND_HALF_UP).toPlainString() + " second, ";
			else
				output += new BigDecimal(secondsToDisplay).setScale(
						maxDecimal, BigDecimal.ROUND_HALF_UP).toPlainString()  + " seconds, ";
		}
		
		// return the string minus the last comma and space
		if (output.length() >= 2)
			return output.substring(0, output.length()-2);		
		else
			return output;
	}

	/**
	 * Request a sync for all your accounts. Make sure to include:
	 * <uses-permission android:name="android.permission.READ_SYNC_SETTINGS"/> 
	 * in your manifest
	 * @param ctx
	 */
	public static void requestSync(Context ctx)
	{
		try{
			AccountManager am = AccountManager.get(ctx);
			Account[] accounts = am.getAccounts();

			for (Account account : accounts)
			{
				int isSyncable = ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY);

				if (isSyncable > 0)
				{
					Bundle extras = new Bundle();
					extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
					ContentResolver.requestSync(account, ContactsContract.AUTHORITY, extras);
				}
			}
		}catch(Exception e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));
		}
	}

	/**
	 * Take string as input (a phone number) and pull off all non numeric characters and any leading 1 or +
	 * @param input the input phone number, if null, null will be output
	 * @return The only numeric phone number
	 */
	public static String fixPhoneString(String input){

		if (input == null)
			return input;

		// only numerics
		String out = com.tools.Tools.keepOnlyNumerics(input);

		// strip off 1 and +
		if (out.length()==0)
			return out;
		if (out.charAt(0) == '+')
			out = out.substring(1);
		if (out.length()==0)
			return out;
		if (out.charAt(0) == '1')
			out = out.substring(1);

		return out;
	}

	/**
	 * Determine if folder is empty. If it has empty folders in it, that is still considered empty.
	 * @param folderPath The path to determine if it is empty.
	 * @param ignoreFile a file name to be ignored, can be null or "" and is ignored. For example, ".nomedia" or "thumbnails.db"
	 * @return true if folder is empty or only contains empty folders, false otherwise
	 */
	public static boolean isFolderEmpty(String folderPath, String ignoreFile){
		// create file object
		File folder = new File(folderPath);

		// if it's a file, return false, otherwise iterate through the folder
		if (folder.isDirectory()){
			// get the list of files and check if they themselves are empty
			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++){
				if (!isFolderEmpty(files[i].getAbsolutePath(), ignoreFile))
					return false;
			}
		}else{
			if (ignoreFile == null || ignoreFile.length() == 0)
				return false;
			else if (ignoreFile.compareTo(folder.getName()) == 0)
				return true;
			else
				return false;
		}

		// if we got here, then it must be empty
		return true;	
	}

	/**
	 * Delete a folder and all of its sub folders given they are empty. <br>
	 * If they are not empty, then nothing happens
	 * @param folder The folder to delete
	 * @param ignoreFile a file name to be ignored, can be null or "" and is ignored. For example, ".nomedia" or "thumbnails.db"
	 */
	public static void deleteEmptyFolder(File folder, String ignoreFile) {
		if (!isFolderEmpty(folder.getAbsolutePath(), ignoreFile))
			return;

		File[] files = folder.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()) {
					deleteEmptyFolder(f, ignoreFile);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/**
	 * Parse an arraylist into a list separated by the input delim.
	 * @param array the array to put into a string
	 * @param delim the delimiter to user when separatting strings (ie ",")
	 * @return A string, ie "bob,jane,bill"
	 * @see setArrayFromString
	 */
	public static <TYPE>String parseArrayIntoString(ArrayList<TYPE> array, String delim){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.size()-1; i++){
			builder.append(array.get(i).toString());
			builder.append(delim);
		}
		if (array.size() >= 1){
			builder.append(array.get(array.size()-1).toString());
			builder.append(delim);
		}
		return builder.toString();
	}

	/**
	 * Get the bitmap from a given image view. Will return null if none available
	 * @param image The imageView to extract bitmap from
	 * @return The bitmap of the imageView, null if none.
	 */
	public static Bitmap getImageViewBitmap(ImageView image){
		Drawable drawable = image.getDrawable();
		Bitmap bitmap = null;
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			bitmap = bitmapDrawable.getBitmap();
		}

		return bitmap;
	}

	/**
	 *  Parse a string into an arraylist separating by delimiter
	 *  @param listString the string to parse 
	 * 	@param delim the delimiter that separates strings
	 * 	@return An arrayList of the individual strings
	 *  @see parseArrayIntoString
	 */
	public static ArrayList<String> setArrayFromString(String listString, String delim){
		if (listString == null)
			return new ArrayList<String>(0);

		// break up string by commas
		String[] tokens = listString.split(delim);
		ArrayList<String> array = new ArrayList<String>(tokens.length);

		// fill array
		for (int i = 0; i < tokens.length; i++){
			array.add(tokens[i]);
		}

		return array;
	}

	/**
	 * Generate a random string using 0-9, and a-v
	 * @param nCharacters The number of characters to create
	 * @return The random string
	 */
	public static String randomString(int nCharacters){
		SecureRandom random = new SecureRandom();
		return new BigInteger(nCharacters*5, random).toString(32);
	}

	/**
	 * Post a notification to the notification bar and use default notification sound and lights.
	 * @param act The calling activity
	 * @param icon The id to the icon to use
	 * @param tickerText The text that shows in the notification bar
	 * @param contentTitle The bolded text that shows once the user pulls down the menu
	 * @param contentText The non-bolded text to show when the user pulls down the menu
	 * @param notificationId An id used to keep track of what notification this is. If
	 * you post a new notification with the same ID, and an old one is still present,
	 * the old one will be overwritten.
	 * @param playSound should we play the default notification sound?
	 * @param intentToLaunch the intent to launch when the user click the notification. Use null for nothing.
	 */
	public static void postNotification(
			Context ctx,
			int icon,
			String tickerText,
			String contentTitle,
			String contentText,
			int notificationId,
			boolean playSound,
			Intent intentToLaunch){

		// load the manager
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(ns);

		//Instantiate the Notification:
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		if (playSound)
			notification.defaults =  Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND;
		else
			notification.defaults =  Notification.DEFAULT_LIGHTS;
		
		//Define the notification's message and PendingIntent:
		Context context = ctx.getApplicationContext();
		if (intentToLaunch == null)
			intentToLaunch = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intentToLaunch, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		//Pass the Notification to the NotificationManager:
		mNotificationManager.notify(notificationId, notification);
	}

	/**
	 * Share a picture with a sharing intent
	 * @param ctx The context required to start the intent
	 * @param subject The subject of the message to send
	 * @param body The body in the message
	 * @param fileName The filename to send. 
	 * @param prompt The prompt to the user when selecting an app to share the picture with
	 * @return true if we could launch the intent
	 */
	public static boolean sharePicture(Context ctx, String subject, String body, String fileName, String prompt){

		// if any values are null, then return false
		if (ctx == null || subject == null || body == null || prompt == null || fileName == null || fileName.length() == 0)
			return false;

		// grab the file
		File file = new File(fileName);

		// if no exist, then return false
		if (!file.exists())
			return false;

		// create the intent
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);

		// set the type
		sharingIntent.setType("image/jpeg");

		// create and load the message and subject
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
		sharingIntent.putExtra(Intent.EXTRA_TITLE, body);

		// put picture in intent
		sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

		// launch the intent
		ctx.startActivity(Intent.createChooser(sharingIntent, prompt));

		// return true
		return true;
	}

	/**
	 * Determine if the external storage is currently mounted
	 * @param requireWriteAccess true to require write access and false to only require read access
	 * @return True if mounted, false otherwise
	 */
	static public boolean isStorageAvailable(boolean requireWriteAccess) {

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			if (requireWriteAccess) {
				boolean writable = checkFsWritable();
				return writable;
			} else {
				return true;
			}
		} else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the file system is writable.
	 *  Create a temporary file to see whether a volume is really writeable.
	 *  It's important not to put it in the root directory which may have a
	 *  limit on the number of files. No file is actually written
	 * @return true if we can write to the external file system, and false otherwise
	 */
	private static boolean checkFsWritable() {

		String directoryName = Environment.getExternalStorageDirectory().toString() + "/DCIM";
		File directory = new File(directoryName);
		if (!directory.isDirectory()) {
			if (!directory.mkdirs()) {
				return false;
			}
		}
		return directory.canWrite();
	}

	/**
	 * Read a file into a byte[]. Output will be null if file cannot be read <br>
	 * @See ImageProcessing.readFullFile ***
	 * @param fileName the file to read
	 * @return the byte[] of the file or null if couldn't be read
	 * @throws IOException 
	 */
	public static byte[] readFile(String fileName) throws IOException{

		// open the file for reading
		if (fileName == null || fileName.length() == 0)
			return null;
		RandomAccessFile f = new RandomAccessFile(fileName, "r");

		// read the file data
		byte[] b = null;
		try{
			b = new byte[(int)f.length()];
			f.read(b);
		}finally{
			try{
				f.close();
			}catch(Exception e){
				Log.e(LOG_TAG, Log.getStackTraceString(e));
			}
		}

		return b;
	}
	
	/**
	 * Recycle the bitmap of the given imageView. If there is no bitmap, no action, and no error
	 * @param imageView The imageView to recycle the bitmap of
	 */
	public static void recycleImageViewBitmap(ImageView imageView){
		
		// delete imageview bitmap
		if (imageView != null){
			Drawable drawable = imageView.getDrawable();
			if (drawable != null && drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				bitmap.recycle();
			}
		}
	}

	/**
	 * Save the view and all its children to a bitmap. if View is null, null is returned <br>
	 * @param view the view to save
	 * @param viewsToHide a list of views to hide before creating the bitmap, null is acceptabl
	 * @param viewToTakeFocus allow for the given view to take focus. Null is accetpable
	 * This is helpful, because if you selected a button to trigger this method, the button will be highlighted
	 * A layout view is a usually a good choice
	 * @param nullColor the color to ignore and to only extract the center region, for example to chop the black edges
	 * enter Color.rgb(0, 0, 0); Enter null to ignore
	 * @return the bitmap created, null if error occured
	 */
	public static Bitmap saveViewToBitmap(View view, ArrayList<View> viewsToHide, View viewToTakeFocus, Integer nullColor){

		// null view
		if (view == null){
			Log.e(LOG_TAG, "null view was input");
			return null;
		}

		// find which view has focus and turn it off
		int childHasFocus = -1;
		if (view instanceof ViewGroup){
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++){
				View v = vg.getChildAt(i);
				if (v.hasFocus()){
					childHasFocus = i;
					break;
				}
			}
		}

		// hide requested views
		HashMap<View, Integer> previousStateViews = new HashMap<View, Integer>();
		if (viewsToHide != null){
			for (View item : viewsToHide){
				if (item != null){
					previousStateViews.put(item, item.getVisibility());
					item.setVisibility(View.INVISIBLE);
					item.invalidate();
				}
			}
		}

		// the the focus to the given view
		boolean isFocusable = false;
		if (viewToTakeFocus != null){
			isFocusable = viewToTakeFocus.isFocusableInTouchMode();
			viewToTakeFocus.setFocusableInTouchMode(true);
			viewToTakeFocus.requestFocus();
		}

		// create the bitmap
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		if (bitmap != null){
			Canvas c = new Canvas(bitmap);
			view.draw(c);
		}

		// extract center region of bitmap
		if (nullColor != null)
			bitmap = ImageProcessing.bitmapExtractCenter(bitmap, nullColor);

		// show the views that we previously hid
		if (viewsToHide != null){
			for (View item : viewsToHide){
				if (item != null){
					item.setVisibility(previousStateViews.get(item));
					item.invalidate();
				}
			}
		}

		// reset focus
		if (viewToTakeFocus != null){
			viewToTakeFocus.setFocusableInTouchMode(isFocusable);
		}
		if (view instanceof ViewGroup && childHasFocus != -1){
			ViewGroup vg = (ViewGroup) view;
			View v = vg.getChildAt(childHasFocus);
			v.requestFocus();
		}

		// the bitmap
		return bitmap;
	}

	/**
	 * Checks if a bitmap with the specified size fits in memory
	 * @param bmpwidth Bitmap width
	 * @param bmpheight Bitmap height
	 * @param bmpdensity Bitmap bpp (use 2 as default)
	 * @return true if the bitmap fits in memory false otherwise
	 */
	public static boolean checkBitmapFitsInMemory(long bmpwidth,long bmpheight, int bmpdensity ){
		long reqsize=bmpwidth*bmpheight*bmpdensity;
		long allocNativeHeap = Debug.getNativeHeapAllocatedSize();

		final long heapPad=(long) Math.max(4*1024*1024,Runtime.getRuntime().maxMemory()*0.1);
		if ((reqsize + allocNativeHeap + heapPad) >= Runtime.getRuntime().maxMemory())
		{
			return false;
		}
		return true;
	}

	/**
	 * If we input a valid email address. If null or empty, then false. <br>
	 * ie someone@gmail.com vs someone.com
	 * @param email The email to check
	 * @return True if acceptible format, false otherwise
	 */
	public static boolean isValidEmail(String email){
		if (email == null || email.length() == 0)
			return false;
		Pattern pattern;
		Matcher matcher;

		final String EMAIL_PATTERN = 
			"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

		pattern = Pattern.compile(EMAIL_PATTERN);

		// Validate hex with regular expression
		matcher = pattern.matcher(email);
		return matcher.matches();
	}

	/**
	 * Write an input stream to a file. The required folders will be written if not present.
	 * @param inputStream The input stream to read from
	 * @param filePath The path to write to
	 * @param dataLength the total length of the data to read, <0 if unknown
	 * @param progressBar A progress bar to update as will write the file, null if none. Will be kept as a weak reference, so it won't cause a leak
	 * @return the number of bytes written to file
	 * @throws IOException
	 */
	public static long writeInputStreamToFile(
			InputStream inputStream,
			String filePath,
			final long dataLength,
			ProgressBar progressBar)
	throws IOException{

		// store weakReference
		WeakReference<ProgressBar> weakProgress = new WeakReference<ProgressBar>(progressBar);
		progressBar = null;

		// if there was a bad input file
		if (filePath == null)
			throw(new FileNotFoundException());

		// write the required directories to the file
		com.tools.Tools.writeRequiredFolders(filePath);

		// initialize some variables
		OutputStream output = null;
		long total = 0;

		// wrap in try-catch, so we can perform cleanup
		try{
			// make sure the save file path is accessible
			output = new FileOutputStream(filePath);

			// setup for downloading
			byte data[] = new byte[BUFFER_SIZE];
			int count;							

			// write in buffered increments
			while ((count = inputStream.read(data)) != -1) {
				output.write(data, 0, count);
				total += count;

				// show the progress bar
				final ProgressBar prog = weakProgress.get();
				if (prog != null && dataLength > 0){
					Activity act = (Activity)prog.getContext();
					final long total2 = total;
					act.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							prog.setVisibility(View.VISIBLE);
							prog.setProgress((int) (100 * total2 / dataLength));

						}
					});
				}
			}

		}finally{

			try{
				final ProgressBar prog = weakProgress.get();
				if (prog != null && dataLength > 0){
					Activity act = (Activity)prog.getContext();
					act.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							prog.setVisibility(View.INVISIBLE);								
						}
					});
				}
			}catch(Exception e){
				Log.e(LOG_TAG, Log.getStackTraceString(e));
			}

			// perform cleanup
			if (output != null){
				try{ 
					output.flush();
				}catch(Exception e){
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
			}
			if (output != null){
				try{ 
					output.close();
				}catch(Exception e){
					Log.e(LOG_TAG, Log.getStackTraceString(e));
				}
			}
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, Log.getStackTraceString(e));
			}
		}

		return total;
	}

	/**
	 * Write all the folders required to write the given file or folder path. <br>
	 * ie. filePath = "/sdcard/appName/picture.jpg". This will make sure that the folder at /sdcard/appName exists
	 * @param filePath The path we want to eventually write
	 * @throws IOException Throws an exception if we couldn't create the path
	 */
	public static void writeRequiredFolders(String filePath)
	throws IOException{

		// no file
		if (filePath == null || filePath.length() == 0)
			return;

		// grab the parent folder
		File file = new File(filePath);
		File parent = file.getParentFile();

		// no parent
		if (parent == null)
			return;

		// check for existence and make if not exist
		if (!parent.exists())
			if(!parent.mkdirs())
				throw new IOException("Cannot create folder " + parent.getAbsolutePath());
	}
	
	/**
	 * Launch gmail intent (if found on device) to a new screen with the given search.
	 * @param ctx Context required to launch intent
	 * @param searchQuery The search query
	 * @throws Exception thrown if gmail client not found on device
	 */
	public static void launchGmailSearchDOESNTWORK(Context ctx, String searchQuery)
			throws Exception{
		try{
			Intent mailClient = new Intent(Intent.ACTION_SEARCH);
			mailClient.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mailClient.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivity");
			mailClient.putExtra("query", "Share Bear");
			ctx.startActivity(mailClient);
		}catch (ActivityNotFoundException e){
			throw new Exception("Gmail not on device");
		}
	}
	
	/**
	 * Launch gmail intent (if found on device)
	 * @param ctx Context required to launch intent
	 * @throws Exception thrown if gmail client not found on device
	 */
	public static void launchGmailAppDOESNWORK(Context ctx)
			throws Exception{
		try{
			Intent gmintent = new Intent(Intent.ACTION_VIEW); 
			gmintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			gmintent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivityGoogleMail"); 
			ctx.startActivity(gmintent);
		}catch (ActivityNotFoundException e){
			throw new Exception("Gmail not on device");
		}
	}

	
	
	//TODO: the below class need to be commented and/or verified
	/**
	 * Read a picture from the given path, return null if unsuffessful <br>
	 * Make sure to NOT call on main UI thread because it's slow <br>
	 * Will be properly rotated based on exif data stored in image. <br>
	 * Performs no resizeing, so if this picture is not already small, you may get a memory crash
	 * @param path The path where the picture is stored
	 * @return the bitmap data, null if unsuccessful
	 */
	public static Bitmap getThumbnail(String path){
		try{
			// open the path if it exists
			if (path != null && path.length() != 0 && (new File(path)).exists()){

				// read the bitmap
				Bitmap bmp = BitmapFactory.decodeFile(path);
				if (bmp == null)
					return bmp;

				// now do the rotation
				float angle =  ImageProcessing.getExifOrientationAngle(path);
				if (angle != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(angle);

					bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
							bmp.getHeight(), matrix, true);
				}
				return bmp;
			}
			else	
				return null;
		}catch(IOException e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Show an alert window if we can (the activity is not finishing and not null), <br>
	 * else launch a new window using applicationContext, else show a toast
	 * @param act The activity that will launch the dialog
	 * @param ctx The context to launch a new activity window
	 * @param message The message to show
	 * @return the dialog if it was used or null
	 */
	public static AlertDialog showAlert(Activity act, Context applicationContext, String message){

		// force the application context
		Context ctx = null;
		if (applicationContext != null)
			ctx = applicationContext.getApplicationContext();
		if (ctx == null && act != null)
			ctx = act.getApplicationContext();

		AlertDialog dialog = null;

		try{
			// first the dialog if we can
			if (act != null && !act.isFinishing()){
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(act);

				dlgAlert.setMessage(message);
				dlgAlert.setCancelable(true);
				dlgAlert.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//dismiss the dialog  
					}
				});
				dialog = dlgAlert.create();
				dialog.show();
			}else{

				// the new window if we must
				Intent intent = new Intent(ctx, com.tools.MessageDialog.class);
				intent.putExtra(com.tools.MessageDialog.TITLE_BUNDLE, "Message");
				intent.putExtra(com.tools.MessageDialog.DEFAULT_TEXT, message);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ctx.startActivity(intent);
			}
		}catch(Exception e){
			Log.e(LOG_TAG, Log.getStackTraceString(e));

			// failsafe toast
			if (ctx != null){
				try{
					Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
				}catch(Exception e2){
					Log.e(LOG_TAG, Log.getStackTraceString(e2));
				}
			}
		}

		return dialog;
	}
	
	/**
	 * Copy file. Will create folders if needed
	 * @param sourceFile the source file 
	 * @param destFile the destination file
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		// create required folders
		writeRequiredFolders(destFile.getAbsolutePath());
		
		// make the new file
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;
	    try {
	    	
	    	// open file streams
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();

	        // previous code: destination.transferFrom(source, 0, source.size());
	        // to avoid infinite loops, should be:
	        long count = 0;
	        long size = source.size();              
	        while((count += destination.transferFrom(source, count, size-count))<size);
	    }
	    finally {
	    	
	    	// clean up
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	/**
	 * Attempt to find the path to the external ad card. Will return null if none found.
	 * Not guaranteed to find the path or that the returned path is indeed the external sd_card, as 
	 * there is currently no public api for such a command
	 * @return The external sd card
	 */
	public static File getExternalSdCardPath(){
		// strings to look for
		String[] keywords = {"external_sd", "external", "extern_sd"};//, "sd"};
		
		// base path
		File topPath = Environment.getExternalStorageDirectory();
		
		// null
		if (topPath == null || topPath.getAbsolutePath().length() == 0)
			return null;

		// grab list of files looking for external
		File[] files = topPath.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			
			// check over various keywords
			for (String keyword : keywords){
				// loop over all files
				for(File f: files) {
					if (f.isDirectory() && f.getName().contains(keyword)){
						return f;
					}
				}
			}
		}
		
		// if we made it here, there must have been no matching file
		return null;
	}
	
	/**
	 * Attempt to determine to mirrored path from the input file path to the external sd_card.
	 * Will return null if none found.
	 * Not guaranteed to find the path or that the returned path is indeed the external sd_card, as 
	 * there is currently no public api for such a command </p>
	 * For example </p>
	 * getMirroredExternalPath("/mnt/sdcard/yourFolder/yourFile.jpg")
	 * could return "/mnt/sdcard/external_sd/yourFolder/yourFile.jpg"
	 * @param originalPath the path we are starting with and we are goign to mirrow
	 * @return The external sd card path
	 */
	public static String getMirroredExternalPath(String originalPath){
		
		// base path
		File topPath = Environment.getExternalStorageDirectory();
		
		// the external path
		File external = getExternalSdCardPath();
		
		// null
		if (external == null || topPath == null ||
				topPath.getAbsolutePath().length() == 0 ||
				external.getAbsolutePath().length() == 0)
			return null;
		
		// replace the string
		String output = new String(originalPath);
		if (output.contains(topPath.getAbsolutePath()) && !output.contains(external.getAbsolutePath()))
			output = output.replace(topPath.getAbsolutePath(), external.getAbsolutePath());
		else
			output = null;

		return output;
	}
	
	/**
	 * @param path the path we wish to analyze
	 * @return Number of bytes available at given location
	 */
	public static long getAvailableSpace(String path) {
	    long availableSpace = -1L;
	    StatFs stat = new StatFs(path);
	    availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

	    return availableSpace;
	}
	
	/**
	 * Convert units of dp (device independent pixels) to normal pixels based on the screen
	 * @param ctx Required context to convert
	 * @param dp the number of dp
	 * @return the pixels
	 */
	public static float convertDpToPix(Context ctx, int dp){
		Resources r = ctx.getResources();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
	
	/**
	 * Convert units of pixles to dp (device independent pixels) based on the screen
	 * @param act required activity to convert
	 * @param pix the number of pixels
	 * @return the dp
	 */
	public static float convertPixToDp(Activity act, int dp){
		DisplayMetrics metrics = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float logicalDensity = metrics.density;
		return dp * logicalDensity;
	}
	
	/**
	 * Set a grid view to have no spacing between items and fully fill desired width. Also make selector on top. Otherwise we couldn't see
	 * the selection as it would be behind item.
	 * @param act the activity this is called in
	 * @param desiredGridItemWidth The desired width of the grid item
	 * @param gridView The gridview to act on
	 * @param desiredGridViewWidth The desired width of the full gridview (if useFullScreen is true, this is ignored)
	 * @param useFullScreen Should we fill the whole screen or use desiredGridViewWidth
	 * @return the size of the grid item
	 */
	public static int setGridViewColsBasedOnScreen(
			Activity act,
			int desiredGridItemWidth,
			GridView gridView,
			int desiredGridViewWidth,
			boolean useFullScreen){
		
		// the grid item width in pixels
		float desiredPicWidthPx = com.tools.Tools.convertDpToPix(act, desiredGridItemWidth); // in px
		
		// the screen width in pixels
		if (useFullScreen){
			Display display = act.getWindowManager().getDefaultDisplay(); 
			desiredGridViewWidth = display.getWidth();
		}
		
		// how many items can we fit
		int nPics = Math.round(desiredGridViewWidth/desiredPicWidthPx);
		if (nPics <= 0) // make sure at least 1 pictures
			nPics = 1;
		
		// actual width 
		int actualPicWidth = (int) Math.floor(desiredGridViewWidth/nPics);
		
		// set the width and number of items.
		gridView.setColumnWidth(actualPicWidth);
		gridView.setNumColumns(nPics);
		gridView.setDrawSelectorOnTop(true);
		
		return actualPicWidth;
	}
	
	/**
	 * Get the time required to read for a toast message, based on number of words. Maxes out at 10 seconds
	 * @param message The message to parse
	 * @return Time in milliseconds to show toast
	 */
	public static long getTimeToReadForToast(String message){
		// some constants for timeing
		final double wordsPerSecond = 3.3;
		final double maxTime = 10;
		final double initialTime = 1.5;
		
		// count the words
		String trim = message.trim();
		if (trim.isEmpty()) return 0;
		int nWords = trim.split("\\s+").length; //separate string around spaces
		
		// convert to time
		double time = nWords/wordsPerSecond + initialTime;
		if (time > maxTime)
			time = maxTime;
		
		return (long) (time*1000);
	}
	
	/**
	 * Return the device manufacturer and model
	 * @return
	 */
	public static String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	/**
	 * Captialize the given string
	 * @param s
	 * @return
	 */
	public static String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	} 
	
	/**
	 * Some devices, the camera must be reset after changing flash mode 
	 * for it to stick. This will return true if we are on one of those devices.
	 * @return
	 */
	public static boolean isDeviceThatRequiresNewCameraOnFlashChange(){
		HashSet<String> badDevices = new HashSet<String>(1);
		badDevices.add("Samsung SGH-T769");
		
		String thisDevice = getDeviceName();
		if (badDevices.contains(thisDevice))
			return true;
		else
			return false;
	}
	
	/**
	 * Calculate md5 checksum of a file
	 * @param fileName the name of the file
	 * @return the checksum
	 * @throws IOException
	 * @throws EncryptionException 
	 */
	public static byte[] md5CheckSumFile(String fileName) throws IOException, EncryptionException{
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}
		InputStream is = new FileInputStream(fileName);
		try {
			is = new DigestInputStream(is, md);
			// read stream to EOF as normal...
		}
		finally {
			is.close();
		}
		byte[] digest = md.digest();
		return digest;
	}
	
	/**
	 * Calculate the md5 checksum of two files and compare them
	 * @param fileName1 first file
	 * @param fileName2 second file
	 * @return true if their md5 checksums are equal
	 * @throws IOException 
	 * @throws EncryptionException 
	 */
	public static boolean isFileEqual(String fileName1, String fileName2) throws IOException, EncryptionException{
		byte[] a = md5CheckSumFile(fileName1);
		byte[] b = md5CheckSumFile(fileName2);
		return Arrays.equals(a, b);
	}
	
	/**
	 * takes a name and returns an allowable file/directory name. It replaces all non acceptable characters
	 * with _. ie file:name returns file_name
	 * @param name input name
	 * @return The acceptible file name
	 */
	public static String getAllowableFileName(String name){

		String out = name;
		for (int i = 0; i < ILLEGAL_CHARACTERS.length; i++)
			out = out.replace(ILLEGAL_CHARACTERS[i], '_');

		return out.trim();
	}
	

	/**
	 * Make the string plural if number is != 1. For example if number is 2, and noun is cat, then "2 cats" will be the output
	 * @param number the number
	 * @param noun the noun to convert
	 * @return The number with the noun
	 */
	public static String pluralString(double number, String noun){
		String out;
		if (number== 1)
			out = number+" "+ noun;
		else
			out = number+" "+ noun+"s";
		return out;
	}
}