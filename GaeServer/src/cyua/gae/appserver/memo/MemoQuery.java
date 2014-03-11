package cyua.gae.appserver.memo;

import java.util.ArrayList;
import java.util.List;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import cyua.java.shared.RMIException;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.Column;

public class MemoQuery <T extends ObjectSh>
{
private Class<T> typeClass;
private Query query;
private Filter filter;
private FetchOptions opts;

public MemoQuery(Class<T> typeCls)
{
	typeClass = typeCls;
	String type = Memo.getKeyType(typeCls);
	query = new Query(type);
	opts = FetchOptions.Builder.withDefaults();
}

public MemoQuery<T> addFilter(Column column, FilterOperator filterOp, Object value)
{
	String pty = column.name;
	Filter f =  new FilterPredicate(pty, filterOp, value);
	filter = filter == null ? f : CompositeFilterOperator.and(filter, f);
//	query.addFilter(pty, filterOp, value);
	return this;
}

public MemoQuery<T> addSort(Column column, SortDirection dir)
{
	String pty = column.name;
	dir = dir == null ? SortDirection.ASCENDING : dir;
	query.addSort(pty, dir);
	return this;
}

public MemoQuery<T> withLimit(int limit)
{
	opts.limit(limit);
	return this;
}

public MemoQuery<T> setKeysOnly()
{
	query.setKeysOnly();
	return this;
}

// -------------------------------------------------------------------
public List<T> execute() throws RMIException
{
	DatastoreService store = MStore.getDatastore(false);
	if (filter != null) query.setFilter(filter);
	PreparedQuery pq = store.prepare(query);
	List<T> objects = new ArrayList<T>();
	for (Entity entity : pq.asIterable(opts))
	{
		T obj = Memo.objectFromEntity(typeClass, entity);
		if (obj != null) objects.add(obj);
	}
	return objects;
}
}
