package com.tools;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AppRater {
    
	// settings
    private final static int DAYS_UNTIL_PROMPT = 3;
    private final static int LAUNCHES_UNTIL_PROMPT = 5;
    
    // keys
    private static final String DATE_FIRST_LAUNCH = "AppRater.DATE_FIRST_LAUNCH";
    private static final String LAUNCH_COUNT = "AppRater.LAUNCH_COUNT";
    private static final String DONT_SHOW_AGAIN = "AppRater.DONT_SHOW_AGAIN";
    
    public static void app_launched(
    		Context mContext,
    		final String appName,
    		final String packageName) {
        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        if (prefs.getBoolean(DONT_SHOW_AGAIN, false)) { return ; }
        
        SharedPreferences.Editor editor = prefs.edit();
        
        // Increment launch counter
        long launch_count = prefs.getLong(LAUNCH_COUNT, 0) + 1;
        editor.putLong(LAUNCH_COUNT, launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong(DATE_FIRST_LAUNCH, 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(DATE_FIRST_LAUNCH, date_firstLaunch);
        }
        
        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + 
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, editor, appName, packageName);
            }
        }
        
        editor.commit();
    }   
    
    private static void showRateDialog(
    		final Context mContext,
    		final SharedPreferences.Editor editor,
    		final String appName,
    		final String packageName) {
        final Dialog dialog = new Dialog(mContext);
        dialog.setTitle("Rate " + appName);

        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        
        TextView tv = new TextView(mContext);
        tv.setText("If you enjoy using " + appName + ", please take a moment to rate it. Thanks for your support!");
        tv.setWidth(240);
        tv.setPadding(4, 0, 4, 10);
        ll.addView(tv);
        
        Button b1 = new Button(mContext);
        b1.setText("Rate " + appName);
        b1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	editor.putBoolean(DONT_SHOW_AGAIN, true);
                editor.commit();
            	try {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
            	} catch(ActivityNotFoundException e){
            		Toast.makeText(mContext, "No Market App on phone.", Toast.LENGTH_LONG).show();
            	}
                dialog.dismiss();
            }
        });        
        ll.addView(b1);

        Button b2 = new Button(mContext);
        b2.setText("Remind me later");
        b2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ll.addView(b2);

        Button b3 = new Button(mContext);
        b3.setText("No, thanks");
        b3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (editor != null) {
                    editor.putBoolean(DONT_SHOW_AGAIN, true);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });
        ll.addView(b3);

        dialog.setContentView(ll);        
        dialog.show();        
    }
}
// see http://www.androidsnippets.com/prompt-engaged-users-to-rate-your-app-in-the-android-market-appirater