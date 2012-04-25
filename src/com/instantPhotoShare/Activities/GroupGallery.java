package com.instantPhotoShare.Activities;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.instantPhotoShare.Adapters.GroupsAdapter.Group;
import com.tools.CustomActivity;

public class GroupGallery 
extends CustomActivity{

	// private variables
	private GridView gridView; 				// the gridview to show folder
    private LazyAdapter adapter; 			// the adapter to show folders
    private ArrayList<Group> groupsArray; 	// An array of all the groups
    private CustomActivity act = this; 		// This activity
    private ImageView takePictureButton; 	// the pointer to the take picture button
    private ImageView screen;
    
	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		// grab cursor for all the groups
		GroupsAdapter groupsAdapter = new GroupsAdapter(this);
		groupsArray = groupsAdapter.getAllGroups();
		
		// initialize layout
		initializeLayout();	
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
		setContentView(R.layout.groups_viewer);
		
		// grab pointers to objects
		gridView = (GridView)findViewById(R.id.groupsView);
		
		// set adapter
        adapter=new LazyAdapter(this, groupsArray);
        gridView.setAdapter(adapter);
        
        // add click listener
        gridView.setOnItemClickListener(gridViewClick);
        
        // pointers to graphics
        takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
        screen = (ImageView)findViewById(R.id.screen);
        
        // set alpha of take button picture
        takePictureButton.setAlpha(Utils.PICTURE_ALPHA);
	}
	
	@Override
	public void onPause(){
		//overridePendingTransition(0, R.anim.picture_scale_down_animation);
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
		
		// set the background picture
        PicturesAdapter pics = new PicturesAdapter(this);
        String picPath = null;
        pics.fetchRandomPicture(this, 10);
        while ((picPath == null || picPath.length() ==0 || !(new File(picPath)).exists()) && pics.moveToNext()){
        	picPath = pics.getThumbnailPath();
        }
        pics.close();
        Utils.setBackground(this, picPath, screen, 0.5f);
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
			Intent intent = new Intent(act, InsideGroupGallery.class);
			intent.putExtra(
					InsideGroupGallery.GROUP_ID,
					groupsArray.get(position).getRowId());

			// load the activity
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, 0);				
		}
	};
	
	/**
	 * When this is clicked, and open camera
	 * @param v
	 */
	public void takePictureClicked(View v){
		Intent intent = new Intent(this, TakePicture.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@Override
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub		
	}

	@Override
	protected void onDestroyOverride() {
		
		// null out adapter
		adapter.imageLoader.clearCache();
		gridView.setAdapter(null);	
	}

    private class LazyAdapter
    extends BaseAdapter {
        
        private ArrayList<Group> data;
        private LayoutInflater inflater = null;
        public ImageLoader imageLoader; 
        
        public LazyAdapter(Activity a, ArrayList<Group> groups) {
            data = groups;
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
            return position;
        }

        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
        	Group group = data.get(position);
            View vi=convertView;
            if(convertView==null)
                vi = inflater.inflate(R.layout.group_view_item, null);

            TextView text=(TextView)vi.findViewById(R.id.groupName);
            ImageView image=(ImageView)vi.findViewById(R.id.groupImage);
            text.setText(Html.fromHtml(group.toString()));

            imageLoader.DisplayImage(group.getPictureId(act), group.getPictureThumbnailPath(act), null, image);
            return vi;
        }
    }
}
