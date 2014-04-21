package cyua.android.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.ui.FloatUiConnector;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiService;
import cyua.java.shared.objects.ConfigSh;


public class TypeSelectorDialog extends FloatUiConnector {
List<ConfigSh.Type> types;

public TypeSelectorDialog() { }// Default required  +

@Override public Dialog onCreateDialog(Bundle savedInstanceState) {
	types = Settings.getTypes();
	final String[] items = new String[types.size()];
	final Integer[] icons = new Integer[types.size()];
	for (int $ = 0; $ < types.size(); $++) {
		ConfigSh.Type tp = types.get($);
		items[$] = tp.name;
		int drid = android.R.drawable.dark_header;
		try { drid = R.drawable.class.getField(tp.marker).getInt(null); }
		catch (Exception ex) {Wow.w("TypeSelectorDialog", "onCreateDialog", "No drawable for typeIndex "+tp.marker);}
		icons[$] = drid;
	}
	ListAdapter adapter = new ArrayAdapterWithIcon(App.uiContext(), items, icons);
	AlertDialog dialog = new AlertDialog.Builder(AppCore.uiContext())
//			.setIcon(android.R.drawable.ic_dialog_alert)
//			.setTitle(R.string.title_select_type)
			.setAdapter(adapter, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					UiState stt = (UiState) UiService.getUiState();
					stt.type = types.get(item);
					new UiCore.UiAction(Ui.UiOp.UPDATE_PAGE).execute();
				}
			})
			.create();
	return dialog;
}




/* ADAPTER */

static class ArrayAdapterWithIcon extends ArrayAdapter<String> {
	private List<Integer> iconRids;
	public ArrayAdapterWithIcon(Context context, String[] items, Integer[] drawableIds) {
		super(context, android.R.layout.select_dialog_item, items);
		iconRids = Arrays.asList(drawableIds);
	}
	@Override public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		if (view == null) return view;
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		if (textView == null) return view;
		textView.setCompoundDrawablesWithIntrinsicBounds(iconRids.get(position), 0, 0, 0);
		textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));
		return view;
	}

}
}
