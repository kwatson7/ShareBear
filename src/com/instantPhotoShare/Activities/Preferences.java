package com.instantPhotoShare.Activities;

import com.instantPhotoShare.MyService;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {        
		super.onCreate(savedInstanceState);        
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Prefs.PREF_FILE);
		prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onPause(){
		super.onPause();
		Intent intent = new Intent(this, MyService.class);
		startService(intent);
	}
}
