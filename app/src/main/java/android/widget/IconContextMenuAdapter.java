package android.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.*;

public class IconContextMenuAdapter extends BaseAdapter {
	private Context context;
    private Menu menu;
    private Integer backgroundColor = null;
    private Integer fontColor = null;
    private Integer selectedBackgroundColor = null;
    private Integer selectedItem = null;
    
    public IconContextMenuAdapter(Context context, Menu menu) {
		this.context = context;
		this.menu = menu;
	}

	@Override
	public int getCount() {
		return menu.size();
	}
	
	@Override
	public MenuItem getItem(int position) {
		return menu.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getItemId();
	}
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        
        TextView res = (TextView) convertView;
        if (res == null) {
        	res = (TextView) LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, null);
        }
        
        // set custom colors
        if (backgroundColor != null)
        	res.setBackgroundColor(backgroundColor);
        if (fontColor != null)
        	res.setTextColor(fontColor);
        if (selectedBackgroundColor != null && selectedItem != null && position == selectedItem)
        	res.setBackgroundColor(selectedBackgroundColor);

        res.setTag(item);
        res.setText(item.getTitle());
        res.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
              
        return res;
    }
    
    /**
     * Set the background color for each item.
     * @param color The color to set, null is the default
     */
    public void setBackgroundColor(Integer color){
    	backgroundColor = color;
    }
    
    /**
     * Set the font color for the items.
     * @param color The color to set, null is the default
     */
    public void setFontColor(Integer color){
    	fontColor = color;
    }
    
    /**
     * Set the background color for the selected item.
     * @param color The color to set, null is the default
     * @param selectedItem the item that will get this color, null doesn't set any
     */
    public void setSelectedBackgroundColor(Integer color, Integer selectedItem){
    	selectedBackgroundColor = color;
    	this.selectedItem = selectedItem;
    }
}