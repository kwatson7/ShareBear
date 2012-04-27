package com.instantPhotoShare;

import android.content.Context;

import com.instantPhotoShare.Adapters.UsersAdapter;
import com.instantPhotoShare.Adapters.UsersInGroupsAdapter;

public class DebugUtils {

	/**
	 * Delete the users with no first name or last name or serverId (must have at least one to not be deleted) <br>
	 * Also deletes links in usersGroups database
	 */
	public static void deleteUsersWithNoNameAndNoServerId(Context ctx){
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchAllUsers();
		while (users.moveToNext()){
			if ((users.getName() == null || users.getName().length() == 0) && (users.getServerId() == -1 || users.getServerId() == 0)){
				users.deleteUserForDebug(ctx, users.getRowId());
			}			
		}
		users.close();
	}
	
	/**
	 * Delete all users that dont' have a server id and also delete all users - group links
	 * @param ctx
	 */
	public static void deleteAllNonServerUsersAndGroupLinks(Context ctx){
		UsersAdapter users = new UsersAdapter(ctx);
		users.fetchAllUsers();
		while (users.moveToNext()){
			if (users.getServerId() == -1 || users.getServerId() == 0){
				users.deleteUserForDebug(ctx, users.getRowId());
			}			
		}
		users.close();
		
		UsersInGroupsAdapter links = new UsersInGroupsAdapter(ctx);
		links.deleteAllLinksDebug();
		
	}
}
