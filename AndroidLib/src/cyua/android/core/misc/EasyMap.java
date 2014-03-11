package cyua.android.core.misc;

import java.util.HashMap;

public class EasyMap<T> extends HashMap<String, T>
{
private static final long serialVersionUID = -6464259359798272131L;

public static <T> EasyMap<T> newMap()
{
	return new EasyMap<T>();
}

public EasyMap<T> plus(String key, T value)
{
	super.put(key, value);
	return this;
}

public EasyMap<T> plus(Object key, T value)
{
	super.put(key.toString(), value);
	return this;
}

}
