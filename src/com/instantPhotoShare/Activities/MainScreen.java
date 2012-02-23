/**
 * 
 */
package com.instantPhotoShare.Activities;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.tools.CustomActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

/**
 * @author Kyle
 * Main screen user sees when launching app
 */
public class MainScreen
extends CustomActivity{
	
	// pointers to graphics objects
	private ImageView createNewGroup;
	
	// enums for menu items
	private enum MENU_ITEMS { 										
		CLEAR_APP_DATA;
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		initializeLayout();	
	}
	
	/**
     * initialize the layout and grab pointers for widgets
     */
	@Override
	protected void initializeLayout() {
		// set the content view
    	setContentView(R.layout.main_screen);

    	// grab pointers for graphics objects
    	createNewGroup = (ImageView) findViewById(R.id.createNewGroupButton);
		
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
		
	}

	

	@Override
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDestroyOverride() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		// clear app data
		menu.add(0,MENU_ITEMS.CLEAR_APP_DATA.ordinal(), 0, "Clear app data");
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		// convert to menu
		MENU_ITEMS val = MENU_ITEMS.convert(item.getItemId());
		// decide on what each button should od
		switch(val) {
		case CLEAR_APP_DATA:
			Utils.clearApplicationData(this);
			break;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	public void createNewGroupClicked(View v){
		Intent intent = new Intent(this, CreateGroup.class);
		startActivity(intent);
	}
}
