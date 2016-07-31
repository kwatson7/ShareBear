package com.instantPhotoShare;

import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.instantPhotoShare.Adapters.GroupsAdapter.Group;

public class GroupSpinnerAdapter
extends ArrayAdapter<Group>
implements SpinnerAdapter{

	private List<Group> data;
	private LayoutInflater inflater = null;
	private int layoutId;
	private int mDropDownResource;

	public GroupSpinnerAdapter(Context context, int textViewResourceId,
			List<Group> objects) {
		super(context, textViewResourceId, objects);		 
		this.data = objects;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutId = textViewResourceId;
	}

	/**
	 * <p>Sets the layout resource to create the drop down views.</p>
	 *
	 * @param resource the layout resource defining the drop down views
	 * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
	 */
	public void setDropDownViewResource(int resource) {
		this.mDropDownResource = resource;
	}

	/**
	 * Returns the Size of the ArrayList
	 */
	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return android.R.layout.simple_spinner_dropdown_item;
	}

	/**
	 * Returns the View that is shown when a element was
	 * selected.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		// grab data
		Group group = data.get(position);

		// inflate new view if we have to
		View vi = convertView;
		if(convertView==null)
			vi = inflater.inflate(layoutId, null);

		// set text
		TextView text = (TextView) vi;
		text.setText(Html.fromHtml(group.toString()));

		return vi; 
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {

	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {

	}

	/**
	 * The Views which are shown in when the arrow is clicked
	 * (In this case, I used the same as for the "getView"-method.
	 */
	@Override
	public View getDropDownView(int position, View convertView,
			ViewGroup parent) {
		// grab data
		Group group = data.get(position);

		// inflate new view if we have to
		View vi = convertView;
		if(convertView==null)
			vi = inflater.inflate(mDropDownResource, null);

		// set text
		TextView text = (TextView) vi;
		text.setText(Html.fromHtml(group.toString()));

		return vi; 
	}
}