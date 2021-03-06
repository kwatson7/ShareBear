package com.instantPhotoShare.Adapters;


import java.util.ArrayList;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper
extends SQLiteOpenHelper{

	// database variables
	private static final String DATABASE_NAME = "data.db";
	public static final int DATABASE_VERSION = 13;
	private int oldVersion = -1;

	// instance of database
	private static DatabaseHelper instance;
	
	/**
	 * Get singleton instance of database
	 * @param context context used to get database (application context will always be used)
	 */
	public static synchronized DatabaseHelper getHelper(Context context)
	throws IllegalArgumentException{
		// check null context
		if (context == null)
			throw new IllegalArgumentException("Context into getHelper cannot be null");
		
		// grab application context
		Context context2 = context.getApplicationContext();
		if (context2 == null)
			throw new IllegalArgumentException("Application context into getHelper cannot be null");
		
		// grab new instance if need be
		if (instance == null)
			instance = new DatabaseHelper(context2);
		
		return instance;
	}

	/**
	 * Constructor
	 * @param context
	 */
	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL(PicturesAdapter.TABLE_CREATE);
		db.execSQL(GroupsAdapter.TABLE_CREATE);
		db.execSQL(UsersAdapter.TABLE_CREATE);
		db.execSQL(CommentsAdapter.TABLE_CREATE);
		db.execSQL(PicturesInGroupsAdapter.TABLE_CREATE);
		db.execSQL(UsersInGroupsAdapter.TABLE_CREATE);
		db.execSQL(NotificationsAdapter.TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		// grab upgrades for various tables
		ArrayList<String> picturesUpgrades = PicturesAdapter.upgradeStrings(oldVersion, newVersion);
		for (String item : picturesUpgrades)
			db.execSQL(item);
		ArrayList<String> groupsUpgrades = GroupsAdapter.upgradeStrings(oldVersion, newVersion);
		for (String item : groupsUpgrades)
			db.execSQL(item);
		ArrayList<String> usersUpgrades = UsersAdapter.upgradeStrings(oldVersion, newVersion);
		for (String item : usersUpgrades)
			db.execSQL(item);
		ArrayList<String> notificationUpgrades = NotificationsAdapter.upgradeStrings(oldVersion, newVersion);
		for (String item : notificationUpgrades)
			db.execSQL(item);
		ArrayList<String> usersInGroupsUpgrades = UsersInGroupsAdapter.upgradeStrings(oldVersion, newVersion);
		for (String item : usersInGroupsUpgrades)
			db.execSQL(item);
		
		this.oldVersion = oldVersion;
	}
	
	/**
	 * Return the old database version. Will be -1 if onUpgrade was not called. open() must first have been called as well
	 */
	public int getOldVersion(){
		return oldVersion;
	}
}
