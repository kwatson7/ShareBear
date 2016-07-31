package com.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Make a listView with checkboxes, where multiple items can be checked. Simply set
 * OnClickListeners for the keys and use getChecked() to determine which items were selected.
 * You can pass an ArrayList of any object, and the toString() will be used to display the 
 * listView. Can handle html returned from toString()
 * @author Kyle
 *
 * @param <objectType>
 */
public class MultipleCheckPopUp2<objectType> extends AlertDialog{
//TODO: make sure this works properly for CustomLists and fully vet it
	
	// class variables
	private CustomList<objectType> mMainArrayList; 
	private boolean isTwoStringsListView = false;
	protected ListView listView;
	private Context mCtx;

	/**
	 * Constructor.
	 * @param ctx The context which calls this
	 * @param mainArrayList The arrayList of objects to show in listView
	 * @param title The title of the listView
	 * @param defaultClicked Whether items should be clicked or not
	 * @param overrideDefaultClickedIds an array of items that override the default behavior of clicked
	 */
	public MultipleCheckPopUp2(
			Context ctx, 
			CustomList<objectType> mainArrayList,
			String title,
			boolean defaultClicked,
			long [] overrideDefaultClickedIds){

		// construct it
		super(ctx);
		
		// assign inputs
		this.mMainArrayList = mainArrayList;
		mCtx = ctx;
		
		// what kind of list view are we going to do
		if (mainArrayList.get(0) instanceof TwoStrings)
			isTwoStringsListView = true;
		else
			isTwoStringsListView = false;
		
		// create the dialog
		if(title != null){
			this.setTitle(title);
		}
		listView = new ListView(mCtx);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		// make view for array
			
		// make items clickable
		listView.setItemsCanFocus(true);

		// background is white for twostrings array
		this.setInverseBackgroundForced(true);

		listView.setCacheColorHint(Color.WHITE);

		// create a hashset of checked items
		HashSet <Integer> checkedItems = new HashSet<Integer>(mMainArrayList.size());
		if (defaultClicked){
			for (int i = 0; i < mMainArrayList.size(); i++)
				checkedItems.add(i);
			if (overrideDefaultClickedIds != null)
				for (int i = 0; i<overrideDefaultClickedIds.length; i++)
					checkedItems.remove(i);
		}
		if (!defaultClicked && overrideDefaultClickedIds != null){
			for (int i = 0; i<overrideDefaultClickedIds.length; i++)
				checkedItems.add(i);
		}

		//create and link adapters
		ListViewAdapter adapter = new ListViewAdapter(ctx, mMainArrayList, checkedItems);
		listView.setAdapter(adapter);

		// add list view to dialog
		this.setView(listView);

		// make it cancelable
		this.setCancelable(true);
	}

	
	/**
	 * An array of the checked items.
	 * @return
	 */
	public int[] getChecked(){
		return ((ListViewAdapter)listView.getAdapter()).getChecked();
	}
	
	public void setSelection(int pos){
		listView.setSelection(pos);
	}
	
	public int getFirstVisiblePosition(){
		int out = -1;
		out = listView.getFirstVisiblePosition();
		return out;
	}
	
	/**
	 * Return the list of checked items. <br>
	 *  *** Caution, this may have the same underlying data as was passed in, especially if it was a CursorWrapper *** <br>
	 * @return
	 */
	public CustomList<objectType> getCheckedItems(){
		 mMainArrayList.resample(getChecked());
		 return mMainArrayList;
	}
	
	/**
     * A sample ListAdapter that presents content from arrays of two strings.
     * 
     */
    private class ListViewAdapter
    extends BaseAdapter {

       	// variables to be used
        private Context mContext;
    	private CustomList<objectType> data;
    	private HashSet<Integer> mCheckedItems;
    	
        public ListViewAdapter(Context context, CustomList<objectType> data, HashSet<Integer> checkedItems) {
        	this.data = data;
            mContext = context;
            mCheckedItems = checkedItems;
        }

        /**
         * The number of items in the list is determined by the number of items in our array
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return data.size();
        }
        
        public int[] getChecked(){
        	int[] out = new int[mCheckedItems.size()];
        	int i = 0;
        	for (Iterator<Integer> ii = mCheckedItems.iterator(); ii.hasNext(); ){
        		out[i] = ii.next();
        		i++;
        	}
        	return out;
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
                sv = new TwoStringView(mContext, data.get(position));
            } else {
                sv = (TwoStringView) convertView;
                sv.setStrings(data.get(position));
            }
            
            // set the view to be clickable
            sv.setClickable(true);
            sv.setFocusable(true);
            
            // set checked button
            if (mCheckedItems.contains(position))
            	sv.setChecked(true);
            else
            	sv.setChecked(false);
            
            // make the click listener, to save the position and update views
            sv.setOnClickListener(new View.OnClickListener() {	
				@Override
				public void onClick(View v) {
					if (mCheckedItems.contains(position))
						mCheckedItems.remove(position);
					else
						mCheckedItems.add(position);
		            ((ListViewAdapter) listView.getAdapter()).notifyDataSetChanged();
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
    private class TwoStringView
    extends LinearLayout {
        public TwoStringView(Context context, objectType words) {
            super(context);

            this.setOrientation(HORIZONTAL);

            // Here we build the child views in code. They could also have            
            // vertical layout of two strings
            LinearLayout vert = new LinearLayout(context);
            vert.setOrientation(VERTICAL);
            vert.setGravity(Gravity.CENTER_VERTICAL);
            addView(vert, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, 
            		LayoutParams.FILL_PARENT,
            		1));

            // main string
            mPrimaryString = new TextView(context);
            mPrimaryString.setTextSize(20);
            vert.addView(mPrimaryString, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            mPrimaryString.setTextColor(Color.DKGRAY);
            
            // second string
            mSecondaryString = new TextView(context);
            mSecondaryString.setTextSize(15);
            vert.addView(mSecondaryString, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            // radio button
            mCheck = new CheckBox(context);
            mCheck.setClickable(false);
            this.addView(mCheck, new LinearLayout.LayoutParams(
            		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            // set visibility
            if (words instanceof TwoStrings){
            	mPrimaryString.setVisibility(VISIBLE);
        		mSecondaryString.setVisibility(VISIBLE);
            }else{
            	mPrimaryString.setVisibility(VISIBLE);
        		mSecondaryString.setVisibility(GONE);
        		mPrimaryString.setGravity(Gravity.CENTER_VERTICAL);
            }
            
            setStrings(words);
        }

        /**
         * Convenience method to set the list of listView
         */
        public void setStrings(objectType data) {
        	if (data instanceof TwoStrings){
        		TwoStrings words = (TwoStrings) data;
        		SpannableString content = new SpannableString(Html.fromHtml(words.mObject1));
        		content.setSpan(new  StyleSpan(Typeface.BOLD), 0, content.length(), 0);
        		mPrimaryString.setText(content);
        		mSecondaryString.setText(Html.fromHtml(words.mObject2));
        	}else{
        		mPrimaryString.setText(Html.fromHtml(data.toString()));
        	}
        		
        }
        
        public void setChecked(boolean checked){
        	mCheck.setChecked(checked);
        }
        
        public boolean isChecked(){
        	if (mCheck == null)
        		return false;
        	return mCheck.isChecked();
        }

        private TextView mTitle;
        private TextView mPrimaryString;
        private TextView mSecondaryString;
        private CheckBox mCheck;
    }
}
