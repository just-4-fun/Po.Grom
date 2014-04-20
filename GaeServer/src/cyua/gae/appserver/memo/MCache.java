package cyua.gae.appserver.memo;

import java.util.logging.Logger;

import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import cyua.gae.appserver.App;
import cyua.gae.appserver.Tool;
import cyua.java.shared.RMIException;


public class MCache {
private static final Logger log = Logger.getLogger(MCache.class.getName());
//
static final int CACHE_EXPIRE_SPAN = 432000;// sec ~ 5 days
//
public static enum CacheKeys {
	ACC_TOKENS,
	LAST_VERSION,
	APP_READY,
	DAY_LAST_TIC,
	LAST_FTDB_INIT,
	BACKEND_LAST_CALL, BACKEND_MS_USED, CFG_LAST_CHK, CFG_TYPES,
}

// ===================================================
//-------------------------------------------------------------------
public static <T> T getValue(CacheKeys pfxKey, String id) throws RMIException {
// WARNING Accept only Object result for primitive types
	return doGetValue(pfxKey + "" + id);
}
public static <T> T getValue(CacheKeys key) throws RMIException {
// WARNING Accept only Object result for primitive types
	return doGetValue(key.toString());
}
private static <T> T doGetValue(String key) throws RMIException {
// WARNING Accept only Object result for primitive types
	try {
		if (!App.isServiceAvailable(Capability.MEMCACHE))
			throw new RMIException(RMIException.RETRY_CODE, "Cache service is unavailable.");
		MemcacheService mc = MemcacheServiceFactory.getMemcacheService();
		Object val = mc.get(key + Memo.PFX);
		return (T) val;
	} catch (Throwable ex) {
		log.warning(ex.toString());
		throw new RMIException(ex);
	}
}

//-------------------------------------------------------------------
public static boolean saveValue(CacheKeys pfxKey, String id, Object object) {
	return doSaveValue(pfxKey + "" + id, object, CACHE_EXPIRE_SPAN);
}
public static boolean saveValue(CacheKeys pfxKey, String id, Object object, int exprireSecs) {
	return doSaveValue(pfxKey + "" + id, object, exprireSecs);
}
public static boolean saveValue(CacheKeys key, Object object) {
	return doSaveValue(key.toString(), object, CACHE_EXPIRE_SPAN);
}
public static boolean saveValue(CacheKeys key, Object object, int exprireSecs) {
	return doSaveValue(key.toString(), object, exprireSecs);
}
static boolean doSaveValue(String key, Object object, int exprireSecs) {
	if (!App.isServiceAvailable(Capability.MEMCACHE)) return false;
	MemcacheService mc = MemcacheServiceFactory.getMemcacheService();
	try {
		mc.put(key + Memo.PFX, object, Expiration.byDeltaSeconds(exprireSecs));
	} catch (Exception ex) {log.severe("[saveValue]: " + ex.toString()); return false;}
	return true;
}


// ===================================================
//-------------------------------------------------------------------
static MemcacheService getCache(String keyType) {
	if (!App.isServiceAvailable(Capability.MEMCACHE)) return null;
	MemcacheService mc = MemcacheServiceFactory.getMemcacheService(keyType);
	return mc;
}

//-------------------------------------------------------------------
private static AsyncMemcacheService getAsyncCache(String keyType) {
	if (!App.isServiceAvailable(Capability.MEMCACHE)) return null;
	AsyncMemcacheService mc = MemcacheServiceFactory.getAsyncMemcacheService(keyType);
	return mc;
}

//-------------------------------------------------------------------
public static <T> T getObject(Class<T> typeCls, String id) throws RMIException {
	try {
		MemcacheService cache = getCache(Memo.getKeyType(typeCls));
		if (cache == null) throw new RMIException("Memcache is NOT available for Type:" + Memo.getKeyType(typeCls));
		return (T) cache.get(id);
	} catch (Throwable ex) {
		log.severe("[getObjet]: " + ex.toString());
		throw new RMIException(ex);
	}// IllegalArgumentException|InvalidValueException|MemcacheServiceException
}

//-------------------------------------------------------------------
public static boolean saveObject(String id, Object object) {
	MemcacheService cache = getCache(Memo.getKeyType(object.getClass()));
	return saveObject(id, object, cache, CACHE_EXPIRE_SPAN);
}
public static boolean saveObject(String id, Object object, int exprireSecs) {
	MemcacheService cache = getCache(Memo.getKeyType(object.getClass()));
	return saveObject(id, object, cache, exprireSecs);
}
public static boolean saveObject(String id, Object object, MemcacheService cache) {
	return saveObject(id, object, cache, CACHE_EXPIRE_SPAN);
}

//-------------------------------------------------------------------
static boolean saveObject(String id, Object object, MemcacheService cache, int exprireSecs) {
	try {
		if (cache == null)
			throw new RMIException("Memcache is NOT available for Type:" + Memo.getKeyType(object.getClass()));
		cache.put(id, object, Expiration.byDeltaSeconds(exprireSecs));// 5 days
		return true;
	} catch (Throwable ex)// IllegalArgumentException|InvalidValueException|MemcacheServiceException
	{
		// TODO inconsistent state
		log.severe("[saveObject]: " + ex.toString());
	}
	return false;
}

//-------------------------------------------------------------------
public static boolean removeObject(Class<?> typeCls, String id) {
	MemcacheService cache = getCache(Memo.getKeyType(typeCls));
	return removeObject(id, cache);
}

//-------------------------------------------------------------------
static boolean removeObject(String id, MemcacheService cache) {
	try {
		if (cache == null) throw new RMIException("Cache is null.");
		cache.delete(id);
		return true;
	} catch (Throwable ex)// TODO no sever IllegalArgumentException|InvalidValueException|MemcacheServiceException
	{
		// TODO inconsistent state
		log.severe("[removeObject]: " + ex.toString());
	}
	return false;
}

}
