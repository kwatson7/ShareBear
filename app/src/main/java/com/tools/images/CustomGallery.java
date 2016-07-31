package com.tools.images;

import java.io.IOException;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Gallery;
import android.widget.ImageView;

public class CustomGallery
extends Gallery{

	private static final long SCROLL_TIME_THRESHOLD = 250l;
	private int pictureId = -1;
	
	
	public CustomGallery(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public CustomGallery(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomGallery(Context context)
	{
		super(context);
	}

	public void setImageViewTouchId(int id){
		pictureId = id;
	}
	
	/**
	 * Grab the current imageViewTouch that is within this gallery
	 * @return
	 */
	private ImageViewTouch getImageViewTouch(){
		View selected = getSelectedView();
		if (selected == null)
			return null;
		View view = selected.findViewById(pictureId);
		ImageViewTouch imageViewTouch = null;
		if (view != null)
			imageViewTouch = (com.tools.images.ImageViewTouch) view;
		return imageViewTouch;
	}
	
	@Override
	public boolean onFling(
			MotionEvent e1,
			MotionEvent e2,
			float velocityX,
			float velocityY) {        
		int kEvent = -1;
		if(isScrollingLeft(e1, e2)){
			//Check if scrolling left
			kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
			onKeyDown(kEvent, null);
			return true;
		}
		else if (isScrollingRight(e1, e2)){
			// check if scrolling right
			kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
			onKeyDown(kEvent, null);
			return true;
		}

		return false;
	}
	
	@Override
	public boolean onKeyDown(int code, KeyEvent event){
		if (event != null){
			int pos = getSelectedItemPosition();
			if (pos == Gallery.INVALID_POSITION)
				pos = getFirstVisiblePosition();
			if (pos == Gallery.INVALID_POSITION)
				return super.onKeyDown(code, event);

			switch(code){
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (pos > 0){
					setSelection(pos-1);
					return true;
				}
				break;
	
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (pos < this.getAdapter().getCount()-1){
					setSelection(pos+1);
					return true;
				}
			}
		}

		return super.onKeyDown(code, event);
	}

	private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2){
		return e2.getX() > e1.getX();
	}

	private boolean isScrollingRight(MotionEvent e1, MotionEvent e2){
		return e2.getX() < e1.getX();
	}

	private long mLastScrollEvent;

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		// XXX Hack alert!
		// Ignore layout calls if we've had a scroll event in the last 250 msec;
		// that'll ignore the per-second layout calls we get through BooPlayerView's
		// updating.
		long now = SystemClock.uptimeMillis();
		if (Math.abs(now - mLastScrollEvent) > SCROLL_TIME_THRESHOLD) {
			super.onLayout(changed, l, t, r, b);
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		mLastScrollEvent = SystemClock.uptimeMillis();
		didWeScrollGallery = true;
		if (didWeScrollGallery && didWeTouchGallery){
			ImageViewTouch image = getImageViewTouch();
			if (image != null)
				image.isDraggable = false;
		}
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
	
	@Override
	public int pointToPosition(int x, int y){
		// this is done to not pass touch events down to children
		return Gallery.INVALID_POSITION;
	}
	
	private boolean didWeTouchGallery = false;
	private boolean didWeScrollGallery = false;
	private boolean didWeMultiTouch = false;
	@Override
	public boolean onInterceptTouchEvent( MotionEvent ev ) {

	    ImageViewTouch image = getImageViewTouch();

	    // keep track if we are multi touch - required on some phones
	    if (ev.getActionIndex() > 0){
	    	didWeMultiTouch = true;
	    }
	    if (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP)
	    	didWeMultiTouch = false;
	    
	    if (image==null || !image.isDraggable() && !didWeMultiTouch) {
	        onTouchEvent( ev );
	    }
	    
	    if (ev.getAction() == MotionEvent.ACTION_DOWN)
	    	didWeTouchGallery = true;
	    if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL){
	    	didWeTouchGallery = false;
	    	didWeScrollGallery = false;
	    	if (image != null)
	    		image.isDraggable = true;
	    }
	    
	    if (image != null){
	    	if (didWeScrollGallery && didWeTouchGallery)
	    		image.isDraggable = false;
	    	else
	    		image.isDraggable = true;
	    }

	   super.onInterceptTouchEvent( ev );
	   return false;
	}
	
	/*
	private PointF m_start = new PointF();
	
	@Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
		View view = getSelectedView();
	    ImageViewTouch imageView = (ImageViewTouch) view.findViewById( R.id.picture );
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                m_start.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                imageView.isDraggable = true;
                break;
            case MotionEvent.ACTION_MOVE:
            	if ((event.getX() - m_start.x < 0) ||
                        (event.getX() - m_start.x > 0))
                imageView.isDraggable = false;
                break;
        }
        if (!imageView.isDraggable) {
            onTouchEvent(event);
        }
        return super.onInterceptTouchEvent(event);
    }
    */
}
