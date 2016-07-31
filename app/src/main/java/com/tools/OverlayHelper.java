package com.tools;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * A wrapper class for an overlay that appears when desired and disappears on click
 * @author Kyle
 *
 */
public class OverlayHelper <ACTIVITY_TYPE extends Activity>{

	// private variables
	private FrameLayout frameLayout;			// the frame layout of the main window
	private int drawableResourceId;	 			// the resource id to show when 
	private CheckToShowCallback<ACTIVITY_TYPE> checkToShowCallback;	// the callback to check if we shoulds how overlay

	/**
	 * Create an OverlayHelper for showing an overlay when desired that will disappear when touched
	 * @param frameLayout The framelayout to display it in
	 * @param drawableResourceId The image to display
	 * @param callback The function used to check if we should show it
	 */
	public OverlayHelper(FrameLayout frameLayout, int drawableResourceId, CheckToShowCallback<ACTIVITY_TYPE> callback){
		this.frameLayout = frameLayout;
		this.checkToShowCallback = callback;
		this.drawableResourceId = drawableResourceId;
	}
	
	/**
	 * Show if the conditions are met to show
	 * @param act The activity to work on 
	 * @param drawableResourceId The image of overlay to show
	 * @return True if we showed it, false otherwise
	 */
	public boolean showIfNeeded(ACTIVITY_TYPE act){
		boolean isShouldShow = checkToShowCallback.onCheck(act);
		
		// show if supposed to
		if (isShouldShow){
			inflateOverlay(act, drawableResourceId);
		}
		
		// return if showed
		return isShouldShow;
	}

	/**
	 * Inflate the overlay
	 * @param act
	 * @param drawableResourceId
	 */
	private void inflateOverlay(Activity act, int drawableResourceId){

		// create the view
		ImageView image = new ImageView(act);
		image.setImageResource(drawableResourceId);

		// how to layout the view
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);

		// add it to layout
		frameLayout.addView(image, params);

		// remove overlay when touched
		image.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				
				// when touched, start an animation that fades out
				final View v1 = view;
				Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
				anim.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {	}
					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						frameLayout.removeView(v1);
						v1.setVisibility(View.GONE);
					}
				});

				// actually start the animation
				view.startAnimation(anim);
				return true;
			}
		});	

		/*
		View view = inflater.inflate(R.layout.help_layout, frame);
		View help = view.findViewById(R.id.helpImage);	
		((ViewGroup)help.getParent()).removeView(help);
		frame.addView(help);

		// remove overlay when touched
		help.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				final View v1 = v;
				new AnimationUtils();
				Animation anim = AnimationUtils.loadAnimation(ctx, R.anim.fade_out);

				anim.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {	}
					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						FrameLayout frame = (FrameLayout) findViewById(R.id.frameLayout);
						frame.removeView((View) v1.getParent());
						v1.setVisibility(View.GONE);
					}
				});

				((View)v.getParent()).startAnimation(anim);
				return true;
			}
		});		
		 */	
	}
	
	public interface CheckToShowCallback<ACTIVITY_TYPE>{
		/**
		 * Checks if the overlay should be shown. <br>
		 * *** NOTE: do not reference member variables of enclosing activity if this is an anonymous call, or the activity will leak.
		 * Use the activity passed in this method to access those variables, not the variables directly. <br>
		 * eg. activity.memberVariable, not memberVariable directly. *** <br>
		 * @param activity the calling this method
		 * @return True if the overlay should be shown, false otherwise
		 */
		public boolean onCheck(ACTIVITY_TYPE activity);
	}
}
