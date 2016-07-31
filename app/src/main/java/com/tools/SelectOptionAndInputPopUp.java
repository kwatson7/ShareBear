package com.tools;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * Create a dialog window with a list of objects. There is a primary string and secondary string.
 * The user selects one list item, and also inputs a string in the input box. The developer should
 * set the on click listener for the OK and cancel buttons.
 * @author Kyle
 *
 */
public class SelectOptionAndInputPopUp extends AlertDialog{

	//TODO: seperate out view and adapter into their own files
	// member variables
	private ArrayList<TwoStrings> mMainArrayList;   		// holds data
	private ListView listView; 								// the list view
	private Context mCtx; 											
	private int mCheckedItem; 								// The position of the checked item
	private EditText mInput; 								// The editText for the input

	/**
	 * Constructor
	 * @param ctx The context where this dialog is called
	 * @param mainArrayList The arrayList of TwoStrings to show
	 * @param title The title of the listView
	 * @param defaultSelected Which item should start of checked
	 * @param hint The hint to show in the input box
	 * @param layoutResID An id of view to use to show items. NOT FUNCTIONAL
	 */
	public SelectOptionAndInputPopUp(Context ctx, 
			ArrayList < TwoStrings> mainArrayList,
			String title,
			int defaultSelected,
			String hint, 
			Integer layoutResID,
			boolean isShowInputBox){

		// create it
		super(ctx);

		// assign inputs
		mMainArrayList = mainArrayList;
		mCtx = ctx;

		// create the dialog
		this.setInverseBackgroundForced(true);
		if(title != null){
			this.setTitle(title);
		}
		
		// create parent view to hold it all
		LinearLayout linear = new LinearLayout(mCtx);
		linear.setOrientation(LinearLayout.VERTICAL);
		
		// create list view
		listView = new ListView(mCtx);		
		
		// make items clickable
		listView.setItemsCanFocus(true);

		//create and link adapters
		TwoStringListViewAdapter adapter = new TwoStringListViewAdapter(ctx, mMainArrayList);
		listView.setAdapter(adapter);

		// create input box at bottom
		LinearLayout horiz = new LinearLayout(mCtx);
		horiz.setOrientation(LinearLayout.HORIZONTAL);
		mInput = new EditText(mCtx);
		mInput.setHint(hint);
		horiz.addView(mInput, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		final Button done = new Button(mCtx);
		done.setText("Done");
		done.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				com.tools.Tools.hideKeyboard(mCtx, done);
				
			}
		});
		horiz.addView(done, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		// put linear holder into dialog
		this.setView(linear);
		linear.addView(horiz, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		// add list view to dialog
		linear.addView(listView);
		
		// should we actually show the input box
		if (isShowInputBox == false)
			mInput.setVisibility(View.GONE);
		
		// make it cancelable
		this.setCancelable(true);
		
		// check the default
		mCheckedItem = defaultSelected;
	}
	
	/**
	 * Get the id of the item that is checked
	 * @return
	 */
	public int getChecked(){
		return mCheckedItem;
	}
	
	/**
	 * Get the input string
	 * @return
	 */
	public String getInput(){
		return mInput.getText().toString();
	}
	
	 /**
     * A sample ListAdapter that presents content from arrays of two strings.
     * 
     */
    private class TwoStringListViewAdapter
    extends BaseAdapter {

       	// variables to be used
        private Context mContext;
    	private ArrayList<TwoStrings> mTwoStrings;
    	
        public TwoStringListViewAdapter(Context context, ArrayList<TwoStrings> mTwoStrings) {
        	this.mTwoStrings = mTwoStrings;
            mContext = context;
        }

        /**
         * The number of items in the list is determined by the number of items in our array
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return mTwoStrings.size();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         * 
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         * 
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a TwoStringView to hold each row.
         * 
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(final int position, View convertView, ViewGroup parent) {
        	final TwoStringView sv;
        	
        	// convert view to TwoStringsView, or create it if necessary
            if (convertView == null) {
                sv = new TwoStringView(mContext, mTwoStrings.get(position));
            } else {
                sv = (TwoStringView) convertView;
                sv.setStrings(mTwoStrings.get(position));
            }
            
            // set the view to be clickable
            sv.setClickable(true);
            sv.setFocusable(true);
            
            // set checked button
            if (position == mCheckedItem)
            	sv.setChecked(true);
            else
            	sv.setChecked(false);
            
            // make the click listener, to save the position and update views
            sv.setOnClickListener(new View.OnClickListener() {	
				@Override
				public void onClick(View v) {
					mCheckedItem = position;
		            ((TwoStringListViewAdapter) listView.getAdapter()).notifyDataSetChanged();
				}
			});
                        
            return sv;
        }
    }

    /**
     * We will use a TwoStringView to display each two strings. It's just a LinearLayout
     * with two text fields and a button
     *
     */
    private class TwoStringView extends LinearLayout {
        public TwoStringView(Context context, TwoStrings words) {
            super(context);

            this.setOrientation(HORIZONTAL);

            // Here we build the child views in code. They could also have            
            // vertical layout of two strings
            LinearLayout vert = new LinearLayout(context);
            vert.setOrientation(VERTICAL);
            addView(vert, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, 
            		LayoutParams.WRAP_CONTENT,
            		1));

            // main string
            mPrimaryString = new TextView(context);
            mPrimaryString.setTextSize(25);
            vert.addView(mPrimaryString, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            // second string
            mSecondaryString = new TextView(context);
            mSecondaryString.setTextSize(15);
            vert.addView(mSecondaryString, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            // radio button
            mRadio = new RadioButton(context);
            mRadio.setClickable(false);
            this.addView(mRadio, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            setStrings(words);
        }

        /**
         * Convenience method to set the dialogue of a SpeechView
         */
        public void setStrings(TwoStrings words) {
        	SpannableString content = new SpannableString(words.mObject1);
        	content.setSpan(new  StyleSpan(Typeface.BOLD), 0, content.length(), 0);
            mPrimaryString.setText(content);
            mPrimaryString.setText(words.mObject1);
            mSecondaryString.setText(words.mObject2);
        }
        
        public void setChecked(boolean checked){
        	mRadio.setChecked(checked);
        }
        
        public boolean isChecked(){
        	if (mRadio == null)
        		return false;
        	return mRadio.isChecked();
        }

        private TextView mTitle;
        private TextView mPrimaryString;
        private TextView mSecondaryString;
        private RadioButton mRadio;
    }
}
