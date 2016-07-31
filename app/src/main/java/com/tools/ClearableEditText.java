package com.tools;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

/**
 * A simple EditText with a clear button on the right side of the EditText
 * @author Kyle
 *
 */
public class ClearableEditText
extends EditText{
	
	// private variables
	/**
	 * The drawable on the right of the EditText
	 */
	private Drawable dRight;
	/**
	 * The rectangle bounding the drawable on the right side
	 */
	private Rect rBounds;

	// Constructors
	/**
	 * Constructor for creating the ClearableEditText.
	 * @param context The context this view is underneath
	 * @param attrs The set of attributes for the EditText
	 * @param defStyle
	 */
	public ClearableEditText(Context context, AttributeSet attrs, int defStyle) {
		// call super constructor
		super(context, attrs, defStyle);
		
		// set the clear button
		setClearButton();
	}
	public ClearableEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClearButton();
	}
	public ClearableEditText(Context context) {
		super(context);
		setClearButton();
	}
	
	/**
	 * Set the clear button for the EditText. Makes the drawable and sets the bounds
	 */
	private void setClearButton(){
		Drawable x = 
			getResources().getDrawable(android.R.drawable.ic_notification_clear_all);
		x.setBounds(0-(int)(x.getIntrinsicWidth()*.1*0), 
				0-(int)(x.getIntrinsicHeight()*.1*0), 
				(int)(x.getIntrinsicWidth()*1.2), 
				(int)(x.getIntrinsicHeight()*1.2));		
		setCompoundDrawables(null, null, x, null);
	}

	@Override
	public void setCompoundDrawables(Drawable left, Drawable top,
			Drawable right, Drawable bottom)
	{
		// simply save the drawable and then call the super
		if(right !=null)
		{
			dRight = right;
		}
		super.setCompoundDrawables(left, top, right, bottom);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// see if we are inside the bounds of the clear button and if so, then clear the text
		if(dRight!=null && 
				(event.getAction() == MotionEvent.ACTION_UP ||
						event.getAction() == MotionEvent.ACTION_DOWN))
		{
			rBounds = dRight.getBounds();
			final int x = (int)event.getX();
			final int y = (int)event.getY();
			if(x>=(this.getRight()-rBounds.width()) && x<=(this.getRight()-this.getPaddingRight())
					&& y>=this.getPaddingTop() && y<=(this.getHeight()-this.getPaddingBottom()))
			{
				if (event.getAction() == MotionEvent.ACTION_UP)
					this.setText("");
				event.setAction(MotionEvent.ACTION_CANCEL);//use this to prevent the keyboard from coming up
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void finalize() throws Throwable
	{
		dRight = null;
		rBounds = null;
		super.finalize();
	}
}
