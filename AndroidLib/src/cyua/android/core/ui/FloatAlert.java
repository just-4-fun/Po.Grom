package cyua.android.core.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import cyua.android.core.AppCore;
import cyua.android.core.R;
import cyua.android.core.misc.EasyBundle;


/** Created by far.be on 7/13/13. */
public class FloatAlert extends FloatUiConnector{


/** STATIC */

public static Dialog create(int titleRid, int msgRid) {
	AlertDialog dialog = new AlertDialog.Builder(AppCore.uiContext())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(titleRid)
			.setMessage(msgRid > 0 ? msgRid : R.string.empty)
			.create();
	return dialog;
}


/** INSTANCE */

private final String TITLE = "title", MESSAGE = "message";

public FloatAlert() { }// Default required

public FloatAlert(int titleRid, int msgRid) {
	Bundle args = new EasyBundle().putInt(TITLE, titleRid).putInt(MESSAGE, msgRid).get();
	setArguments(args);
}

@Override public Dialog onCreateDialog(Bundle savedInstanceState) {
	int titleRid = getArguments().getInt(TITLE);
	int msgRid = getArguments().getInt(MESSAGE);
	return create(titleRid, msgRid);
}


}
