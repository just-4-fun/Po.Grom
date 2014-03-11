package cyua.gae.appserver.memo;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.appengine.api.datastore.Text;
import com.google.gson.Gson;
import cyua.java.shared.BaseTool;

public class MemoUtils
{

//-------------------------------------------------------------------
public static Object convert2Entity(Object value, Class<?> ptyType)
{
	if (value == null);
	else if (value instanceof String)
	{
		if (((String)value).length() >= DataTypeUtils.MAX_STRING_PROPERTY_LENGTH)
			value = new Text((String)value);
	}
	else if (!(value instanceof Number || value instanceof Boolean))
	{
		value = new Text(new Gson().toJson(value, ptyType));
	}
	return value;
}

//-------------------------------------------------------------------
public static Object convertFromEntity(Object value, Class<?> ptyType, Class<?> valType) throws Exception
{
	if (!valType.equals(ptyType))
	{
		if (valType.equals(Text.class)) value = ((Text)value).getValue();
		if (ptyType.equals(String.class)) value = value.toString();
		else if (ptyType.getSuperclass().equals(Number.class)
				|| ptyType.equals(Boolean.class)) value = BaseTool.cast(value, ptyType);
		else value = new Gson().fromJson(value.toString(), ptyType);
	}
	return value;
}

}
