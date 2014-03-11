package cyua.android.core.ui;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;


public class UiHelper
{
private static final String TAG = UiHelper.class.getSimpleName();


/** FRAGMENT FINDERS */

@SuppressWarnings("unchecked")
public static <F extends Fragment> F fragment(int fragmentId)
{
	try {
		return (F)AppCore.uiContext().fragmentManager().findFragmentById(fragmentId);
	} catch (Throwable ex) { Wow.w(TAG, "fragment", "not found by id " + fragmentId + ".  " + ex.getMessage());}
	return null;
}
@SuppressWarnings("unchecked")
public static <F extends Fragment> F fragment(String fragmentTag)
{
	try {
		if (fragmentTag == null) return null;
		return (F)AppCore.uiContext().fragmentManager().findFragmentByTag(fragmentTag);
	} catch (Throwable ex) { Wow.w(TAG, "fragment", "not found by tag " + fragmentTag + ".  " + ex.getMessage());}
	return null;
}
@SuppressWarnings("unchecked")
public static <V extends View> V view(int viewId)
{
	try {
		return (V)AppCore.uiContext().findViewById(viewId);
	} catch (Throwable ex) { Wow.w(TAG, "[view] not found by id " + viewId + ".  " + ex.getMessage());}
	return null;
}
@SuppressWarnings("unchecked")
public static <V extends View> V view(int fragmentId, int viewId)
{
	try {
		Fragment f = fragment(fragmentId);
		return (V)f.getView().findViewById(viewId);
	} catch (Throwable ex) { Wow.w(TAG, "view", "not found by id " + viewId + ".  " + ex.getMessage());}
	return null;
}
@SuppressWarnings("unchecked")
public static <V extends View> V view(String frgmentTag, int viewId)
{
	try {
		Fragment f = fragment(frgmentTag);
		return (V)f.getView().findViewById(viewId);
	} catch (Throwable ex) { Wow.w(TAG, "view", "not found by id " + viewId + ".  " + ex.getMessage());}
	return null;
}



/** UTILS */

public static View inflate(int rid)
{
	try
	{
		LayoutInflater inflater = (LayoutInflater)AppCore.uiContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(rid, null);
	}
	catch (Exception ex) { Wow.w(TAG, Tool.stackTrace(ex));}
	return null;
}



public static String string(int rid)
{
	try
	{
		String text = AppCore.context().getString(rid);
		return text != null ? text : "";
	}
	catch (Exception ex) { Wow.w(TAG, "string", Tool.stackTrace(ex));}
	return "";
}
public static String string(int rid, Object... args)
{
	try
	{
		String text = AppCore.context().getString(rid, args);
		return text != null ? text : "";
	}
	catch (Exception ex) { Wow.w(TAG, "string", Tool.stackTrace(ex));}
	return "";
}


public static float dimension(int dimenRid) {
	return AppCore.context().getResources().getDimension(dimenRid);
}

public static Drawable drawable(int rid) {
	try {
		return AppCore.context().getResources().getDrawable(rid);
	}
	catch (Exception ex) { Wow.w(TAG, "drawable", Tool.stackTrace(ex));}
	return null;
}

}
