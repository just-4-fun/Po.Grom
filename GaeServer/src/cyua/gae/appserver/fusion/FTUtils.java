package cyua.gae.appserver.fusion;

import com.google.gson.Gson;

import cyua.java.shared.BaseTool;


/**
 Created by Marvell on 2/16/14.
 */
public class FTUtils {
// ===================================================
//-------------------------------------------------------------------
public static String convert2FT(Object val)
{
String value = "";
if (val == null) return null;
else if (val instanceof String) value = val.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\\"");
else if (val instanceof Number || val instanceof Boolean) value = val.toString();
else value = new Gson().toJson(val);
return value;
}
//-------------------------------------------------------------------
public static Object convertFromFT(Object value, Class<?> ptyType) throws Exception
{
Class<?> valType = value.getClass();
if (!valType.equals(ptyType))
{
	if (ptyType.equals(String.class)) value = value.toString();
	else if (BaseTool.isEmpty(value)) value = null;
	else if (Number.class.isAssignableFrom(ptyType)) {
		value = BaseTool.cast(value, ptyType);
	}
	else if (ptyType == Boolean.class) {
		value = BaseTool.cast(value, ptyType);
	}
	else value = new Gson().fromJson(value.toString(), ptyType);
}
return value;
}
}
