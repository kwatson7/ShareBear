package com.instantPhotoShare.Activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.tools.CustomActivity;

public class NotificationsScreen
extends CustomActivity{

	// class members
	private NotificationsAdapter notificationsAdapter = null;
	private ListViewAdapter listViewAdapter = null;
	
	// graphics
	private ListView listView = null;
	private ImageView noNotes = null;
	
	// menu items
	private enum MENU_ITEMS {
		DELETE_ALL_NOTIFICATIONS, MARK_ALL_AS_READ;
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}
	
	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		// initialize the layout first
		initializeLayout();
	}
	
	/**
	 * Callback used when we are done fetching the notes
	 */
	private static class NotesFetched 
	implements com.tools.CustomAsyncTask.FinishedCallback<NotificationsScreen, NotificationsAdapter>{

		@Override
		public void onFinish(
				NotificationsScreen activity,
				NotificationsAdapter result) {
			activity.loadNotifications(result);
		}
	}
	
	/**
	 * Load the notifications to the list
	 * @param notes The new notifications adapter
	 */
	private void loadNotifications(NotificationsAdapter notes){
		// close the old one
		if (notificationsAdapter != null)
			notificationsAdapter.close();
		
		// store the new one
		notificationsAdapter = notes;
		notificationsAdapter.startManagingCursor(this);
		if(notificationsAdapter.size() == 0)
			noNotes.setVisibility(View.VISIBLE);
		else
			noNotes.setVisibility(View.GONE);
		
		// load the adapter
		listViewAdapter = new ListViewAdapter(this, notificationsAdapter);
		
		// set adapter to listView
		listView.setAdapter(listViewAdapter);
		listView.setOnItemClickListener(listViewAdapter);
		listViewAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// load the notifications
		NotificationsAdapter notes = new NotificationsAdapter(ctx);
		notes.fetchAllNotificationsInBackground(this, new NotesFetched());
		
		// remove from notification bar
		((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE))
			.cancel(Utils.notificationsIds.GENERIC_NEW_NOTIFICATIONS.ordinal());
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if (notificationsAdapter != null)
			notificationsAdapter.setAllIsNew(false);
	}

	@Override
	protected void initializeLayout() {

		// load content
		setContentView(R.layout.notifications_screen);
		
		// graphics
		listView = (ListView) findViewById(R.id.listView);	
		noNotes = (ImageView) findViewById(R.id.noNotifications);
	}

	@Override
	protected void additionalConfigurationStoring() {
		
	}

	@Override
	protected void onDestroyOverride() {
		// close the adapter
		if (notificationsAdapter != null)
			notificationsAdapter.close();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem delete = menu.add(0,MENU_ITEMS.DELETE_ALL_NOTIFICATIONS.ordinal(), 0, "Delete All");
	//	MenuItem markAsRead = menu.add(0, MENU_ITEMS.MARK_ALL_AS_READ.ordinal(), 0, "Mark All as Read");

		// add icons
		delete.setIcon(R.drawable.delete);
	//	markAsRead.setIcon(R.drawable.check_box);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS call = MENU_ITEMS.convert(item.getItemId());
		
		// decide on what each button should od
		switch(call) {
		case DELETE_ALL_NOTIFICATIONS:
			notificationsAdapter.deleteAll();
			(new NotificationsAdapter(ctx)).fetchAllNotificationsInBackground(this, new NotesFetched());
			return true;
		case MARK_ALL_AS_READ:
			notificationsAdapter.setAllIsNew(false);
			(new NotificationsAdapter(ctx)).fetchAllNotificationsInBackground(this, new NotesFetched());
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	/**
	 * The list view displayer
	 */
	private static class ListViewAdapter
	extends BaseAdapter
	implements android.widget.AdapterView.OnItemClickListener{

		private NotificationsAdapter notificationsAdapter;
		private LayoutInflater inflater = null;
		private Activity act;

		public ListViewAdapter(Activity a, NotificationsAdapter notificationsAdapter) {
			act = a;
			this.notificationsAdapter = notificationsAdapter;
			inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
		}

		public int getCount() {
			return notificationsAdapter.size();
		}

		public Object getItem(int position) {
			if(notificationsAdapter.moveToPosition(position))
				return notificationsAdapter;
			else
				return null;
		}

		public long getItemId(int position) {
			if(notificationsAdapter.moveToPosition(position))
				return notificationsAdapter.getRowId();
			else
				return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			// inflate a new view if we have to
			View vi=convertView;
			if(convertView==null){
				vi = inflater.inflate(R.layout.notifications_item, null);
			}

			// grab the items to display
			TextView mainText = (TextView) vi.findViewById(android.R.id.text1);
			TextView dateText = (TextView) vi.findViewById(android.R.id.text2);

			// move to correct location and fill views
			if (notificationsAdapter.moveToPosition(position)){
				
				// set main text
				mainText.setText(notificationsAdapter.getMessage());
				
				// set color based on new or not
				if (notificationsAdapter.isNew())
					vi.setBackgroundResource(R.drawable.blue_background_default_selector);
				else
					vi.setBackgroundResource(R.drawable.gray_background_default_selector);
				
				// set date
				dateText.setText(notificationsAdapter.getTimeAgoNotification() + " ago");
			}

			// return the view
			return vi;
		}

		@Override
		public void onItemClick(
				AdapterView<?> adapter,
				View view,
				int position,
				long id) {
			// load a helper adapter
			NotificationsAdapter notes = new NotificationsAdapter(act);
			notes.fetchNotification(id);
			
			// get the type of notification
			NotificationsAdapter.NOTIFICATION_TYPES type = notes.getType();
			
			// some default values
			long group = -1;
			Intent intent = null;
			
			// update that this is no longer a new notification
			if (notes.isNew())
				notes.setIsNew(false);
			
			// switch over the different types
			switch (type){
			case NEW_PICTURE_IN_GROUP:
				group = notes.getHelperLong();
				if (group == -1){
					Utils.showCustomToast(act, "No group error!", true, 1);
					break;
				}
				
				// load the intent for this groups gallery
				intent = new Intent(act, InsideGroupGallery.class);
				intent.putExtra(
						InsideGroupGallery.GROUP_ID,
						group);

				// load the activity
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				act.startActivity(intent);	
				break;
				
			case ADD_TO_NEW_GROUP:
				group = notes.getHelperLong();
				if (group != -1){
					// load the intent for this groups gallery
					intent = new Intent(act, InsideGroupGallery.class);
					intent.putExtra(
							InsideGroupGallery.GROUP_ID,
							group);

					// load the activity
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					act.startActivity(intent);
				}else{
					intent = new Intent(act, GroupGallery.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					act.startActivity(intent);
				}
				break;
			case SERVER_ERROR:
				
				// the picture this error references will be shown in whatever group it's in
				long picId = notes.getHelperLong();
				if (picId > -1){
					// find a group this pic is in
					GroupsAdapter groups = new GroupsAdapter(act);
					groups.fetchGroupsContainPicture(picId);
					long groupId = -1;
					while (groupId == -1 && groups.moveToNext()){
						if(groups.isAccessibleGroup())
							groupId = groups.getRowId();
					}			
					groups.close();

					// no group, do nothing
					if (groupId == -1){
						break;
					}

					// find position in cursor
					PicturesAdapter helper = new PicturesAdapter(act);
					helper.fetchPicturesInGroup(groupId);
					int positionForNewAdapter = -1;
					while (helper.moveToNext()){
						if (helper.getRowId() == picId){
							positionForNewAdapter = helper.getPosition();
							break;
						}
					}
					helper.close();

					// couldn't find picture, do nothing
					if (positionForNewAdapter == -1){
						break;
					}

					// load the intent for this groups gallery
					intent = new Intent(act, SinglePictureGallery.class);
					intent.putExtra(
							SinglePictureGallery.GROUP_ID,
							groupId);
					intent.putExtra(
							SinglePictureGallery.PICTURE_POSITION,
							positionForNewAdapter);

					// load the activity
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					act.startActivity(intent);	
				}
			}
		}
	}
}
