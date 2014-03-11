package cyua.android.core;

import cyua.android.core.misc.Tool;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class Memo
{
private static final String PREFS_FILE_NAME = "preferences";
//
public static SharedPreferences prefs;



public static void init ()
{
	prefs = AppCore.context().getSharedPreferences(PREFS_FILE_NAME, 0);
}

public static void initDefaults()
{
	PreferenceManager.setDefaultValues(AppCore.context(), PREFS_FILE_NAME, 0, Define.R_XML_PREFERENCES, false);
}

/*** WRITE *****************************/

public static String set(String key, CharSequence val)
{
	SharedPreferences.Editor editor = prefs.edit();
	editor.putString(key, val.toString());
	editor.commit();
	return getString(key);// TODO RELEASE for test (return val;)
}
public static double set(String key, Double val)
{
	set(key, Tool.toNumString(val));
	return getDouble(key);
}
public static double set(String key, Float val)
{
	set(key, Tool.toNumString(val));
	return getDouble(key);
}
public static long set(String key, Long val)
{
	set(key, Tool.toNumString(val));
	return getLong(key);
}
public static int set(String key, Integer val)
{
	set(key, Tool.toNumString(val));
	return getInt(key);
}
public static boolean set(String key, Boolean val)
{
	set(key, Tool.asString(val));
	return getBoolean(key);
}

/*** READ *******************************/

public static String getString(String key)
{
	return prefs.getString(key, "");
}
public static double getDouble(String key)
{
	return Tool.toDouble(prefs.getString(key, "0"));
}
public static long getLong(String key)
{
	return Tool.toLong(prefs.getString(key, "0"));
}
public static int getInt(String key)
{
	return Tool.toInt(prefs.getString(key, "0"));
}
public static boolean getBoolean(String key)
{
	return Tool.toBoolean(prefs.getString(key, ""));
}

/*** REMOVE ************************************/

public static void remove(String key)
{
	SharedPreferences.Editor editor = prefs.edit();
	editor.remove(key);
	editor.commit();
}



/*****   defaultCache KEYS   */

public static class Key
{
public static final String TIME_SHIFT_MS = "time_shift_ms";
public static final String TIME_AT_BOOT = "time_at_boot";

}


}
