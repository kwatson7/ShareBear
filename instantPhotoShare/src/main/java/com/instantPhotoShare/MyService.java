package com.instantPhotoShare;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class MyService extends Service
{
	Alarm alarm = new Alarm();

	@Override
	public void onCreate()
	{
		super.onCreate();       
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(Prefs.isLaunchBackgroundOnStart(this)){
			alarm.startAlarm(this);
		}else{
			alarm.cancelAlarm(this);
			stopSelf();
		}
		
		return START_STICKY;

	}

	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}

	@Override
	public void onDestroy(){
		alarm.cancelAlarm(this);
		super.onDestroy();
	}
}