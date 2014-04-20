package cyua.android.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.WeakHashMap;

import cyua.android.core.CacheVar.BooleanVar;
import cyua.android.core.CacheVar.IntVar;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.log.LogService;
import cyua.android.core.log.Wow;
import cyua.java.shared.BitState;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Timer;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.FloatInfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import static java.lang.Thread.UncaughtExceptionHandler;
import static cyua.android.core.CacheVar.StringVar;

// NOTE To use KeepAliveSrvice it should be added to MANIFEST as Service
// NOTE To use any onConnected call it's [use] method in static initializer
// NOTE  Do not extend static methods. Do it through instance impementation if needed

public abstract class AppCore extends Application
//WARNING compatibility donwngrade: for API > 10 uncomment
// implements ActivityLifecycleCallbacks
{
private static final String TAG = AppCore.class.getSimpleName();
protected static final String THREAD_NAME = "Main_Thread";
// Setup config
public static enum ExecType {REAL, TEST}
public static ExecType execType;
// exec environment connecting to
public static enum RMIType {REMOTE, LOCAL, LOCAL_EMU}// if LOCAL see localIP
public static RMIType rmiType;


private enum Op {CHECK_INIT, CHECK_EXIT, CHECK_MAYEXIT;}
;
protected static int INIT_WAIT_SECS = 10;
protected static int EXIT_WAIT_SECS = 120;
//
//WARNING compatibility donwngrade: do not use  ACTIVITY_RECONFIGURING
public static final String CAUSE_KEY = "cause_key";
public static final String ACTIVITY_CREATED = "created";
public static final String ACTIVITY_CAMEBACK = "cameback";
public static final String ACTIVITY_REPLACING = "replacing";
public static final String ACTIVITY_RECONF_DONE = "reconf_done";
public static final String ACTIVITY_RECONF_START = "reconf_start";
public static final String ACTIVITY_MAYBACK = "mayback";
public static final String ACTIVITY_GONE = "gone";
public static final String ACTIVITY_DEAD = "dead";// activity started while app exiting
//
// SINGLETONE instance
protected static AppCore I;
//
public static String packageRoot = "cyua";
public static boolean D = BuildConfig.DEBUG;
public static String name;
public static int version, apiVersion;
public static String deviceInfo;
public static StringVar deviceUidVar;
public static long launchTime = Tool.deviceNow();
//
// FIXME may throw ConcurrntModification > implement as Listeners class
protected static List<AppService> services = new ArrayList<AppService>();




static {
	Thread.currentThread().setName(THREAD_NAME);
}



/** PUBLIC STATIC Members */

public static boolean isMainThread() {
	return THREAD_NAME.equals(Thread.currentThread().getName());
}




public static void exit() {
	if (!isExitStarted()) I.startExit();
}


public static boolean isInitStarted() {
	return I.state.has(Bit.INIT);
}

public static boolean isInitFinished() {
	return I.state.has(Bit.INITED_$);
}
public static boolean isInitFailed() {
	return I.state.has(Bit.FAILED);
}
public static boolean isExitStarted() {
	return I.state.has(Bit.EXIT);
}
public static boolean isExitFinished() {
	return I.state.has(Bit.EXITED_$);
}
public static boolean isRecycled() {
	return I.state.has(Bit.RECYCLED);
}
public static boolean isUiStarted() {
	return I.uiState.has(ABit.STARTED);
}
public static boolean isUiDestroyed() {
	return I.uiState.has(ABit.DESTROYED, ABit.DEAD);
}
public static boolean isReconfiguring() {
	return ACTIVITY_RECONF_START.equals(I.cause);
}

public static boolean isLastCrashed() {
	return I.isLastCrashed;
}



@SuppressWarnings("unchecked")
public static <T extends AppCore> T context() {
	return (T) I;
}

@SuppressWarnings("unchecked")
public static <T extends ActivityCore> T uiContext() {
	return (T) I.activity.get();
}

public static void addService(AppService service, boolean autoRemoveOnExit) {
	if (D) Wow.i(TAG, "addService", "" + service);
	service.autoRemove = autoRemoveOnExit;
	if (!services.contains(service)) services.add(service);
}


public static void removeService(AppService service) {
	if (D) Wow.i(TAG, "removeService", service + "");
	services.remove(service);
}
public static void increaseTimeoutSecs(int addSecs) {
	if (isExitStarted()) I.exitSecsLast += addSecs;
	else I.initSecsLast += addSecs;
}

public static String getAppState() {
	StringBuilder info = new StringBuilder();
	String life = Tool.ms2hms(Tool.deviceNow() - AppCore.launchTime);
	info.append("APP life = ").append(life).append("; ")
			.append("State = ").append(I.state).append("; ")
			.append("Activity = ").append(I.uiState).append("; ")
			.append("Cause = ").append(I.getCause(I.activity.get()));
	for (AppService service : services) {
		info.append(";\n").append(service.getClass().getSimpleName()).append(" :: ");
		String state = null;
		try {state = service.getStateInfo();} catch (Throwable ex) { state = ex.getClass().getName()+"; "+ex.toString(); }
		if (state != null) info.append(state);
	}
	return info.toString();
}

/** Instatnce Members */

// App level
//protected boolean initStarted, initFinished, exitStarted, exitFinished;

protected AppState state = new AppState();
protected BitState uiState = new BitState();
protected int cycles;
protected String cause;
protected Tiker<Op> tiker;
protected int initSecsLast = INIT_WAIT_SECS;
protected int exitSecsLast = EXIT_WAIT_SECS;
protected String initErrorReport;
// Activity level
protected WeakHashMap<Activity, Boolean> activities = new WeakHashMap<Activity, Boolean>();
private WeakReference<ActivityCore> activity = new WeakReference<ActivityCore>(null);
private long lastReconfig, lastStop, lastDestroy;
private boolean killOnExit;
//
private IntVar lastVersionVar;
private BooleanVar crashedVar;// to test if app was crashedVar (true) of finished (false)
private boolean isLastCrashed;
private boolean backPressed;




// NOTE is called on any of Components (Activity, Service, Reciever) is created
// so do NOT init App here
@Override public void onCreate() {
	super.onCreate();
	//
	I = this;
	state.set(Bit.PREINIT);
	//
	UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
	UncaughtExceptionHandler newDefaultHandler = new DefaultExceptionHandlerCore(defaultHandler);
	UncaughtExceptionHandler mainHandler = new UncaughtExceptionHandlerCore(defaultHandler);
	//
	Thread.setDefaultUncaughtExceptionHandler(newDefaultHandler);
	Thread.currentThread().setUncaughtExceptionHandler(mainHandler);
	//
	// extract Application Info
	try {
		CacheVar.init();// init default SharedPrefs
		CacheVar.initVars(this, null, null);
		isLastCrashed = crashedVar.get();
		crashedVar.set(true);
		PackageManager pm = getPackageManager();
		ApplicationInfo appinfo = pm.getApplicationInfo(getPackageName(), 0);
		PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
		version = info.versionCode;
		name = getString(appinfo.labelRes);
		apiVersion = getApiVersion();
		deviceInfo = Build.MANUFACTURER + ", " + Build.MODEL + ", " + Build.PRODUCT;
		if (deviceUidVar.isEmpty()) deviceUidVar.set(Tool.randomString(8));
		// check debug mode
		boolean dbg = (appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		if (dbg != D) Wow.e(null, "DEBUG mode mismatch", "DEBUG = " + D, "real debug = " + dbg);
		D = dbg;
	} catch (Exception ex) {
		Wow.e(ex);
		Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
	}
	if (D)
		Wow.i(TAG, "createRoot", "DEBUG ? " + D + ",  pid=" + Process.myPid() + ",  crashed ? " + isLastCrashed + ",   VERSION=" + version + ",  NAME=" + name + ",  API=" + apiVersion + ",  Device=" + deviceInfo);
	//
	// assign HANDLER
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case CHECK_INIT: checkInit(); break;
				case CHECK_EXIT: checkExit(); break;
				case CHECK_MAYEXIT: checkMayExit(); break;
			}
		}
	};
	//
//	Memo.init();
//	Tool.setTimeZone();
	// WARNING compatibility donwngrade: for API > 10 uncomment
//	registerActivityLifecycleCallbacks(this);
	registerServices();
	//
	preInit();
	state.set(Bit.PREINIT_$);
}
//private void assignInstance()
//{
//	Class<?> superCls = getClass();
//	while (superCls != null && AppBase.class.isAssignableFrom(superCls))
//	{
//		try { superCls.getDeclaredField("I").set(null, this); }catch (Throwable ex) {Wow.e(TAG, BaseTool.printObject(ex));}
//		superCls = superCls.getSuperclass();
//	}
//}

/** Subclass Override this method to add services */
public abstract void registerServices();





/** INIT */

protected void preInit() {
	sortServicesByInitOrder();
	//
	for (AppService service : services) {
		try {service.onPreInit(I);} catch (Throwable ex) {Wow.e(ex);}
	}
}
protected void startInit() {
	if (state.has(Bit.INIT)) return;
	// else
	state.clear(Bit.RECYCLED);
	state.set(Bit.INIT);
	crashedVar.set(true);
	//
	sortServicesByInitOrder();// reorder again in restarting
	for (AppService service : services) {
		try {service.onInitStart(I);} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	tiker.setTik(Op.CHECK_INIT, 500);
	state.set(Bit.INIT_$);
}
protected void checkInit() {
	state.set(Bit.INITING);
	boolean notRready = false;
	//
	for (AppService service : services) {
		try {notRready |= !service.isInited(I);} catch (Throwable ex) { Wow.e(ex); }
	}
	//
	state.set(true, Bit.INITING_$);
	if (notRready && initSecsLast-- > 0) tiker.setTik(Op.CHECK_INIT, 100);
	else finishInit();
}
protected void finishInit() {
	state.set(Bit.INITED);
	StringBuilder errReport = new StringBuilder();
	for (AppService service : services) {
		try {
			String err = service.onInitFinish(I);
			if (err != null) errReport.append(err).append('\n');
		} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	state.set(Bit.INITED_$);
	initErrorReport = errReport.toString();
	// TODO Stop app may be And show report
	if (Tool.notEmpty(initErrorReport)) {
		Wow.e(null, "initErrorReport = " + initErrorReport);
		state.set(Bit.FAILED);
		//
		for (AppService service : services) {
			try {
				service.onInitFailed(I);
			} catch (Throwable ex) {Wow.e(ex);}
		}
	}
}

/** EXIT */

protected void startExit() {
	state.set(Bit.EXIT);
	tiker.cancelTik(Op.CHECK_INIT);// WARNING Exit can be started before init finished
	killOnExit = KeepAliveService.isKeepAlive();
	//
	for (Activity _activity : activities.keySet()) {
		if (!_activity.isFinishing())
			try {_activity.finish();} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	sortServicesByExitOrder();
	//
	for (AppService service : services) {
		try {service.onExitStart(I);} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	tiker.setTik(Op.CHECK_EXIT, 1000);
	state.set(Bit.EXIT_$);
}
protected void checkExit() {
	state.set(Bit.EXITING);
	boolean notRready = false;
	//
	for (AppService service : services) {
		try {notRready |= !service.isExited(I);} catch (Throwable ex) { Wow.e(ex); }
	}
	//
	state.set(true, Bit.EXITING_$);
	if ((notRready || !activities.isEmpty()) && exitSecsLast-- > 0) tiker.setTik(Op.CHECK_EXIT, 500);
	else finishExit();
}
protected void finishExit() {
	state.set(Bit.EXITED);
	for (AppService service : services) {
		try {service.onExitFinish(I);} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	crashedVar.set(false);
	try {AppHelper.clearSingletons();} catch (Throwable ex) {}
	try {Wow.writeCache();} catch (Throwable ex) {}
	state.set(Bit.EXITED_$);
	recycle();
	if (killOnExit) kill();
}
protected void recycle() {
	ListIterator<AppService> iterator = services.listIterator();
	while (iterator.hasNext()) {
		AppService service = iterator.next();
		if (service.autoRemove) iterator.remove();
	}
	//
	activity = new WeakReference<ActivityCore>(null);
	activities.clear();
	cause = null;
	//
	ActivityCore.cleanup();
	//
	tiker.clear();
	initSecsLast = INIT_WAIT_SECS;
	exitSecsLast = EXIT_WAIT_SECS;
	backPressed = false;
	initErrorReport = "";
	state.setOnly(Bit.RECYCLED, Bit.PREINIT, Bit.PREINIT_$);
}
private void kill() {
	// NOTE PERMISSION required android.permission.KILL_BACKGROUND_PROCESSES
	try {
		ActivityManager actvityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		actvityManager.killBackgroundProcesses(getPackageName());
	} finally {
		Process.killProcess(Process.myPid());
		System.exit(10);
	}
}

public void checkMayExit() {
	if (!state.has(Bit.EXIT) && !KeepAliveService.isKeepAlive() && activity.get() == null) {
		if (ACTIVITY_GONE.equals(cause)) exit();
		else if (ACTIVITY_MAYBACK.equals(cause)) {
			// avoid starange case when hiden activity is destroyed and created again right away
			long dnow = Tool.deviceNow();
			boolean enouhgWait = dnow - lastStop > 5000 && dnow - lastDestroy > 5000;
			if (enouhgWait) exit();
			else tiker.setTik(Op.CHECK_MAYEXIT, 1000);
		}
	}
}






/** ACTIVITY LIFE CYCLE CALLBACKS */

//@Override 
public void onActivityCreated(ActivityCore _activity, Bundle savedState) {
	activityPhase(ABit.CREATED, _activity, isExitStarted() ? "EXITING !!!" : null);
	tiker.cancelTik(Op.CHECK_MAYEXIT);
	if (state.has(Bit.EXIT)) {
		FloatInfo.show(R.string.warn_appexiting, FloatInfo.LONG);
		setCause(_activity, ACTIVITY_DEAD);
		_activity.finish();
		return;
	}
	//
	cause = activity.get() != null ? ACTIVITY_REPLACING : Tool.deviceNow() - lastReconfig < 5000 || ACTIVITY_RECONF_START.equals(cause) ? ACTIVITY_RECONF_DONE : ACTIVITY_CREATED;
	setCause(_activity, cause);
	// 
	activity = new WeakReference<ActivityCore>(_activity);
	activities.put(_activity, false);
	//
	startInit();
	//
	for (AppService service : services) {
		try {service.onActivityCreated(_activity, cause, savedState);} catch (Throwable ex) { Wow.e(ex); }
	}
	printCause();
}
//@Override 
public void onActivityStarted(ActivityCore _activity) {
	activityPhase(ABit.STARTED, _activity, null);
	if (state.has(Bit.EXIT)) {_activity.finish(); return;}
	//
	cause = getCause(_activity);
	if (ACTIVITY_GONE.equals(cause) || ACTIVITY_MAYBACK.equals(cause)) cause = ACTIVITY_CAMEBACK;
	else if (cause == null)
		cause = activity.get() != null && activity.get() != _activity && Tool.deviceNow() - lastStop < 5000 ? ACTIVITY_REPLACING : ACTIVITY_CAMEBACK;
	setCause(_activity, cause);
	//
	activity = new WeakReference<ActivityCore>(_activity);
	if (!activities.containsKey(_activity)) activities.put(_activity, false);
	//
	for (AppService service : services) {
		try {service.onActivityStarted(_activity, cause);} catch (Throwable ex) { Wow.e(ex);
		}
	}
	printCause();
}
//@Override 
public void onActivityResumed(ActivityCore _activity) {
	activityPhase(ABit.RESUMED, _activity, null);
	if (state.has(Bit.EXIT)) {_activity.finish(); return;}
	//
	activity = new WeakReference<ActivityCore>(_activity);
	//
	for (AppService service : services) {
		try {service.onActivityResumed(_activity, cause);} catch (Throwable ex) { Wow.e(ex); }
	}
	printCause();
	// RESET CAUSE
	setCause(_activity, null);
	lastReconfig = 0;
}
@Override public void onConfigurationChanged(Configuration newConfig) {
	activityPhase(ABit.RECONFIG, activity.get(), null);
	lastReconfig = Tool.deviceNow();
	super.onConfigurationChanged(newConfig);
}
//@Override 
public void onActivitySaveInstanceState(ActivityCore _activity, Bundle outState) {
	activityPhase(ABit.SAVED, _activity, null);
	// WARNING due to downgrade the isChangingConfigurations is not available so the only way is to emulate Reconfiguring here
	// REMOVE this line upon drop to support pre 11 API
	// TODO weak resultCode
	cause = Tool.deviceNow() - lastReconfig < 500 ? ACTIVITY_RECONF_START : ACTIVITY_MAYBACK;
	setCause(_activity, cause);
	//
	if (state.has(Bit.EXIT)) return;
	for (AppService service : services) {
		try {service.onSaveState(_activity, outState);} catch (Throwable ex) { Wow.e(ex);
		}
	}
	printCause();
}
//@Override 
public void onActivityPaused(ActivityCore _activity) {
	activityPhase(ABit.PAUSED, _activity, null);
	// WARNING due to downgrade the isChangingConfigurations is not available so the only way is to emulate Reconfiguring here
//	lastRefresh = _activity.isChangingConfigurations() ? ToolCore.now() : 0;
//	String cause = lastRefresh > 0 ? ACTIVITY_RECONFIGURING : null;
//	setCause(_activity, cause);
	cause = getCause(_activity);
	if (cause == null && _activity == activity.get()) cause = ACTIVITY_GONE;
	setCause(_activity, cause);
	//
	for (AppService service : services) {
		try {service.onActivityPaused(_activity, cause);} catch (Throwable ex) { Wow.e(ex); }
	}
	printCause();
}
//@Override 
public void onActivityStopped(ActivityCore _activity) {
	activityPhase(ABit.STOPPED, _activity, null);
	lastStop = Tool.deviceNow();
	cause = getCause(_activity);
	if (cause == null && ACTIVITY_REPLACING.equals(getCause(activity.get()))) cause = ACTIVITY_REPLACING;
	setCause(_activity, cause);
	//
	for (AppService service : services) {
		try {service.onActivityStopped(_activity, cause);} catch (Throwable ex) { Wow.e(ex); }
	}
	printCause();
	//
	if (isInitFailed()) exit();
	else if (!KeepAliveService.isKeepAlive()) {
		boolean exit = ACTIVITY_GONE.equals(cause) && backPressed;
		if (exit && !_activity.isFinishing()) _activity.finish();
	}
	backPressed = false;
}
//@Override 
public void onActivityDestroyed(ActivityCore _activity) {
	boolean isDead = ACTIVITY_DEAD.equals(getCause(_activity)) && _activity != activity.get();
	activityPhase(isDead ? ABit.DEAD : ABit.DESTROYED, _activity, state.has(Bit.EXITED_$) ? "DOUBLE DESTROY !!!" : null);
	if (state.has(Bit.EXITED_$) || isDead) return;// by ? there sometimes is double destroy
	lastDestroy = Tool.deviceNow();
	//
	cause = getCause(_activity);
	if (cause == null && activity.get() == _activity) cause = ACTIVITY_GONE;
	setCause(_activity, cause);
	printCause();
	//
	activities.remove(_activity);
//	if (_activity == activity.get()) activity = new WeakReference<ActivityCore>(null);
	//
	for (AppService service : services) {
		try {service.onActivityDestroyed(_activity, cause);} catch (Throwable ex) { Wow.e(ex); }
	}
}
void onActivityDestroyFinished(ActivityCore _activity) {
	// due to glich in API 2.3 (FragmentActivity try to call Ui after destroy)
	if (_activity == activity.get()) activity = new WeakReference<ActivityCore>(null);
	// exit
	checkMayExit();
}

public String getCause(Activity _activity) {
	if (_activity == null) return null;
	return _activity.getIntent().getStringExtra(CAUSE_KEY);
}
public void setCause(ActivityCore _activity, String cause) {
	_activity.getIntent().putExtra(CAUSE_KEY, cause);
}




/** MISC */

public void onCloseByBackPressed() {
	backPressed = true;
}

private void sortServicesByInitOrder() {
	Collections.sort(services, new Comparator<AppService>() {
		@Override public int compare(AppService cur, AppService nxt) {
			if (cur.initOrder == nxt.initOrder) return 0;
			else if (cur.initOrder > nxt.initOrder) return 1;
			return -1;
		}
	});
}
private void sortServicesByExitOrder() {
	Collections.sort(services, new Comparator<AppService>() {
		@Override public int compare(AppService cur, AppService nxt) {
			if (cur.exitOrder == nxt.exitOrder) return 0;
			else if (cur.exitOrder > nxt.exitOrder) return 1;
			return -1;
		}
	});
}

/* for case of modified build */
private int getApiVersion() {
	int v = Build.VERSION.SDK_INT;
	if (v == 0) {
		String s = Build.VERSION.RELEASE;
		if (s.startsWith("1.")) v = 7;
		else if (s.startsWith("2.3.")) v = 10;
		else if (s.startsWith("2.2")) v = 8;
		else if (s.startsWith("2.")) v = 7;
		else if (s.startsWith("3.")) v = 11;
		else if (s.startsWith("4.0")) v = 14;
		else if (s.startsWith("4.1")) v = 16;
		else if (s.startsWith("4.2")) v = 17;
	}
	return v;
}








/** LIFECYCLE LISTENER */

public abstract static class AppService {
	public static final int INIT_FIRST = 1, INIT_MID = 5, INIT_LAST = 10;
	public static final int EXIT_FIRST = 1, EXIT_MID = 5, EXIT_LAST = 10;
	//
	public static boolean isInitialized() {return true;}

	//
	protected int initOrder = INIT_MID, exitOrder = EXIT_MID;
	protected String initError;
	protected boolean autoRemove;
	//
// App Lifecycle
	public void onPreInit(AppCore app) throws Throwable {}
	public void onInitStart(AppCore app) throws Throwable {}
	public boolean isInited(AppCore app) throws Throwable {return true;}
	public String onInitFinish(AppCore app) throws Throwable {return initError;}
	public void onInitFailed(AppCore app) throws Throwable {}
	public void onExitStart(AppCore app) throws Throwable {}
	public boolean isExited(AppCore app) throws Throwable {return true;}
	public void onExitFinish(AppCore app) throws Throwable {}
	//Activity Lifecycle
	public void onActivityCreated(Activity activity, String cause, Bundle state) {}
	public void onActivityStarted(Activity activity, String cause) {}
	public void onActivityResumed(Activity activity, String cause) {}
	public void onActivityPaused(Activity activity, String cause) {}
	public void onSaveState(Activity activity, Bundle state) {}
	public void onActivityStopped(Activity activity, String cause) {}
	public void onActivityDestroyed(Activity activity, String cause) {}
	//
	public String getStateInfo() throws Throwable {return null;}

}





/** BIT STATE */

protected long timePoint;

protected enum Bit {
	PREINIT, PREINIT_$,
	INIT, INIT_$, INITING, INITING_$, INITED, INITED_$,
	EXIT, EXIT_$, EXITING, EXITING_$, EXITED, EXITED_$,
	RECYCLED, FAILED
}

protected enum ABit {
	CREATED, STARTED, RESUMED, PAUSED, SAVED, STOPPED, RECONFIG, DESTROYED, DEAD
}

private void activityPhase(ABit bit, Activity _activity, String info) {
	if (activity.get() == _activity || activity.get() == null) {
		if (bit == ABit.CREATED) uiState.clear();
		uiState.set(bit);
	}
	if (D)
		Wow.d(TAG, "activityPhase", "ACTIVITY  is Current ? [" + (activity.get() == _activity) + "];  PHASE = [" + bit + "];  " + (info == null ? "" : info));
}
private void printCause() {
	if (D)
		Wow.d(TAG, "printCause", "ACTIVITY  STATE = [" + uiState + "];  CAUSE = [" + getCause(activity.get()) + "];  ");
}

protected class AppState extends BitState {
	public void set(boolean cycled, Enum... position) {
		super.set(position);
		//
		String info = "";
		if (cycled) {
			cycles++;
			info = "Cycles = " + cycles + ";  ";
		}
		if (D)
			Wow.d(TAG, "set", info + "step(ms) = [" + stepTime() + "];  STATE = [" + state + "];  BIT = [" + position[0] + "]");
	}
	@Override public void set(Enum... position) {
		set(false, position);
	}
	@Override public void setOnly(Enum... position) {
		super.setOnly(position);
		if (D)
			Wow.d(TAG, "setOnly", "step(ms) = [" + stepTime() + "];  STATE = [" + state + "];  BIT = [" + position[0] + "]");
		Tool.setTimer();
	}
	private long stepTime() {
		long now = SystemClock.elapsedRealtime();
		long time = now - timePoint;
		timePoint = now;
		return time;
	}
}








/** UNCAUGHT EXCEPTION HANDLER */

private class UncaughtExceptionHandlerCore implements UncaughtExceptionHandler {
	UncaughtExceptionHandler defaultHandler;
	private UncaughtExceptionHandlerCore(UncaughtExceptionHandler defaultHandler) {
		this.defaultHandler = defaultHandler;
	}
	@Override public void uncaughtException(Thread thread, Throwable ex) {
		Wow.e(ex, "FATAL uncaught Exception");
		try {exit();} catch (Throwable exx) {}
		try {checkExit();} catch (Throwable exx) {}
		killOnExit = false;
		try {finishExit();} catch (Throwable exx) {}
		// let Report error be sent
		Timer t = new Timer().setExpire(5000);
		while (LogService.isBussy() && !t.isExpired()) try {
			Thread.sleep(100);
		} catch (Throwable exx) {}
		//
		try {showOops();} catch (Exception exx) {}
		if (D) Wow.d(TAG, "uncaughtException", "BYE...");
		if (defaultHandler != null) defaultHandler.uncaughtException(thread, ex);
	}
	private void showOops() {
		new Thread() {
			@Override public void run() {
				Looper.prepare();
				Toast.makeText(AppCore.this, R.string.dialog_crash_title, Toast.LENGTH_LONG).show();
				Looper.loop();
			}
		}.start();
	}
}



private class DefaultExceptionHandlerCore implements UncaughtExceptionHandler {
	UncaughtExceptionHandler defaultHandler;
	private DefaultExceptionHandlerCore(UncaughtExceptionHandler defaultHandler) {
		this.defaultHandler = defaultHandler;
	}
	@Override public void uncaughtException(Thread thread, Throwable ex) {
		Wow.e(ex.getCause(), "Uncaught");
		if (defaultHandler != null) defaultHandler.uncaughtException(thread, ex);
	}
}

}


/* ACTIVITY CALLBACK SEQUENCES

ON LAUNCH (fiirst time)
{
	App.createRoot
	ON RELAUNCH (from Home)
	{
		Activity.createRoot
		Activity.onContentChange
		Activity.onStart
		Activity.onPostCreate
		Activity.onResume
		Service.createRoot
		Activity.onPostResume
		Service.onStart
		Activity.onAttach
	}
}
LAUNCHED > Press Home / Launch another from Context / Device Off
{
	Activity.onPause
	Activity.onSaveInstance
	Activity.onStop
}
SOPPED > Launch from Home / Launch from Context / Device On
{
	Activity.onRestart
	Activity.onStart
	Activity.onResume
	Activity.onPostResume
}
STARTED > Back
{
	Activity.onPause
	Activity.onStop
	Activity.onDestroy
	Activity.onDetach
}
STARTED > Rotate (change configuration)
{
	Service.onConfigurationChange
	App.onConfigurationChange
	Activity.onPause
	Activity..onSaveInstance
	Activity.onStop
	Activity.onDestroy
	Activity.onDetach
	______________
	Activity.createRoot
	Activity.onContentChange
	Activity.onStart
	onRestoreInstance
	onPostCreate
	Activity.onResume
	Activity.onPostResume
	Activity.onAttach
}

*/
