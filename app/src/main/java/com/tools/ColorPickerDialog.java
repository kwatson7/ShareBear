/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tools;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerDialog
extends Dialog {

	// member variables
	private OnColorChangedListener mListener;
    private int mInitialColor;
    
    /**
     * Create a color picker dialog. It's a color wheel
     * @param context Context that owns dialog
     * @param listener this is called when the color is changed
     * @param initialColor the initial default color, see Color
     */
	public ColorPickerDialog(
    		Context context,
    		OnColorChangedListener listener,
    		int initialColor) {
    	super(context, R.style.CenteredDialog);

    	mListener = listener;
    	mInitialColor = initialColor;
    }

	/**
	 * Create a color picker dialog. It's a color wheel
	 * @param context Context that owns dialog
     * @param listener this is called when the color is changed
	 * @param initialRed inital red color 0-255
	 * @param initialGreen initial green color 0-255
	 * @param initialBlue initial blue color 0-255
	 */
    public ColorPickerDialog(
    		Context context,
    		OnColorChangedListener listener,
    		int initialRed,
    		int initialGreen,
    		int initialBlue) {
    	super(context, R.style.CenteredDialog);

    	mListener = listener;
    	mInitialColor = Color.rgb(initialRed, initialGreen, initialBlue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnColorChangedListener l = new OnColorChangedListener() {
            public void colorChanged(int color, int red, int green, int blue) {
                mListener.colorChanged(color);
                dismiss();
            }
        };

        // set the title
        setTitle("Select Color");
        
        // set the color wheel
        setContentView(new ColorPickerView(getContext(), l, mInitialColor));
    }
    
    /**
     * Listener called when the color is changed
     */
    public static abstract class OnColorChangedListener {
    	/**
    	 * Called when the color is changed
    	 * @param color the int color, see Color for usage
    	 * @param red the red value
    	 * @param green the grren value
    	 * @param blue the blue value
    	 */
        public abstract void colorChanged(int color, int red, int green, int blue);
        void colorChanged(int color){
        	int red = Color.red(color);
        	int green = Color.green(color);
        	int blue = Color.blue(color);
        	colorChanged(color, red, green, blue);
        }
    }

    /**
     * Color wheel view
     */
    private static class ColorPickerView extends View {
    	// member variables
        private Paint mCenterPaint;
        private OnColorChangedListener mListener;
        private Bitmap colorWheelBitmap = null;
        private TextPaint textPaint;
        
        // constants
        private static final int CENTER_X = 200;
        private static final int CENTER_Y = 200;
        private static final int WIDTH = CENTER_X*2;
        private static final int HEIGHT = CENTER_Y*2;
        private static final int RADIUS = 200;
        private static final int CENTER_RADIUS = 32;
        private static final int HOLE_RADIUS = 48; 
        private static final float SCALE = 4;

        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;

            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
            colorWheelBitmap = createColorWheelBitmap();
            
            textPaint = new TextPaint();
            textPaint.setTextAlign(Align.CENTER);     
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setColor(Color.rgb(0, 0, 0));
            textPaint.setTextSize(25);  
        }

        private boolean mTrackingCenter;
        private boolean mHighlightCenter;

        @Override
        protected void onDraw(Canvas canvas) {

            canvas.translate(CENTER_X, CENTER_Y);

            canvas.drawBitmap(colorWheelBitmap, -CENTER_X, -CENTER_Y, null);
            
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

            // set color for text
            int color = mCenterPaint.getColor();
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            if (r >= 128) r = 0; else r = 255;
            if (g >= 128) g = 0; else g = 255;
            if (b >= 128) b = 0; else b = 255;
            textPaint.setColor(Color.rgb(r, g, b));
            Rect bounds = new Rect();
            textPaint.getTextBounds("OK", 0, 2, bounds);
            
            // draw the text
            canvas.drawText("OK", 0f, (bounds.bottom-bounds.top)/2, textPaint);
 
            if (mTrackingCenter) {
                int c = mCenterPaint.getColor();
                mCenterPaint.setStyle(Paint.Style.STROKE);

                if (mHighlightCenter) {
                    mCenterPaint.setAlpha(0xFF);
                } else {
                    mCenterPaint.setAlpha(0x80);
                }
                canvas.drawCircle(0, 0,
                                  CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
                                  mCenterPaint);

                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setColor(c);
            }
        }
        
        /**
         * Create the bitmap of the color wheel
         * @return
         */
        private Bitmap createColorWheelBitmap(){
        	
        	// initalize values
        	float x;
        	float y;
        	float r;
        	float theta;
        	float[] hsv = new float[3];
        	int[] colors = new int[WIDTH*HEIGHT];
        	float centerX = Math.round(CENTER_X/SCALE);
        	float centerY = Math.round(CENTER_Y/SCALE);
        	int width = Math.round(WIDTH/SCALE);
        	int height = Math.round(HEIGHT/SCALE);
        	float radius = Math.round(RADIUS/SCALE);
        	float holeRadius = Math.round(HOLE_RADIUS/SCALE);

        	// create the bitmap
        	for (int i = 0; i < height; i++){
        		for (int j = 0; j < width; j++){
        			
        			// calcualte r and theta
        			x = j-centerX;
        			y = i-centerY;
        			r = (float) Math.sqrt(x*x + y*y);
        			theta = (float) Math.atan2(-y, x);
        			
        			// calculate hue
        			hsv[0] = (float) ((Math.cos(theta*2*Math.PI/(Math.PI + theta + 1))/2+0.5)*360);
        			
        			// calcualte saturation
        			hsv[1] = (float) (theta/(2*Math.PI)*1.1+0.5);
        			if (hsv[1] > 1)
        				hsv[1] = 1;
        			else if (hsv[1] < 0)
        				hsv[1] = 0;
        			
        			// calcualte value
        			hsv[2] = (r-holeRadius)/(radius-holeRadius);
        			if (hsv[2] > 1)
        				hsv[2] = 1;
        			else if (hsv[2] < 0)
        				hsv[2] = 0;
        			
        			// assing to colors
        			colors[i*width + j] = Color.HSVToColor(hsv);
        		}
        	}
        	return Bitmap.createScaledBitmap(
        			Bitmap.createBitmap(colors, 0, width, width, height, Bitmap.Config.RGB_565),
        			WIDTH, HEIGHT, true);
        	//return Bitmap.createBitmap(colors, 0, WIDTH, WIDTH, HEIGHT, Bitmap.Config.RGB_565);
        }
        
        /**
         * Get the color on the colorwheel
         * @param y the y value (center subtracted)
         * @param x the x value (center subtracted)
         * @return the color
         */
        private int getColor(float y, float x){
        	// calcualte r and theta
			float r = (float) Math.sqrt(x*x + y*y);
			float theta = (float) Math.atan2(-y, x);
			
			// calculate hue
			float[] hsv = new float[3];
			hsv[0] = (float) ((Math.cos(theta*2*Math.PI/(Math.PI + theta + 1))/2+0.5)*360);
			
			// calculate saturation
			hsv[1] = (float) (theta/(2*Math.PI)*1.1+0.5);
			if (hsv[1] > 1)
				hsv[1] = 1;
			else if (hsv[1] < 0)
				hsv[1] = 0;
			
			// calvualte value
			hsv[2] = (r-HOLE_RADIUS)/(RADIUS-HOLE_RADIUS);
			if (hsv[2] > 1)
				hsv[2] = 1;
			else if (hsv[2] < 0)
				hsv[2] = 0;
			
			// assign to colors
			return Color.HSVToColor(hsv);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(WIDTH, HEIGHT);
        }       

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X;
            float y = event.getY() - CENTER_Y;
            boolean inCenter = java.lang.Math.sqrt(x*x + y*y) <= CENTER_RADIUS;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        mCenterPaint.setColor(getColor(y, x));
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackingCenter) {
                        if (inCenter) {
                            mListener.colorChanged(mCenterPaint.getColor());
                        }
                        mTrackingCenter = false;    // so we draw w/o halo
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    } 
}