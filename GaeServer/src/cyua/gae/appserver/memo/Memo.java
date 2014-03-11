package cyua.gae.appserver.memo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import cyua.gae.appserver.App;
import cyua.gae.appserver.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.RMIException;
import cyua.java.shared.ObjectSh;


/*
 * WARNING ! 
 * ! API is intended for use of combined storage of objects in Datastore backed by Memcache..
 * ! Advantage of using Memcache only in [get] method
 * ! [get] uses Memcache first. [getAll] do not use Memcache
 * ! Class that is intended to use Memcache should implement Serializable interface and have no-arg constructor and serialVersionUID
 * ! Only public non-trancient fields will be stored.
 * ! All get/set/delete methods insist on ConcurrentModificationException
 * */

public class Memo
{
static final Logger log = Logger.getLogger(Memo.class.getName());

private static final String GROUP_TOKEN = "_group";
//
static String PFX = "";//"_"+App.appVersion;


// ===================================================
// OPERATIONS with objects
public static <T extends ObjectSh> T get(Class<T> typeCls, String id) throws RMIException
{
	Tool.setTimer();
	T object = null;
	String type = getKeyType(typeCls);
	// MEMCACHE first
	if (ObjectSh.isCacheable(typeCls))
	{
		try{object = MCache.getObject(typeCls, id);} catch(Throwable ex) {}
log.info("    [FOUND IN CACHE] ? "+(object != null)+ ", time:"+Tool.getTimer(true)+", Type:"+type+", ID:"+id+", OBJ:"+object);
		if (object != null) return object;
	}
	// DATASTORE else
	try
	{
		// TODO read more about transaction and concurrent modification
		Key group = KeyFactory.createKey(GROUP_TOKEN, type);
		Key key = KeyFactory.createKey(group, type, id);
		DatastoreService store = MStore.getDatastore(false);
		Entity entity = store.get(key);
		object = objectFromEntity(typeCls, entity);
		// save to CACHE
		if (object != null) try{MCache.saveObject(id, object);} catch(Throwable ex) {}
	}
	catch (EntityNotFoundException ex) {}
	catch (Throwable ex)
	{
		log.severe("[get]: "+ex.toString());
		throw new RMIException(ex);
	}// IllegalArgumentException|DatastoreFailureException
log.info("    [FOUND IN STORE] ? "+(object != null)+ ", time:"+Tool.getTimer(false)+", Type:"+type+", ID:"+id+", OBJ:"+object);
	return object;
}
// -------------------------------------------------------------------
public static <T extends ObjectSh> List<T> getAll(Class<T> typeCls, boolean delete, int limit) throws RMIException
{
	Tool.setTimer();
	// objects can not be loaded from cache
	List<T> objects = new ArrayList<T>();
	List<Key> keys = new ArrayList<Key>();
	String type = getKeyType(typeCls);
	// from DATASTORE
	try
	{
		Key group = KeyFactory.createKey(GROUP_TOKEN, type);
		DatastoreService store = MStore.getDatastore(delete);
		//
		Query q = new Query(type, group);
		FetchOptions opts = FetchOptions.Builder.withDefaults();
		if (limit > 0) opts.limit(limit);
		Iterator<Entity> $e = store.prepare(q).asIterator(opts);
		int count = 0;
		while ($e.hasNext())
		{
			count++;
			Entity entity = $e.next();
			T object = objectFromEntity(typeCls, entity);
			if (object == null) continue;
			objects.add(object);
			if (delete) keys.add(entity.getKey());
		}
	log.info("    [GET ALL] time:"+Tool.getTimer(true)+",  Type:"+type+",  Size:"+objects.size()+",  inStore count:"+count);
		// DELETE
		if (delete && keys.size() > 0)
		{
			// from CACHE
			int cch = 0;
			if (ObjectSh.isCacheable(typeCls))
			{
				MemcacheService cache = MCache.getCache(type);
				for (Key key : keys) if (MCache.removeObject(key.getName(), cache)) cch++;
				if (cch != keys.size()) log.warning("    [getAll]: Not all objects Deleted from cache. cch="+cch+"  va objects="+keys.size());
			}
			// from STORE
			boolean isok = MStore.deleteEntities(keys);
	log.info("    [DELETE ALL]   ? "+isok+",  time:"+Tool.getTimer(true)+", Type:"+type+", Size:"+keys.size()+", CCH:"+cch);
		}
	}
	catch (Throwable ex)
	{
		log.severe("[getAll]: "+ex.toString());
		throw new RMIException(ex);
	}
	return objects;
}

//-------------------------------------------------------------------
public static <T extends ObjectSh> boolean save(T object) throws RMIException
{
	Tool.setTimer();
	Class<T> typeCls = (Class<T>) object.getClass();
	String type = getKeyType(typeCls);
	String id = object.getStorableID();
	if (Tool.isEmpty(id)) throw new RMIException(RMIException.FAIL_CODE, "[saveAll] object has no id.");
	// save to MEMCACHE
	if (object.isCacheable())
	{
		boolean saved = MCache.saveObject(id, object);
log.info("    [SAVED TO CACHE] ? "+ saved+", time:"+Tool.getTimer(true)+", Type:"+type+", ID:"+id);
	}
	// save to DATASTORE
	Key group = KeyFactory.createKey(GROUP_TOKEN, type);
	Key key = KeyFactory.createKey(group, type, id);
	Entity entity = entityFromObject(object, key);
	boolean saved = MStore.saveEntity(entity);
	log.info("    [SAVED TO STORE] ? "+ saved+", time:"+Tool.getTimer(true)+", Type:"+type+", ID:"+id);
	return saved;
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> boolean saveAll(List<T> objects) throws RMIException
{
	Tool.setTimer();
	if (Tool.isEmpty(objects)) return true;
	//
	Class<T> typeCls = (Class<T>) objects.get(0).getClass();
	String type = getKeyType(typeCls);
	Key group = KeyFactory.createKey(GROUP_TOKEN, type);
	List<Entity> entities = new ArrayList<Entity>();
	boolean isCacheable = ObjectSh.isCacheable(typeCls);
	MemcacheService cache = isCacheable ? MCache.getCache(type) : null;
	int cch = 0;
	for (T object : objects)
	{
		String id = object.getStorableID();
		if (Tool.isEmpty(id)) throw new RMIException(RMIException.FAIL_CODE, "[saveAll "+type+"]  object: "+Tool.printObject(object)+" has no id.");
		// to MEMCACHE
		if (isCacheable) if (MCache.saveObject(id, object, cache)) cch++;
		// to DATASTORE
		Key key = KeyFactory.createKey(group, type, id);
		Entity entity = entityFromObject(object, key);
		entities.add(entity);
	}
	boolean isok = MStore.saveEntities(entities);
	if (isCacheable && cch != objects.size()) log.warning("    [saveAll]: Not all objects Saved to cache. cch="+cch+"  va objects="+objects.size());
log.info("    [SAVE ALL] ? "+isok+",  time:"+Tool.getTimer(true)+", Type:"+type+", Size:"+entities.size()+", CCH:"+cch);
	return isok;
}

//-------------------------------------------------------------------
public static <T extends ObjectSh> boolean delete(T object) throws RMIException
{
	Tool.setTimer();
	Class<T> typeCls = (Class<T>) object.getClass();
	String type = getKeyType(typeCls);
	String id = object.getStorableID();
	if (Tool.isEmpty(id)) throw new RMIException(RMIException.FAIL_CODE, "[saveAll] object has no id.");
	// delete from CACHE
	if (ObjectSh.isCacheable(typeCls))
	{
		boolean removed = MCache.removeObject(typeCls, id);
log.info("    [REMOVED FROM CACHE] ? "+removed+", time:"+Tool.getTimer(true)+", Type:"+type+", ID:"+id);
	}
	// delete from STORE
	Key group = KeyFactory.createKey(GROUP_TOKEN, type);
	Key key = KeyFactory.createKey(group, type, id);
	boolean removed = MStore.deleteEntity(key);
	log.info("    [REMOVED FROM STORE] ? "+removed+", time:"+Tool.getTimer(true)+", Type:"+type+", ID:"+id);
	return removed;
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> boolean deleteAll(Class<T> typeCls)
{
	Tool.setTimer();
	boolean isok = false;
	String type = getKeyType(typeCls);
	int size = 0, cch = 0;
	try
	{
		Key group = KeyFactory.createKey(GROUP_TOKEN, type);
		Query q = new Query(type, group).setKeysOnly();
		DatastoreService store = MStore.getDatastore(true);
		List<Key> keys = new ArrayList<Key>();
		boolean isCacheable = ObjectSh.isCacheable(typeCls);
		MemcacheService cache = isCacheable ? MCache.getCache(type) : null;
		Iterator<Entity> $e = store.prepare(q).asIterator();
		while ($e.hasNext())
		{
			Key key = $e.next().getKey();
			keys.add(key);
			// from CACHE
			if (isCacheable) if (MCache.removeObject(key.getName(), cache)) cch++;
		}
		size = keys.size();
		if (isCacheable && cch != keys.size()) log.warning("    [deleteAll]: Not all objects Deleted from cache. cch="+cch+"  va objects="+keys.size());
		// from STORE
		MStore.deleteEntities(keys);
		isok = true;
	}
	catch (Throwable ex) {log.severe("[deleteAll]: "+ex.toString());}
log.info("    [DELETE ALL]   ? "+isok+",  time:"+Tool.getTimer(true)+", Type:"+type+", Size:"+size+", CCH:"+cch);
	return isok;
}



// ===================================================
// UTILS API
//-------------------------------------------------------------------
public static <T extends ObjectSh> T objectFromEntity(Class<T> typeCls, Entity entity) {
T object = null;
try {object = typeCls.newInstance();}
catch(Throwable ex) {log.severe("[objectFromEntity]: "+ex.toString()); return null;}
//
for (Column col : ObjectSh.getSchema(typeCls).columns)
{
	try
	{
		Object value = entity.getProperty(col.name);
		if (value == null) continue;
		value = MemoUtils.convertFromEntity(value, col.objField.getType(), value.getClass());
		col.set(object, value);
	}
	catch (Throwable ex) {log.severe(Tool.stackTrace(ex));}//TODO no sever
}
return object;
}

//-------------------------------------------------------------------
public static <T extends ObjectSh> Entity entityFromObject(T object, Key key)
{
	Class<T> typeCls = (Class<T>) object.getClass();
	Entity entity = new Entity(key);
	//
	for (Column col : ObjectSh.getSchema(typeCls).columns)
	{
		try
		{
			Object value = col.get(object);
			value = MemoUtils.convert2Entity(value, col.objField.getType());
			entity.setProperty(col.name, value);
		}
		catch (Throwable ex) {log.severe("[entityFromObject]: "+ex.toString());}
	}
	return entity;
}

// -------------------------------------------------------------------
protected static String getKeyType(Class<?> typeCls)
{
	return typeCls.getSimpleName() + PFX;
}

}
