package android.widget;

import android.app.AlertDialog;
import android.content.*;
import android.view.*;

public class IconContextMenu {
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info);
	}

	private final AlertDialog dialog;
	private final Menu menu;
	private Integer backgroundColor = null;
	private Integer fontColor = null;
	private Integer selectedBackgroundColor = null;
	private Integer selectedItem = null;
	private IconContextMenuAdapter adapter;

	private IconContextItemSelectedListener iconContextItemSelectedListener;
	private Object info;

	public IconContextMenu(Context context, int menuId) {
		this(context, newMenu(context, menuId));
	}

	public static Menu newMenu(Context context, int menuId) {
		Menu menu = new MenuBuilder(context);
		new MenuInflater(context).inflate(menuId, menu);
		return menu;
	}

	/**
	 * Set the background color for the items.
	 * @param color The color to set, null is the default
	 */
	public void setBackgroundColor(Integer color){
		backgroundColor = color;
		if (adapter != null)
			adapter.setBackgroundColor(color);
	}

	/**
	 * Set the font color for the items.
	 * @param color The color to set, null is the default
	 */
	public void setFontColor(Integer color){
		fontColor = color;
		if (adapter != null)
			adapter.setFontColor(fontColor);
	}

	/**
	 * Set the background color for the selected item.
	 * @param color The color to set, null is the default
	 * @param selectedItem the item that will get this color, null doesn't set any
	 */
	public void setSelectedBackgroundColor(Integer color, Integer selectedItem){
		selectedBackgroundColor = color;
		this.selectedItem = selectedItem;
		if (adapter != null)
			adapter.setSelectedBackgroundColor(selectedBackgroundColor, selectedItem);
	}

	public IconContextMenu(Context context, Menu menu) {
		this.menu = menu;

		adapter = new IconContextMenuAdapter(context, menu);
		adapter.setBackgroundColor(backgroundColor);
		adapter.setFontColor(fontColor);
		adapter.setSelectedBackgroundColor(selectedBackgroundColor, selectedItem);

		this.dialog = new AlertDialog.Builder(context)
		.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (iconContextItemSelectedListener != null) {
					iconContextItemSelectedListener.onIconContextItemSelected(adapter.getItem(which), info);
				}
			}
		})
		.setInverseBackgroundForced(true)
		.create();
	}

	public void setInfo(Object info) {
		this.info = info;
	}

	public Object getInfo() {
		return info;
	}

	public Menu getMenu() {
		return menu;
	}

	public void setOnIconContextItemSelectedListener(IconContextItemSelectedListener iconContextItemSelectedListener) {
		this.iconContextItemSelectedListener = iconContextItemSelectedListener;
	}

	public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
		dialog.setOnCancelListener(onCancelListener);
	}

	public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
		dialog.setOnDismissListener(onDismissListener);
	}

	public void setTitle(CharSequence title) {
		dialog.setTitle(title);
	}

	public void setTitle(int titleId) {
		dialog.setTitle(titleId);
	}

	public void show() {
		dialog.show();
	}

	public void dismiss() {
		dialog.dismiss();
	}

	public void cancel() {
		dialog.cancel();
	}

	public AlertDialog getDialog() {
		return dialog;
	}
}