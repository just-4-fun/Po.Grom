package cyua.android.core.map;

import cyua.android.core.AppCore;
import cyua.android.core.ui.FloatUiConnector;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**  ERROR DIALOG*/
public class PlayServiceErrorDialog extends FloatUiConnector
{
@Override public Dialog onCreateDialog(Bundle savedInstanceState)
{
	int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(AppCore.context());
//	Dialog
	return AppCore.uiContext() == null ? null : GooglePlayServicesUtil.getErrorDialog(status, AppCore.uiContext(), 0xC0F5);
}
@Override public void onDismiss(DialogInterface dialog) {
	super.onDismiss(dialog);
	AppCore.exit();
}
}