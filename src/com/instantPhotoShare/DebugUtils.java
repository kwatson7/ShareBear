package com.instantPhotoShare;

import android.content.Context;

import com.instantPhotoShare.Adapters.UsersAdapter;

public class DebugUtils {

	/**
	 * Delete the users with no first name or last name or serverId (must have at least one to not be deleted) <br>
	 * Also deletes links in usersGroups database
	 */
	public static void deleteUsersWithNoNameAndNoServerId(Context ctx){
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchAllUsersCursor();
		while (users.moveToNext()){
			if ((users.getName() == null || users.getName().length() == 0) && (users.getServerId() == -1 || users.getServerId() == 0)){
				int connections = users.deleteUser(ctx, users.getRowId());
			}			
		}
	}
}
