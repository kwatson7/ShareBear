package com.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * A simple TextView that is rotated 90 degrees
 * @author Kyle
 *
 */
public class TextViewRotated extends TextView {
//TODO: class not commented and candidate for deletion
	private int rotation = 90;
	final boolean topDown;

	public TextViewRotated(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final int gravity = getGravity();
		if(Gravity.isVertical(gravity) && (gravity&Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
			topDown = false;
		}else
			topDown = true;
	}

	public TextViewRotated(Context context) {
		super(context);
		final int gravity = getGravity();
		if(Gravity.isVertical(gravity) && (gravity&Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
			topDown = false;
		}else
			topDown = true;
	}

	public TextViewRotated(Context context, AttributeSet attrs) {
		super(context, attrs);
		final int gravity = getGravity();
		if(Gravity.isVertical(gravity) && (gravity&Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
			topDown = false;
		}else
			topDown = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	@Override
	protected void onDraw(Canvas canvas){
		TextPaint textPaint = getPaint(); 
		textPaint.setColor(getCurrentTextColor());
		textPaint.drawableState = getDrawableState();

		canvas.save();

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
	}
}
