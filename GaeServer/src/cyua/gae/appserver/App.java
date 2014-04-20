package cyua.gae.appserver;

import java.util.Calendar;
import java.util.logging.Logger;

import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.capabilities.CapabilityStatus;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;
import cyua.gae.appserver.TaskServlet.Queues;
import cyua.gae.appserver.TaskServlet.Tasks;
import cyua.gae.appserver.fusion.FTDB;
import cyua.gae.appserver.memo.MCache;
import cyua.gae.appserver.memo.MCache.CacheKeys;
import cyua.gae.appserver.urlfetch.Account;

import static cyua.java.shared.Phantom.Gae;


public class App
{
private static final Logger log = Logger.getLogger(App.class.getName());
//
public static final String REAL_NAME = "po-grom";
//
public static final String appName = SystemProperty.applicationId.get();
public static final boolean isProduction = SystemProperty.environment.value() == SystemProperty.Environment.Value.Production;
public static Long subVersion;// on production Server it means deployment timestamp. assigned in getAppVersion
public static String appVersion;
public static boolean isRealVersion;
public static boolean isBackend;
public static int instanceId;
//browser key cy.entry po-grom
public static final String apiKey = Gae.APP_BROWSER_APIKEY;
//server api key cy.entry po-grom : LIMITED 2500 requests per 24 hours
public static final String serverApiKey = Gae.APP_SERVER_APIKEY;
//
private static boolean ready;


// -------------------------------------------------------------------
public static void preInit(int _instanceId)
{
	if (_instanceId >= 0)// from MainServlet.init or retry from tic
	{
		// set UA timezone as default
		Tool.setTimeZone();
		//
		String v = SystemProperty.applicationVersion.get();
		// first check if instance is not BACKEND instance
		isBackend = !Character.isDigit(v.charAt(0));
		// else FRONTEND
		instanceId = _instanceId;
		int ix = v.indexOf('.');
		if (ix >= 0)
		{
			subVersion = Tool.toLong(v.substring(ix+1, v.length()));
	//		if (subVersion == 1) isLocal = true;
			v = v.substring(0, ix);
		}
		appVersion = v;// App version or Backend name
		//
		if (appName.equals(REAL_NAME)) isRealVersion = true;
		//
		log.info("    [APP PRE-INIT] isFrontend:"+!isBackend+",  REAL ? "+isRealVersion+",  production:"+isProduction+",  name:"+appName+",  version:"+appVersion+",  startTime:"+subVersion+",  instId:"+instanceId+",  now:"+Tool.nowDateTimeStr());
		//
		Account.init();
	}
	//
	if (isBackend)
	{
		ready = true;
		try {Backend.init();} catch (Throwable ex) {ready = false; log.severe("[BACKEND INIT Failed. ]"+Tool.stackTrace(ex));}
		return;
	}
	// INIT if new Version only
	try
	{
		String lastVersion = MCache.getValue(CacheKeys.LAST_VERSION);
		if (appVersion.equals(lastVersion))
		{
			MCache.saveValue(CacheKeys.APP_READY, true);
			ready = true;
			return;
		}
		// else
		MCache.saveValue(CacheKeys.APP_READY, false);
		if (MCache.saveValue(CacheKeys.LAST_VERSION, appVersion))
		{
			if (!TaskServlet.addTask(Queues.DEFAULT, Tasks.INIT, null, 0, 0, null, false))
				MCache.saveValue(CacheKeys.LAST_VERSION, lastVersion);
		}
	}
	catch (Throwable ex) {log.severe(Tool.stackTrace(ex));}
}
/*WARNING add Task and execute Task may be done by different Instatnces
 * so static vars may not be synced as are not from same instance.*/
// -------------------------------------------------------------------
public static void init() throws Throwable
{
	log.info("    [APP INIT...]");
	try
	{
		FTDB.init();
		//	TaskServlet.purgeQueue(Queues.DEFAULT);
		TaskServlet.purgeQueue(Queues.CYCLIC);
		MCache.saveValue(CacheKeys.APP_READY, true);
	}
	catch (Throwable ex)
	{
		MCache.saveValue(CacheKeys.LAST_VERSION, "0");
		throw ex;
	}
}
//-------------------------------------------------------------------
public static boolean isReady()
{
	if (!ready)
	{
		try {ready = Tool.toBoolean(MCache.getValue(CacheKeys.APP_READY));}
		catch (Throwable ex) {return true;}
	}
	return ready;
}

// -------------------------------------------------------------------
synchronized public static long msBeforeDie()
{
return ApiProxy.getCurrentEnvironment().getRemainingMillis();
}
// -------------------------------------------------------------------
public static boolean isServiceAvailable(Capability service)
{
CapabilitiesService cs = CapabilitiesServiceFactory.getCapabilitiesService();
CapabilityStatus status = cs.getStatus(service).getStatus();
return status != CapabilityStatus.DISABLED;
}
// -------------------------------------------------------------------
public static boolean isStoreAvailable()
{
	return isServiceAvailable(Capability.DATASTORE_WRITE);
}
public static boolean isCacheAvailable()
{
	return isServiceAvailable(Capability.MEMCACHE);
}
public static boolean isDegraded()
{
	return !isStoreAvailable() && !isCacheAvailable();
}
// -------------------------------------------------------------------
public static String getApiKey()
{
	int hour = Tool.nowDate().get(Calendar.HOUR_OF_DAY);
	String key = apiKey;
//	if (hour < 8) key = testMonKey;
//	else if (hour < 16) key = rootKey;
//	else key = backupKey;
	return key;
//	return Tool.nowDate().get(Calendar.HOUR_OF_DAY) < 12 ? backupKey : rootKey;
}

}
