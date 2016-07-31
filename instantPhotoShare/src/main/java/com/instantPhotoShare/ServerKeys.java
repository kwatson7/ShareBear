package com.instantPhotoShare;

import org.json.JSONException;

import android.util.Log;

/**
 * Nested classes holding keys for server commands
 */
public class ServerKeys {

	/**
	 * The user id making the request
	 */
	public static final String POST_KEY_USER_ID = "user_id";
	
	/**
	 * The secret code of the user making the request
	 */
	public static final String POST_KEY_SECRET_CODE = "secret_code";
	
	public static class GetThumbnails extends ServerKeys{
		public static final String COMMAND = "get_thumbnails";
		public static final String POST_KEY_THUMBNAIL_IDS = "thumbnail_ids";
		public static final String POST_KEY_GROUP_ID = "group_id";
		
		public static final String RETURN_KEY_THUMBNAIL_DATA = "thumbnail_data";
		public static final String RETURN_KEY_OWNER_ID = "owner_id";
		public static final String RETURN_KEY_DATE_UPLOADED = "date_uploaded";	
		public static final String RETURN_KEY_THUMBNAIL_ID = "thumbnail_id";
	}
	
	public static class AddUsersToGroup extends ServerKeys{
		public static final String COMMAND = "add_user_to_group";
		public static final String POST_KEY_PERSON_F_NAME = "person_fname";
		public static final String POST_KEY_PERSON_L_NAME = "person_lname";
		public static final String POST_KEY_PERSON_EMAIL = "person_email";
		public static final String POST_KEY_PHONE_NUMBER = "phone_number";
		public static final String POST_KEY_GROUP_ID = "group_id";
		
		public static final String RETURN_KEY_CODE = "user_message_code";
		public static final String RETURN_USER_ID = "user_id";
	}
	
	public static class HasValidatedEmail extends ServerKeys{
		public static final String COMMAND = "has_validated_email";
	}
	
	public static class GetGroupImageIds extends ServerKeys{
		public static final String COMMAND = "get_image_ids";
		public static final String POST_KEY_GROUP_ID = "group_id";
	}
	
	public static class GetGroups extends ServerKeys{
		public static final String COMMAND = "get_groups";
		
		public static final String RETURN_KEY_DATE_CREATED = "date_created";
		public static final String RETURN_KEY_OWNER_ID = "owner_id";
		public static final String RETURN_KEY_NAME = "name";
		public static final String RETURN_KEY_GROUP_ID = "group_id";
		public static final String RETURN_KEY_USER_COUNT = "user_count";
		public static final String RETURN_KEY_PHOTO_COUNT = "photo_count";
	}
	
	public static class GetFullSize extends ServerKeys{
		public static final String COMMAND = "get_fullsize";
		public static final String POST_KEY_IMAGE_ID = "image_id";
		public static final String POST_KEY_GROUP_ID = "group_id";
	}
	
	public static class CopyImage extends ServerKeys{
		public static final String COMMAND = "copy_image";
		public static final String POST_KEY_IMAGE_ID = "image_id";
		public static final String POST_KEY_SOURCE_GROUP_ID = "source_group_id";
		public static final String POST_KEY_NEW_GROUP_ID = "new_group_id";
	}
	
	public static class DeleteImage extends ServerKeys{
		public static final String COMMAND = "delete_image";
		public static final String POST_KEY_IMAGE_ID = "image_id";
	}
	
	public static class GetUsers extends ServerKeys{
		public static final String COMMAND = "get_users";
		public static final String POST_KEY_USER_IDS_ARRAY = "user_ids";
		
		public static final String RETURN_KEY_FIRST_NAME = "first_name";
		public static final String RETURN_KEY_LAST_NAME = "last_name";
	}
	
	public static class CreateGroup extends ServerKeys{
		public static final String COMMAND = "create_group";
		public static final String POST_KEY_GROUP_NAME = "group_name";
		
		public static final String RETURN_KEY_GROUP_SERVER_ID = "group_id";
	}
	
	public static class CreateNewAccount extends ServerKeys{
		// codes to be sent to server
		public static final String COMMAND = "create_user";

		// field that Person class needs to access to know what data we need to send to server
		public static final String POST_KEY_PERSON_FIRST_NAME = "person_fname";
		public static final String POST_KEY_PERSON_LAST_NAME = "person_lname";
		public static final String POST_KEY_PERSON_PHONE = "phone_number";
		public static final String POST_KEY_PERSON_EMAIL = "person_email";
		public static final String POST_KEY_USER_NAME = "user_name";
		public static final String POST_KEY_PASSWORD = "password";
		
		public static final String RETURN_KEY_UNIQUE_KEY = "secret_code";
		public static final String RETURN_KEY_USER_ID = "user_id";
	}
	
	public static class Login extends ServerKeys{
		public static final String COMMAND = "user_login";
		
		public static final String RETURN_KEY_UNIQUE_KEY = "secret_code";
		public static final String RETURN_KEY_USER_ID = "user_id";
	}
	
	public static class SavePicture extends ServerKeys{
		public static final String COMMAND = "upload_image";
		public static final String POST_KEY_GROUP_IDS_ARRAY = "group_id";
	}
}
