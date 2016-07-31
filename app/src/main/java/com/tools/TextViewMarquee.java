package com.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class TextViewMarquee 
extends TextView{
//TODO: class not commented
//TODO: does not rotate correctly
	private ROTATION rotation = ROTATION.ROTATION_NORMAL;
	private boolean topDown;

	public enum ROTATION{
		ROTATION_NORMAL, ROTATION_90
	}

	public TextViewMarquee(Context context) {
		super(context);
		setProperties();
	}

	public TextViewMarquee(Context context, AttributeSet attrs) {
		super(context, attrs);
		setProperties();
	}

	public TextViewMarquee(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setProperties();
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		if(focused)
			super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}

	@Override
	public void onWindowFocusChanged(boolean focused) {
		if(focused)
			super.onWindowFocusChanged(focused);
	}


	@Override
	public boolean isFocused() {
		return true;
	}

	private void setProperties(){
		this.setSingleLine(true);
		this.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		this.setMarqueeRepeatLimit(-1);
		/*
		final int gravity = getGravity();
		if(Gravity.isVertical(gravity) && (gravity&Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
			topDown = false;
		}else
			topDown = true;
			*/
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		switch (rotation){
		case ROTATION_90:
			super.onMeasure(heightMeasureSpec, widthMeasureSpec);
			setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
			break;
		case ROTATION_NORMAL:
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
			break;
		}
	}

	@Override
	protected boolean setFrame(int l, int t, int r, int b){
		boolean output = false;
		switch (rotation){
		case ROTATION_90:
			output = super.setFrame(l, t, l+(b-t), t+(r-l));
			break;
		case ROTATION_NORMAL:
			output =  super.setFrame(l, t, r, b);
			break;
		}
		return output;
	}

	@Override
	public void draw(Canvas canvas){
		switch (rotation){
		case ROTATION_90:
			if(topDown){
				canvas.translate(getHeight(), 0);
				canvas.rotate(90);
			}else {
				canvas.translate(0, getWidth());
				canvas.rotate(-90);
			}
			canvas.clipRect(0, 0, getWidth(), getHeight(), android.graphics.Region.Op.REPLACE);
			break;
		case ROTATION_NORMAL:
			break;
		}
		super.draw(canvas);
	}

	/*
	@Override
	protected void onDraw(Canvas canvas){
		TextPaint textPaint = getPaint(); 
		textPaint.setColor(getCurrentTextColor());
		textPaint.drawableState = getDrawableState();

		canvas.save();

		switch (rotation){
		case ROTATION_90:
			if(topDown){
				canvas.translate(getWidth(), 0);
				canvas.rotate(90);
			}else {
				canvas.translate(0, getHeight());
				canvas.rotate(-90);
			}

			canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

			getLayout().draw(canvas);
			canvas.restore();
			break;
		case ROTATION_NORMAL:
			super.onDraw(canvas);
			break;
		}
	}	
	*/

	/**
	 * Set the rotation of the textView. 90 is rotated object and 0 is default normal.
	 * @param rotation
	 */
	 public void setRotation(ROTATION rotation){
		 this.rotation = rotation;
		 this.invalidate();
	 }
}
