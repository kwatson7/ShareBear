package com.tools;

/**
 * Simple class for storing a width and height. Has various manipulation tools.
 * @author Kyle
 *
 */
public class WidthHeight {

	/** The width of object */
	public int width;
	
	/** The height of the object */
	public int height;

	/**
	 * Simply construct the object saving the parameters
	 * @param WIDTH width
	 * @param HEIGHT height
	 */
	public WidthHeight(int WIDTH, int HEIGHT){
		width = WIDTH;
		height = HEIGHT;
	}

	/**
	 * Grab the aspect ratio defined as width/height
	 * @return
	 */
	public float getAspectRatio(){
		return (float)width/height;
	}
	
	/**
	 * Switch the width with the height and return a new object with these switched parameters.
	 * *** The original object is unchanged ***
	 * @return
	 */
	public WidthHeight switchDimensions(){
		return new WidthHeight(height, width);
	}
}
