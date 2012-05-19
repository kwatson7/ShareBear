package com.instantPhotoShare;

import com.instantPhotoShare.images.ImageViewTouch;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Gallery;

public class CustomGallery
extends Gallery{

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
		if (Math.abs(now - mLastScrollEvent) > 250) {
			super.onLayout(changed, l, t, r, b);
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		mLastScrollEvent = SystemClock.uptimeMillis();
		didWeScrollGallery = true;
		if (didWeScrollGallery && didWeTouchGallery)
	    	((com.instantPhotoShare.images.ImageViewTouch) getSelectedView().findViewById( R.id.picture )).isDraggable = false;
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
	
	@Override
	public int pointToPosition(int x, int y){
		// this is done to not pass touch events down to children
		return Gallery.INVALID_POSITION;
	}
	
	private boolean didWeTouchGallery = false;
	private boolean didWeScrollGallery = false;
	@Override
	public boolean onInterceptTouchEvent( MotionEvent ev ) {

	    View view = getSelectedView();
	    ImageViewTouch image = (ImageViewTouch) view.findViewById( R.id.picture );

	    if ( !image.isDraggable() ) {
	        onTouchEvent( ev );
	    }
	    
	    if (ev.getAction() == MotionEvent.ACTION_DOWN)
	    	didWeTouchGallery = true;
	    if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL){
	    	didWeTouchGallery = false;
	    	didWeScrollGallery = false;
	    	image.isDraggable = true;
	    }
	    
	    if (didWeScrollGallery && didWeTouchGallery)
	    	image.isDraggable = false;
	    else
	    	image.isDraggable = true;

	   return super.onInterceptTouchEvent( ev );
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
