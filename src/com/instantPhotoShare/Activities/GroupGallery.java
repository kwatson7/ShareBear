package com.instantPhotoShare.Activities;

import java.io.File;
import java.lang.ref.WeakReference;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.instantPhotoShare.R;
import com.instantPhotoShare.Utils;
import com.instantPhotoShare.Adapters.GroupsAdapter;
import com.instantPhotoShare.Adapters.PicturesAdapter;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.images.ImageLoader.LoadImage;

public class GroupGallery 
extends CustomActivity{

	// private constants
	private static final int DEFAULT_PIC_WIDTH_DP = 96;
	
	// private variables
	private GridView gridView; 				// the gridview to show folder
	private LazyAdapter adapter; 			// the adapter to show folders
	//private ArrayList<Group> groupsArray; 	// An array of all the groups
	private GroupsAdapter groupsArray;
	private CustomActivity act = this; 		// This activity
	private ImageView takePictureButton; 	// the pointer to the take picture button
	private ImageView screen;
	private int actualPicWidth = DEFAULT_PIC_WIDTH_DP; // the width of the pic item. Will be set later.
	

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// initialize layout
		initializeLayout();	
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {

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

		// add click listener
		gridView.setOnItemClickListener(gridViewClick);
		
		// change size of images in grid view to have no space between tiles
		actualPicWidth = com.tools.Tools.setGridViewColsBasedOnScreen(act, DEFAULT_PIC_WIDTH_DP, gridView, 0, true);

		// pointers to graphics
		takePictureButton = (ImageView) findViewById(R.id.takePictureButton);
		screen = (ImageView)findViewById(R.id.screen);

		// set alpha of take button picture
		takePictureButton.setAlpha(Utils.PICTURE_ALPHA);
	}

	@Override
	public void onPause(){
		super.onPause();

		if (adapter != null)
			adapter.imageLoader.stopThreads();
		
		// recyle the old bitmap if one exists
		Drawable drawable = screen.getDrawable();
		Bitmap bitmapOld = null;
		if (drawable instanceof BitmapDrawable) {
		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		    bitmapOld = bitmapDrawable.getBitmap();
		}
		screen.setImageResource(android.R.color.background_dark);
		if (bitmapOld != null)
			bitmapOld.recycle();
		screen.invalidate();
	}

	@Override
	public void onResume(){
		super.onResume();
		
		// make adapter
		if (groupsArray == null)
			groupsArray = new GroupsAdapter(ctx);
		groupsArray.stopManagingCursor(act);
		groupsArray.fetchAllGroups();
		groupsArray.startManagingCursor(act);
		if (adapter == null){
			adapter=new LazyAdapter(this, groupsArray);
			gridView.setAdapter(adapter);
		}else{
			adapter.updateGroups(groupsArray);
			adapter.notifyDataSetChanged();
		}

		if (adapter != null){
			adapter.imageLoader.restartThreads();
			adapter.notifyDataSetChanged();
		}

		// set the background picture
		PicturesAdapter pics = new PicturesAdapter(this);
		String picPath = null;
		pics.fetchRandomPicture(10);
		while ((picPath == null || picPath.length() ==0 || !(new File(picPath)).exists()) && pics.moveToNext()){
			picPath = pics.getThumbnailPath();
		}
		pics.close();
		Utils.setBackground(this, picPath, screen, 0.5f);
		
		// show toast if only 1 group
		new CustomAsyncTask<GroupGallery, Void, Integer>(GroupGallery.this, -1, true, true, null){

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Integer doInBackground(Void... params) {
				GroupsAdapter groups = new GroupsAdapter(applicationCtx);
				return groups.getNGroups();
			}

			@Override
			protected void onProgressUpdate(Void... progress) {
			}

			@Override
			protected void onPostExectueOverride(Integer result) {
				if (result <= 1 && callingActivity != null)
					Toast.makeText(callingActivity, "You should make a shared group.", Toast.LENGTH_LONG).show();
			}

			@Override
			protected void setupDialog() {

			}

		}.execute();
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

	}

	@Override
	protected void onDestroyOverride() {

		// null out adapter
		adapter.imageLoader.clearCache();
		gridView.setAdapter(null);	
		
		// delete screen bitmap
		if (screen != null){
			Drawable drawable = screen.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				bitmap.recycle();
			}
		}
	}

	private class LazyAdapter
	extends BaseAdapter {

		private GroupsAdapter data;
		private LayoutInflater inflater = null;
		public com.tools.images.ImageLoader<Long,GroupHelper,GroupHelper> imageLoader; 

		public LazyAdapter(Activity a, GroupsAdapter groups) {
			data = groups;
			inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			imageLoader = new com.tools.images.ImageLoader<Long, GroupHelper, GroupHelper>(
					R.drawable.stub,
					0,
					0,
					false,
					new LoadImage<GroupHelper, GroupHelper>() {

						@Override
						public Bitmap onThumbnailLocal(GroupHelper thumbnailData) {
							long picId = thumbnailData.picId;
							if (picId <= 0)
								return null;
							PicturesAdapter pics = new PicturesAdapter(ctx);
							pics.fetchPicture(picId);
							Bitmap bmp = pics.getThumbnail();
							pics.close();
							return bmp; 
							//return com.tools.images.ImageLoader.getThumbnail(thumbnailData.getPictureThumbnailPath(act));
						}

						@Override
						public Bitmap onThumbnailWeb(GroupHelper thumbnailData) {
							long picId = thumbnailData.picId;
							long groupId = thumbnailData.groupId;
							if (picId <= 0 || groupId <= 0)
								return null;
							return PicturesAdapter.getThumbnailFromServer(ctx, picId, groupId);
						}

						@Override
						public Bitmap onFullSizeLocal(GroupHelper fullSizeData,
								int desiredWidth, int desiredHeight) {
							return null;
						}

						@Override
						public Bitmap onFullSizeWeb(GroupHelper fullSizeData,
								int desiredWidth, int desiredHeight, WeakReference<ProgressBar> weakProgress) {
							return null;
						}

						@Override
						public void createThumbnailFromFull(
								GroupHelper thumbnailData, GroupHelper fullSizeData) {
							long picId = thumbnailData.picId;
							PicturesAdapter pics = new PicturesAdapter(ctx);
							pics.fetchPicture(picId);
							com.tools.images.ImageLoader.createThumbnailFromFull(
									pics.getThumbnailPath(),
									pics.getFullPicturePath(),
									Utils.MAX_THUMBNAIL_DIMENSION,
									Utils.FORCE_BASE2_THUMBNAIL_RESIZE,
									Utils.IMAGE_QUALITY);
							pics.close();
						}
					});
		}

		public void updateGroups(GroupsAdapter groups){
			//imageLoader.clearCache();
			this.data = groups;
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
			
			View vi=convertView;
			if(convertView==null)
				vi = inflater.inflate(R.layout.group_view_item, null);

			TextView text=(TextView)vi.findViewById(R.id.groupName);
			ImageView image=(ImageView)vi.findViewById(R.id.groupImage);
			
			// change view size
			image.getLayoutParams().height = actualPicWidth;
			image.getLayoutParams().width = actualPicWidth;

			// move to correct position
			if (!data.moveToPosition(position)){
				Log.e(Utils.LOG_TAG, "could not move to correct position in group gallery");
				return vi;
			}else{
				text.setText(Html.fromHtml(data.toString()));
				GroupHelper helper = new GroupHelper(data.getPictureId(), data.getRowId());
				imageLoader.DisplayImage(helper.picId, helper, helper, image, null);
			}
			
			return vi;
		}
	}

	private static class GroupHelper{
		private long picId;
		private long groupId;

		private GroupHelper(long picId, long groupId){
			this.picId = picId;
			this.groupId = groupId;
		}
	}
}
