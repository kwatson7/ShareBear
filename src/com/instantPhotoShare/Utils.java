package com.instantPhotoShare;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

public class Utils {

	// public constants
	public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public static void clearApplicationData(Context ctx) {
		File cache = ctx.getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
				}
			}
		}
	}
	
	private static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}
	
	public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
	
	/**
	 * Get the current date and time formatted as "yyyy-MM-dd HH:mm:ss"
	 * @return The time date string
	 */
	public static String getNowTime(){
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT); 
		Date date = new Date();
		return dateFormat.format(date);
	}
}
