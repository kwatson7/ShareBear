package com.instantPhotoShare.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.instantPhotoShare.ImageLoader;
import com.instantPhotoShare.MemoryCache;
import com.instantPhotoShare.Prefs;
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.tools.CustomActivity;

public class InsideGroupGallery 
extends CustomActivity{

	// graphcis
	private GridView gridView; 					// the gridview to show pictures
	private ImageView screen; 					// The background screen image
	private ImageView takePictureButton; 		// the pointer to the take picture button
	private TextView groupNameText; 			// Pointer to textView showing the group name
	
	// misc private variables
    private PicturesGridAdapter adapter; 		// the adapter to show pictures
    private PicturesAdapter picturesAdapater;	// An array of all the pictures
    private long groupId; 						// the groupId that we are currently showing
    private CustomActivity act = this; 			// This activity
    private Drawable backgroundDrawable = null;
    private String groupName; 					// The name of the group we are in
    
    // variables to indicate what can be passed in through intents
    public static String GROUP_ID = "GROUP_ID";
    
	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		
		// load in passed info
        Bundle extras = getIntent().getExtras(); 
        if (extras == null)
        	throw new IllegalArgumentException("InsideGroupGallery cannot be called without a groupId passed in");

        // the group id passed
        groupId = extras.getLong(GROUP_ID, -1);
        if (groupId == -1)
        	throw new IllegalArgumentException("InsideGroupGallery cannot be called without a groupId passed in");
		
     // get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationData data = (ConfigurationData) config.customData;
			if (data != null){
				backgroundDrawable = data.backgroundDrawable;	
			}
		}
		
		// grab the group name
		GroupsAdapter groups = new GroupsAdapter(this);
		try{
			groupName = groups.getGroup(groupId).toString();
		}catch(Exception e){
			e.printStackTrace();
			finish();
			return;
		}
		
		// initialize layout
		initializeLayout();	

		// grab cursor for all the groups
		getPictures();
		fillPictures();
	}
	
	// fill list with the pictures
	private void fillPictures() {
		// set adapter
        adapter = new PicturesGridAdapter(this, picturesAdapater);
        gridView.setAdapter(adapter);
	}

	/**
	 * Find the cursor required for searching Contacts
	 */
	private void getPictures(){
		picturesAdapater = new PicturesAdapter(this);
		picturesAdapater.fetchPicturesInGroup(groupId);
		picturesAdapater.startManagingCursor(this);
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
		
	}

	@Override
	protected void initializeLayout() {

		// make full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		// set main view
		setContentView(R.layout.inside_group_viewer);
		
		// grab pointers to objects
		gridView = (GridView)findViewById(R.id.photosView);
        
        // add click listener
        gridView.setOnItemClickListener(gridViewClick);
        
        // pointers to graphics
        takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
        screen = (ImageView)findViewById(R.id.screen);
        groupNameText = (TextView) findViewById(R.id.groupName);
        
        // set alpha of take button picture
        takePictureButton.setAlpha(Utils.PICTURE_ALPHA);
        
        // set the name
		groupNameText.setText(Html.fromHtml(groupName));
        
        // set the background picture
        GroupsAdapter groups = new GroupsAdapter(this);
        Group group = groups.getGroup(groupId);
        if (backgroundDrawable != null){
        	screen.setImageDrawable(backgroundDrawable);
        	backgroundDrawable = null;
        }else
        	Utils.setBackground(this, group.getPictureThumbnailPath(this), screen, 0.5f);
	}
	
	@Override
	public void onPause(){
	//	overridePendingTransition(0, R.anim.picture_scale_down_animation);
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		super.onPause();
	}
	
	@Override
	public void onResume(){
		if (adapter != null){
			adapter.imageLoader.restartThreads();
			adapter.notifyDataSetChanged();
		}
		super.onResume();
	}
	
	private OnItemClickListener gridViewClick =  new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> parent,
				View view,
				int position,
				long id) {
			
			// load the intent for this groups gallery
			SinglePictureGallery.passedCache = adapter.getMemoryCache();
			Intent intent = new Intent(act, SinglePictureGallery.class);
			intent.putExtra(
					SinglePictureGallery.GROUP_ID,
					groupId);
			picturesAdapater.moveToPosition(position);
			intent.putExtra(
					SinglePictureGallery.PICTURE_POSITION,
					position);

			// load the activity
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, 0);								
		}
	};

	/**
	 * When this is clicked, set default group to this one, and open cameara
	 * @param v
	 */
	public void takePictureClicked(View v){
		Prefs.setGroupIds(this, groupId);
		Intent intent = new Intent(this, TakePicture.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationData data = new ConfigurationData();
		data.backgroundDrawable = screen.getDrawable();
		configurationProperties.customData = data;
	}
	
	private class ConfigurationData{
		public Drawable backgroundDrawable;
	}

	@Override
	protected void onDestroyOverride() {
		
		// null out adapter
		adapter.imageLoader.clearCache();
		gridView.setAdapter(null);	
	}

    private class PicturesGridAdapter
    extends BaseAdapter {

        private PicturesAdapter data;
        private LayoutInflater inflater = null;
        private ImageLoader imageLoader; 
        
        public PicturesGridAdapter(Activity a, PicturesAdapter pictures) {
            data = pictures;
            inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            imageLoader=new ImageLoader(
            		R.drawable.stub,
            		0,
            		0,
            		false);
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            if(data.moveToPosition(position))
            	return data;
            else
            	return null;
        }

        public long getItemId(int position) {
        	if(data.moveToPosition(position))
        		return data.getRowId();
        	else
        		return 0;
        }
        
        /**
	     * Return the memory cache.<br>
	     * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
	     * for example on orientation changes *****
	     * @return
	     */
		public MemoryCache getMemoryCache(){
			return imageLoader.getMemoryCache();
		}
        
        public View getView(int position, View convertView, ViewGroup parent) {

        	// attempt to use recycled view
            View vi=convertView;
            if(convertView==null)
                vi = inflater.inflate(R.layout.photo_item, null);
            
            // grab pointers
            TextView text=(TextView)vi.findViewById(R.id.photoCaption);
            ImageView image=(ImageView)vi.findViewById(R.id.photoImage);
            
            // fill the views
            text.setText("");
            if (data.moveToPosition(position))
            	imageLoader.DisplayImage(data.getRowId(), data.getThumbnailPath(), null, image);

            return vi;
        }
    }
}
