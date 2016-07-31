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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
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
public class MultipleCheckPopUp<objectType>
extends AlertDialog{
//TODO: clean code
//TODO: comment code
//TODO: use instanceof to change implementation for TwoStrings

	// class variables
	private ArrayList<objectType> mMainArrayList; 
	private ArrayList<TwoStrings> useThisArrayInstead;
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
	 * @param useThisArrayInstead A TwoStrings array, that if not null, will be used instead of mainArrayList
	 * This allows for a main strings and substring to be displayed in listView. Simply enter null for mainArrayList
	 * if it's not needed
	 */
	public MultipleCheckPopUp(Context ctx, 
			ArrayList < objectType> mainArrayList,
			String title,
			boolean defaultClicked,
			long [] overrideDefaultClickedIds,
			ArrayList <TwoStrings> useThisArrayInstead){

		// construct it
		super(ctx);
		
		// assign inputs
		mMainArrayList = mainArrayList;
		mCtx = ctx;
		this.useThisArrayInstead = useThisArrayInstead;
		
		// what kind of list view are we going to do
		if (useThisArrayInstead != null)
			isTwoStringsListView = true;
		else
			isTwoStringsListView = false;
		//if (mainArrayList.get(0) instanceof TwoStrings)
		//	isTwoStringsListView = true;
		//else
		//	isTwoStringsListView = false;
		
		// create the dialog
		if(title != null){
			this.setTitle(title);
		}
		listView = new ListView(mCtx);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		// make view for twoStrings array
		if (isTwoStringsListView){
			
			// make items clickable
			listView.setItemsCanFocus(true);
			
			// background is white for twostrings array
			this.setInverseBackgroundForced(true);
			
			listView.setCacheColorHint(Color.WHITE);
			
			// create a hashset of checked items
			HashSet <Integer> checkedItems = new HashSet<Integer>(useThisArrayInstead.size());
			if (defaultClicked){
				for (int i = 0; i < useThisArrayInstead.size(); i++)
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
			TwoStringListViewAdapter adapter = new TwoStringListViewAdapter(ctx, useThisArrayInstead, checkedItems);
			listView.setAdapter(adapter);
			
		// make view for standard array	
		}else{

			//create and link adapters
			CustomArrayAdapter<objectType> adapter = null;
				adapter = new CustomArrayAdapter<objectType>(mCtx, 
					android.R.layout.simple_list_item_multiple_choice, mMainArrayList);
			listView.setAdapter(adapter);
			
			// set them to checked or not checked
			for (int i = 0; i < listView.getCount(); i++){
				listView.setItemChecked(i, defaultClicked);
			}
			
			// override default
			if (overrideDefaultClickedIds != null)
				for (int i = 0; i<overrideDefaultClickedIds.length; i++)
					listView.setItemChecked((int) overrideDefaultClickedIds[i], !defaultClicked);
		}

		// add list view to dialog
		this.setView(listView);

		// make it cancelable
		this.setCancelable(true);
	}

	
	/**
	 * An array of the checked items.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public long[] getChecked(){
		if (isTwoStringsListView){
			return ((TwoStringListViewAdapter)listView.getAdapter()).getChecked();
		}
			
		return listView.getCheckItemIds();
	}
	
	public void setSelection(int pos){
		try{
			listView.setSelection(pos);
		}catch(Exception e){}
	}
	
	public int getFirstVisiblePosition(){
		int out = -1;
		try{
			out = listView.getFirstVisiblePosition();
		}catch(Exception e){}
		return out;
	}
	
	public ArrayList<objectType> getCheckedItemsGeneric(){
		// loop over selected items
		long[] selected = getChecked();
		if (selected == null)
			return null;
		
		ArrayList<objectType> output = new ArrayList<objectType>(selected.length);
		if (!isTwoStringsListView){
			for (int i = 0; i < selected.length; i++)
				output.add(mMainArrayList.get((int) selected[i]));
		}else{
			throw new IllegalArgumentException("use getCheckedItemsTwoStrings instead of getCheckedItemsGeneric for TwoStrings useThisListInstead != null");
		}
		return output;
	}
	
	public ArrayList<TwoStrings> getCheckedItemsTwoStrings(){
		// loop over selected items
		long[] selected = getChecked();
		if (selected == null)
			return null;
		
		ArrayList<TwoStrings> output = new ArrayList<TwoStrings>(selected.length);
		if (isTwoStringsListView){
			for (int i = 0; i < selected.length; i++)
				output.add(useThisArrayInstead.get((int) selected[i]));
		}else{
			throw new IllegalArgumentException("use getCheckedItemsGeneric instead of getCheckedItemsTwoStrings for TwoStrings useThisListInstead == null");
		}
		return output;
	}
	
	/**
     * A sample ListAdapter that presents content from arrays of two strings.
     * 
     */
    private class TwoStringListViewAdapter extends BaseAdapter {

       	// variables to be used
        private Context mContext;
    	private ArrayList<TwoStrings> mTwoStrings;
    	private HashSet<Integer> mCheckedItems;
    	
        public TwoStringListViewAdapter(Context context, ArrayList<TwoStrings> mTwoStrings, HashSet<Integer> checkedItems) {
        	this.mTwoStrings = mTwoStrings;
            mContext = context;
            mCheckedItems = checkedItems;
        }

        /**
         * The number of items in the list is determined by the number of items in our array
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return mTwoStrings.size();
        }
        
        public long[] getChecked(){
        	long[] out = new long[mCheckedItems.size()];
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
                sv = new TwoStringView(mContext, mTwoStrings.get(position));
            } else {
                sv = (TwoStringView) convertView;
                sv.setStrings(mTwoStrings.get(position));
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
					else{
						mCheckedItems.add(position);
						((TwoStringListViewAdapter) listView.getAdapter()).notifyDataSetChanged();
					}
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
            
            setStrings(words);
        }

        /**
         * Convenience method to set the dialogue of a SpeechView
         */
        public void setStrings(TwoStrings words) {
        	SpannableString content = new SpannableString(Html.fromHtml(words.mObject1));
    		content.setSpan(new  StyleSpan(Typeface.BOLD), 0, content.length(), 0);
    		mPrimaryString.setText(content);
    		mSecondaryString.setText(Html.fromHtml(words.mObject2));
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
    
    private class CustomArrayAdapter<TYPE>
    extends ArrayAdapter<TYPE>{

    	private final Context context;
    	private final ArrayList<TYPE> values;
    	private int id;
    	
		public CustomArrayAdapter(
				Context context,
				int textViewResourceId,
				ArrayList<TYPE> objects) {
			super(context, textViewResourceId, objects);
			this.context = context;
			this.values = objects;
			this.id = textViewResourceId;
		}
		
		@Override
        public View getView(
        		int position,
        		View convertView,
        		ViewGroup parent) {
        	
			// retrieve the old view if it is there, else inflate it
			CheckedTextView rowView;
			if (convertView == null){
				LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = (CheckedTextView) inflater.inflate(id, parent, false);
			}else
				rowView = (CheckedTextView) convertView;
			rowView.setText(Html.fromHtml(values.get(position).toString()));
			return rowView;
        }
    }
}
