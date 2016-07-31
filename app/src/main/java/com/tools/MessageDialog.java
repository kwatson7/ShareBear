package com.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

/**
 * A simple activity that emulates a message dialog <br>
 * See the public constants for different input and output keys that can be set/read from intents
 * @author Kyle
 */
public class MessageDialog
extends Activity {
	
	public static final String TITLE_BUNDLE = "title";
	public static final String DEFAULT_TEXT = "defaultText";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		setContentView(R.layout.transparant);

		// initialize variables
		String title=null;
		String defaultText=null;

		// grab extras
		Bundle extras = getIntent().getExtras();
		if (extras!=null){
			title = extras.getString(TITLE_BUNDLE);
			defaultText = extras.getString(DEFAULT_TEXT);	
		}
		
		// create dialog and edit text
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		
		// set title
		if (title != null){alert.setTitle(title);}
		
		// set default text
		if (defaultText !=null){alert.setMessage(defaultText);}
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
                finish();
			}
		});
		alert.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface arg0) {
				finish();
				
			}
		});
		alert.show();

	}
	
	@Override
	public void onPause(){
		overridePendingTransition(0, R.anim.picture_scale_down_animation);
		super.onPause();
	}

	@Override
	public void onResume(){
		overridePendingTransition(R.anim.picture_scale_up_animation, 0);
		super.onResume();
	}
}