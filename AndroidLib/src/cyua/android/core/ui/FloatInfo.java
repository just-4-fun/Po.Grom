package cyua.android.core.ui;

import cyua.android.core.AppCore;
import cyua.android.core.misc.Tool;
import android.widget.Toast;


public class FloatInfo
{

public static final int SHORT = Toast.LENGTH_SHORT;
public static final int LONG = Toast.LENGTH_LONG;

public static void show(CharSequence message, int duration)
{
	if (duration == 0) duration = Toast.LENGTH_LONG;
	Toast.makeText(AppCore.context(), message, duration).show();
}
public static void show(int rid, int duration)
{
	String message = UiHelper.string(rid);
	if (Tool.notEmpty(message)) show(message, duration);
}


}
