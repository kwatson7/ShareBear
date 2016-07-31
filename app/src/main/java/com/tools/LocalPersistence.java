package com.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.content.Context;

/**
 * Writes/reads an object to/from a private local file
 */
public class LocalPersistence {


	/**
	 * @param context Context required to write the object
	 * @param object The object to write
	 * @param filename the filename to write to
	 * @throws IOException 
	 */
	public static void witeObjectToFile(Context context, Object object, String filename)
	throws IOException {

		ObjectOutputStream objectOut = null;
		try {

			// create output stream
			FileOutputStream fileOut = context.openFileOutput(filename, Activity.MODE_PRIVATE);
			objectOut = new ObjectOutputStream(fileOut);

			// write the object
			objectOut.writeObject(object);
			fileOut.getFD().sync();

		} finally {
			if (objectOut != null)
				objectOut.close();
		}
	}


	/**
	 * @param context Context required to read
	 * @param filename the filename to read from
	 * @return the object
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static Object readObjectFromFile(Context context, String filename)
	throws IOException, ClassNotFoundException {

		ObjectInputStream objectIn = null;
		Object object = null;
		try {

			// create input stream
			FileInputStream fileIn = context.getApplicationContext().openFileInput(filename);
			objectIn = new ObjectInputStream(fileIn);

			// read it
			object = objectIn.readObject();

		} finally {
			if (objectIn != null) 
				objectIn.close();
		}
		return object;
	}
}