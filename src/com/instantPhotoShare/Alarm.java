package com.instantPhotoShare;

import com.instantPhotoShare.Activities.InitialLaunch;
import com.instantPhotoShare.Activities.MainScreen;
import com.instantPhotoShare.Activities.NotificationsScreen;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.NotificationsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.ItemsFetchedCallback;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.CustomAsyncTask.FinishedCallback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class Alarm extends BroadcastReceiver 
{    
	// constants
	private static final float POLLING_MINUTES = 10f;

	// members
	private PowerManager.WakeLock wakeLock = null;
	private Context ctx;

	@Override
	public void onReceive(Context context, Intent intent) 
	{   
		ctx = context;

		// wake the cpu
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Utils.LOG_TAG);
		wakeLock.acquire();

		try{
			if (InitialLaunch.isUserAccountInfoAvailable(ctx) && Prefs.isLaunchBackgroundOnStart(ctx)){
				GroupsAdapter groups = new GroupsAdapter(ctx);
				if(!groups.fetchAllGroupsFromServer(null, null, groupsFetchedCallback)){
					wakeLock.release();
					wakeLock = null;
				}
			}else{
				wakeLock.release();
				wakeLock = null;
			}
		}catch (Exception e){
			// realase the cpu
			wakeLock.release();
			wakeLock = null;
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
	}

	/**
	 * This will be called when groups are done being fetched. Post a notification to the notification bar
	 */
	private ItemsFetchedCallback<CustomActivity> groupsFetchedCallback =  new ItemsFetchedCallback<CustomActivity>() {

		@Override
		public void onItemsFetchedUiThread(CustomActivity act, String errorCode) {
		}

		@Override
		public void onItemsFetchedBackground(
				CustomActivity act,
				int nNewItems,
				String errorCode) {

			// error
			if (errorCode != null){
				Log.e(Utils.LOG_TAG, errorCode);
				wakeLock.release();
				wakeLock = null;
				return;
			}

			// check if there are new notifications
			try{
				setNotificationsNumber();
			}catch(Exception e){
				// realase the cpu
				wakeLock.release();
				wakeLock = null;
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}
	};

	/**
	 * Set the number of new notifications
	 */
	private void setNotificationsNumber(){
		// get the number of unread notificiations
		NotificationsAdapter notes = new NotificationsAdapter(ctx);
		notes.getNumberNewNotifications(null, getNumberNewNotificationsCallback);
	}

	private CustomAsyncTask.FinishedCallback<CustomActivity, Integer> getNumberNewNotificationsCallback = new FinishedCallback<CustomActivity, Integer>() {

		@Override
		public void onFinish(CustomActivity activity, Integer result) {
			if (result == null){
				wakeLock.release();
				wakeLock = null;
				return;
			}			

			// set the notification here
			try{
				int unreadNotifications = (java.lang.Integer) result;
				if (unreadNotifications > 0){
					String str;
					if (unreadNotifications == 1)
						str = " notification.";
					else
						str = " notifications.";

					Intent intent = new Intent(ctx, NotificationsScreen.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					com.tools.Tools.postNotification(
							ctx,
							R.drawable.icon3,
							"Share Bear Notifications!",
							"New Share Bear Pictures",
							"You have " + unreadNotifications + str,
							Utils.notificationsIds.GENERIC_NEW_NOTIFICATIONS.ordinal(),
							intent);
				}
			}finally{
				wakeLock.release();
				wakeLock = null;
			}
		}
	};

	/**
	 * Set an alarm to go off every x minutes
	 * @param context context required to set the alarm
	 */
	public void startAlarm(Context context)
	{
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent("com.instantPhotoShare.myAction");
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (long) (1000 * 60 * POLLING_MINUTES), pi);
	}

	/**
	 * Cancel this alarm
	 * @param context The context required to cancel the alarm
	 */
	public void cancelAlarm(Context context)
	{
		Intent intent = new Intent("com.instantPhotoShare.myAction");
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}