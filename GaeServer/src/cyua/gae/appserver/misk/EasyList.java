package cyua.gae.appserver.misk;

import java.util.ArrayList;

public class EasyList<T> extends ArrayList<T>
{
private static final long serialVersionUID = -6881403450098201784L;

public static EasyList newList()
{
	return new EasyList();
}

public EasyList<T> plus(T value)
{
	super.add(value);
	return this;
}

}
