package cyua.gae.appserver;

import com.google.appengine.api.LifecycleManager;
import com.google.appengine.api.backends.BackendService;
import com.google.appengine.api.backends.BackendServiceFactory;
import cyua.gae.appserver.memo.MCache;
import cyua.gae.appserver.memo.MCache.CacheKeys;
import cyua.java.shared.RMIException;

public class Backend
{
public static final String BACKEND_ID = "bee";
//
static final long BACKEND_DAY_LIMIT = 8*60*60000L;// 8 hours


// -------------------------------------------------------------------
public static void init() throws RMIException
{
}

//-------------------------------------------------------------------
static boolean resetTimeUsed()
{
	return MCache.saveValue(CacheKeys.BACKEND_MS_USED, 0);
}
//-------------------------------------------------------------------
static boolean isOverLimit()
{
boolean isOver = true;
try { isOver = Tool.toLong(MCache.getValue(CacheKeys.BACKEND_MS_USED)) >= Backend.BACKEND_DAY_LIMIT;} catch (Throwable ex) {}
return isOver;
}
// -------------------------------------------------------------------
static void appendTimeUsed(long timeCount)
{
try
{
	long timeUsed = Tool.toLong(MCache.getValue(CacheKeys.BACKEND_MS_USED));
	MCache.saveValue(CacheKeys.BACKEND_MS_USED, timeUsed+timeCount);
}
catch (Throwable ex) {}
}
// -------------------------------------------------------------------
public static String getAddress()
{
	return BackendServiceFactory.getBackendService().getBackendAddress(BACKEND_ID);
}
// -------------------------------------------------------------------
public static boolean isBackendHost(String host)
{
	BackendService bs = BackendServiceFactory.getBackendService();
	try {return bs.getBackendAddress(BACKEND_ID).equals(host);} catch (Exception ex) {}
	return false;
}
// -------------------------------------------------------------------
public static boolean isShuttingDown()
{
	return LifecycleManager.getInstance().isShuttingDown();
}
}
