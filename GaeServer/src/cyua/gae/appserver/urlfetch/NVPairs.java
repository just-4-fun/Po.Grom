package cyua.gae.appserver.urlfetch;

import java.util.ArrayList;

public class NVPairs<T extends NVPair> extends ArrayList<NVPair>
{
private static final long serialVersionUID = 1L;
//

public static NVPairs<NVPair> newList()
{
	return new NVPairs<NVPair>();
}






public NVPairs<T> add(String name, String value)
{
	add(new NVPair(name, value));
	return this;
}


}
