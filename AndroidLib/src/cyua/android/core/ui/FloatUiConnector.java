package cyua.android.core.ui;

import cyua.android.core.AppCore;
import android.support.v4.app.DialogFragment;


public class FloatUiConnector extends DialogFragment
{
private static final String TAG = FloatUiConnector.class.getSimpleName();

String name;

public FloatUiConnector name(String _name)
{
	name = _name;
	return this;
}

public void show()
{
	if (AppCore.uiContext() != null) show(AppCore.uiContext().fragmentManager(), name == null ? TAG : name);
}
}
